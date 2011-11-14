// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_decode.c,v 1.51 2011/11/14 00:45:44 drmiller Exp $"

#include <unistd.h>
#include <time.h>

#include "wang-sim.h"
#include "w600_ucode.h"

#ifdef TRACE
extern int diwang(char *buf, uint64_t *t);
extern char *get_psw_str(w600_sys_t *sys);
#endif // TRACE

#ifdef COVERAGE
uint8_t cov[2048] = {0};
#endif // COVERAGE

uint8_t __keytrc = 0;
uint8_t __systrc[16] = {0};

uint16_t trc_adr = 0xff0;

static uint8_t add3_i(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t sub3_i(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a - b - c;
	sys->cpu.zo = ((s & 0x0f) == 0);
	sys->cpu.cc = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t and2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a & b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t or2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a | b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t xor2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a ^ b;
	sys->cpu.zo = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t add3_c(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = add3_i(sys, a, b, c);
	sys->cpu.sc = sys->cpu.cc;
	return s;
}

static uint8_t sub3_c(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
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

static void tape_write(w600_sys_t *sys, int dat) {
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

static int tape_read(w600_sys_t *sys) {
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

static void tape_on(w600_sys_t *sys, int wr) {
	(void)sys->tape(sys, wr, 0x40); // i.e. open file...
	if (!wr) {
		tape_read(NULL);
	}
}

static void tape_off(w600_sys_t *sys) {
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
}

static void dev_out(w600_sys_t *sys) {
	uint8_t c = (sys->cpu.ka << 4) | sys->cpu.kb;
//fprintf(stderr, "DEV> %02x %x\n", c, sys->cpu.iob);
	sys->dev(sys, c, sys->cpu.iob);
}

static uint8_t pr_drum = 0;
static uint32_t pr_hammers = 0;
static uint8_t pr_tach = 0;
static int pr_col = 0;

static void printer_status(w600_sys_t *sys) {
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

static void printer_hammers(w600_sys_t *sys) {
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

static void printer_feed(w600_sys_t *sys) {
	// now, actually print it...
	sys->printer(sys, -1, 0);
}

extern uint16_t ram_mask;

static void rd_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
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

static void wr_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
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

static void instr_trace(w600_sys_t *sys) {
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

static inline void display_check(w600_sys_t *sys) {
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

int instr_exec(w600_sys_t *sys) {
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
		sys->cpu.ov = 1;
		sys->display(sys, -2);
		break;
	case 13:
		sys->cpu.s = 0;
		break;
	case 14:
		sys->cpu.err = 1;
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
			next |= (sys->cpu.ov << 1);
//fprintf(stderr,"%03x: chk pe\n", sys->cpu.sys.pc);
			sys->cpu.ov = 0;
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

	sys->cpu.sys.pc = sys->cpu.sys.next;
	return rc;
}
