#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/types.h>
#include <signal.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/wait.h>

#include "w600_gui.h"

pid_t __gui_pid = 0;
int __gui_kfd = -1;
int __gui_dfd = -1;

static void guidisplay(w600_sys_t *sys, int on) {
	static char buf[16] = { "xxxxxxxxxxxxxxxx" };
	static uint16_t last = 0;
	uint16_t b = 0;
	int rc;

	if (!on) return;

	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}

	// piggy-back error lights in hi order bits...
	if (sys->cpu.pe) {
		b |= 0x100;
	}
	if (sys->cpu.me) {
		b |= 0x200;
	}
	uint8_t ds = sys->cpu.al;
	uint8_t dc = sys->cpu.mr;
	if (last != b || buf[ds] != dc) {
		buf[ds] = dc;
		last = b;
		b |= (ds << 4) | dc;
		rc = write(__gui_dfd, &b, sizeof(b));
		if (rc < 0) {
			perror("guidisplay");
			// silently quit...
			sys->run = 0;
			return;
		}
	}
}

static void guikeyboard(w600_sys_t *sys, uint8_t *kc) {
	uint16_t b;

	int rc;
	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}

	rc = read(__gui_kfd, &b, sizeof(b));
	if (rc < 0 && errno != EAGAIN) {
		perror("guikeyboard");
		// silently quit...
		sys->run = 0;
		return;
	}
	if (rc == sizeof(b)) {
		switch(b >> 8) {
		case 0:
			// can't really avoid overrun...
			*kc = b;
			sys->cpu.kp = 1;
			break;
		case 1:
			// jam new PC...
			b &= 0x07;
			sys->cpu.pc = b;
			if (b < 4) {
				sys->cpu.pe = 0;
			}
			if (b == 0) {
				sys->cpu.me = 0;
			}
			break;
		case 2:
			// FE gave us complete mode word... just update
			sys->cpu.mode0 = b & 0x0f;
			break;
		case 3:
			// DEG/RAD is inverted...
			b ^= MODE1_DEGREES;
			sys->cpu.mode1 = b & 0x0f;
			break;
		default:
			break;
		}
	}
}

static int spawn_fe(w600_sys_t *sys) {
	int fd[2];
	int fe[2];

	int rc = pipe(fd);
	if (rc < 0) {
		perror("pipe()");
		return -1;
	}
	rc = pipe(fe);
	if (rc < 0) {
		perror("pipe()");
		close(fd[0]);
		close(fd[1]);
		return -1;
	}
	__gui_kfd = fd[0];
	__gui_dfd = fe[1];
	__gui_pid = fork();
	if (__gui_pid < 0) {
		perror("fork()");
		close(fd[0]);
		close(fd[1]);
		close(fe[0]);
		close(fe[1]);
		return -1;
	}

	if (__gui_pid == 0) {
		char fdn[16];
		char fen[16];
		close(fd[0]);
		close(fe[1]);
		setsid();
		sprintf(fdn, "%d", fd[1]);
		sprintf(fen, "%d", fe[0]);
#if 0
		execlp("xterm", "xterm", "-e", "./w600_fe", fdn, (char *)NULL);
		perror("xterm -e ./w600_fe");
#else
		execlp("java", "java", "w600_fe", fdn, fen, (char *)NULL);
		perror("java w600_fe");
#endif
		exit(1);
	}
	close(fd[1]);
	close(fe[0]);
	long fl = fcntl(__gui_kfd, F_GETFL, 0);
	fl |= O_NONBLOCK;
	fcntl(__gui_kfd, F_SETFL, fl);
	sys->keyboard = guikeyboard;
	sys->display = guidisplay;
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
