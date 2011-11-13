// Copyright (c) 2011 Douglas Miller

#ident "$Id: w1200_decode.c,v 1.2 2011/11/13 18:50:23 drmiller Exp $"

#include <unistd.h>
#include <time.h>

#include "wang-sim.h"
#include "w1200_ucode.h"

#ifdef TRACE
extern int diwang(char *buf, uint64_t *t);
extern char *get_psw_str(w1200_sys_t *sys);
#endif // TRACE

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

uint16_t trc_adr = 0x0f0;

static uint8_t add3_i(w1200_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t sub3_i(w1200_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a - b - c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t and2(w1200_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a & b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t or2(w1200_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a | b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t xor2(w1200_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a ^ b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t add3_c(w1200_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t sub3_c(w1200_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
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

static void tape_write(w1200_sys_t *sys, int dat) {
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

static int tape_read(w1200_sys_t *sys) {
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

static void tape_on(w1200_sys_t *sys, int wr) {
	(void)sys->tape(sys, wr, 0x40); // i.e. open file...
	if (!wr) {
		tape_read(NULL);
	}
}

static void tape_off(w1200_sys_t *sys) {
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
}

static void dev_out(w1200_sys_t *sys) {
	uint8_t c = (sys->cpu.to << 4) | sys->cpu.ro;
//fprintf(stderr, "DEV> %02x (%d)\n", c, sys->cpu.function);
	sys->dev(sys, c, 1);
}

extern uint16_t ram_mask;

static void rd_ram_i(w1200_sys_t *sys, uint8_t am, uint8_t al) {
	uint16_t adr = (am << 4) | al;
	adr &= ram_mask;
	uint8_t b = sys->ram[adr];
	sys->cpu.ca = (b >> 4) & 0x0f;
	sys->cpu.cb = b & 0x0f;
}

static void wr_ram_i(w1200_sys_t *sys, uint8_t am, uint8_t al) {
	uint16_t adr = (am << 4) | al;
	adr &= ram_mask;
	uint8_t a = sys->cpu.ca;
	uint8_t b = sys->cpu.cb;
	sys->ram[adr] = b | (a << 4);
	if ((adr & 0xff0) == trc_adr) {
		if (__systrc[adr & 0x00f]) {
			fprintf(stderr, "[%03x] %x -> %x\n", adr, d, c);
		}
	}
}

static void instr_trace(w1200_sys_t *sys) {
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

int instr_exec(w1200_sys_t *sys) {
	w1200_ucode_t *u = (w1200_ucode_t *)&sys->ucode[sys->cpu.sys.pc];
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

	sys->cpu.m = sys->cpu.u;
	sys->cpu.n = sys->cpu.v;

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
		break;
	case 13:
		sys->cpu.s = 0;
		break;
	case 14:
		break;
	}

	switch(u->mop) {
	case 1:	wr_ram_i(sys, sys->cpu.m, sys->cpu.n); break;
	case 2:	wr_ram_i(sys, br_k, sys->cpu.n); break;
	case 3:	wr_ram_i(sys, 15, br_k); break;
	case 4:	rd_ram_i(sys, sys->cpu.m, sys->cpu.n); break;
	case 5:	rd_ram_i(sys, br_k, sys->cpu.n); break;
	case 6:	rd_ram_i(sys, 15, br_k); break;
	case 7:
		break;
	case 8:
		if (br_k & 4) {
			sys->cpu.to = sys->cpu.ka;
			sys->cpu.ro = sys->cpu.kb;
			sys->cpu.function = (u->bi & 1);
			dev_out(sys);
		}
		break;
	case 9:	rc = 2; break;
	case 10:
		sys->cpu.kb = 0; // SKB0, PSKB1, LOP, ROP
		break;
	case 11:
		sys->cpu.din0 = (sys->cpu.kb & 1);
		sys->cpu.din1 = (sys->cpu.ka & 1);
		//tape_write(sys);
		break;
	case 12:
		sys->cpu.kb = 0; // L/S, R/B, LHS, RHS
		break;
	case 13:
		sys->cpu.right = br_k & 1;
		sys->cpu.fr[sys->cpu.right] = (br_k >> 1) & 1;
		sys->cpu.hi[sys->cpu.right] = (br_k >> 2) & 1;
		sys->cpu.mv[sys->cpu.fr[sys->cpu.right]][sys->cpu.right] = (br_k >> 3) & 1;
		sys->cpu.rc = (u->bi & 1);
		tape_on(sys);
		break;
	case 14:
		sys->cpu.right = br_k & 1;
		sys->cpu.rc = (u->bi & 1);
		tape_off(sys);
		break;
	case 15:
		sys->cpu.to = sys->cpu.ka;	// sys->cpu.gioa = g;
		sys->cpu.ro = sys->cpu.kb;	// sys->cpu.giob = h;
		dev_out(sys);
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

	sys->keyboard(sys, &key, 0);

	sys->cpu.sys.pc = sys->cpu.sys.next;
	return rc;
}
