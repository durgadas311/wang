// Copyright (c) 2011 Douglas Miller

#ident "$Id: w700_decode.c,v 1.19 2011/11/16 15:45:30 drmiller Exp $"

#include <unistd.h>
#include <time.h>
#include <string.h>

#include "wang-sim.h"
#include "w700_ucode.h"

#ifdef TRACE
extern int diwang(char *buf, uint64_t *t);
#endif // TRACE

extern uint16_t ram_mask;

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

static char *get_mach_str(wang_sys_t *sys) { 
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%01x", sys->cpu.d);
	if (sys->cpu.ind.ind.ofl) s += sprintf(s, "|Prog Err");
	if (sys->cpu.ind.ind.err) s += sprintf(s, "|Mach Err");
	if (sys->cpu.kbd) s += sprintf(s, "|Key Pressed");

	*s = '\0'; 
	return buf;
}

static char *get_psw_str(wang_sys_t *sys) { 
	static char buf[32];
	char *s = buf;

	if (sys->cpu.alu) *s++ = 'Z';
	else *s++ = 'z';
	if (sys->cpu.cc) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.sc) *s++ = 'C';
	else *s++ = 'c';
	if (sys->cpu.q) *s++ = 'Q';
	else *s++ = 'q';

	*s = '\0'; 
	return buf;
}

static void get_reg_str(wang_sys_t *sys, char *buf) {
	char *s = buf;

	s += sprintf(s, "T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
			sys->cpu.t, sys->cpu.u, sys->cpu.v, sys->cpu.ca, sys->cpu.cb);
	s += sprintf(s, "S = %01x ALU = %d CC = %d SC = %d Q = %d\n",
			sys->cpu.s, sys->cpu.alu, sys->cpu.cc, sys->cpu.sc, sys->cpu.q);
	s += sprintf(s, "KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
			sys->cpu.ka, sys->cpu.kb, sys->cpu.gioa, sys->cpu.giob, sys->cpu.iob);

}

static int special_key(wang_sys_t *sys, uint16_t b) {
	switch(b >> 8) {
	case 2: // mode0 switches changed
		// FE gave us complete mode word... just update
		sys->cpu.d = b & 0x0f;
		break;
	default:
		return -1;
		break;
	}
	return 0;
}

void w700_init(wang_sys_t *sys) {
	sys->cpu.d = 0; // default?
	sys->get_psw_str = get_psw_str;
	sys->get_mach_str = get_mach_str;
	sys->get_reg_str = get_reg_str;
	// sys->ucode_override = ucode_override; // not for wire-weave ROM...
	sys->special_key = special_key;
}

#if 0 // NO!!!
static uint8_t bcd_logic[32] = {
[0] = 0x00,
[1] = 0x01,
[2] = 0x02,
[3] = 0x03,
[4] = 0x04,
[5] = 0x05,
[6] = 0x06,
[7] = 0x07,
[8] = 0x08,
[9] = 0x09,
[10] = 0x10,
[11] = 0x11,
[12] = 0x12,
[13] = 0x13,
[14] = 0x14,
[15] = 0x15,
[16] = 0x06,
[17] = 0x07,
[18] = 0x08,
[19] = 0x09,
[20] = 0x16,
[21] = 0x17,
[22] = 0x1c,
[23] = 0x1d,
[24] = 0x06,
[25] = 0x07,
[26] = 0x18,
[27] = 0x19,
[28] = 0x16,
[29] = 0x17,
[30] = 0x1c,
[31] = 0x1d
};
#endif

static uint8_t bin_add3_i(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.alu = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t bcd_add3_i(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	uint8_t cc = 0;
	//s = bcd_logic[s & 0x1f];
	while (s >= 10) {
		s -= 10;
		cc = 1;
	}
	sys->cpu.alu = ((s & 0x0f) == 0);
	sys->cpu.cc = cc;
	return s & 0x0f;
}

static uint8_t bin_add3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bin_add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s & 0x0f;
}

static uint8_t bcd_add3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bcd_add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s & 0x0f;
}

static uint8_t bin_shift3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bin_add3_i(sys, a, b, 0);
	s |= (c << 4);
	sys->cpu.sc = (s & 1);
	s >>= 1;
	return s & 0x0f;
}

static uint8_t bcd_shift3_c(wang_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bcd_add3_i(sys, a, b, 0);
	s |= (c << 4);
	sys->cpu.sc = (s & 1);
	s >>= 1;
	return s & 0x0f;
}

static uint8_t bin_and2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bin_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a & b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bcd_and2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bcd_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a & b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bin_xor2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bin_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a ^ b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bcd_xor2(wang_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bcd_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a ^ b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
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
	uint8_t c = (sys->cpu.gioa << 4) | sys->cpu.giob;
fprintf(stderr, "DEV> %02x %x\n", c, sys->cpu.iob);
	sys->dev(sys, c, sys->cpu.iob);
}

uint16_t trc_adr = 0xfd0;

static void rd_ram_i(wang_sys_t *sys) {
	uint16_t adr = (sys->cpu.l << 8) | (sys->cpu.m << 4) | sys->cpu.n;
	adr &= ram_mask;
	uint8_t b = sys->ram[adr];
	sys->ram[adr] = 0; // core memory has destructive read!
	sys->cpu.ra = (b >> 4) & 0x0f;
	sys->cpu.rb = b & 0x0f;
}

static void wr_ram_i(wang_sys_t *sys) {
	uint16_t adr = (sys->cpu.l << 8) | (sys->cpu.m << 4) | sys->cpu.n;
	uint16_t madr = adr & ram_mask;
	uint8_t a = sys->ram[madr];
	uint8_t b = (sys->cpu.ra << 4) | sys->cpu.rb;
	sys->ram[madr] = b;
#if 0
	if (__keytrc && adr == 0xff8) {
		fprintf(stderr, "Code %02d %02d\n", (b >> 4) & 0x0f, b & 0x0f);
	}
#endif
	if ((adr & 0xff0) == trc_adr) {
		if (__systrc[adr & 0x00f]) {
			fprintf(stderr, "[%03x] %02x -> %02x\n", adr, a, b);
		}
	}
}

static void instr_trace(wang_sys_t *sys) {
	uint64_t *m;
	char buf[128];
	m = &sys->ucode[sys->cpu.sys.pc];
	diwang(buf, m);
#ifdef TRACE_RAW_UCODE
	w700_ucode_t *u = (w700_ucode_t *)(m);
#endif // TRACE_RAW_UCODE
	fprintf(sys->trc_fp, "TRACE: %03x: "
		"[%03x] %01x %01x %01x %01x %01x "
		"[%s] %01x %01x %01x : "
#ifdef TRACE_RAW_UCODE
		"[%x%x%x%x%x%x%x%x%x%x%03x%x%x] "
#endif // TRACE_RAW_UCODE
		"%s\n",
		sys->cpu.sys.pc,
		sys->cpu.sys.next,
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
		u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->bd, u->mop, u->kk, u->st,
		u->jad << 2, u->jh, u->jl,
#endif // TRACE_RAW_UCODE
		buf);
}

static inline void display_check(wang_sys_t *sys) {
	// 034: begin display-refresh delay loop... short-cut to 472...
	if ((sys->cpu.sys.pc & 0xffe) == 0x034) {	// display refresh routine...
		sys->cpu.sys.next = 0x472;	// update some regs too?
		sys->cpu.sys.cycles += 431;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Display Warp... %lld\n",
							sys->cpu.sys.pc, sys->cpu.sys.cycles);
		}
		sys->display(sys, 1);	// might sleep
	// 5ed: begin alpha-stop display-refresh delay loop... short-cut to 4ae... 531cy
	} else if (sys->cpu.sys.pc == 0x5ed) {	// alpha-stop refresh routine...
		sys->cpu.sys.next = 0x4ae;
		sys->cpu.sys.cycles += 531;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Alpha-Stop Warp... %lld\n",
							sys->cpu.sys.pc, sys->cpu.sys.cycles);
		}
		sys->display(sys, -1);	// must not sleep!
	} else if (sys->cpu.sys.pc == 0x4af) {	// alpha-stop done...
		if (sys->cpu.sys.next == 0x081) { // alpha-stop in running program...
			// currently can't tell difference!
			// observed 528385 cycles, or about 0.727 second
			if (sys->trace) {
				fprintf(sys->trc_fp, "TRACE: %03x: "
						"Alpha-Stop Sleep... %lld\n",
						sys->cpu.sys.pc, sys->cpu.sys.cycles);
			}
			sys->keyboard(sys, NULL, 727);
		}
		sys->display(sys, 0);
	}
}

int instr_exec(wang_sys_t *sys) {
	w700_ucode_t *u = (w700_ucode_t *)&sys->ucode[sys->cpu.sys.pc];
	uint16_t next;
	int rc = 0;
	static uint16_t key = 0;

	// For conditional jump, these bits are latched early...
	uint8_t br_s = sys->cpu.s;
	uint8_t br_sc = sys->cpu.sc;
	uint8_t br_q = sys->cpu.q;
	uint8_t br_k = u->kk;
#ifdef COVERAGE
	if (cov[sys->cpu.sys.pc] < 255) ++cov[sys->cpu.sys.pc];
#endif // COVERAGE
	next = u->jad << 2;

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
		g = sys->cpu.d;
		sys->cpu.d &= ~D13_STEP;
		break;
	case 3: g = 0; break;
	case 4: g = sys->cpu.ka; break;
	case 5: g = sys->cpu.kb; break;
	case 6: g = sys->cpu.ca; break;
	case 7: g = sys->cpu.cb; break;
	}

	uint8_t alu = 0;

	if (!u->ac) h = 0; 
	switch (u->bc) {
	case 0:
		g = 0;
		break;
	case 1:
		break;
	case 2:
		g = (u->bd ? 9 : 15);
		break;
	case 3:
		g = ((u->bd ? 9 : 15) - g) & 0x0f;
		break;
	}

	if (u->bd) {
		switch (u->aop) {
		case 0:
			alu = bcd_add3_i(sys, h, g, 0);
			break;
		case 1:
			alu = bcd_add3_i(sys, h, g, 1);
			break;
		case 2:
			alu = bcd_add3_c(sys, h, g, 0);
			break;
		case 3:
			alu = bcd_add3_c(sys, h, g, br_sc);
			break;
		case 4:
			alu = bcd_add3_c(sys, h, g, 1);
			break;
		case 5:
			alu = bcd_and2(sys, h, g);
			break;
		case 6:
			alu = bcd_xor2(sys, h, g);
			break;
		case 7:
			alu = bcd_shift3_c(sys, h, g, br_sc);
			break;
		}
	} else {
		switch (u->aop) {
		case 0:
			alu = bin_add3_i(sys, h, g, 0);
			break;
		case 1:
			alu = bin_add3_i(sys, h, g, 1);
			break;
		case 2:
			alu = bin_add3_c(sys, h, g, 0);
			break;
		case 3:
			alu = bin_add3_c(sys, h, g, br_sc);
			break;
		case 4:
			alu = bin_add3_c(sys, h, g, 1);
			break;
		case 5:
			alu = bin_and2(sys, h, g);
			break;
		case 6:
			alu = bin_xor2(sys, h, g);
			break;
		case 7:
			alu = bin_shift3_c(sys, h, g, br_sc);
			break;
		}
	}

	/*
	 * Now... we start changing machine state... pay attention to clock phases!
	 */

	// clock = P4
	if (u->mop >= 2 && u->mop <= 5) {
		sys->cpu.l = (u->mop >= 4 ?   15 : sys->cpu.t);
		sys->cpu.m = (u->mop >= 4 ? br_k : sys->cpu.u);
		sys->cpu.n = sys->cpu.v;
	}

	// clock = P4-5
	switch(u->mop) {
	case 10:
		sys->cpu.kb = (sys->cpu.kb & ~1) | tape_read(sys);
		break;
	case 11:
		tape_write(sys, sys->cpu.kb & 1);
		break;
	case 12:
		tape_on(sys, u->bi & 1);
		break;
	case 13:
		tape_off(sys);
		break;
	}

	// clock = P5-6
	switch(u->mop) {
	case 7:
		sys->cpu.iob = sys->cpu.kb & 0x07;
		break;
	case 14:
		sys->cpu.gioa = sys->cpu.ka;
		sys->cpu.giob = sys->cpu.kb;
		dev_out(sys);
		break;
	}

	// clock = P9
	switch(u->zo) {
	case 0:	sys->cpu.s = alu; break;
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
		// clock = P10
		sys->cpu.s |= 1;
		break;
	case 2:
		// clock = P10
		sys->cpu.s |= 2;
		break;
	case 3:
		// clock = P10
		sys->cpu.s |= 4;
		break;
	case 4:
		// clock = P10
		sys->cpu.s |= 8;
		break;
	case 5:
		// clock = P10
		sys->cpu.s &= ~1;
		break;
	case 6:
		// clock = P10
		sys->cpu.s &= ~2;
		break;
	case 7:
		// clock = P10
		sys->cpu.s &= ~4;
		break;
	case 8:
		// clock = P10
		sys->cpu.s &= ~8;
		break;
	case 9:
		sys->cpu.kbd = 0;
		break;
	case 10:
		// clock = P10
		sys->cpu.s = (sys->cpu.s & 0x0e) | (sys->cpu.alu ^ 1);
		break;
	case 11:
		// clock = P10
		sys->cpu.s = (sys->cpu.s & 0x0d) | (sys->cpu.alu << 1);
		break;
	case 12:
		sys->cpu.ind.ind.ofl = 1;
		sys->display(sys, -2);
		break;
	case 13:
		// clock = P10
		sys->cpu.s = 0;
		break;
	case 14:
		sys->cpu.ind.ind.err = 1;
		sys->display(sys, -2);
		break;
	case 15:
		// error?
		break;
	}

	switch(u->mop) {
	case 0:
		// L,M,N already setup...
		// clock = P9
		sys->cpu.ra = alu;
		wr_ram_i(sys);
		break;
	case 1:
		// L,M,N already setup...
		// clock = P9
		sys->cpu.rb = alu;
		wr_ram_i(sys);
		break;
	case 2:
		// L,M,N already setup...
		// clock = P9 (overrides ZO)
		rd_ram_i(sys);
		sys->cpu.ca = sys->cpu.ra;
		sys->cpu.cb = sys->cpu.rb;
		break;
	case 3:
		// L,M,N already setup...
		// clock = P9
		rd_ram_i(sys);
		break;
	case 4:
		// L,M,N already setup...
		// clock = P9 (overrides ZO)
		rd_ram_i(sys);
		sys->cpu.ca = sys->cpu.ra;
		sys->cpu.cb = sys->cpu.rb;
		break;
	case 5:
		// L,M,N already setup...
		// clock = P9
		rd_ram_i(sys);
		break;
	case 6:
		// CN-24 status... RBS
		sys->cpu.kb |= 1;
		break;
	case 7:
		// done at clock P5-6
		break;
	case 8:	break;
	case 9:
		// clock = P9
		if (u->aop == 7) {
			sys->cpu.q = sys->cpu.sc;
		} else {
			sys->cpu.q = sys->cpu.cc;
		}
		break;
	case 10:
	case 11:
	case 12:
	case 13:
		// done at clock = P4-5
		break;
	case 14:
		// done at clock P5-6
		break;
	case 15:
		rc = 2;
		break;
	}

	// This is done "late" to ensure we use most recent flags for I and Z
	// clock = P9
	switch(u->jh) {
	case 0: next |= (0 << 1); break;
	case 1: next |= (1 << 1); break;
	case 2: next |= ((br_s & 2) >> 0); break;
	case 3: next |= ((br_s & 8) >> 2); break;
	case 4:
		next |= (sys->cpu.ind.ind.ofl << 1);
//fprintf(stderr,"%03x: chk pe\n", sys->cpu.sys.pc);
		sys->cpu.ind.ind.ofl = 0;
		sys->display(sys, -2);
		break;
	case 5: next |= (sys->cpu.cc << 1); break;
	case 6:
		// todo: clean this up!
		if (key) {
//fprintf(stderr,"%03x: pop %04x\n", sys->cpu.sys.pc, key);
//if (1 || __keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
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
	case 2: next |= ((br_s & 1) >> 0); break;
	case 3: next |= ((br_s & 4) >> 2); break;
	case 4: next |= (sys->cpu.alu << 0); break;
	case 5: next |= (br_q << 0); break;
	case 6: next |= (br_sc << 0); break;
	case 7: rc = 5; break;
	}
	sys->cpu.sys.next = next;

	++sys->cpu.sys.cycles;
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
		sys->cpu.ind.ind.ofl = 0;
		if (sys->cpu.sys.next == 0) { // PRIME
			sys->cpu.ind.ind.err = 0;
		}
		sys->display(sys, -2);
	}

	sys->cpu.sys.pc = sys->cpu.sys.next;
	return rc;
}
