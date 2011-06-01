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

#ident "$Id: w600_gui.c,v 1.24 2011/06/01 19:56:46 drmiller Exp $"

pid_t __gui_pid = 0;
int __gui_kfd = -1;
int __gui_dfd = -1;

static inline void wait_key() {
	struct pollfd fds;
	fds.fd = __gui_kfd;
	fds.events = POLLIN;
	fds.revents = 0;
	/* int rc = */ poll(&fds, 1, -1);
}

static int disp_good = 0;

// 'on' = 1: refresh one digit (AL, MR)
// 'on' = -1: refresh one digit (AL, MR) - DO NOT SLEEP!
// 'on' = -2: refresh only error lights - DO NOT SLEEP!
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
	if (on == -2) {
		// do not change any digits...
		dc = buf[ds];
	}
	if (on == 0) {
		// signal to blank display
		memset(buf, 'x', sizeof(buf));
		last = 0;
		b |= 0x400;
		++flush;
	} else {
		if (buf[ds] != dc) {
			disp_good = 0;
			buf[ds] = dc;
			++flush;
		}
		// these fields are always interpretted,
		// so send valid data...
		b |= (ds << 4) | dc;
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
				wait_key(); // sleep until key event
			}
		}
	}
}

static uint16_t extraneous = 0;

static void guidevinput(w600_sys_t *sys, uint16_t *kc, uint16_t b);

static void guikeyboard(w600_sys_t *sys, uint16_t *kc, int ack) {
	uint16_t b;

	int rc;
	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}
	if (kc == NULL) {
		wait_key();
		return;
	}
	if (ack) {
		b = *kc;
		*kc = 0;
		if ((b & 0xfc00) != 0) {
			guidevinput(sys, NULL, b); // maybe ACK...
		}
		// don't ACK simple keyboard traffic
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
	if ((b & 0xfc00) != 0) {
		guidevinput(sys, kc, b);
		return;
	}
	switch(b >> 8) {
	case 0:	// simple key pressed
		// can't really avoid overrun... ?
		*kc = 0x0100 | b;	// ensure non-zero...
		break;
	case 1:	// special key - force new microcode PC
		// jam new PC...
		b &= 0x07;
		sys->cpu.next = b;
		if (b < 4) {
			sys->cpu.pe = 0;
		}
		if (b == 0) {
			sys->cpu.me = 0;
		}
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Key Jam PC %03x\n",
				sys->cpu.pc, sys->cpu.next);
		}
		break;
	case 2:	// mode0 switches changed
		// FE gave us complete mode word... just update
		sys->cpu.mode0 = b & 0x0f;
		break;
	case 3:	// mode1 switches changed
		// DEG/RAD is inverted...
		b ^= MODE1_DEGREES;
		sys->cpu.mode1 = b & 0x0f;
		break;
	}
}

static void guidevinput(w600_sys_t *sys, uint16_t *kc, uint16_t b) {
	int rc;
	if (kc == NULL) {	// ACK
		if ((b & 0x0f00) != 0) return; // don't ACK ACK's
		b = (b & 0xf0ff) | 0x0100;
		write(__gui_dfd, &b, sizeof(b));
		return;
	}
	if ((b & 0xe000) == 0x2000) {
		//if ((b & 0x0f00) == 1) { // ACK
		//	handle just like input...
		//}
		// this is handled exactly like keyboard input...
		// bit make sure XS has valid pattern?
		if (sys->cpu.xs != (b >> 12)) {
			// oops... just spit out an error for now...
			fprintf(stderr, "Unexpected Input %04x [%d]\n", b, sys->cpu.xs);
			return;
		}
		*kc = b; // must be non-zero to be seen
		//if ((b & 0x0f00) == 0) { // not ACK
		//	// ACK is sent when Wang takes "key"...
		//}
		return;
	}
	if ((b & 0xf000) == 0x4000) {
		// TBD
		return;
	}
	if ((b & 0xf000) == 0x5000) {
		// TBD
		return;
	}
	if ((b & 0xf000) == 0x8000) { // ROM download
		int x = 0x0fff >> 1;
		b = (b & 0xf0ff) | 0x0100; // ACK
		write(__gui_dfd, &b, sizeof(b));
		do {
			wait_key();
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
			if (x >= 0 && (b & 0xff00) == 0x8000) {
				sys->rom[x--] = (b & 0x00ff);
			}
		} while ((b & 0xff00) == 0x8000);
		return;
	}
	// uh, this is embarassing...
	// presumably this is tape data, we've lost it and can't continue?
	fprintf(stderr, "gag me! %04x\n", b);
	sys->run = 0;
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
			wait_key(); // sleep until key event
			rc = read(__gui_kfd, &b, sizeof(b));
			if (rc < 0 && errno != EAGAIN) {
				perror("guitape");
				// silently quit...
				sys->run = 0;
				return 0xff;	// EOF
			}
			if ((b >> 8) == 0x0e) {	// EOF
#if 0 // can't do this! wang must detect EOT! maybe use some counter to detect "too long"?
				// nothing good will happen now... until a key is pressed...
				wait_key(); // sleep until key event
#endif
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

static void guidev(w600_sys_t *sys, uint8_t c, uint8_t sts) {
	uint16_t b;
	int rc;

	// sts might be 00... need to send "reset" to GUI...
	b = (sts << 12);
	if (sts == 0) {
		b = 0x7f00;
	} else if (sts == 1) {
		c &= 0x3f;
	}
	b |= c;
	rc = write(__gui_dfd, &b, sizeof(b));
	if (rc < 0) {
		perror("guidev");
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
	sys->dev = guidev;
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
		sys->dev = guidev;
		__gui_kfd = dup(0);	// stdin
		__gui_dfd = dup(1);	// stdout
		dup2(2,1);
		fclose(stdin);
		long fl = fcntl(__gui_kfd, F_GETFL, 0);
		fl |= O_NONBLOCK;
		fcntl(__gui_kfd, F_SETFL, fl);
	}
}
