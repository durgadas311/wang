// Copyright (c) 2011 Douglas Miller

#ident "$Id: wang_sys.c,v 1.11 2011/11/14 23:18:32 drmiller Exp $"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>
#include <errno.h>

#include <syslog.h>
#include <sys/socket.h>
#include <netdb.h>
#include <time.h>
#include <sys/time.h>
#include <sys/resource.h>

#include "wang-sim.h"

extern int start_fe(wang_sys_t *sys);
extern void stop_fe(wang_sys_t *sys);
extern void setup_fe(wang_sys_t *sys);

uint16_t ram_mask = sizeof(((wang_sys_t *)0)->ram) - 1;

char peer_name[256] = {"unknown"};
static time_t peer_start;

extern int diwang(char *buf, uint64_t *t);

// allow others to access, if they know it
uint8_t __keyb[32];
int __klen;
int __keyp;

static void fatal_error(wang_sys_t *sys, const char *s, const char *e) {
	if ((sys->ops & SYS_WEB_BACKEND) != 0) {
		uint16_t b = 0xf000;
		write(2, &b, sizeof(b));
	}
	if (e) {
		fprintf(stderr, "%s: %s\n", s, e);
	} else {
		fprintf(stderr, "%s\n", s);
	}
}

static void syskeyboard(wang_sys_t *sys, uint16_t *kc, int ack) {
	if (kc == NULL) {
		// what to do?
		return;
	}
	if (ack) {
		*kc = 0;
		return;
	}
	if (__klen && !*kc) {
		--__klen;
		*kc = 0x0100 | __keyb[__keyp];
		++__keyp;
		return;
	}
}

static char cn24_xlate[256] = {
[0x00] = '-',
[0x01] = 'y',
[0x02] = ' ',
[0x03] = '\b',
[0x04] = 'q',
[0x05] = 'p',
[0x06] = '=',
[0x07] = 'j',
// [0x08] = ' ',
[0x09] = '/',
//[0x0a] = ' ',
//[0x0b] = ' ',
[0x0c] = ',',
[0x0d] = ';',
[0x0e] = 'f',
[0x0f] = 'g',

[0x10] = 'w',
[0x11] = 's',
//[0x12] = '.';       // shift dn
//[0x13] = '.';       // shift up
[0x14] = 'i',
[0x15] = '\'',
[0x16] = '.',
//[0x17] = '\u00BD';  // 1/2...
[0x18] = '\n',
[0x19] = 'o',
[0x1a] = '\n',
//[0x1b] = '\n';      // rev index
[0x1c] = 'a',
[0x1d] = 'r',
[0x1e] = 'v',
[0x1f] = 'm',

[0x20] = 'b',
[0x21] = 'h',
//[0x22] = '+';       // step x+ 
//[0x23] = '+';       // step x- 
[0x24] = 'k',
[0x25] = 'e',
[0x26] = 'n',
[0x27] = 't',
//[0x28] = '';        // print mode
[0x29] = '1',
//[0x2a] = '+';       // step y+ 
//[0x2b] = '+';       // step y- 
[0x2c] = 'c',
[0x2d] = 'd',
[0x2e] = 'u',
[0x2f] = 'x',

[0x30] = '9',
[0x31] = '0',
//[0x32] = '+';       // step x+y+
//[0x33] = '+';       // step x-y+
[0x34] = '6',
[0x35] = '5',
[0x36] = '2',
[0x37] = 'z',
//[0x38] = 'z';       // plot mode
[0x39] = '4',
//[0x3a] = '4';       // step x+y-
//[0x3b] = '4';       // step x-y-
[0x3c] = '8',
[0x3d] = '7',
[0x3e] = '3',
[0x3f] = 'l',

// shifted versions...
[0x40] = '_',
[0x41] = 'Y',
[0x44] = 'Q',
[0x45] = 'P',
[0x46] = '+',
[0x47] = 'J',
[0x49] = '?',
[0x4c] = ',',
[0x4d] = ':',
[0x4e] = 'F',
[0x4f] = 'G',

[0x50] = 'W',
[0x51] = 'S',
[0x54] = 'I',
[0x55] = '"',
[0x56] = '.',
//[0x57] = '\u00BC',	// 1/4
[0x59] = 'O',
[0x5c] = 'A',
[0x5d] = 'R',
[0x5e] = 'V',
[0x5f] = 'M',

[0x20] = 'B',
[0x21] = 'H',
[0x64] = 'K',
[0x65] = 'E',
[0x66] = 'N',
[0x67] = 'T',
[0x69] = '!',
[0x6c] = 'C',
[0x6d] = 'D',
[0x6e] = 'U',
[0x6f] = 'X',

[0x70] = '(',
[0x71] = ')',
//[0x74] = '\u00A2',	// cent
[0x75] = '%',
[0x76] = '@',
[0x77] = 'Z',
[0x79] = '$',
[0x7c] = '*',
[0x7d] = '&',
[0x7e] = '#',
[0x7f] = 'L',

};

static void syscn24(wang_sys_t *sys, uint8_t c, uint8_t sts) {
	if (sts != 1) {
		fprintf(stderr, "GIOA/GIOB = %02x [%d]\n", c, sts);
		return;
	}
	char p = cn24_xlate[c];
	if (c == 0x28) return;
	if (!p) {
		printf("\n<%02x>", c);
	} else {
		fputc(p, stdout);
	}
	fflush(stdout);
}

static int intr(wang_sys_t *sys, int sig) {
	if (sig == SIGINT && sys->run) {
		fflush(sys->trc_fp);
		sys->run = 0;
		return 0;
	}
	return 1;
}

static void dump(wang_sys_t *sys) {
	char buf[1024];
	fprintf(stderr, "at cycle %lld\n", sys->cpu.sys.cycles);

	uint64_t *pc = &sys->ucode[sys->cpu.sys.pc];

	diwang(buf, pc);
	fprintf(stderr, "PC = %03x [ %s ]\n", sys->cpu.sys.pc, buf);
	sys->get_reg_str(sys, buf);
	fprintf(stderr, "%s", buf);
	// more...
}

#if 0
static void iofault(void *v, uint8_t port) {
	wang_sys_t *sys = (wang_sys_t *)v;
	fprintf(stderr, "I/O Fault at port %01x\n", port);
	dump(sys);
	exit(1);
}

static void segfault(void *v, uint16_t adr) {
	wang_sys_t *sys = (wang_sys_t *)v;
	fprintf(stderr, "Seg Fault at address %03x\n", adr);
	dump(sys);
	exit(1);
}
#endif

static void sysfault(wang_sys_t *sys, const char *str) {
	fatal_error(sys, str, NULL);
	if ((sys->ops & SYS_WEB_BACKEND) == 0) {
		dump(sys);
	}
	exit(1);
}

void sys_init(wang_sys_t *sys) {
	memset(sys, 0, sizeof(*sys));
	sys->fault = sysfault;
	sys->keyboard = syskeyboard;
#ifdef WANG_HAS_DEV
	sys->dev = syscn24;
#endif
	sys->cpu.sys.cylimit = (uint64_t)-1;

// put special pattern in RAM for debugging...
if (0) { int x;
for (x = 0; x < sizeof(sys->ram); ++x) {
	sys->ram[x] = x & 0x0ff;
}
}

	sys->intr = intr;
#ifdef TRACE
	sys->trace = 0;
	sys->trc_fp = stderr;
#endif // TRACE
	// now install all devices and peripherals...

	WANG_SYS_INIT(sys);
}

void sys_start(wang_sys_t *sys) {
	if (sys->ops & SYS_START_GUI) {
		int rc = start_fe(sys);
		if (rc) {
			fprintf(stderr, "GUI startup failed, reverting to stdio\n");
		}
	}
	if ((sys->ops & SYS_WEB_BACKEND) != 0) {
		peer_start = time(NULL);
		openlog(WANG_SIM, LOG_PID, LOG_USER);

		struct sockaddr addr;
		socklen_t len = sizeof(addr);
		getpeername(0, &addr, &len);
		getnameinfo(&addr, len, peer_name, sizeof(peer_name), NULL, 0, NI_NUMERICHOST);

		syslog(LOG_INFO, "starting simulation for %s\n", peer_name);
	}
	if (sys->ops & SYS_BACK_END) {
		setup_fe(sys);
	} else {
		printf("Wang %d Programmable Calculator\n", WANG_SERIES);
	}
}

void sys_stop(wang_sys_t *sys) {
	stop_fe(sys);
	if ((sys->ops & SYS_WEB_BACKEND) != 0) {
		struct rusage ru;
		getrusage(RUSAGE_SELF, &ru);
		time_t t = time(NULL) - peer_start;
		int s = t % 60;
		t /= 60;
		int m = t % 60;
		t /= 60;
		int h = t;
		double st = ru.ru_stime.tv_usec;
		st /= 1.0e6;
		st += ru.ru_stime.tv_sec;
		double ut = ru.ru_utime.tv_usec;
		ut /= 1.0e6;
		ut += ru.ru_utime.tv_sec;
		syslog(LOG_INFO, "ending simulation for %s (%gs %gu %d:%02d:%02d)\n",
					peer_name, st, ut, h, m, s);

		closelog();
	}
}

static int sys_interact(wang_sys_t *sys) {
	extern int sys_command(wang_sys_t *sys);
	int rc;

	struct sigaction save_intr;
	struct sigaction temp_intr;

	sigemptyset(&temp_intr.sa_mask);
	temp_intr.sa_handler = SIG_IGN;
	temp_intr.sa_flags = SA_RESTART;
	sigaction(SIGINT, &temp_intr, &save_intr);

	printf("break at %03x\n", sys->cpu.sys.pc);
	do {
		rc = sys_command(sys);
	} while (rc == 0 && !sys->run);
	sigaction(SIGINT, &save_intr, NULL);
	return rc;
}

static void __load_mem(wang_sys_t *sys, uint8_t *mem, char *file) {
	int fd;
	uint32_t max = 2 * 1024;

	fd = open(file, O_RDONLY);
	if (fd < 0) {
		fatal_error(sys, file, strerror(errno));
		exit(1);
	}
	int rc = read(fd, mem, max);
	if (rc < 0) {
		fatal_error(sys, file, strerror(errno));
		exit(1);
	}
	close(fd);
}

// NOTE: program steps are reverse order from registers.
// F6F is program step 000, but is register 15 06.
// Register 01 00 is the last program steps (1840..1847).
// F6F is hi nibble of byte, F6E is lo nibble of byte.
// The above addresses are NIBBLE addresses, as used by the
// Wang. Byte offsets in file are >> 1.
#if defined(__wang600__)
void sys_loadrom(wang_sys_t *sys, char *rom) {
	uint8_t *mem = sys->rom;
	__load_mem(sys, mem, rom);
}

#endif // __wang600__

void sys_loadram(wang_sys_t *sys, char *ram) {
	uint8_t *mem = sys->ram;
	__load_mem(sys, mem, ram);
}

// ! This loads a microcode image!
void sys_loaducode(wang_sys_t *sys, char *exe, uint16_t adr, uint16_t entry) {
	int fd;
	uint32_t max = 2 * 1024;
	adr &= 0x0fff;
	entry &= 0x0fff;
	int len = (max - adr) * sizeof(sys->ucode[0]);

	fd = open(exe, O_RDONLY);
	if (fd < 0) {
		fatal_error(sys, exe, strerror(errno));
		exit(1);
	}
	struct stat stb;
	fstat(fd, &stb);
	uint64_t *m = &sys->ucode[adr];
	int rc;
	if (stb.st_size > 2*1024*8) { /* must be text format... */
		extern int loaducode_txt(int fd, uint64_t *m, int len);
		rc = loaducode_txt(fd, m, len);
		// what is good error-checking?
	} else {
		rc = read(fd, m, len);
		if (rc < 0) {
			fatal_error(sys, exe, strerror(errno));
			exit(1);
		}
	}
	close(fd);
	if (sys->ucode_override) sys->ucode_override(sys);
}

// need to load *backwards* since program steps advance backwards in RAM...
void sys_loadpgm(wang_sys_t *sys, char *pgm) {
	int fd;
	uint8_t *buf;
	struct stat stb;

	fd = open(pgm, O_RDONLY);
	if (fd < 0) {
		fatal_error(sys, pgm, strerror(errno));
		exit(1);
	}
	fstat(fd, &stb);
	buf = malloc(stb.st_size);
	if (!buf) {
		static char buf[1024];
		sprintf(buf, "unable to malloc %ld bytes for \"%s\"\n", stb.st_size, pgm);
		fatal_error(sys, buf, strerror(errno));
		exit(1);
	}
	int rc = read(fd, buf, stb.st_size);
	if (rc < 0) {
		fatal_error(sys, pgm, strerror(errno));
		exit(1);
	}
	close(fd);

	int w, x, y, z;
	z = stb.st_size;
	w = (0x0ff - 0xf6f) >> 1;	// max num bytes (prog steps)
	x = 0xf6f >> 1;			// first program step, byte-address
	for (y = 0; y < z && w > 0; ++y, --x) {
		sys->ram[x] = buf[y];
	}

	free(buf);
}

static int run_some(wang_sys_t *sys, uint16_t entry) {
	int rc;
	sys->cpu.sys.pc = entry;
	do {
		while (!sys->run) {
			rc = sys_interact(sys);
			if (rc) return rc;
		}
		sys_exec(sys); // single-step
		if (sys->cpu.sys.cycles >= sys->cpu.sys.cylimit) {
			sys->cpu.sys.cylimit = (uint64_t)-1;
			sys->run = 0;
		}
	} while (sys->run || sys->cmd);
	return 0;
}


int sys_go(wang_sys_t *sys, uint16_t entry) {
	entry &= 0x0fff;
	int rc = run_some(sys, entry);
	printf("exit at cycle %lld\n", sys->cpu.sys.cycles);
#ifdef TRACE
	if (sys->trace) {
		dump(sys);
	}
#endif // TRACE
	return rc;
}
