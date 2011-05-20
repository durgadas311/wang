// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_decode.c,v 1.29 2011/05/20 09:45:35 drmiller Exp $"

#include "w600_sys.h"
#include "w600_ucode.h"

static uint8_t add3_i(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a + b + c;
	sys->cpu.z = ((s & 0x0f) == 0);
	sys->cpu.i = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t sub3_i(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = a - b - c;
	sys->cpu.z = ((s & 0x0f) == 0);
	sys->cpu.i = ((s & 0x10) != 0);
	return s & 0x0f;
}

static uint8_t and2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a & b;
	sys->cpu.z = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t or2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a | b;
	sys->cpu.z = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t xor2(w600_sys_t *sys, uint8_t a, uint8_t b) {
	uint8_t s = a ^ b;
	sys->cpu.z = ((s & 0x0f) == 0);
	return s & 0x0f;
}

static uint8_t add3_c(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = add3_i(sys, a, b, c);
	sys->cpu.c = sys->cpu.i;
	return s;
}

static uint8_t sub3_c(w600_sys_t *sys, uint8_t a, uint8_t b, uint8_t c) {
	uint8_t s = sub3_i(sys, a, b, c);
	sys->cpu.c = sys->cpu.i;
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
		sigc = 20;
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
		repc = sys->cpu.cycles + 300;
		bit = 0;
		goto reps;
	}
	data = (nib << 1) | odd_parity[nib];
	bitc = 5;
	goto bits;
}

static int cass_on = 0;

static void tape_on(w600_sys_t *sys, int wr) {
	if (cass_on) return;
	cass_on = 1;

	(void)sys->tape(sys, wr, 0x40); // i.e. open file...
	if (!wr) {
		tape_read(NULL);
	}
}

static void tape_off(w600_sys_t *sys) {
	cass_on = 0;
	(void)sys->tape(sys, 0, 0x80); // i.e. close file...
}

static void cn24_out(w600_sys_t *sys) {
	// how to detect "carriage return"... or "new line"...
	uint8_t c = (sys->cpu.dh << 4) | sys->cpu.dl;
	c &= 0x3f;
	sys->cn24(sys, c);
}

static uint8_t pr_drum = 0;
static uint32_t pr_hammers = 0;
static uint8_t pr_tach = 0;
static int pr_col = 0;

static void printer_status(w600_sys_t *sys) {
	if ((sys->cpu.mode1 & MODE1_PRT_ON) == 0) {
		// only if running program doesn't get here...
		// printer is off, tach will never pulse, so don't spin
		sys->keyboard(sys, NULL); // sleep until key event
		return;
	}
	if (pr_tach) {
		pr_col = 0;
		pr_drum = (pr_drum + 1) & 0x0f;
		pr_hammers = 0;
	}
	pr_tach ^= 0x08;
	sys->cpu.dh = pr_drum;
	sys->cpu.dl = pr_tach;
}

static void printer_hammers(w600_sys_t *sys) {
	int x;
	uint32_t h;

	pr_hammers <<= 1;
	pr_hammers &= 0x0fffff;
	pr_hammers |= sys->cpu.dl & 1;
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

static void rd_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->cpu.mr = b;
	b = sys->rom[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->cpu.xr = b;
}

static void wr_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t a = sys->cpu.mr;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		a <<= 4;
		b &= 0x0f;
	} else {
		b &= 0xf0;
	}
	sys->ram[adr >> 1] = b | a;
}

static inline void display_check(w600_sys_t *sys) {
	// 51c: begin display-refresh delay loop... short-cut to 51f...
	if (sys->cpu.pc == 0x51c) {	// display refresh routine...
		sys->cpu.pc = 0x51f;	// update some regs too?
		sys->cpu.cycles += 272;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: 51c: Display Warp...\n");
		}
		sys->display(sys, 1);	// might sleep
	// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
	} else if (sys->cpu.pc == 0x5c0) {	// alpha-stop refresh routine...
		sys->cpu.pc = 0x5c3;
		sys->cpu.cycles += 272;
		if (sys->trace) {
			fprintf(sys->trc_fp, "TRACE: 5c0: Alpha-Stop Warp...\n");
		}
		sys->display(sys, -1);	// must not sleep!
	} else if (sys->cpu.pc == 0x5c6) {	// alpha-stop done... "return"...
		sys->display(sys, 0);
	} else if (sys->cpu.pc == 0x27b) {	// alpha-stop in running program...
		sleep(1);
	}
}

int instr_exec(w600_sys_t *sys) {
	w600_ucode_t *u = (w600_ucode_t *)&sys->ucode[sys->cpu.pc];
	uint16_t next;
	int rc = 0;
	static uint8_t key = 0;

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
	uint8_t br_acc = sys->cpu.acc;
	uint8_t br_c = sys->cpu.c;
	uint8_t m_ah = sys->cpu.ah;
	uint8_t m_am = sys->cpu.am;
	uint8_t m_al = sys->cpu.al;
	uint8_t br_k = u->k;
	if (sys->cpu.pc == 0x008) br_k = 15;

	if (u->f == 7) {
		sys->cpu.pc = sys->cpu.stk1 | 1;
		if (u->j) {
			sys->cpu.stk1 = sys->cpu.stk2;
		} else {
			sys->cpu.stk1 = sys->cpu.stk2; // bugfix?
			//sys->cpu.stk1 = sys->cpu.pc;	// bad?
			// rc = 1;
		}
	}

	uint8_t g = 0, h = 0;
	switch(u->h) {
	case 0: h = sys->cpu.acc; break;
	case 1: h = sys->cpu.ah; break;
	case 2: h = sys->cpu.am; break;
	case 3: h = sys->cpu.al; break;
	case 4: h = sys->cpu.dh; break;
	case 5: h = sys->cpu.dl; break;
	case 6: h = sys->cpu.mr; break;
	case 7: h = sys->cpu.xr; break;
	}

	switch(u->g) {
	case 0: g = 0; break;
	case 1: g = br_k; break;
	case 2:
		g = sys->cpu.mode0;
		sys->cpu.mode0 &= ~MODE0_STEP;
		break;
	case 3: g = sys->cpu.mode1; break;
	case 4: g = sys->cpu.dh; break;
	case 5: g = sys->cpu.dl; break;
	case 6: g = sys->cpu.mr; break;
	case 7: g = sys->cpu.xr; break;
	}

	uint8_t alu = 0;

	if (!u->l) h = 0; // "15"? "0"? ???
	switch (u->d) {
	case 0:
		if (u->dd) alu = sub3_i(sys, h, g, 0);
		else alu = add3_i(sys, h, g, 0);
		break;
	case 1:
		if (u->dd) alu = sub3_i(sys, h, g, 1);
		else alu = add3_i(sys, h, g, 1);
		break;
	case 2:
		if (u->dd) alu = sub3_c(sys, h, g, 0);
		else alu = add3_c(sys, h, g, 0);
		break;
	case 3:
		if (u->dd) alu = sub3_c(sys, h, g, sys->cpu.c);
		else alu = add3_c(sys, h, g, sys->cpu.c);
		break;
	case 4:
		if (u->dd) alu = sub3_c(sys, h, g, 1);
		else alu = add3_c(sys, h, g, 1);
		break;
	case 5:
		alu = and2(sys, h, g);
		break;
	case 6:
		if (u->dd) alu = xor2(sys, h, g);
		else alu = or2(sys, h, g);
		break;
	case 7:
		// alu = 0;
		break;
	}

	switch(u->c) {
	case 0:	if (u->b == 15) sys->cpu.acc = alu; break;
	case 1:	sys->cpu.ah = alu; break;
	case 2:	sys->cpu.am = alu; break;
	case 3:	sys->cpu.al = alu; break;
	case 4:	sys->cpu.dh = alu; break;
	case 5:	sys->cpu.dl = alu; break;
	case 6:	sys->cpu.mr = alu; break;
	}

	switch(u->b) {
	case 0:
		// nop
		break;
	case 1:
		sys->cpu.acc |= 1;
		break;
	case 2:
		sys->cpu.acc |= 2;
		break;
	case 3:
		sys->cpu.acc |= 4;
		break;
	case 4:
		sys->cpu.acc |= 8;
		break;
	case 5:
		sys->cpu.acc &= ~1;
		break;
	case 6:
		sys->cpu.acc &= ~2;
		break;
	case 7:
		sys->cpu.acc &= ~4;
		break;
	case 8:
		sys->cpu.acc &= ~8;
		break;
	case 9:
		// T.B.D. reset 6184...
		sys->cpu.kp = 0;
		break;
	case 10:
		sys->cpu.acc = (sys->cpu.acc & 0x0e) | (sys->cpu.z ^ 1);
		break;
	case 11:
		sys->cpu.acc = (sys->cpu.acc & 0x0d) | (sys->cpu.z << 1);
		break;
	case 12:
		sys->cpu.pe = 1;
		break;
	case 13:
		sys->cpu.acc = 0;
		break;
	case 14:
		sys->cpu.me = 1;
		break;
	}

	switch(u->a) {
	case 1:	wr_ram_i(sys, m_ah, m_am, m_al); break;
	case 2:	wr_ram_i(sys, 15, br_k, m_al); break;
	case 3:	wr_ram_i(sys, 15, 15, br_k); break;
	case 4:	rd_ram_i(sys, m_ah, m_am, m_al); break;
	case 5:	rd_ram_i(sys, 15, br_k, m_al); break;
	case 6:	rd_ram_i(sys, 15, 15, br_k); break;
	case 7:	printer_hammers(sys); break;
	case 8:	printer_feed(sys); break;
	case 9:	rc = 2; break;
	case 10:
		sys->cpu.dl = (sys->cpu.dl & ~1) | tape_read(sys);
		break;
	case 11:
		tape_write(sys, sys->cpu.dl & 1);
		break;
	case 12:
		printer_status(sys);
		// not just printer, but CN-24 as well...
		sys->cpu.dl |= 2;
		break;
	case 13:
		tape_on(sys, u->g & 1);
		break;
	case 14:
		tape_off(sys);
		break;
	case 15:
		sys->cpu.xh = sys->cpu.dh;	// sys->cpu.xh = g;
		sys->cpu.xl = sys->cpu.dl;	// sys->cpu.xl = h;
		sys->cpu.xs = br_k & 0x07;
		if (sys->cpu.xs == 1) cn24_out(sys);
else printf("XH/XL = %d %d [%d]\n", sys->cpu.xh, sys->cpu.xl, sys->cpu.xs);
		break;
	}

	// This is done "late" to ensure we use most recent flags for I and Z
	if (u->f != 7) {
		if (u->j) {
			sys->cpu.stk2 = sys->cpu.stk1;
			sys->cpu.stk1 = sys->cpu.pc;
		}
		next = u->next << 2;
		switch(u->e) {
		case 0: next |= (0 << 1); break;
		case 1: next |= (1 << 1); break;
		case 2: next |= ((br_acc & 2) >> 0); break;
		case 3: next |= ((br_acc & 8) >> 2); break;
		case 4: next |= (sys->cpu.pe << 1); break;
		case 5: next |= (sys->cpu.i << 1); break;
		case 6:
			next |= (sys->cpu.kp << 1);
			if (sys->cpu.kp) {
				sys->cpu.dh = key >> 4;
				sys->cpu.dl = key & 0x0f;
				sys->cpu.kp = 0;
				sys->display(sys, 0);
			}
			break;
		case 7: rc = 3; break;
		}
		switch(u->f) {
		case 0: next |= (0 << 0); break;
		case 1: next |= (1 << 0); break;
		case 2: next |= ((br_acc & 1) >> 0); break;
		case 3: next |= ((br_acc & 4) >> 2); break;
		case 4: next |= (sys->cpu.z << 0); break;
		case 5: rc = 4; break;
		case 6: next |= (br_c << 0); break;
		case 7: rc = 5; break;
		}
		sys->cpu.pc = next;
	}

	++sys->cpu.cycles;

	display_check(sys);	// this might sleep until UI event...

	sys->keyboard(sys, &key);

	return rc;
}
