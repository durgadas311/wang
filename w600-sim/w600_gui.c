// Copyright (c) 2011 Douglas Miller

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/wait.h>
#include <poll.h>

#include "w600_gui.h"

#ident "$Id: w600_gui.c,v 1.19 2011/05/22 02:52:02 drmiller Exp $"

pid_t __gui_pid = 0;
int __gui_kfd = -1;
int __gui_dfd = -1;

static int disp_good = 0;

// 'on' = 1: refresh one digit (AL, MR)
// 'on' = 0: blank display (reset everything)
static void guidisplay(w600_sys_t *sys, int on) {
	static char buf[16] = { "xxxxxxxxxxxxxxxx" };
	static uint16_t last = 0;
	uint16_t b = 0;
	int rc;
	int flush = 0;

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
	if (last != b) {
		last = b;
		++flush;
	}

	uint8_t ds = sys->cpu.al;
	uint8_t dc = sys->cpu.mr;
	if (on == 0) {
		// signal to blank display
		memset(buf, 'x', sizeof(buf));
		last = 0;
		b |= 0x400;
		++flush;
	} else if (buf[ds] != dc) {
		disp_good = 0;
		buf[ds] = dc;
		b |= (ds << 4) | dc;
		++flush;
	}

	if (flush) {
		disp_good = 0;
		rc = write(__gui_dfd, &b, sizeof(b));
		if (rc < 0) {
			perror("guidisplay");
			// silently quit...
			sys->run = 0;
			return;
		}
	} else {
		++disp_good;
		if (disp_good > 64) {
			if (on > 0) {
				sys->keyboard(sys, NULL); // sleep until key event
			}
		}
	}
}

static uint16_t extraneous = 0;

static void guikeyboard(w600_sys_t *sys, uint8_t *kc) {
	uint16_t b;

	int rc;
	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}
	if (kc == NULL) {
		struct pollfd fds;
		fds.fd = __gui_kfd;
		fds.events = POLLIN;
		fds.revents = 0;
		/* int rc = */ poll(&fds, 1, -1);
		return;
	}
	if (extraneous) {
		b = extraneous;
		extraneous = 0;
	} else {
		rc = read(__gui_kfd, &b, sizeof(b));
		if (rc < 0 && errno != EAGAIN) {
			perror("guikeyboard");
			// silently quit...
			sys->run = 0;
			return;
		}
		if (rc != sizeof(b)) {
			return;
		}
	}
	// something came down the pipe...
	// make sure display gets refreshed...
	guidisplay(sys, 0);
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
		// uh, this is embarassing...
		// presumably this is tape data, we've lost it and can't continue?
		fprintf(stderr, "gag me! %04x\n", b);
		sys->run = 0;
		return;
		break;
	}
}

static void guiprinter(w600_sys_t *sys, int col, int drum) {
	uint16_t b;
	int rc;

	b = 0x0800 | ((col & 0x1f) << 4) | (drum & 0x0f);
	rc = write(__gui_dfd, &b, sizeof(b));
	if (rc < 0) {
		perror("guiprinter");
		// silently quit...
		sys->run = 0;
		return;
	}
}

static uint8_t guitape(w600_sys_t *sys, int wr, uint8_t nibble) {
	static uint8_t byte;
	static int bc = 0;
	uint16_t b;
	int rc;

	if (nibble & 0x80) { // tape off
		b = 0x0e00;
		bc = 0;
		byte = 0;
	} else if (nibble & 0x40) { // tape on
		b = 0x0d00 | ((wr & 1) << 9);
		bc = 0;
		byte = 0;
	} else if (wr) {
		bc ^= 1;
		if (bc) {
			byte = (byte & 0x0f) | (nibble << 4);
			return 0;
		} else {
			byte = (byte & 0xf0) | nibble;
		}
		b = 0x0c00 | byte;
	} else {
		if (!bc) {
			// not needed? will GUI take care of it?
			if (byte == 0x9e) { // End Prog
				return 0xff;
			}
			byte = 0;

			// there must be data ready OR ELSE!
			if (extraneous) {
				return 0xff;	// EOF
			}
			b = 0x0d01;	// request a byte...
			rc = write(__gui_dfd, &b, sizeof(b));
			if (rc != sizeof(b)) {
				perror("guitape");
				// silently quit...
				sys->run = 0;
				return 0xff;	// EOF
			}
			sys->keyboard(sys, NULL); // sleep until key event
			rc = read(__gui_kfd, &b, sizeof(b));
			if (rc < 0 && errno != EAGAIN) {
				perror("guitape");
				// silently quit...
				sys->run = 0;
				return 0xff;	// EOF
			}
			if ((b >> 8) == 0x0e) {	// EOF
				// nothing good will happen now... until a key is pressed...
				sys->keyboard(sys, NULL); // sleep until key event
				return 0xff;
			}
			if ((b >> 8) != 0x0c) {
				// oops...
				// now we've really done it...
				extraneous = b;
				return 0xff;	// EOF
			}
			bc ^= 1;
			byte = (b & 0x0ff);
			return (byte >> 4);
		} else {
			bc ^= 1;
			return (byte & 0x0f);
		}
	}
	rc = write(__gui_dfd, &b, sizeof(b));
	if (rc < 0) {
		perror("guitape");
		// silently quit...
		sys->run = 0;
		return 0xff;	// probably ignored
	}
	return 0;
}

static void guicn24(w600_sys_t *sys, uint8_t c) {
	uint16_t b;
	int rc;

	b = 0x1000 | c;
	rc = write(__gui_dfd, &b, sizeof(b));
	if (rc < 0) {
		perror("guicn24");
		// silently quit...
		sys->run = 0;
		return;
	}
}

// spawn the GUI as a back-end to us...
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
		close(fd[0]);
		close(fe[1]);
		dup2(fe[0], 0);
		dup2(fd[1], 1);
		setsid();
#if 0
		execlp("xterm", "xterm", "-e", "./w600_fe", fdn, (char *)NULL);
		perror("xterm -e ./w600_fe");
#else
		execlp("java", "java", "w600_fe", "-b", (char *)NULL);
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
	sys->printer = guiprinter;
	sys->tape = guitape;
	sys->cn24 = guicn24;
	return 0;
}

int start_fe(w600_sys_t *sys) {
	extraneous = 0;
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

void setup_fe(w600_sys_t *sys, int ops) {
	if (ops & SYS_BACK_END) {
		sys->keyboard = guikeyboard;
		sys->display = guidisplay;
		sys->printer = guiprinter;
		sys->tape = guitape;
		sys->cn24 = guicn24;
		__gui_kfd = dup(0);	// stdin
		__gui_dfd = dup(1);	// stdout
		dup2(2,1);
		fclose(stdin);
		long fl = fcntl(__gui_kfd, F_GETFL, 0);
		fl |= O_NONBLOCK;
		fcntl(__gui_kfd, F_SETFL, fl);
	}
}
