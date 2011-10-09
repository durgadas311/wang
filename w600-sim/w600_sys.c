// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_sys.c,v 1.27 2011/10/09 15:11:26 drmiller Exp $"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>

#include "w600_sys.h"
#include "w600_exec.h"
#include "w600_gui.h"

extern int diw600(char *buf, uint64_t *t);

// allow others to access, if they know it
uint8_t __keyb[32];
int __klen;
int __keyp;

//
// NOTE: microcode image is aligned in 64-bit ints as follows:
//
// uint64_t bits: | 63...44 | 43...           ...            ...2 | 1 0 |
// ucode fields:  | x ... x | h h h | g g g | ... | e e e | f f f | x x |
//
// Use w600_ucode_t from w600_ucode.h to decode from a uint64_t.
// All microcode image files pad each instruction to 64 bits.
//

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

static void sysprinter(w600_sys_t *sys, int col, int drum) {
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

static void sysdisplay(w600_sys_t *sys, int on) {
	if (!on) {
		// fputc('\b', stdout);
		// fputc(' ', stdout);
		// fflush(stdout);
		return;
	}
	int c = ' ';
	if (sys->cpu.pe || sys->cpu.me) c = '!';
	uint8_t ds = sys->cpu.al;
	uint8_t dc = sys->cpu.mr;
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

static void syskeyboard(w600_sys_t *sys, uint16_t *kc, int ack) {
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
static uint8_t systape(w600_sys_t *sys, int wr, uint8_t nibble) {
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
			if (byte == 0x9e) { // End Prog
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

static void syscn24(w600_sys_t *sys, uint8_t c, uint8_t sts) {
	if (sts != 1) {
		fprintf(stderr, "XH/XL = %02x [%d]\n", c, sts);
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

static int intr(w600_sys_t *sys, int sig) {
	if (sig == SIGINT && sys->run) {
		fflush(sys->trc_fp);
		sys->run = 0;
		return 0;
	}
	return 1;
}

static void dump(w600_sys_t *sys) {
	char buf[1024];
	fprintf(stderr, "at cycle %lld\n", sys->cpu.cycles);

	// todo: call w600_sim_cmd.c:_dump()...
	uint64_t *pc = &sys->ucode[sys->cpu.pc];

	diw600(buf, pc);
	fprintf(stderr, "PC = %03x [ %s ]\n", sys->cpu.pc, buf);
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
	fprintf(stderr, "AH = %01x AM = %01x AL = %01x MR = %01x\n",
				sys->cpu.ah, sys->cpu.am, sys->cpu.al, sys->cpu.mr);
	fprintf(stderr, "ACC = %01x Z = %d I = %d C = %d\n",
				sys->cpu.ms, sys->cpu.z, sys->cpu.i, sys->cpu.c);
	fprintf(stderr, "DH = %01x DL = %01x XH = %01x XL = %01x XR = %01x\n",
			sys->cpu.dh, sys->cpu.dl, sys->cpu.xh, sys->cpu.xl, sys->cpu.xr);
	// more...
}

#if 0
static void iofault(void *v, uint8_t port) {
	w600_sys_t *sys = (w600_sys_t *)v;
	fprintf(stderr, "I/O Fault at port %01x\n", port);
	dump(sys);
	exit(1);
}

static void segfault(void *v, uint16_t adr) {
	w600_sys_t *sys = (w600_sys_t *)v;
	fprintf(stderr, "Seg Fault at address %03x\n", adr);
	dump(sys);
	exit(1);
}
#endif

static void sysfault(w600_sys_t *sys, const char *str) {
	fprintf(stderr, "%s\n", str);
	dump(sys);
	exit(1);
}

void sys_init(w600_sys_t *sys) {
	memset(pr_buf, ' ', sizeof(pr_buf));
	memset(sys, 0, sizeof(*sys));
	sys->fault = sysfault;
	sys->display = sysdisplay;
	sys->keyboard = syskeyboard;
	sys->printer = sysprinter;
	sys->tape = systape;
	sys->dev = syscn24;
	//cpu_init(&sys->cpu);
	sys->cpu.cylimit = (uint64_t)-1;

	// need to get initial values from "keyboard"...
	sys->cpu.mode0 = 0;
	sys->cpu.mode1 = MODE1_DEGREES;	// keyboard default... ?

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

void sys_start(w600_sys_t *sys, int ops) {
	if (ops & SYS_START_GUI) {
		int rc = start_fe(sys);
		if (rc) {
			fprintf(stderr, "GUI startup failed, reverting to stdio\n");
		}
	}
	if (ops & SYS_BACK_END) {
		setup_fe(sys, ops);
	} else {
		printf("Wang 600 Programmable Calculator\n");
	}
}

void sys_stop(w600_sys_t *sys, int ops) {
	stop_fe(sys);
}

static int sys_interact(w600_sys_t *sys) {
	extern int sys_command(w600_sys_t *sys);
	int rc;

	printf("break at %03x\n", sys->cpu.pc);
	do {
		rc = sys_command(sys);
	} while (rc == 0 && !sys->run);
	return rc;
}

static void __load_mem(uint8_t *mem, char *file) {
	int fd;
	uint32_t max = 2 * 1024;

	fd = open(file, O_RDONLY);
	if (fd < 0) {
		perror(file);
		exit(1);
	}
	int rc = read(fd, mem, max);
	if (rc < 0) {
		perror(file);
		exit(1);
	}
	close(fd);
}

void sys_setcass(w600_sys_t *sys, char *cass) {
	_cass_file = cass;
}

// NOTE: program steps are reverse order from registers.
// F6F is program step 000, but is register 15 06.
// Register 01 00 is the last program steps (1840..1847).
// F6F is hi nibble of byte, F6E is lo nibble of byte.
// The above addresses are NIBBLE addresses, as used by the
// Wang. Byte offsets in file are >> 1.
void sys_loadrom(w600_sys_t *sys, char *rom) {
	uint8_t *mem = sys->rom;
	__load_mem(mem, rom);
}
void sys_loadram(w600_sys_t *sys, char *ram) {
	uint8_t *mem = sys->ram;
	__load_mem(mem, ram);
}

// ! This loads a microcode image!
void sys_loaducode(w600_sys_t *sys, char *exe, uint16_t adr, uint16_t entry) {
	int fd;
	uint32_t max = 2 * 1024;
	adr &= 0x0fff;
	entry &= 0x0fff;
	int len = (max - adr) * sizeof(sys->ucode[0]);

	fd = open(exe, O_RDONLY);
	if (fd < 0) {
		perror(exe);
		exit(1);
	}
	uint64_t *m = &sys->ucode[adr];
	int rc = read(fd, m, len);
	if (rc < 0) {
		perror(exe);
		exit(1);
	}
	close(fd);
}

// need to load *backwards* since program steps advance backwards in RAM...
void sys_loadpgm(w600_sys_t *sys, char *pgm) {
	int fd;
	uint8_t *buf;
	struct stat stb;

	fd = open(pgm, O_RDONLY);
	if (fd < 0) {
		perror(pgm);
		exit(1);
	}
	fstat(fd, &stb);
	buf = malloc(stb.st_size);
	if (!buf) {
		fprintf(stderr, "unable to malloc %ld bytes for \"%s\"\n", stb.st_size, pgm);
		exit(1);
	}
	int rc = read(fd, buf, stb.st_size);
	if (rc < 0) {
		perror(pgm);
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

static int run_some(w600_sys_t *sys, uint16_t entry) {
	int rc;
	sys->cpu.pc = entry;
	do {
		while (!sys->run) {
			rc = sys_interact(sys);
			if (rc) return rc;
		}
		sys_exec(sys); // single-step
		if (sys->cpu.cycles >= sys->cpu.cylimit) {
			sys->cpu.cylimit = (uint64_t)-1;
			sys->run = 0;
		}
	} while (sys->run || sys->cmd);
	return 0;
}


int sys_go(w600_sys_t *sys, uint16_t entry) {
	entry &= 0x0fff;
	int rc = run_some(sys, entry);
	printf("exit at cycle %lld\n", sys->cpu.cycles);
#ifdef TRACE
	if (sys->trace) {
		dump(sys);
	}
#endif // TRACE
	return rc;
}
