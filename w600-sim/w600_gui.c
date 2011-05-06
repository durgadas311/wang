#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <signal.h>
#include <fcntl.h>

#include "w600_gui.h"

pid_t __gui_pid = 0;
int __gui_kfd = -1;

static void guikeyboard(w600_sys_t *sys, uint8_t *kc) {
	uint16_t b;

	int rc = read(__gui_kfd, &b, sizeof(b));
	if (rc == sizeof(b)) {
		switch(b >> 8) {
		case 0:
			// can't really avoid overrun...
			*kc = b;
			sys->cpu.kp = 1;
			break;
		case 1:
			// jam new PC...
			sys->cpu.pc = b & 0x07;
			break;
		case 2:
			sys->cpu.mode0 = b & 0x0f;
			break;
		case 3:
			sys->cpu.mode1 = b & 0x0f;
			break;
		default:
			break;
		}
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
#if 0
		execlp("xterm", "xterm", "-e", "./w600_fe", fdn, (char *)NULL);
		perror("xterm -e ./w600_fe");
#else
		execlp("java", "java", "w600_fe", fdn, (char *)NULL);
		perror("java w600_fe");
#endif
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
