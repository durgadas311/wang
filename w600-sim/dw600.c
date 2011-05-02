// $Id: dw600.c,v 1.7 2011/05/02 18:26:59 drmiller Exp $

#include <stdio.h>
#include "w600_ucode.h"

void diw600(char *buf, uint64_t *v) {
	w600_ucode_t *u = (w600_ucode_t *)v;
	char *s;

	char *g = NULL;
	char *h = NULL;
	char *ops = "+++++&|$";
	static char k[4096];
	static char alu[4096];
	static char acc[4096];
	static char mach[4096];
	static char stack[4096];
	static char targ[4096];
	static char opA[4096];
	uint16_t next;

	alu[0] = '\0';
	acc[0] = '\0';
	mach[0] = '\0';
	stack[0] = '\0';
	targ[0] = '\0';
	opA[0] = '\0';

	sprintf(k, "%d", u->k);

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
	s = stack;
	next = u->next << 2;
	if (u->e < 2) {
		next |= (u->e << 1);
	}
	if (u->f < 2) {
		next |= (u->f << 0);
	}
	if (u->f == 7) {
		s += sprintf(s, "return");
	} else {
		if (u->j) {
			s += sprintf(s, "call");
		} else {
			s += sprintf(s, "jump");
		}
		s += sprintf(s, " %03x", next);
		if (u->e >= 2 || u->f >= 2) {
			s += sprintf(s, "[");
			switch(u->e) {
			case 2: s += sprintf(s, "bit1(ACC)"); break;
			case 3: s += sprintf(s, "bit3(ACC)"); break;
			case 4: s += sprintf(s, "Prog Err"); break;
			case 5: s += sprintf(s, "I"); break;
			case 6: s += sprintf(s, "Key Down"); break;
			}
			s += sprintf(s, ":");
			switch(u->f) {
			case 2: s += sprintf(s, "bit0(ACC)"); break;
			case 3: s += sprintf(s, "bit2(ACC)"); break;
			case 4: s += sprintf(s, "Z"); break;
			case 5: s += sprintf(s, "X?"); break;
			case 6: s += sprintf(s, "C"); break;
			case 7: s += sprintf(s, "1?"); break;
			}
			s += sprintf(s, "]");
		}
	}

	switch(u->h) {
	case 0: h = "ACC"; break;
	case 1: h = "AH"; break;
	case 2: h = "AM"; break;
	case 3: h = "AL"; break;
	case 4: h = "DH"; break;
	case 5: h = "DL"; break;
	case 6: h = "MR"; break;
	case 7: h = "XR"; break;
	}

	switch(u->g) {
	case 0: g = "0"; break;
	case 1: g = k; break;
	case 2: g = "mode0"; break;
	case 3: g = "mode1"; break;
	case 4: g = "DH"; break;
	case 5: g = "DL"; break;
	case 6: g = "MR"; break;
	case 7: g = "XR"; break;
	}

	if (!u->l) h = "0*"; // "15"? "0"? ???
	if (u->dd) ops = "-----&^$";
	if (u->d == 7) {
		sprintf(alu, "0");
	} else {
		s = alu;
		s += sprintf(s, "%s %c %s", h, ops[u->d], g);
		switch (u->d) {
		case 1:
		case 4:
			s += sprintf(s, " %c 1", ops[u->d]);
			break;
		case 3:
			s += sprintf(s, " %c CY", ops[u->d]);
			break;
		}
		s += sprintf(s, " ->[Z");
		if (u->d < 5) {
			s += sprintf(s, ",I");
			switch (u->d) {
			case 2:
			case 3:
			case 4:
				s += sprintf(s, ",C");
				break;
			}
		}
		s += sprintf(s, "]");
	}
	char *t = targ;
	if (u->b >=1 && u->b <= 8) {
		sprintf(acc, "bit%d(ACC)=%d", (u->b - 1) & 3, ((u->b - 1) >> 2) ^ 1);
	} else {
		switch(u->b) {
		case 0: /* sprintf(mach, "NOP"); */ break;
		case 9: sprintf(mach, "reset 6184"); break;
		case 10: sprintf(acc, "bit0(ACC)=Z"); break;
		case 11: sprintf(acc, "bit1(ACC)=!Z"); break;
		case 12: sprintf(mach, "Prog Err"); break;
		case 13: sprintf(acc, "ACC=0"); break;
		case 14: sprintf(mach, "Mach Err"); break;
		}
	}
	if (targ[0] && u->c != 7) {
		t += sprintf(t, " = ");
	}
	switch(u->c) {
	case 0:	if (u->b == 15) t += sprintf(t, "ACC"); break;
	case 1:	t += sprintf(t, "AH"); break;
	case 2:	t += sprintf(t, "AM"); break;
	case 3:	t += sprintf(t, "AL"); break;
	case 4:	t += sprintf(t, "DH"); break;
	case 5:	t += sprintf(t, "DL"); break;
	case 6:	t += sprintf(t, "MR"); break;
	}
	if (targ[0]) {
		t += sprintf(t, " = ");
	}

	switch(u->a) {
	case 1:	sprintf(opA, "mem(AH,AM,AL) = MR"); break;
	case 2:	sprintf(opA, "mem(15,%s,AL) = MR", k); break;
	case 3:	sprintf(opA, "mem(15,15,%s) = MR", k); break;
	case 4:	sprintf(opA, "MR = mem(AH,AM,AL)"); break;
	case 5:	sprintf(opA, "MR = mem(15,%s,AL)", k); break;
	case 6:	sprintf(opA, "MR = mem(15,15,%s)", k); break;
	case 7:	sprintf(opA, "Printer Hammer = bit0(DL)"); break;
	case 8:	sprintf(opA, "Printer Feed"); break;
	case 9:	sprintf(opA, "<A9>"); break;
	case 10:	sprintf(opA, "bit0(DL) = Tape Data"); break;
	case 11:	sprintf(opA, "Tape Data = bit0(DL)"); break;
	case 12:	sprintf(opA, "(DH,DL) = (Printer Status)"); break;
	case 13:	sprintf(opA, "Tape On %s", u->g & 1 ? "WR" : "RD"); break;
	case 14:	sprintf(opA, "Tape Off"); break;
	case 15:	sprintf(opA, "XH=%s,XL=%s,XS=%s", g, h, k); break;
	}

	s = buf;
	//if (u->l) *s++ = '*'; else *s++ = '_';
	s += sprintf(s, "%s%s", targ, alu);
	if (acc[0]) {
		s += sprintf(s, "; %s", acc);
	}
	if (mach[0]) {
		s += sprintf(s, "; %s", mach);
	}
	if (opA[0]) {
		s += sprintf(s, "; %s", opA);
	}
	if (stack[0]) {
		s += sprintf(s, "; %s", stack);
	}
}
