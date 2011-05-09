// $Id: w600_sys.c,v 1.16 2011/05/09 21:53:12 drmiller Exp $

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
	int x, c;

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

static void syskeyboard(w600_sys_t *sys, uint8_t *kc) {
	if (__klen && !sys->cpu.kp) {
		--__klen;
		*kc = __keyb[__keyp];
		++__keyp;
		sys->cpu.kp = 1;
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
		if (_cass_fd >= 0) {
			_cass_pos = lseek(_cass_fd, 0L, SEEK_CUR);
fprintf(stderr, "cassette position = %04lx\n", _cass_pos);
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
	} else {
		bc ^= 1;
		if (bc) {
			byte = 0;
			rc = read(_cass_fd, &byte, 1);
			if (rc < 0) {
				perror(_cass_file);
				return 0xff;
			}
			if (rc == 0) {
				bc = 0;
				return 0xff;
			}
			return (byte >> 4);
		} else {
			return (byte & 0x0f);
		}
	}
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
	char *s = buf;
	fprintf(stderr, "at cycle %lld\n", sys->cpu.cycles);

	// todo: call w600_sim_cmd.c:_dump()...
	uint64_t *pc = &sys->ucode[sys->cpu.pc];

	diw600(buf, pc);
	fprintf(stderr, "PC = %03x [ %s ]\n", sys->cpu.pc, buf);
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
	fprintf(stderr, "AH = %01x AM = %01x AL = %01x MR = %01x\n",
				sys->cpu.ah, sys->cpu.am, sys->cpu.al, sys->cpu.mr);
	fprintf(stderr, "ACC = %01x Z = %d I = %d C = %d\n",
				sys->cpu.acc, sys->cpu.z, sys->cpu.i, sys->cpu.c);
	fprintf(stderr, "DH = %01x DL = %01x XH = %01x XL = %01x XR = %01x\n",
			sys->cpu.dh, sys->cpu.dl, sys->cpu.xh, sys->cpu.xl, sys->cpu.xr);
	// more...
}

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

static void sysfault(w600_sys_t *sys, const char *str) {
	fprintf(stderr, "%s\n", str);
	dump(sys);
	exit(1);
}

void sys_init(w600_sys_t *sys) {
	printf("Wang 600 Programmable Calculator\n");
	memset(pr_buf, ' ', sizeof(pr_buf));
	memset(sys, 0, sizeof(*sys));
	sys->fault = sysfault;
	sys->display = sysdisplay;
	sys->keyboard = syskeyboard;
	sys->printer = sysprinter;
	sys->tape = systape;
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
	if (fd < 0) {
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
void sys_loadpgm(w600_sys_t *sys, char *exe, uint16_t adr, uint16_t entry) {
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
	close(fd);
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
