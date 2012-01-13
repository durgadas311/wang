// Copyright (c) 2011 Douglas Miller

#ident "$Id: w1200_decode.c,v 1.28 2012/01/13 17:11:40 drmiller Exp $"

#undef DOUGS_PATCHES

#include <unistd.h>
#include <time.h>
#include <string.h>

#include "wang-sim.h"
#include "w1200_ucode.h"

#ifdef TRACE
extern int diwang(char *buf, uint64_t *t);
#endif // TRACE

extern uint16_t ram_mask;

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

uint16_t trc_adr = 0x0f0;

static uint16_t key = 0;

static char *get_mach_str(wang_sys_t *sys) { 
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode1=%01x", sys->cpu.d1);
	s += sprintf(s, "|mode2=%01x", sys->cpu.d2);
	s += sprintf(s, "|mode3=%01x", sys->cpu.d3);
	// more... ?
	if (sys->cpu.kbd) s += sprintf(s, "|Key Pressed");

	*s = '\0'; 
	return buf;
}

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

static void get_reg_str(wang_sys_t *sys, char *buf) {
	char *s = buf;
	s += sprintf(s, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
	s += sprintf(s, "T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
			sys->cpu.t, sys->cpu.u, sys->cpu.v, sys->cpu.ca, sys->cpu.cb);
	s += sprintf(s, "S = %01x Zo = %d CC = %d SC = %d\n",
			sys->cpu.s, sys->cpu.zo, sys->cpu.cc, sys->cpu.sc);
	s += sprintf(s, "KA = %01x KB = %01x TO = %01x RO = %01x\n",
			sys->cpu.ka, sys->cpu.kb, sys->cpu.to, sys->cpu.ro);

}

struct ucode_ovr_s {
	uint16_t adr;
	union ucode_ovr_u {
		uint64_t word;
		w1200_ucode_t flds;
	} instr[SYS_MODEL_NUM];
};      
static struct ucode_ovr_s ucode_ovr[] = {
// 0 x x 0 1 0 1 1 1 1 0 = 01111010xx0 = 3d0, 3d2, 3d4, 3d6
// 0000 0000 0000  0000  0000 0000 0011 0111 1111 0010 00xx
// 000 000 000 000 0 0 0000 0000 0000 1 101111111 001 000
// AI=0 BI=0 ZO=0 AOP=0 AC=0 BC=0 MOP=0 KK=0 ST=0 SUB=1 JAD=5fc JH=1 JL=0 (5fe)
        { 0x3d0, {
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
        }},
// is logibloc 5293 wrong?
        { 0x3d2, {
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 0 }},
        }},
        { 0x3d4, {
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 0 }},
        }},
        { 0x3d6, {
[SYS_MODEL1220] = { .flds = {.sub = 1, .jad = 0x5fc >> 2, .jh = 1, .jl = 0, .ovr = 0 }},
        }},
#ifdef DOUGS_PATCHES
        { 0x6dd, {
[SYS_MODEL1220] = { .flds = {
//		6dd: [01301012005f804] V = S + 2 ->[Zo,CC]; mem(U,V) = CA,CB; jump 5f8[:Zo]
//		.ai=0, .bi=1, .zo=3, .aop=0, .ac=1, .bc=0,

//		6dd: [31301012005f804] V = V + 2 ->[Zo,CC]; mem(U,V) = CA,CB; jump 5f8[:Zo]
		.ai=3, .bi=1, .zo=3, .aop=0, .ac=1, .bc=0,
		.mop=1, .kk=2, .st=0,
		.sub=0, .jad=0x5f8>>2, .jh=0, .jl=4,
		.ovr = 1
		}},
        }},
        { 0x4ae, {
[SYS_MODEL1220] = { .flds = {
//		4ae: [00231067004ac11] U = S + 0 + SC ->[Zo,CC,SC]; CA,CB = mem(15,7); jump 4af
//		4ae: [20231067004ac11] U = U + 0 + SC ->[Zo,CC,SC]; CA,CB = mem(15,7); jump 4af
		.ai=2, .bi=0, .zo=2, .aop=3, .ac=1, .bc=0,
		.mop=6, .kk=7, .st=0,
		.sub=0, .jad=0x4ac>>2, .jh=1, .jl=1,
		.ovr = 1
		}},
        }},
#endif // DOUGS_PATCHES

        { 0x052, {
[SYS_MODEL1220] = { .flds = {
			.sub = 0, .jad = 0x058 >> 2, .jh = 0, .jl = 3, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.sub = 0, .jad = 0x058 >> 2, .jh = 0, .jl = 3, .ovr = 1 }},
        }},
        { 0x423, {
[SYS_MODEL1220] = { .flds = {
			.ai = 6, .bi = 1, .ac = 1, .bc = 1, .kk = 11,
			.sub = 0, .jad = 0x424 >> 2, .jh = 5, .jl = 4, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 6, .bi = 1, .ac = 1, .bc = 1, .kk = 11,
			.sub = 0, .jad = 0x424 >> 2, .jh = 5, .jl = 4, .ovr = 1 }},
        }},
        { 0x42f, {
[SYS_MODEL1220] = { .flds = {
			.ai = 6, .ac = 1, .mop = 6, .kk = 4, .st = 3,
			.sub = 0, .jad = 0x424 >> 2, .jh = 7, .jl = 4, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 6, .ac = 1, .mop = 6, .kk = 4, .st = 3,
			.sub = 0, .jad = 0x424 >> 2, .jh = 7, .jl = 4, .ovr = 1 }},
        }},
        { 0x506, {
[SYS_MODEL1220] = { .flds = {
			.zo = 7, .aop = 1, .mop = 3, .kk = 12, .st = 13,
			.sub = 1, .jad = 0x7fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.zo = 7, .aop = 1, .mop = 3, .kk = 12, .st = 13,
			.sub = 1, .jad = 0x7fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
        }},
        { 0x558, {
[SYS_MODEL1220] = { .flds = {
			.ai = 1, .bi = 6, .zo = 1, .ac = 1, .st = 13,
			.sub = 1, .jad = 0x7fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 1, .bi = 6, .zo = 1, .ac = 1, .st = 13,
			.sub = 1, .jad = 0x7fc >> 2, .jh = 1, .jl = 0, .ovr = 1 }},
        }},
        { 0x5ec, {
[SYS_MODEL1220] = { .flds = {
			.ai = 5, .bi = 1, .aop = 6, .ac = 1, .bc = 1, .kk = 11,
			.sub = 0, .jad = 0x5ec >> 2, .jh = 1, .jl = 4, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 5, .bi = 1, .aop = 6, .ac = 1, .bc = 1, .kk = 11,
			.sub = 0, .jad = 0x5ec >> 2, .jh = 1, .jl = 4, .ovr = 1 }},
        }},
        { 0x6ee, {
[SYS_MODEL1220] = { .flds = {
			.bi = 1, .zo = 7, .kk = 2,
			.sub = 0, .jad = 0x6c8 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.bi = 1, .zo = 7, .kk = 2,
			.sub = 0, .jad = 0x6c8 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
        }},
        { 0x7fe, {
[SYS_MODEL1220] = { .flds = {
			.bi = 1, .zo = 7, .aop = 6, .ac = 1, .kk = 3,
			.sub = 0, .jad = 0x6c8 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.bi = 1, .zo = 7, .aop = 6, .ac = 1, .kk = 3,
			.sub = 0, .jad = 0x6c8 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
        }},
        { 0x44f, {
[SYS_MODEL1220] = { .flds = {
			.ai = 6, .zo = 6, .aop = 3, .ac = 1, .bc = 1,
			.sub = 0, .jad = 0x450 >> 2, .jh = 0, .jl = 1, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 6, .zo = 6, .aop = 3, .ac = 1, .bc = 1,
			.sub = 0, .jad = 0x450 >> 2, .jh = 0, .jl = 1, .ovr = 1 }},
        }},
        { 0x33c, {
[SYS_MODEL1220] = { .flds = {
			.ai = 7, .bi = 7, .zo = 7, .aop = 6, .ac = 1, .mop = 10, .kk = 15, .st = 15,
			.sub = 1, .jad = 0x414 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 7, .bi = 7, .zo = 7, .aop = 6, .ac = 1, .mop = 10, .kk = 15, .st = 15,
			.sub = 1, .jad = 0x414 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
        }},
        { 0x33d, {
[SYS_MODEL1220] = { .flds = {
			.ai = 7, .bi = 7, .zo = 7, .aop = 6, .ac = 1, .mop = 10, .kk = 15, .st = 15,
			.sub = 0, .jad = 0x034 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
[SYS_MODEL1222] = { .flds = {
			.ai = 7, .bi = 7, .zo = 7, .aop = 6, .ac = 1, .mop = 10, .kk = 15, .st = 15,
			.sub = 0, .jad = 0x034 >> 2, .jh = 0, .jl = 0, .ovr = 1 }},
        }},
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
}

static int _indicators = 0;

static int special_key(wang_sys_t *sys, uint16_t b) {
	switch(b >> 8) {
	case 2: // mode0 switches changed
		sys->cpu.d1 = b & 0x0f;
		break;
	case 3: // mode1 switches changed
		sys->cpu.d2 = (sys->cpu.d2 & 0x08) | (b & 0x07);
		break;
	case 4: // special alt keys - all update lamps...
		switch(b & 0x0ff) {
		case 1:	// SKIP
			sys->cpu.ind.ind.skl ^= 1;
			sys->cpu.d2 = (sys->cpu.d2 & 0x07) | (sys->cpu.ind.ind.skl << 3);
			break;
		case 2:	// SEARCH
			sys->cpu.ind.ind.shl ^= 1;
			key = 0x100 | (sys->cpu.ind.ind.shl ? 0x42 : 0x52);
			break;
		default:
			return -1;
			break;
		}
		_indicators = 0;
		sys->display(sys, -2);
		break;
	case 5: // mode2
		sys->cpu.d3 = b & 0x0f;
		break;
	case 0x0f: // tape status change
//fprintf(stderr, "tape status %02x\n", b & 0x0ff);
		if ((b & 1) != 0) {	// RIGHT
			sys->cpu.rop = (b & 2) >> 1;
		} else {		// LEFT
			sys->cpu.lop = (b & 2) >> 1;
		}
		break;
	default:
		return -1;
		break;
	}
	return 0;
}

void w1200_init(wang_sys_t *sys) {
	sys->cpu.d1 = 0; // default?
	sys->cpu.d2 = 0; // default?
	sys->cpu.d3 = 1; // default?
	sys->get_psw_str = get_psw_str;
	sys->get_mach_str = get_mach_str;
	sys->get_reg_str = get_reg_str;
	sys->ucode_override = ucode_override;
	sys->special_key = special_key;
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

static void tape_write(wang_sys_t *sys) {
	static uint8_t last = 0;
	static uint8_t data = 0;
	static int bitc = 0;

#if 1
	if (sys == NULL) {
		if (bitc) fprintf(stderr, "tape residual (%d) 0x%02x\n",
					bitc, data << (8 - bitc));
		last = 0;
		data = 0;
		bitc = 0;
		return;
	}
	uint8_t curr = (sys->cpu.din1 << 1) | sys->cpu.din0;
	uint8_t chg = (curr ^ last);
	last = curr;
	if (chg) {
		chg -= 1;
		data <<= 1;
		data |= chg;
		if (++bitc >= 8) {
//fprintf(stderr, "tape write 0x%02x %lld\n", data, sys->cpu.sys.cycles);
			// interface requires nibbles, not bytes...
			(void)sys->tape(sys, 1, (data >> 4) & 0x0f);
			(void)sys->tape(sys, 1, data & 0x0f);
			data = 0;
			bitc = 0;
		}
	}
#else
fprintf(stderr, "tape %d %d %lld\n", sys->cpu.din0, sys->cpu.din1, sys->cpu.sys.cycles);
#endif
}

//
// Tape format:
// +-------------+------------+------------+--------------+--------------+
// | >=900cy gap | 66-bit hdr | 1600cy gap | 800-bit data | 16,000cy gap |
// +-------------+------------+------------+--------------+--------------+
//
// ">=900cy" and "16,000cy" gaps are same - each are the gaps for the next/previous block.
//
static int tape_read(wang_sys_t *sys) {
	static uint8_t lastc = 0;
	static uint8_t lastd = 0;
	static uint8_t data = 0;
	static int sigc = 0;
	static int bitc = 0;
	static int init = 0;
	static uint64_t repc = 0;
	static int chunks[5];
	static int chunk;
	static int curr;
	uint8_t nib;

#if 1
	if (sys == NULL) {
		lastc = 0;
		lastd = 0;
		sigc = 0;
		bitc = 0;
		init = 0;
		return 0;	// don't care...
	}

	if (!init) {
		if (sys->cpu.rv) { // no data just fake signals in reverse...
			chunks[0] = -900;	// gap
			chunks[1] = 800;	// bits of data
			chunks[2] = -1600;	// gap
			chunks[3] = 66;		// bits of header
			chunks[4] = -16000;	// gap
		} else {
			chunks[0] = -900;	// gap
			chunks[1] = 66;		// bits of header
			chunks[2] = -1600;	// gap
			chunks[3] = 800;	// bits of data
			chunks[4] = -16000;	// gap
		}
		chunk = 0;
		curr = 0;
		init = 1;
	}

	if (sys->cpu.right && sys->cpu.rhs == 0) {
		return 0;	// do not read tape unless read-head is engaged...
	}
	if (!sys->cpu.right && sys->cpu.lhs == 0) {
		return 0;	// do not read tape unless read-head is engaged...
	}

	if (sys->cpu.sys.cycles < repc) {
reps:
		return 0;
	}
	if (sigc) {
sigs:
		--sigc;
		sys->cpu.tck = lastc & 1;
		sys->cpu.dk = lastd & 1;
		lastc >>= 1;
		lastd >>= 1;
		repc = sys->cpu.sys.cycles + 10;	// sensitive?
		goto reps;
	}
	if (bitc) {
bits:
		--bitc;
		data <<= 1;
		if (data & 0x10) {
			lastc = 0x00;
			lastd = 0x01;
		} else {
			lastc = 0x01;
			lastd = 0x00;
		}
		sigc = 5;
		goto sigs;

	}
	if (curr) {
nibs:
		if (sys->cpu.rv) { // no data just fake signals in reverse...
			nib = 0;
		} else {
			nib = sys->tape(sys, 0, 0);
		}
		if (nib == 0xff) { // EOF
			repc = sys->cpu.sys.cycles + 900;	// 27,928cy... ?
			goto reps;
		}
		data = nib;
		bitc = 4;
		curr -= bitc;
		if (curr < 4) {
			// hack for 66-bits in 8-bytes storage...
			bitc += curr;
			curr = 0;
		}
//fprintf(stderr, "re-encoding %01x (%d)\n", data, bitc);
		goto bits;
	}
	curr = chunks[chunk];
	if (++chunk > 4) chunk = 0;	// nothing else?
	if (curr < 0) { // gap
		// leave signal unchanged...
		repc = sys->cpu.sys.cycles + -(curr);
		curr = 0;
		goto reps;
	} else {
		// curr: num bits to play back...
		goto nibs;
	}
#else
	sys->cpu.tck ^= 1;
	sys->cpu.dk ^= 1;
#endif
}

static void tape_on(wang_sys_t *sys) {
	uint8_t hi;
	if (sys->cpu.right) {
		sys->cpu.ind.ind.tmr = sys->cpu.tm;
		hi = sys->cpu.rhs;
	} else {
		sys->cpu.ind.ind.tml = sys->cpu.tm;
		hi = sys->cpu.lhs;
	}
	_indicators = 8;
	sys->cpu.tck = 0;
	sys->cpu.dk = 0;
	sys->cpu.din0 = 0;
	sys->cpu.din1 = 0;
	tape_write(NULL);
#if 1
	(void)sys->tape(sys, sys->cpu.rc, 0x40); // i.e. open file...
	if (!sys->cpu.rc) {
		tape_read(NULL);
	}
#endif
#if 1
//fprintf(stderr, "tape on %s rv=%d hi=%d hl=%d %s %lld\n",
//	sys->cpu.right ? "R" : "L",
//	sys->cpu.rv, hi, sys->cpu.hl,
//	sys->cpu.rc ? "wr" : "rd",
//	sys->cpu.sys.cycles);
#endif
}

static void tape_off(wang_sys_t *sys) {
	uint8_t hi;
	if (sys->cpu.right) {
		sys->cpu.ind.ind.tmr = sys->cpu.tm;
		hi = sys->cpu.rhs;
	} else {
		sys->cpu.ind.ind.tml = sys->cpu.tm;
		hi = sys->cpu.lhs;
	}
	_indicators = 8;
	sys->cpu.tck = 0;
	sys->cpu.dk = 0;
	sys->cpu.din0 = 0;
	sys->cpu.din1 = 0;
	tape_write(NULL);
#if 1
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
#endif
#if 1
//fprintf(stderr, "tape off %s rv=%d hi=%d hl=%d %s %lld\n",
//	sys->cpu.right ? "R" : "L",
//	sys->cpu.rv, hi, sys->cpu.hl,
//	sys->cpu.rc ? "wr" : "rd",
//	sys->cpu.sys.cycles);
#endif
}

static void dev_spc(wang_sys_t *sys, int c) {
	sys->dev(sys, c, 1);
}

static void dev_out(wang_sys_t *sys) {
	uint8_t c = (sys->cpu.to << 4) | sys->cpu.ro;
	dev_spc(sys, c);
}

static void rd_ram_i(wang_sys_t *sys) {
	uint16_t adr = (sys->cpu.m << 4) | sys->cpu.n;
	adr &= ram_mask;
	uint8_t b = sys->ram[adr];
	sys->cpu.ca = (b >> 4) & 0x0f;
	sys->cpu.cb = b & 0x0f;
}

static void wr_ram_i(wang_sys_t *sys) {
	uint16_t adr = (sys->cpu.m << 4) | sys->cpu.n;
	adr &= ram_mask;
	uint8_t b = sys->cpu.cb | (sys->cpu.ca << 4);
	uint8_t d = sys->ram[adr];
	sys->ram[adr] = b;
//if ((adr == 0x0ed || adr == 0xf5) && d != b) fprintf(stderr, "[%03x] %02x -> %02x from %03x\n", adr, d, b, sys->cpu.sys.pc);
	if ((adr & 0xff0) == trc_adr) {
		if (__systrc[adr & 0x00f]) {
			fprintf(stderr, "[%03x] %x -> %x\n", adr, d, b);
		}
	}
}

static void instr_trace(wang_sys_t *sys) {
	uint64_t *m;
	char buf[128];
	m = &sys->ucode[sys->cpu.sys.pc];
	diwang(buf, m);
#ifdef TRACE_RAW_UCODE
	w1200_ucode_t *u = (w1200_ucode_t *)(m);
#endif // TRACE_RAW_UCODE
	fprintf(sys->trc_fp, "TRACE: %03x: "
		"[%03x %03x %03x] %01x %01x %01x %01x %01x "
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
		sys->cpu.cb,
		get_psw_str(sys),
		sys->cpu.s,
		sys->cpu.ka,
		sys->cpu.kb,
#ifdef TRACE_RAW_UCODE
		u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->mop, u->kk, u->st,
		u->sub, u->jad << 2, u->jh, u->jl,
#endif // TRACE_RAW_UCODE
		buf);
}

static inline void display_check(wang_sys_t *sys) {
}

int instr_exec(wang_sys_t *sys) {
	w1200_ucode_t *u = (w1200_ucode_t *)&sys->ucode[sys->cpu.sys.pc];
	uint16_t next;
	int rc = 0;

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
		if (u->sub) {
			sys->cpu.stk1 = sys->cpu.stk2;
		} else {
			sys->cpu.stk1 = sys->cpu.stk2; // bugfix?
			//sys->cpu.stk1 = sys->cpu.pc;	// bad?
			// rc = 1;
		}
	} else {
		next = u->jad << 2;
	}

	switch(u->mop) {
	case 1:
	case 4:
	default:
		sys->cpu.m = sys->cpu.u;
		sys->cpu.n = sys->cpu.v;
		break;
	case 2:
	case 5:
		sys->cpu.m = br_k;
		sys->cpu.n = sys->cpu.v;
		break;
	case 3:
	case 6:
		sys->cpu.m = 15;
		sys->cpu.n = br_k;
		break;
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
	case 2: g = sys->cpu.d1; break;
	case 3:
		g = sys->cpu.d2;
		sys->cpu.d2 = 0;
		break;
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
	case 7:	sys->cpu.cb = alu; break;
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
		sys->cpu.s = (sys->cpu.s & ~1) | (sys->cpu.zo ^ 1);
		break;
	case 11:
		sys->cpu.s = (sys->cpu.s & ~2) | (sys->cpu.zo << 1);
		break;
	case 12:
		break;
	case 13:
		sys->cpu.s = 0;
		break;
	case 14:
		break;
	}

	switch(u->mop) {
	case 1:	wr_ram_i(sys); break;
	case 2:	wr_ram_i(sys); break;
	case 3:	wr_ram_i(sys); break;
	case 4:	rd_ram_i(sys); break;
	case 5:	rd_ram_i(sys); break;
	case 6:	rd_ram_i(sys); break;
	case 7:
		if (br_k & 1) {
			sys->cpu.ind.ind.csl = (u->bi & 1);
		}
		if (br_k & 2) {
			sys->cpu.ind.ind.eln = (u->bi & 1);
		}
		if (br_k & 4) {
			sys->cpu.ind.ind.ern = (u->bi & 1);
		}
		if (br_k & 8) {
			sys->cpu.ind.ind.nan = (u->bi & 1);
		}
		_indicators = 8;
		break;
	case 8:
		if (br_k & 1) {
			// lock/unlock keyboard
			sys->cpu.function = 1;
			dev_spc(sys, 0xd0 + ((u->bi & 1) << 4));
		}
		if (br_k & 2) {
			// sound alarm/bell
			sys->cpu.function = 1;
			dev_spc(sys, 0xf0);
		}
		if (br_k & 4) {
			sys->cpu.to = sys->cpu.ka;
			sys->cpu.ro = sys->cpu.kb;
			sys->cpu.function = (u->bi & 1);
			dev_out(sys);
		}
		break;
	case 9:
		switch(br_k & 0x07) {
		case 0:
			sys->cpu.ka = sys->cpu.d3;
			break;
		case 1:
			sys->cpu.ka = 4; // temp workaround for real UART sim
			break;
		case 4:
			sys->cpu.ka = // TRE, SHC, PRINT, ATTN...
				(sys->cpu.ls << 2);
			break;
		}
		break;
	case 10:
		tape_read(sys);	// must not block, if no data being read...
		// Dout<0>, Dout<1>, LeftProt, RightProt
		sys->cpu.kb =	(sys->cpu.tck << 1) |
				(sys->cpu.dk  << 0) |
				(sys->cpu.lop << 2) |
				(sys->cpu.rop << 3);
		break;
	case 11:
		sys->cpu.din0 = (sys->cpu.kb & 1);
		sys->cpu.din1 = (sys->cpu.ka & 1);
		tape_write(sys);
		break;
	case 12:
		// RHS : LHS : R/B : L/S
		sys->cpu.kb =	(sys->cpu.rhs << 3) |
				(sys->cpu.lhs << 2) |
				(1 << 1) |		// R/B
				(sys->cpu.ls << 0);	// L/S
		break;
	case 13:
	case 14:
		sys->cpu.right = (br_k >> 0) & 1;
		if (sys->cpu.right) {
			sys->cpu.rhs = ((br_k >> 2) & 1) ^ 1;
		} else {
			sys->cpu.lhs = ((br_k >> 2) & 1) ^ 1;
		}
		sys->cpu.rc = (u->bi & 1);
		sys->cpu.hl = (br_k >> 3) & 1;	// tape direction set/enable?
		sys->cpu.rv = (br_k >> 1) & 1;	// tape motor must be off to set direction!
		sys->cpu.tm = (u->mop == 13);
		if (sys->cpu.tm) {
			tape_on(sys);
		} else {
			tape_off(sys);
		}
		break;
	case 15:
		switch(br_k & 0x07) {
		case 0:
			// all UART functions?
			break;
		}
		break;
	}

	// This is done "late" to ensure we use most recent flags for I and Z
	if (!opf7) {
		if (u->sub) {
			sys->cpu.stk2 = sys->cpu.stk1;
			sys->cpu.stk1 = sys->cpu.sys.pc;
		}
		switch(u->jh) {
		case 0: next |= (0 << 1); break;
		case 1: next |= (1 << 1); break;
		case 2: next |= ((br_acc & 2) >> 0); break;
		case 3: next |= ((br_acc & 8) >> 2); break;
		case 4: next |= (0 << 1); break;
		case 5: next |= (sys->cpu.cc << 1); break;
		case 6:
			// todo: clean this up!
			if (key) {
//fprintf(stderr,"%03x: pop %04x\n", sys->cpu.sys.pc, key);
//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
				sys->cpu.kbd = 1;
				// is this bit ever set for other reasons?
				//sys->cpu.ls = ((key & 0x0080) != 0);
				sys->cpu.ka = (key >> 4) & 0x0f;
				sys->cpu.kb = key & 0x0f;
				sys->keyboard(sys, &key, 1); // ack only, maybe
			}
			next |= (sys->cpu.kbd << 1);
			if (sys->cpu.kbd) {
				sys->cpu.kbd = 0;
				sys->display(sys, 0);
			}
			// do this before any possible sleep!
			if (_indicators) {
				_indicators = 0;
				sys->display(sys, -2);
			}
			break;
		case 7: next |= (sys->cpu.zo << 1); break;
		}
		switch(u->jl) {
		case 0: next |= (0 << 0); break;
		case 1: next |= (1 << 0); break;
		case 2: next |= ((br_acc & 1) >> 0); break;
		case 3: next |= ((br_acc & 4) >> 2); break;
		case 4: next |= (sys->cpu.zo << 0); break;
		case 5: next |= (sys->cpu.cc << 0); break;
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
	// some indicators are set/reset quickly, not intended to be seen.
	// so only update indicators after 8 cycles from last change.
	if (_indicators && --_indicators == 0) {
		//_indicators = 0;
		sys->display(sys, -2);
	}

	display_check(sys);	// this might sleep until UI event...

	sys->keyboard(sys, &key, 0); // this actually gets a key...

	if (sys->cpu.sys.jam) {
		sys->cpu.sys.next = sys->cpu.sys.jam & 0x0fff; 
		sys->cpu.sys.jam = 0;
		if (sys->cpu.sys.next == 0) { // RESET
			sys->cpu.ind.ind.skl = 0;
			sys->cpu.ind.ind.shl = 0;
			++_indicators;
		}
	}

	sys->cpu.sys.pc = sys->cpu.sys.next;

	u = (w1200_ucode_t *)&sys->ucode[sys->cpu.sys.pc];
	if (!rc && u->brkpt) {
		// 'u' points into ucode[], so updates are stored there...
		u->brkpt = 0; // one-shot breakpoint turned off...
		sys->run = 0;
		return 0;
	}
	return rc;
}
