// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_decode.c,v 1.53 2011/11/14 17:18:10 drmiller Exp $"

#include <unistd.h>
#include <time.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "wang-sim.h"
#include "w600_ucode.h"

#ifdef TRACE
extern int diwang(char *buf, uint64_t *t);
#endif // TRACE

extern uint16_t ram_mask;

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

uint16_t trc_adr = 0xff0;

static char *get_psw_str(wang_sys_t *sys) { 
	static char buf[32];
	char *s = buf;

	if (sys->cpu.zo) *s++ = 'Z';
	else *s++ = 'z';
	if (sys->cpu.cc) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.sc) *s++ = 'C';
	else *s++ = 'c';

	*s = '\0'; 
	return buf;
}

static char *get_mach_str(wang_sys_t *sys) { 
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%01x", sys->cpu.d1);
	s += sprintf(s, "|mode1=%01x", sys->cpu.d2);
	if (sys->cpu.ind.ind.ov) s += sprintf(s, "|Prog Err");
	if (sys->cpu.ind.ind.err) s += sprintf(s, "|Mach Err");
	if (sys->cpu.kbd) s += sprintf(s, "|Key Pressed");

	*s = '\0'; 
	return buf;
}

static void get_reg_str(wang_sys_t *sys, char *buf) {
	char *s = buf;

	s += sprintf(s, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
	s += sprintf(s, "T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
			sys->cpu.t, sys->cpu.u, sys->cpu.v, sys->cpu.ca, sys->cpu.cb);
	s += sprintf(s, "S = %01x Zo = %d CC = %d SC = %d\n",
			sys->cpu.s, sys->cpu.zo, sys->cpu.cc, sys->cpu.sc);
	s += sprintf(s, "KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
			sys->cpu.ka, sys->cpu.kb, sys->cpu.gioa, sys->cpu.giob, sys->cpu.iob);
}

struct ucode_ovr_s {
	uint16_t adr;
	union ucode_ovr_u {
		uint64_t word;
		w600_ucode_t flds;
	} instr[SYS_MODEL_NUM];
};      
static struct ucode_ovr_s ucode_ovr[] = {
	{ 0x008, {
[SYS_MODEL600_2TP]  = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk =  3, .ovr = 1 }},
[SYS_MODEL600_6TP]  = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk =  7, .ovr = 1 }},
[SYS_MODEL600_14TP] = { .flds = {.bi = 1, .zo = 6, .jl = 7, .kk = 15, .ovr = 1 }}
	}}
};
#define NUM_UCODE_OVR (sizeof(ucode_ovr) / sizeof(ucode_ovr[0]))

static void ucode_override(wang_sys_t *sys) {
	int x;
	/*
	 * overidden instructions were done as ucode was executed,
	 * but rather than searching table on every instruction
	 * we just patch our local copy of the ucode now.
	 */     
	int model = (sys->ops & SYS_MODEL_MASK) >> SYS_MODEL_SHIFT;
	for (x = 0; x < NUM_UCODE_OVR; ++x) {
		union ucode_ovr_u u;
		u.word = ucode_ovr[x].instr[model].word;
		if (u.flds.ovr != 0) {
			sys->ucode[ucode_ovr[x].adr] = u.word;
		}
	}
	/* now get RAM address mask... */
	w600_ucode_t *u = (w600_ucode_t *)&sys->ucode[0x008];
	ram_mask = (u->kk << 8) | 0x0ff;
}

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
	if (sys->cpu.ind.ind.ov || sys->cpu.ind.ind.err) c = '!';
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

static int special_key(wang_sys_t *sys, uint16_t b) {
	switch(b >> 8) {
	case 2: // mode0 switches changed
		// FE gave us complete mode word... just update
		sys->cpu.d1 = b & 0x0f;
		break;
	case 3: // mode1 switches changed
		// DEG/RAD is inverted...
		b ^= D20_DEGREES; 
		sys->cpu.d2 = b & 0x0f;
		break;
	default:
		return -1;
		break;
	}
	return 0;
}

void w600_init(wang_sys_t *sys) {
	sys->cpu.d1 = 0;
	sys->cpu.d2 = D20_DEGREES;      // keyboard default... ?
	sys->get_psw_str = get_psw_str;
	sys->get_mach_str = get_mach_str;
	sys->ucode_override = ucode_override;
	sys->special_key = special_key;

	sys->display = sysdisplay;
	memset(pr_buf, ' ', sizeof(pr_buf));
	sys->printer = sysprinter;
	sys->tape = systape;
}

static uint8_t add3_i(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t sub3_i(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a - b - c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t and2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a & b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t or2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a | b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t xor2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a ^ b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t add3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t sub3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = sub3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t odd_parity[16] = {
[0x0] = 1,
[0x1] = 0,
[0x2] = 0,
[0x3] = 1,
[0x4] = 0,
[0x5] = 1,
[0x6] = 1,
[0x7] = 0,
[0x8] = 0,
[0x9] = 1,
[0xa] = 1,
[0xb] = 0,
[0xc] = 1,
[0xd] = 0,
[0xe] = 0,
[0xf] = 1,
};

static void tape_write(wang_sys_t *sys, int dat) {
	static uint8_t last = 0;
	static uint8_t data = 0;
	static int bitc = 0;
	static int sigc = 0;

	last <<= 1;
	last |= dat;
	sigc ^= 1;
	if (sigc) return;
	uint8_t bit = 0; 
	uint8_t h = (last & 0x03);
	if (h == 0x02 || h == 0x01) bit = 1;
	if (++bitc == 5) {
		(void)sys->tape(sys, 1, data);
		if (odd_parity[data] != bit) {
			fprintf(stderr, "parity error in tape write %x %d [%02x]\n", data, bit, last);
		}
		data = 0;
		bitc = 0;
	} else {
		data <<= 1;
		data |= bit;
	}
}

static int tape_read(wang_sys_t *sys) {
	static uint8_t last = 0;
	static uint8_t data = 0;
	static int bitc = 0;
	static int sigc = 0;
	static uint64_t repc = 0;
	static uint8_t bit = 0;
	uint8_t nib;

	// wait for TD 0->1
	// delay 56 cycles
	// wait 220 cycles (sample TD for end of loop)
	// [15,15,6] ^= DL	; compute parity?
	// CY = 0 - DL		; CY = bit0
	// [15,15,5] <<= 1	; make space
	// [15,15,5] += CY	; insert new bit
	// ACC += 1		; count bits
	// wait up to 256 cycles for TD 0->1
	//         __    __
	// "1" = _|  |__|  |_
	//         __
	// "0" = _|  |_______
	//
	if (sys == NULL) {
		bit = 0;
		last = 0;
		sigc = 0;
		bitc = 0;
		repc = 0;
		return 0;	// don't care...
	}

	if (sys->cpu.sys.cycles < repc) {
reps:
		return bit;
	}
	if (sigc) {
sigs:
		--sigc;
		bit = last & 1;
		last >>= 1;
		repc = sys->cpu.sys.cycles + 97;	// very sensitive...
		goto reps;
	}
	if (bitc) {
bits:
		--bitc;
		data <<= 1;
		if (data & 0x20) {
			last = 0x05;	// lsb first out...
		} else {
			last = 0x01;	// lsb first out...
		}
		sigc = 4;
		goto sigs;

	}
	nib = sys->tape(sys, 0, 0);
	if (nib == 0xff) { // EOF
		repc = sys->cpu.sys.cycles + 700;	// expects at least 650?
		bit = 0;
		goto reps;
	}
	data = (nib << 1) | odd_parity[nib];
	bitc = 5;
	goto bits;
}

static void tape_on(wang_sys_t *sys, int wr) {
	(void)sys->tape(sys, wr, 0x40); // i.e. open file...
	if (!wr) {
		tape_read(NULL);
	}
}

static void tape_off(wang_sys_t *sys) {
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
}

static void dev_out(wang_sys_t *sys) {
	uint8_t c = (sys->cpu.ka << 4) | sys->cpu.kb;
//fprintf(stderr, "DEV> %02x %x\n", c, sys->cpu.iob);
	sys->dev(sys, c, sys->cpu.iob);
}

static uint8_t pr_drum = 0;
static uint32_t pr_hammers = 0;
static uint8_t pr_tach = 0;
static int pr_col = 0;

static void printer_status(wang_sys_t *sys) {
	// we don't want to do this unless it is really the
	// drum printer we're looking at... can't tell?
	if ((sys->cpu.d2 & D21_PRT_ON) == 0) {
		// only if running program doesn't get here...
		// printer is off, tach will never pulse, so don't spin
		if (sys->cpu.sys.pc == 0x6db) {
			sys->keyboard(sys, NULL, 0); // sleep until key event
		}
		return;
	}
	if (pr_tach) {
		pr_col = 0;
		pr_drum = (pr_drum + 1) & 0x0f;
		pr_hammers = 0;
	}
	pr_tach ^= 0x08;
	sys->cpu.ka = pr_drum;
	sys->cpu.kb = pr_tach;
}

static void printer_hammers(wang_sys_t *sys) {
	int x;
	uint32_t h;

	pr_hammers <<= 1;
	pr_hammers &= 0x0fffff;
	pr_hammers |= sys->cpu.kb & 1;
	if (++pr_col >= 20) {
		h = pr_hammers;
		for (x = 0; h; ++x) {
			if (h & 1) {
				sys->printer(sys, x, pr_drum);
			}
			h >>= 1;
		}
		pr_col = 0;
	}
}

static void printer_feed(wang_sys_t *sys) {
	// now, actually print it...
	sys->printer(sys, -1, 0);
}

static void rd_ram_i(wang_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	adr &= ram_mask;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->cpu.rb = sys->cpu.ca = b;
	b = sys->rom[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->cpu.cb = b;
}

static void wr_ram_i(wang_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	adr &= ram_mask;
	uint8_t a = sys->cpu.ca;
	uint8_t b = sys->ram[adr >> 1];
	uint8_t c = a;
	uint8_t d;
	if (adr & 1) {
		a <<= 4;
		d = (b >> 4) & 0x0f;
		b &= 0x0f;
	} else {
		d = b & 0x0f;
		b &= 0xf0;
	}
	sys->ram[adr >> 1] = b | a;
	if (__keytrc && adr == 0xff8) {
		b = sys->ram[0xff8 >> 1];
		a = sys->ram[0xff7 >> 1];
		fprintf(stderr, "Code %02d %02d\n", (a >> 4) & 0x0f, b & 0x0f);
	}
	if ((adr & 0xff0) == trc_adr) {
		if (__systrc[adr & 0x00f]) {
			fprintf(stderr, "[%03x] %x -> %x\n", adr, d, c);
		}
	}
}

static void instr_trace(wang_sys_t *sys) {
	uint64_t *m;
	char buf[128];
	m = &sys->ucode[sys->cpu.sys.pc];
	diwang(buf, m);
#ifdef TRACE_RAW_UCODE
	w600_ucode_t *u = (w600_ucode_t *)(m);
#endif // TRACE_RAW_UCODE
	fprintf(sys->trc_fp, "TRACE: %03x: "
		"[%03x %03x %03x] %01x %01x %01x %01x "
		"[%s] %01x %01x %01x : "
#ifdef TRACE_RAW_UCODE
		"[%x%x%x%x%x%x%x%x%x%x%03x%x%x] "
#endif // TRACE_RAW_UCODE
		"%s\n",
		sys->cpu.sys.pc,
		sys->cpu.sys.next,
		sys->cpu.stk1,
		sys->cpu.stk2,
		sys->cpu.t,
		sys->cpu.u,
		sys->cpu.v,
		sys->cpu.ca,
		get_psw_str(sys),
		sys->cpu.s,
		sys->cpu.ka,
		sys->cpu.kb,
#ifdef TRACE_RAW_UCODE
		u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->mop, u->kk, u->st,
		u->jc, u->jad << 2, u->jh, u->jl,
#endif // TRACE_RAW_UCODE
		buf);
}

static inline void display_check(wang_sys_t *sys) {
if (sys->cpu.sys.pc == 0x252) {
}
	// 51c: begin display-refresh delay loop... short-cut to 51f...
	if (sys->cpu.sys.pc == 0x51c) {	// display refresh routine...
		sys->cpu.sys.next = 0x51f;	// update some regs too?
		sys->cpu.sys.cycles += 272;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: 51c: Display Warp... %lld\n",
									sys->cpu.sys.cycles);
		}
		sys->display(sys, 1);	// might sleep
	// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
	} else if (sys->cpu.sys.pc == 0x5c0) {	// alpha-stop refresh routine...
		sys->cpu.sys.next = 0x5c3;
		sys->cpu.sys.cycles += 272;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: 5c0: Alpha-Stop Warp... %lld\n",
									sys->cpu.sys.cycles);
		}
		sys->display(sys, -1);	// must not sleep!
	} else if (sys->cpu.sys.pc == 0x5c6) {	// alpha-stop done... "return"...
		if (sys->cpu.sys.next == 0x27b) { // alpha-stop in running program...
			// observed 211975 cycles or about 0.53 second
			static struct timespec alpha_stop = {
				0, 529937500L
			};
			if (sys->trace) {
				fprintf(sys->trc_fp, "TRACE: %03x: "
						"Alpha-Stop Sleep... %lld\n",
						sys->cpu.sys.pc, sys->cpu.sys.cycles);
			}
			// todo: should not sleep if key pressed - e.g. PRIME
			nanosleep(&alpha_stop, NULL);
			// sleep(1);
		}
		sys->display(sys, 0);
	}
}

int instr_exec(wang_sys_t *sys) {
	w600_ucode_t *u = (w600_ucode_t *)&sys->ucode[sys->cpu.sys.pc];
	uint16_t next;
	int rc = 0;
	static uint16_t key = 0;

	// F==7 && J==0:
	//	PC <= STK1, STK1 <= PC, STK2 <= STK1
	//
	// F==7 && J==1:
	//	PC <= STK1, STK1 <= STK2, STK2 <= STK1
	//
	// F!=7 && J==0:
	//	PC <= NEXT**
	//
	// F!=7 && J==1:
	//	STK2 = STK1, STK1 <= PC, PC <= NEXT**
	//
	// For conditional jump/call, these bits are latched early...
	uint8_t br_acc = sys->cpu.s;
	uint8_t br_c = sys->cpu.sc;
	uint8_t br_k = u->kk;
#ifdef COVERAGE
	if (cov[sys->cpu.sys.pc] < 255) ++cov[sys->cpu.sys.pc];
#endif // COVERAGE
	int opf7 = (u->jl == 7);
	if (opf7) {
		next = sys->cpu.stk1 | 1;
		if (u->jc) {
			sys->cpu.stk1 = sys->cpu.stk2;
		} else {
			sys->cpu.stk1 = sys->cpu.stk2; // bugfix?
			//sys->cpu.stk1 = sys->cpu.pc;	// bad?
			// rc = 1;
		}
	} else {
		next = u->jad << 2;
	}

	if (u->mop >= 1 && u->mop <= 6) {
		sys->cpu.l = sys->cpu.t;
		sys->cpu.m = sys->cpu.u;
		sys->cpu.n = sys->cpu.v;
	}

	uint8_t g = 0, h = 0;
	switch(u->ai) {
	case 0: h = sys->cpu.s; break;
	case 1: h = sys->cpu.t; break;
	case 2: h = sys->cpu.u; break;
	case 3: h = sys->cpu.v; break;
	case 4: h = sys->cpu.ka; break;
	case 5: h = sys->cpu.kb; break;
	case 6: h = sys->cpu.ca; break;
	case 7: h = sys->cpu.cb; break;
	}

	switch(u->bi) {
	case 0: g = 0; break;
	case 1: g = br_k; break;
	case 2:
		g = sys->cpu.d1;
		sys->cpu.d1 &= ~D13_STEP;
		break;
	case 3: g = sys->cpu.d2; break;
	case 4: g = sys->cpu.ka; break;
	case 5: g = sys->cpu.kb; break;
	case 6: g = sys->cpu.ca; break;
	case 7: g = sys->cpu.cb; break;
	}

	uint8_t alu = 0;

	if (!u->ac) h = 0; // "15"? "0"? ???
	switch (u->aop) {
	case 0:
		if (u->bc) alu = sub3_i(sys, h, g, 0);
		else alu = add3_i(sys, h, g, 0);
		break;
	case 1:
		if (u->bc) alu = sub3_i(sys, h, g, 1);
		else alu = add3_i(sys, h, g, 1);
		break;
	case 2:
		if (u->bc) alu = sub3_c(sys, h, g, 0);
		else alu = add3_c(sys, h, g, 0);
		break;
	case 3:
		if (u->bc) alu = sub3_c(sys, h, g, sys->cpu.sc);
		else alu = add3_c(sys, h, g, sys->cpu.sc);
		break;
	case 4:
		if (u->bc) alu = sub3_c(sys, h, g, 1);
		else alu = add3_c(sys, h, g, 1);
		break;
	case 5:
		alu = and2(sys, h, g);
		break;
	case 6:
		if (u->bc) alu = xor2(sys, h, g);
		else alu = or2(sys, h, g);
		break;
	case 7:
		// alu = 0;
		break;
	}

	switch(u->zo) {
	case 0:	if (u->st == 15) sys->cpu.s = alu; break;
	case 1:	sys->cpu.t = alu; break;
	case 2:	sys->cpu.u = alu; break;
	case 3:	sys->cpu.v = alu; break;
	case 4:	sys->cpu.ka = alu; break;
	case 5:	sys->cpu.kb = alu; break;
	case 6:	sys->cpu.ca = alu; break;
	}

	switch(u->st) {
	case 0:
		// nop
		break;
	case 1:
		sys->cpu.s |= 1;
		break;
	case 2:
		sys->cpu.s |= 2;
		break;
	case 3:
		sys->cpu.s |= 4;
		break;
	case 4:
		sys->cpu.s |= 8;
		break;
	case 5:
		sys->cpu.s &= ~1;
		break;
	case 6:
		sys->cpu.s &= ~2;
		break;
	case 7:
		sys->cpu.s &= ~4;
		break;
	case 8:
		sys->cpu.s &= ~8;
		break;
	case 9:
		// T.B.D. reset 6184...
//fprintf(stderr, "%03x: res (%04x)\n", sys->cpu.sys.pc, key);
		sys->cpu.kbd = 0;
		break;
	case 10:
		sys->cpu.s = (sys->cpu.s & 0x0e) | (sys->cpu.zo ^ 1);
		break;
	case 11:
		sys->cpu.s = (sys->cpu.s & 0x0d) | (sys->cpu.zo << 1);
		break;
	case 12:
		sys->cpu.ind.ind.ov = 1;
		sys->display(sys, -2);
		break;
	case 13:
		sys->cpu.s = 0;
		break;
	case 14:
		sys->cpu.ind.ind.err = 1;
		sys->display(sys, -2);
		break;
	}

	switch(u->mop) {
	case 1:	wr_ram_i(sys, sys->cpu.l, sys->cpu.m, sys->cpu.n); break;
	case 2:	wr_ram_i(sys, 15, br_k, sys->cpu.n); break;
	case 3:	wr_ram_i(sys, 15, 15, br_k); break;
	case 4:	rd_ram_i(sys, sys->cpu.l, sys->cpu.m, sys->cpu.n); break;
	case 5:	rd_ram_i(sys, 15, br_k, sys->cpu.n); break;
	case 6:	rd_ram_i(sys, 15, 15, br_k); break;
	case 7:	printer_hammers(sys); break;
	case 8:	printer_feed(sys); break;
	case 9:	rc = 2; break;
	case 10:
		sys->cpu.kb = (sys->cpu.kb & ~1) | tape_read(sys);
		break;
	case 11:
		tape_write(sys, sys->cpu.kb & 1);
		break;
	case 12:
		printer_status(sys);
		// not just printer, but CN-24 as well...
		sys->cpu.kb |= 2;
		break;
	case 13:
		tape_on(sys, u->bi & 1);
		break;
	case 14:
		tape_off(sys); // u->bi & 1 affects this...
		break;
	case 15:
		sys->cpu.gioa = sys->cpu.ka;	// sys->cpu.gioa = g;
		sys->cpu.giob = sys->cpu.kb;	// sys->cpu.giob = h;
		sys->cpu.iob = br_k & 0x07;
		dev_out(sys);
		break;
	}

	// This is done "late" to ensure we use most recent flags for I and Z
	if (!opf7) {
		if (u->jc) {
			sys->cpu.stk2 = sys->cpu.stk1;
			sys->cpu.stk1 = sys->cpu.sys.pc;
		}
		switch(u->jh) {
		case 0: next |= (0 << 1); break;
		case 1: next |= (1 << 1); break;
		case 2: next |= ((br_acc & 2) >> 0); break;
		case 3: next |= ((br_acc & 8) >> 2); break;
		case 4:
			next |= (sys->cpu.ind.ind.ov << 1);
//fprintf(stderr,"%03x: chk pe\n", sys->cpu.sys.pc);
			sys->cpu.ind.ind.ov = 0;
			sys->display(sys, -2);
			break;
		case 5: next |= (sys->cpu.cc << 1); break;
		case 6:
			// todo: clean this up!
			if (key) {
//fprintf(stderr,"%03x: pop %04x\n", sys->cpu.sys.pc, key);
//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
				sys->cpu.kbd = 1;
				sys->cpu.ka = (key >> 4) & 0x0f;
				sys->cpu.kb = key & 0x0f;
				sys->keyboard(sys, &key, 1);
			}
			next |= (sys->cpu.kbd << 1);
			if (sys->cpu.kbd) {
				sys->cpu.kbd = 0;
				sys->display(sys, 0);
			}
			break;
		case 7: rc = 3; break;
		}
		switch(u->jl) {
		case 0: next |= (0 << 0); break;
		case 1: next |= (1 << 0); break;
		case 2: next |= ((br_acc & 1) >> 0); break;
		case 3: next |= ((br_acc & 4) >> 2); break;
		case 4: next |= (sys->cpu.zo << 0); break;
		case 5: rc = 4; break;
		case 6: next |= (br_c << 0); break;
		case 7: rc = 5; break;
		}
	}

	++sys->cpu.sys.cycles;
	sys->cpu.sys.next = next;
#ifdef TRACE
	if (sys->trace) {
		instr_trace(sys);
	}
#endif // TRACE
	// the following are called in specific order...
	// keyboard injection of next pc must override all, so is last.

	display_check(sys);	// this might sleep until UI event...

	sys->keyboard(sys, &key, 0);

	if (sys->cpu.sys.jam) {
		sys->cpu.sys.next = sys->cpu.sys.jam & 0x0fff;
		sys->cpu.sys.jam = 0;
		sys->cpu.ind.ind.ov = 0;
		if (sys->cpu.sys.next == 0) { // PRIME
			sys->cpu.ind.ind.err = 0;
		}
		sys->display(sys, -2);
	}

	sys->cpu.sys.pc = sys->cpu.sys.next;
	return rc;
}
