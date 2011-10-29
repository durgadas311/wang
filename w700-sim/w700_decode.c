// Copyright (c) 2011 Douglas Miller

#ident "$Id: w700_decode.c,v 1.9 2011/10/29 22:31:39 drmiller Exp $"

#include <unistd.h>
#include <time.h>

#include "w700_sys.h"
#include "w700_ucode.h"

#ifdef TRACE
extern int diw700(char *buf, uint64_t *t);
extern char *get_psw_str(w700_sys_t *sys);
#endif // TRACE

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

static uint8_t bin_add3_i(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.alu = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t bcd_add3_i(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	if (s > 9) {
		s -= 10;
		s |= 0x10;
	}
	sys->cpu.alu = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t bin_add3_c(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bin_add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t bcd_add3_c(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bcd_add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t bin_shift3_c(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bin_add3_i(sys, a, b, 0);
	s |= (c << 4);
	sys->cpu.sc = (s & 1);
	s >>= 1;
	return s;
}

static uint8_t bcd_shift3_c(w700_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = bcd_add3_i(sys, a, b, 0);
	s |= (c << 4);
	sys->cpu.sc = (s & 1);
	s >>= 1;
	return s;
}

static uint8_t bin_and2(w700_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bin_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a & b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bcd_and2(w700_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bcd_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a & b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bin_xor2(w700_sys_t *sys, uint8_t a, uint8_t b) {
	(void)bin_add3_i(sys, a, b, 0);	// set CC
	uint8_t s = a ^ b;
	sys->cpu.alu = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t bcd_xor2(w700_sys_t *sys, uint8_t a, uint8_t b) {
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

static void tape_write(w700_sys_t *sys, int dat) {
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

static int tape_read(w700_sys_t *sys) {
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

	if (sys->cpu.cycles < repc) {
reps:
		return bit;
	}
	if (sigc) {
sigs:
		--sigc;
		bit = last & 1;
		last >>= 1;
		repc = sys->cpu.cycles + 97;	// very sensitive...
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
		repc = sys->cpu.cycles + 700;	// expects at least 650?
		bit = 0;
		goto reps;
	}
	data = (nib << 1) | odd_parity[nib];
	bitc = 5;
	goto bits;
}

static void tape_on(w700_sys_t *sys, int wr) {
	(void)sys->tape(sys, wr, 0x40); // i.e. open file...
	if (!wr) {
		tape_read(NULL);
	}
}

static void tape_off(w700_sys_t *sys) {
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
}

static void dev_out(w700_sys_t *sys) {
	uint8_t c = (sys->cpu.gioa << 4) | sys->cpu.giob;
fprintf(stderr, "DEV> %02x %x\n", c, sys->cpu.iob);
	sys->dev(sys, c, sys->cpu.iob);
}

extern uint16_t ram_mask;

static void rd_ram_i(w700_sys_t *sys) {
	uint16_t adr = (sys->cpu.l << 8) | (sys->cpu.m << 4) | sys->cpu.n;
	adr &= ram_mask;
	uint8_t b = sys->ram[adr];
	sys->ram[adr] = 0; // core memory has destructive read!
	sys->cpu.ra = (b >> 4) & 0x0f;
	sys->cpu.rb = b & 0x0f;
}

static void wr_ram_i(w700_sys_t *sys) {
	uint16_t adr = (sys->cpu.l << 8) | (sys->cpu.m << 4) | sys->cpu.n;
	adr &= ram_mask;
	uint8_t a = sys->ram[adr];
	uint8_t b = (sys->cpu.ra << 4) | sys->cpu.rb;
	sys->ram[adr] = b;
#if 0
	if (__keytrc && adr == 0xff8) {
		fprintf(stderr, "Code %02d %02d\n", (b >> 4) & 0x0f, b & 0x0f);
	}
	if (adr >= 0xff0) {
		if (__systrc[adr & 0x00f]) {
			fprintf(stderr, "[%03x] %02x -> %02x\n", adr, a, b);
		}
	}
#endif
}

static void instr_trace(w700_sys_t *sys) {
	uint64_t *m;
	char buf[128];
	m = &sys->ucode[sys->cpu.pc];
	diw700(buf, m);
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
		sys->cpu.pc,
		sys->cpu.next,
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

static inline void display_check(w700_sys_t *sys) {
	// 034: begin display-refresh delay loop... short-cut to 472...
	if ((sys->cpu.pc & 0xffe) == 0x034) {	// display refresh routine...
		sys->cpu.next = 0x472;	// update some regs too?
		sys->cpu.cycles += 431;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Display Warp... %lld\n",
							sys->cpu.pc, sys->cpu.cycles);
		}
		sys->display(sys, 1);	// might sleep
	// 5ed: begin alpha-stop display-refresh delay loop... short-cut to 4ae... 531cy
	} else if (sys->cpu.pc == 0x5ed) {	// alpha-stop refresh routine...
		sys->cpu.next = 0x4ae;
		sys->cpu.cycles += 531;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: %03x: Alpha-Stop Warp... %lld\n",
							sys->cpu.pc, sys->cpu.cycles);
		}
		sys->display(sys, -1);	// must not sleep!
	} else if (sys->cpu.pc == 0x4af) {	// alpha-stop done...
		if (sys->cpu.next == 0x081) { // alpha-stop in running program...
			// currently can't tell difference!
			// observed 528385 cycles, or 0.66 second
			static struct timespec alpha_stop = {
				0, 666666666L
			};
			// todo: should not sleep if key pressed - e.g. PRIME
			nanosleep(&alpha_stop, NULL);
			// sleep(1);
		}
		sys->display(sys, 0);
	}
}

int instr_exec(w700_sys_t *sys) {
	w700_ucode_t *u = (w700_ucode_t *)&sys->ucode[sys->cpu.pc];
	uint16_t next;
	int rc = 0;
	static uint16_t key = 0;

	// For conditional jump, these bits are latched early...
	uint8_t br_s = sys->cpu.s;
	uint8_t br_sc = sys->cpu.sc;
	uint8_t br_q = sys->cpu.q;
	uint8_t br_k = u->kk;
#ifdef COVERAGE
	if (cov[sys->cpu.pc] < 255) ++cov[sys->cpu.pc];
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
		sys->cpu.d &= ~MODE0_STEP;
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
		// T.B.D. reset 6184...
//fprintf(stderr, "%03x: res (%04x)\n", sys->cpu.pc, key);
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
		sys->cpu.ofl = 1;
		sys->display(sys, -2);
		break;
	case 13:
		// clock = P10
		sys->cpu.s = 0;
		break;
	case 14:
		sys->cpu.err = 1;
		sys->display(sys, -2);
		break;
	case 15:
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
		next |= (sys->cpu.ofl << 1);
//fprintf(stderr,"%03x: chk pe\n", sys->cpu.pc);
		sys->cpu.ofl = 0;
		break;
	case 5: next |= (sys->cpu.cc << 1); break;
	case 6:
		// todo: clean this up!
		if (key) {
//fprintf(stderr,"%03x: pop %04x\n", sys->cpu.pc, key);
if (1 || __keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
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
	sys->cpu.next = next;

	++sys->cpu.cycles;
#ifdef TRACE
	if (sys->trace) {
		instr_trace(sys);
	}
#endif // TRACE
	// the following are called in specific order...
	// keyboard injection of next pc must override all, so is last.

	display_check(sys);	// this might sleep until UI event...

	sys->keyboard(sys, &key, 0);

	sys->cpu.pc = sys->cpu.next;
	return rc;
}
