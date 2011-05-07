// $Id: w600_decode.c,v 1.11 2011/05/07 03:28:01 drmiller Exp $

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
		sys->cpu.pe = 0;
		sys->cpu.me = 0;
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
	case 7:	/* sys->print(); */ break;
	case 8:	/* sys->print_feed(); */ break;
	case 9:	rc = 2; break;
	case 10: /* sys->tape_rd(); */ break;
	case 11: /* sys->tape_wr(); */ break;
	case 12: /* sys->print_stat(); */ break;
	case 13: /* sys->tape_on(u->g & 1); */ break;
	case 14: /* sys->tape_off(); */ break;
	case 15:
		sys->cpu.xh = g;
		sys->cpu.xl = h;
		sys->cpu.xs = br_k & 0x07;
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

	static int disp = 0;
	if ((sys->cpu.pc & 0xffc) == 0x51c) { // display refresh routine...
 		if (!disp) {
			++disp;
			sys->display(sys, disp);
		}
	} else if (disp) {
		disp = 0;
		sys->display(sys, disp);
	}

	sys->keyboard(sys, &key);

	return rc;
}
