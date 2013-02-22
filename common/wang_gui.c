// Copyright (c) 2011, 2012 Douglas Miller

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
#include <sys/stat.h>

#ident "$Id: wang_gui.c,v 1.25 2013/02/22 21:28:32 drmiller Exp $"

#include "wang-sim.h"

pid_t __gui_pid = 0;
int __gui_kfd = -1;
int __gui_dfd = -1;

static inline void wait_key(int timeout) {
	struct pollfd fds;
	fds.fd = __gui_kfd;
	fds.events = POLLIN;
	fds.revents = 0;
	/* int rc = */ poll(&fds, 1, timeout);
}

static inline int test_kbd() {
	struct pollfd fds;
	fds.fd = __gui_kfd;
	fds.events = POLLIN;
	fds.revents = 0;
	/* int rc = */ poll(&fds, 1, 0);
	return (fds.revents & POLLIN) != 0;
}

static int disp_good = 0;

// 'on' = 1: refresh one digit (AL, MR)
// 'on' = -1: refresh one digit (AL, MR) - DO NOT SLEEP!
// 'on' = -2: refresh only error lights - DO NOT SLEEP!
// 'on' = 0: blank display (reset everything)
static void guidisplay(wang_sys_t *sys, int on) {
#ifndef __wang1200__
	static uint16_t bufx[16];
	static uint16_t lastx = 0;
#endif // ! __wang1200__
	uint16_t bx = 0;
#ifdef __wang700__
	static uint16_t bufy[16];
	uint16_t by = 0;
#endif // __wang700__
	int rc;
	int flush = 0;

	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}

	if (on == -2) {
		// error lights changed - only
		bx = (sys->cpu.ind.word & 0x03ff) | 0x0400;
		flush = 1;
#ifndef __wang1200__
	} else if (on == 0) {
		// blank display(s)
		disp_good = 0;
		lastx = 0;
		// signal to blank display
		bx = 0x0600;
		memset(bufx, -1, sizeof(bufx));
#ifdef __wang700__
		memset(bufy, -1, sizeof(bufy));
#endif // __wang700__
		flush = 1;
	} else {
		uint8_t ds = sys->cpu.n;
		bx |= (ds << 4) | sys->cpu.rb;
#ifdef __wang700__
		bx |= (sys->cpu.s & 2) << 7;	// FXDX
		//bx ^= 0x0100;
#endif // __wang700__
		if (bufx[ds] != bx) {
			disp_good = 0;
			bufx[ds] = bx;
		}
		// these fields are always interpretted,
		// so send valid data...
#ifdef __wang700__
		by |= 0x0200 | (ds << 4) | sys->cpu.ra;
		by |= (sys->cpu.s & 1) << 8;	// FXDY
		by ^= 0x0100;
		if (bufy[ds] != by) {
			disp_good = 0;
			bufy[ds] = by;
		}
#endif // __wang700__
		if (++lastx >= 16) {
			flush = 16;
			lastx = 0;
		}
	}

	if (flush == 1) {
#endif // ! __wang1200__
		disp_good = 0;
		rc = write(__gui_dfd, &bx, sizeof(bx));
		if (rc < 0) {
			perror("guidisplay");
			// silently quit...
			sys->run = 0;
			return;
		}
#ifndef __wang1200__
	} else if (flush > 1) {	// must be 16
		++disp_good;
		rc = write(__gui_dfd, &bx, sizeof(bx));
		if (rc < 0) {
			perror("guidisplay");
			// silently quit...
			sys->run = 0;
			return;
		}
		rc = write(__gui_dfd, bufx, sizeof(bufx));
		if (rc < 0) {
			perror("guidisplay");
			// silently quit...
			sys->run = 0;
			return;
		}
#ifdef __wang700__
		if ((sys->cpu.d & D12_LRN_L_P) == 0) {
			rc = write(__gui_dfd, &by, sizeof(by));
			if (rc < 0) {
				perror("guidisplay");
				// silently quit...
				sys->run = 0;
				return;
			}
			rc = write(__gui_dfd, bufy, sizeof(bufy));
			if (rc < 0) {
				perror("guidisplay");
				// silently quit...
				sys->run = 0;
				return;
			}
		}
#endif // __wang700__
	} else {
		if (disp_good > 4) {
			if (on > 0) {
				wait_key(-1); // sleep until key event
			}
		}
#endif // ! __wang1200__
	}
}

static uint16_t extraneous = 0;

static void guikeyboard(wang_sys_t *sys, uint16_t *kc, int ack) {
	uint16_t b;

	int rc;
	rc = waitpid(__gui_pid, NULL, WNOHANG);
	if (rc == __gui_pid) {
		// silently quit...
		sys->run = 0;
		return;
	}
	if (kc == NULL) {
		// ack is timeout, usec, 0=infinite
		wait_key(ack);
		return;
	}
	if (ack) {
		b = *kc;
		*kc = 0;
		if ((b & 0xfc00) != 0) {
			if ((b & 0x0f00) != 0) {
#ifdef __wang600__ // will also be 700...
				// check for Group 1/2 asserting GLRN in ACK...
				if ((b & 0xe000) == 0x4000) {
//fprintf(stderr, "glrn = %d\n", (b & 1));
					sys->cpu.glrn = (b & 1);
				}
#endif // __wang600__
				return; // don't ACK ACK's
			}
			b = (b & 0xf0ff) | 0x0100;
			write(__gui_dfd, &b, sizeof(b));
			return;
		}
		// don't ACK simple keyboard traffic
		return;
	}
	if (extraneous) {
		b = extraneous;
		extraneous = 0;
	} else if (test_kbd()) {
		rc = read(__gui_kfd, &b, sizeof(b));
		if (rc < 0 && errno != EAGAIN) {
			perror("guikeyboard");
			// silently quit...
			sys->run = 0;
			return;
		}
		if (rc != sizeof(b)) {
			// probably EOF, silently quit...
			sys->run = 0;
			return;
		}
	} else {
		return;
	}
	// something came down the pipe...
	// make sure display gets refreshed...
	guidisplay(sys, 0);

	switch(b >> 8) {
	case 0:	// simple key pressed
		// can't really avoid overrun... ?
		// implement kbd locking:
		if ((sys->cpu.iob & 0x6) == 0) *kc = 0x0100 | b;
		break;
	case 1:	// special key - force new microcode PC
		// jam new PC...
		b &= 0x07;
		sys->cpu.sys.jam = b | 0x8000; // force non-zero
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Key Jam PC %03x\n",
				sys->cpu.sys.pc, sys->cpu.sys.jam & 0x0fff);
		}
		sys->display(sys, -2);
		break;
	default:

#ifndef __wang1200__
		if ((b & 0xe000) == 0x2000) {
			//if ((b & 0x0f00) == 1) { // ACK
			//	handle just like input...
			//}
			// this is handled exactly like keyboard input...
			// bit make sure XS has valid pattern?
			if (sys->cpu.iob != (b >> 12)) {
				// oops... just spit out an error for now...
				fprintf(stderr, "Unexpected Input %04x [%d]\n", b, sys->cpu.iob);
				return;
			}
//fprintf(stderr, "\tDEV< %02x %x\n", b & 0x0ff, sys->cpu.iob);
			*kc = b; // must be non-zero to be seen
			//if ((b & 0x0f00) == 0) { // not ACK
			//	// ACK is sent when Wang takes "key"...
			//}
			return;
		}
		if ((b & 0xe000) == 0x4000) {
//fprintf(stderr, "\tGRP2< %02x %x\n", b & 0x0ff, sys->cpu.iob);
			if ((b & 0x0f00) != 0) {
//fprintf(stderr, "glrn = %d\n", (b & 1));
				sys->cpu.glrn = (b & 1);
			} else {
				*kc = b; // must be non-zero to be seen
			}
			return;
		}
#ifdef WANG_ROM_SIZE
		if ((b & 0xf000) == 0x8000) { // ROM download
			int rc;
			int x = 0x0fff >> 1;
			b = (b & 0xf0ff) | 0x0100; // ACK
			write(__gui_dfd, &b, sizeof(b));
			do {
				rc = read(__gui_kfd, &b, sizeof(b));
				if (rc < 0 && errno != EAGAIN) {
					perror("guikeyboard");
					// silently quit...
					sys->run = 0;
					return;
				}
				if (rc != sizeof(b)) {
					// probably EOF, silently quit...
					sys->run = 0;
					return;
				}
				if (x >= 0 && (b & 0xff00) == 0x8000) {
					sys->rom[x--] = (b & 0x00ff);
				}
			} while ((b & 0xff00) == 0x8000);
			return;
		}
#endif // WANG_ROM_SIZE
#endif // ! __wang1200__

		// last resort... platform-specific codes...
		if (sys->special_key(sys, b) == -1) {
			// uh, this is embarassing...
			// presumably this is tape data, we've lost it and can't continue?
			fprintf(stderr, "gag me! %04x\n", b);
			sys->run = 0;
			return;
		}
	}
}

#ifdef WANG_HAS_PRINTER
static void guiprinter(wang_sys_t *sys, int col, int drum) {
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
#endif // WANG_HAS_PRINTER

#ifdef WANG_HAS_TAPE
static uint8_t guitape(wang_sys_t *sys, int wr, uint8_t nibble) {
	static uint8_t byte;
	static int bc = 0;
	uint16_t b;
	int rc;

	if (nibble & 0x80) { // tape off
		b = 0x0e00;
		bc = 0;
		byte = 0;
#ifdef __wang1200__
		b = 0x0ec0 |
			(sys->cpu.rc << 5) |
			(sys->cpu.rv << 4) |
			((sys->cpu.lhs | sys->cpu.rhs) << 3) |
			(sys->cpu.hl << 2) |
			(sys->cpu.right << 0);
#endif // __wang1200__
	} else if (nibble & 0x40) { // tape on
		b = 0x0d00 | ((wr & 1) << 9);
		bc = 0;
		byte = 0;
#ifdef __wang1200__
		b = 0x0e00 |
			(sys->cpu.rc << 5) |
			(sys->cpu.rv << 4) |
			((sys->cpu.lhs | sys->cpu.rhs) << 3) |
			(sys->cpu.hl << 2) |
			(sys->cpu.right << 0);
#endif // __wang1200__
	} else if (wr) {
		bc ^= 1;
		if (bc) {
			byte = (byte & 0x0f) | (nibble << 4);
			return 0;
		} else {
			byte = (byte & 0xf0) | nibble;
		}
		b = 0x0c00 | byte;
#ifdef __wang1200__
		b |= (sys->cpu.right << 8);
#endif // __wang1200__
	} else {
		if (!bc) {
			// not needed? will GUI take care of it?
			if (byte == WANG_END_PROG) { // End Prog
				return 0xff;
			}
			byte = 0;

			// there must be data ready OR ELSE!
			if (extraneous) {
				return 0xff;	// EOF
			}
			b = 0x0d01;	// request a byte...
#ifdef __wang1200__
			b = 0x0e40 | (sys->cpu.right << 0);
#endif // __wang1200__
			rc = write(__gui_dfd, &b, sizeof(b));
			if (rc != sizeof(b)) {
				perror("guitape");
				// silently quit...
				sys->run = 0;
				return 0xff;	// EOF
			}
			// might get async keyboard code... need to cope...
			int tape;
			do {
				rc = read(__gui_kfd, &b, sizeof(b));
				if (rc < 0 && errno != EAGAIN) {
					perror("guitape");
					// silently quit...
					sys->run = 0;
					return 0xff;	// EOF
				}
				if (rc != sizeof(b)) {
					// probably EOF on pipe, silently quit...
					sys->run = 0;
					return 0xff;	// EOF
				}
				if ((b >> 8) == 0x0e) {	// EOF
#if 0 // can't do this! wang must detect EOT! maybe use some counter to detect "too long"?
					// nothing good will happen now... until a key is pressed...
					wait_key(-1); // sleep until key event
#endif
					return 0xff;
				}
#ifdef __wang1200__
				// assert drive?
				tape = (((b >> 8) & ~1) == 0x0c);
#else // ! __wang1200__
				tape = ((b >> 8) == 0x0c);
#endif // ! __wang1200__
				if (!tape) {
					// oops...
					// now we've really done it...
if (extraneous && extraneous != b) fprintf(stderr, "double extraneous %04x -> %04x\n", extraneous, b);
					extraneous = b;
					//return 0xff;	// EOF
				}
			} while (!tape);
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
#endif // WANG_HAS_TAPE

#ifdef WANG_HAS_DEV
static void guidev(wang_sys_t *sys, uint8_t c, uint8_t sts) {
	uint16_t b;
	int rc;

	// sts might be 00... need to send "reset" to GUI...
	b = (sts << 12);
	if (sts == 0) {
#ifdef __wang600__ // will also be 700...
		sys->cpu.glrn = 0;
#endif // __wang600__
		b = 0x7f00;
	} else if (sts == 1) {
#ifdef __wang1200__
		if (sys->cpu.function) {
			b |= (3 << 11);
			c = (c >> 4) & 0x0f;
		} else {
			c &= 0x3f;
		}
#else // ! __wang1200__
		c &= 0x3f;
#endif // ! __wang1200__
	}
	b |= c;
//fprintf(stderr, "device %04x\n", b);
	rc = write(__gui_dfd, &b, sizeof(b));
	if (rc < 0) {
		perror("guidev");
		// silently quit...
		sys->run = 0;
		return;
	}
}
#endif // WANG_HAS_DEV

static void setup_devices(wang_sys_t *sys) {
	// "keyboard" is actually all input from GUI...
	sys->keyboard = guikeyboard;

	// "display" is actually all output to GUI...
	sys->display = guidisplay;

#ifdef WANG_HAS_PRINTER
	sys->printer = guiprinter;
#endif
#ifdef WANG_HAS_TAPE
	sys->tape = guitape;
#endif
#ifdef WANG_HAS_DEV
	sys->dev = guidev;
#endif
}

// spawn the GUI as a back-end to us...
static int spawn_fe(wang_sys_t *sys) {
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
		execlp("xterm", "xterm", "-e", "./" WANG_GUI_NAME, fdn, (char *)NULL);
		perror("xterm -e ./" WANG_GUI_NAME);
#else
		execlp("java", "java", WANG_GUI_NAME, "-b", (char *)NULL);
		perror("java " WANG_GUI_NAME);
#endif
		exit(1);
	}
	close(fd[1]);
	close(fe[0]);
	//long fl = fcntl(__gui_kfd, F_GETFL, 0);
	//fl |= O_NONBLOCK;
	//fcntl(__gui_kfd, F_SETFL, fl);
	setup_devices(sys);
	return 0;
}

int start_fe(wang_sys_t *sys) {
	extraneous = 0;
	int rc = spawn_fe(sys);
	return rc;
}

void stop_fe(wang_sys_t *sys) {
	if (__gui_pid > 0) {
		close(__gui_kfd);
		kill(__gui_pid, SIGINT);
		waitpid(__gui_pid, NULL, 0);
	}
}

void setup_fe(wang_sys_t *sys) {
	if (sys->ops & SYS_BACK_END) {
		setup_devices(sys);
		__gui_kfd = dup(0);	// stdin
		__gui_dfd = dup(1);	// stdout
		dup2(2,1);
		fclose(stdin);
		//long fl = fcntl(__gui_kfd, F_GETFL, 0);
		//fl |= O_NONBLOCK;
		//fcntl(__gui_kfd, F_SETFL, fl);
	}
}
