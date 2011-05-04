#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <signal.h>
#include <fcntl.h>

#include "w600_gui.h"

pid_t __gui_pid = 0;
int __gui_kfd = -1;

static void guikeyboard(w600_sys_t *sys) {
	uint8_t b;

	int rc = read(__gui_kfd, &b, 1);
	if (rc == 1) {
		sys->cpu.dh = b >> 4;
		sys->cpu.dl = b & 0x0f;
		sys->cpu.kp = 1;
	}
}

static int spawn_fe(w600_sys_t *sys) {
	int fd[2];

	int rc = pipe(fd);
	if (rc < 0) {
		perror("pipe()");
		return -1;
	}
	__gui_kfd = fd[0];
	__gui_pid = fork();
	if (__gui_pid < 0) {
		perror("fork()");
		close(fd[0]);
		close(fd[1]);
		return -1;
	}

	if (__gui_pid == 0) {
		char fdn[16];
		close(fd[0]);
		setsid();
		sprintf(fdn, "%d", fd[1]);
		execlp("xterm", "xterm", "-e", "./w600_fe", fdn, (char *)NULL);
		perror("xterm -e ./w600_fe");
		exit(1);
	}
	close(fd[1]);
	long fl = fcntl(__gui_kfd, F_GETFL, 0);
	fl |= O_NONBLOCK;
	fcntl(__gui_kfd, F_SETFL, fl);
	sys->keyboard = guikeyboard;
	return 0;
}

int start_fe(w600_sys_t *sys) {
	int rc = spawn_fe(sys);
	return rc;
}

void stop_fe(w600_sys_t *sys) {
	if (__gui_pid > 0) {
		close(__gui_kfd);
		kill(__gui_pid, SIGINT);
		waitpid(__gui_pid, NULL, 0);
	}
}
