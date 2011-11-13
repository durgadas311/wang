// Copyright (c) 2011 Douglas Miller

#ident "$Id: wang_sys.c,v 1.8 2011/11/13 04:10:34 drmiller Exp $"

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
extern int sys_ops;

char peer_name[256] = {"unknown"};
static time_t peer_start;

extern int diwang(char *buf, uint64_t *t);

// allow others to access, if they know it
uint8_t __keyb[32];
int __klen;
int __keyp;

static char err_buf[1024];
static void fatal_error(const char *s, const char *e) {
	if ((sys_ops & SYS_WEB_BACKEND) != 0) {
		uint16_t b = 0xf000;
		write(2, &b, sizeof(b));
	}
	if (e) {
		fprintf(stderr, "%s: %s\n", s, e);
	} else {
		fprintf(stderr, "%s\n", s);
	}
}

#ifdef __wang600__
static char pr_ovfl[16] = { "....OVERFLOW...." };
static char pr_0_15[16] = { "0123456789.o\0+- " };
static char *pr_16_20[5][16] = {
[0][0] =  " E ",
[0][1] =  " T ",
[0][2] =  " + ",
[0][3] =  " - ",
[0][4] =  " x ",
[0][5] =  " / ",
[0][6] =  " ST",
[0][7] =  " RE",
[0][8] =  " * ",
[0][9] =  " * ",
[0][10] = " f ",
[0][11] = " F ",
[0][12] = " A ",
[0][13] = " B ",
[0][14] = " C ",
[0][15] = " D ",

[1][0] =  "0  ",
[1][1] =  "1  ",
[1][2] =  "2  ",
[1][3] =  "3  ",
[1][4] =  "4  ",
[1][5] =  "5  ",
[1][6] =  "6  ",
[1][7] =  "7  ",
[1][8] =  "8  ",
[1][9] =  "9  ",
[1][10] = "10 ",
[1][11] = "11 ",
[1][12] = "12 ",
[1][13] = "13 ",
[1][14] = "14 ",
[1][15] = "15 ",

[2][0] =  " S ",
[2][1] =  " RE",
[2][2] =  " W ",
[2][3] =  " Go",
[2][4] =  " Jo",
[2][5] =  " J+",
[2][6] =  " SN",
[2][7] =  " CS",
[2][8] =  " TN",
[2][9] =  " RD",
[2][10] = " LN",
[2][11] = " eX",
[2][12] = " x2",
[2][13] = " vX",
[2][14] = " LP",
[2][15] = "1/x",

[3][0] =  " M ",
[3][1] =  " ST",
[3][2] =  " a ",
[3][3] =  " Sp",
[3][4] =  " Jn",
[3][5] =  " Je",
[3][6] =  " S1",
[3][7] =  " C1",
[3][8] =  " T1",
[3][9] =  " DR",
[3][10] = " LG",
[3][11] = "10X",
[3][12] = " I ",
[3][13] = "|x|",
[3][14] = " EP",
[3][15] = " RT",

[4][0] =  " X ",
[4][1] =  " Y ",
[4][2] =  " Z ",
[4][3] =  " A ",
[4][4] =  " B ",
[4][5] =  " C ",
[4][6] =  " D ",
[4][7] =  " E ",
[4][8] =  " F ",
[4][9] =  " G ",
[4][10] = " H ",
[4][11] = " I ",
[4][12] = " J ",
[4][13] = " K ",
[4][14] = " L ",
[4][15] = " M ",

};

#define PR_NUM_COL	20
#define PR_XCOL_WID	3
#define PR_XCOL_STRT	15

static char pr_buf[128];

static void sysprinter(wang_sys_t *sys, int col, int drum) {
	char *s;
	int c;

	if (col == -1) {
		// print what we got... then reset.
		printf("%.*s\n", (PR_XCOL_STRT + PR_XCOL_WID * (PR_NUM_COL - PR_XCOL_STRT)), pr_buf);
		memset(pr_buf, ' ', sizeof(pr_buf));
	} else {
		if (col < PR_XCOL_STRT) {
			s = &pr_buf[col];
			c = pr_0_15[drum];
			if (!c) {
				c = pr_ovfl[col];
			}
			*s = c;
		} else {
			col -= PR_XCOL_STRT;
			s = &pr_buf[col * PR_XCOL_WID + PR_XCOL_STRT];
			memcpy(s, pr_16_20[col][drum], PR_XCOL_WID);
		}
	}
}

static void sysdisplay(wang_sys_t *sys, int on) {
	if (!on) {
		// fputc('\b', stdout);
		// fputc(' ', stdout);
		// fflush(stdout);
		return;
	}
	int c = ' ';
	if (sys->cpu.ov || sys->cpu.err) c = '!';
	uint8_t ds = sys->cpu.v;
	uint8_t dc = sys->cpu.ca;
	if (ds == 0) {
		fputc('\r', stdout);
		fputc(c, stdout);
	}
	if (ds == 0 || ds == 13) {
		c = "+-+-+-+-+-+-+-+ "[dc];
	} else {
		c = "0123456789.>u<L "[dc];
	}
	fputc(c, stdout);
	fflush(stdout);
}
#endif // __wang600__
#ifdef __wang700__
static void sysdisplay(wang_sys_t *sys, int on) {
	// TBD
}
#endif // __wang700__

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

char *_cass_file = "default_casette_tape.img";
int _cass_fd = -1;
off_t _cass_pos = 0;

// we get "hi" nibble first... must also send "hi" nibble first
// we use End Prog to know when to stop reading...
static uint8_t systape(wang_sys_t *sys, int wr, uint8_t nibble) {
	static uint8_t byte = 0;
	static int bc = 0;
	int rc;
	if (nibble & 0x80) {	// tape-off...
		byte = 0;
		if (_cass_fd >= 0) {
			_cass_pos = lseek(_cass_fd, 0L, SEEK_CUR);
			close(_cass_fd);
			_cass_fd = -1;
		}
		bc = 0;
		return 0;
	}
	if (nibble & 0x40) {	// tape-on...
		if (_cass_fd >= 0) return 0;
		if (wr) {
			_cass_fd = open(_cass_file, O_RDWR | O_CREAT, 0666);
		} else {
			_cass_fd = open(_cass_file, O_RDONLY);
		}
		if (_cass_fd < 0) {
			perror(_cass_file);
			return 0;
		}
		lseek(_cass_fd, _cass_pos, SEEK_SET);
		bc = 0;
		byte = 0;
		return 0;
	}
	if (wr) {
		bc ^= 1;
		if (bc) {
			byte = (byte & 0x0f) | (nibble << 4);
		} else {
			byte = (byte & 0xf0) | nibble;
			rc = write(_cass_fd, &byte, 1);
			if (rc < 0) {
				perror(_cass_file);
			}
		}
		return 0;
	} else {
		if (!bc) {
			if (byte == WANG_END_PROG) {
				return 0xff;
			}
			byte = 0;
			rc = read(_cass_fd, &byte, 1);
			if (rc < 0) {
				perror(_cass_file);
				return 0xff;
			}
			if (rc == 0) {
				return 0xff;
			}
			bc ^= 1;
			return (byte >> 4);
		} else {
			bc ^= 1;
			return (byte & 0x0f);
		}
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
#ifdef __wang600__
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
#endif // __wang600__
	fprintf(stderr, "T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
				sys->cpu.t, sys->cpu.u, sys->cpu.v, sys->cpu.ca, sys->cpu.cb);
#ifdef __wang600__
	fprintf(stderr, "S = %01x Zo = %d CC = %d SC = %d\n",
				sys->cpu.s, sys->cpu.zo, sys->cpu.cc, sys->cpu.sc);
#endif // __wang600__
#ifdef __wang700__
	fprintf(stderr, "S = %01x ALU = %d CC = %d SC = %d Q = %d\n",
				sys->cpu.s, sys->cpu.alu, sys->cpu.cc, sys->cpu.sc, sys->cpu.q);
#endif // __wang700__
	fprintf(stderr, "KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
			sys->cpu.ka, sys->cpu.kb, sys->cpu.gioa, sys->cpu.giob, sys->cpu.iob);
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
	fatal_error(str, NULL);
	if ((sys_ops & SYS_WEB_BACKEND) == 0) {
		dump(sys);
	}
	exit(1);
}

void sys_init(wang_sys_t *sys) {
	memset(sys, 0, sizeof(*sys));
	sys->fault = sysfault;
	sys->display = sysdisplay;
	sys->keyboard = syskeyboard;
#ifdef __wang600__
	memset(pr_buf, ' ', sizeof(pr_buf));
	sys->printer = sysprinter;
#endif // __wang600__
	sys->tape = systape;
	sys->dev = syscn24;
	//cpu_init(&sys->cpu);
	sys->cpu.sys.cylimit = (uint64_t)-1;

	// need to get initial values from "keyboard"...
#ifdef __wang600__
	sys->cpu.d1 = 0;
	sys->cpu.d2 = D20_DEGREES;	// keyboard default... ?
#endif // __wang600__
#ifdef __wang700__
	sys->cpu.d = 0;
#endif // __wang700__

	// already done by memset above...
	//memset(sys->ucode, 0, sizeof(sys->ucode));
	//memset(sys->ram, 0xff, sizeof(sys->ram));
// put special pattern in RAM for debugging...
if (0) { int x;
for (x = 0; x < sizeof(sys->ram); ++x) {
	sys->ram[x] = x & 0x0ff;
}
}

	sys->intr = intr;
	sys->trace = 0;
	sys->trc_fp = stderr;

	// now install all devices and peripherals...
}

void sys_start(wang_sys_t *sys) {
	if (sys_ops & SYS_START_GUI) {
		int rc = start_fe(sys);
		if (rc) {
			fprintf(stderr, "GUI startup failed, reverting to stdio\n");
		}
	}
	if ((sys_ops & SYS_WEB_BACKEND) != 0) {
		peer_start = time(NULL);
		openlog(WANG_SIM, LOG_PID, LOG_USER);

		struct sockaddr addr;
		socklen_t len = sizeof(addr);
		getpeername(0, &addr, &len);
		getnameinfo(&addr, len, peer_name, sizeof(peer_name), NULL, 0, NI_NUMERICHOST);

		syslog(LOG_INFO, "starting simulation for %s\n", peer_name);
	}
	if (sys_ops & SYS_BACK_END) {
		setup_fe(sys);
	} else {
		printf("Wang %d Programmable Calculator\n", WANG_SERIES);
	}
}

void sys_stop(wang_sys_t *sys) {
	stop_fe(sys);
	if ((sys_ops & SYS_WEB_BACKEND) != 0) {
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

static void __load_mem(uint8_t *mem, char *file) {
	int fd;
	uint32_t max = 2 * 1024;

	fd = open(file, O_RDONLY);
	if (fd < 0) {
		fatal_error(file, strerror(errno));
		exit(1);
	}
	int rc = read(fd, mem, max);
	if (rc < 0) {
		fatal_error(file, strerror(errno));
		exit(1);
	}
	close(fd);
}

void sys_setcass(wang_sys_t *sys, char *cass) {
	_cass_file = cass;
}

// NOTE: program steps are reverse order from registers.
// F6F is program step 000, but is register 15 06.
// Register 01 00 is the last program steps (1840..1847).
// F6F is hi nibble of byte, F6E is lo nibble of byte.
// The above addresses are NIBBLE addresses, as used by the
// Wang. Byte offsets in file are >> 1.
#if defined(__wang600__) || defined(__wang1200__)
void sys_loadrom(wang_sys_t *sys, char *rom) {
	uint8_t *mem = sys->rom;
	__load_mem(mem, rom);
}

struct ucode_ovr_s {
	uint16_t adr;
	union {
		uint64_t word;
		w600_ucode_t flds;
	} instr[SYS_MODEL_NUM];
};
static struct ucode_ovr_s ucode_ovr[] = {

#ifdef __wang600__
	{ 0x008, {
[SYS_MODEL600_2TP]  = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk =  3, .ovr = 1 }},
[SYS_MODEL600_6TP]  = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk =  7, .ovr = 1 }},
[SYS_MODEL600_14TP] = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk = 15, .ovr = 1 }}
	}}
#endif // __wang600__

#ifdef __wang1200__
// 0 x x 0 1 0 1 1 1 1 0 = 01111010xx0 = 3d0, 3d2, 3d4, 3d6
// 0000 0000 0000  0000  0000 0000 0011 0111 1111 0010 00xx
// 000 000 000 000 0 0 0000 0000 0000 1 101111111 001 000
// AI=0 BI=0 ZO=0 AOP=0 AC=0 BC=0 MOP=0 KK=0 ST=0 SUB=1 JAD=5fc JH=1 JL=0 (5fe)
	{ 0x3d0, {
[SYS_MODEL1200] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
	}}
	{ 0x3d2, {
[SYS_MODEL1200] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
	}}
	{ 0x3d4, {
[SYS_MODEL1200] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
	}}
	{ 0x3d6, {
[SYS_MODEL1200] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
	}}
#endif // __wang1200__

};
#define NUM_UCODE_OVR (sizeof(ucode_ovr) / sizeof(ucode_ovr[0]))
#endif // __wang600__ || __wang1200__

void sys_loadram(wang_sys_t *sys, char *ram) {
	uint8_t *mem = sys->ram;
	__load_mem(mem, ram);
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
		fatal_error(exe, strerror(errno));
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
			fatal_error(exe, strerror(errno));
			exit(1);
		}
	}
	close(fd);
#if defined(__wang600__) || defined(__wang1200__)
	/*
	 * overidden instructions were done as ucode was executed,
	 * but rather than searching table on every instruction
	 * we just patch our local copy of the ucode now.
	 */
	int model = (sys_ops & SYS_MODEL_MASK) >> SYS_MODEL_SHIFT;
	for (fd = 0; fd < NUM_UCODE_OVR; ++fd) {
		uint64_t u = ucode_ovr[fd].instr[model].word;
		if (u >= 0x8000000000000000) {	// u.ovr == 1
			sys->ucode[ucode_ovr[fd].adr] = u;
		}
	}
#endif // __wang600__ || __wang1200__
#ifdef __wang600__
	/* now get RAM address mask... */
	w600_ucode_t *u = (w600_ucode_t *)&sys->ucode[0x008];
	ram_mask = (u->kk << 8) | 0x0ff;
#endif // __wang600__
}

// need to load *backwards* since program steps advance backwards in RAM...
void sys_loadpgm(wang_sys_t *sys, char *pgm) {
	int fd;
	uint8_t *buf;
	struct stat stb;

	fd = open(pgm, O_RDONLY);
	if (fd < 0) {
		fatal_error(pgm, strerror(errno));
		exit(1);
	}
	fstat(fd, &stb);
	buf = malloc(stb.st_size);
	if (!buf) {
		static char buf[1024];
		sprintf(buf, "unable to malloc %ld bytes for \"%s\"\n", stb.st_size, pgm);
		fatal_error(buf, strerror(errno));
		exit(1);
	}
	int rc = read(fd, buf, stb.st_size);
	if (rc < 0) {
		fatal_error(pgm, strerror(errno));
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
