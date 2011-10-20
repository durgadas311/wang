// Copyright (c) 2011 Douglas Miller

#ident "$Id: dw700.c,v 1.1 2011/10/20 17:18:07 drmiller Exp $"

#include <stdio.h>
#include "w700_ucode.h"

void diw700(char *buf, uint64_t *v) {
	w700_ucode_t *u = (w700_ucode_t *)v;
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

	sprintf(k, "%d", u->kk);

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
	next = u->jad << 2;
	if (u->jh < 2) {
		next |= (u->jh << 1);
	}
	if (u->jl < 2) {
		next |= (u->jl << 0);
	}
	if (u->jl == 7) {
		s += sprintf(s, "return");
	} else {
		if (u->jc) {
			s += sprintf(s, "call");
		} else {
			s += sprintf(s, "jump");
		}
		s += sprintf(s, " %03x", next);
		if (u->jh >= 2 || u->jl >= 2) {
			s += sprintf(s, "[");
			switch(u->jh) {
			case 2: s += sprintf(s, "MS<1>"); break;
			case 3: s += sprintf(s, "MS<3>"); break;
			case 4: s += sprintf(s, "PE"); break;
			case 5: s += sprintf(s, "I"); break;
			case 6: s += sprintf(s, "KP"); break;
			}
			s += sprintf(s, ":");
			switch(u->jl) {
			case 2: s += sprintf(s, "MS<0>"); break;
			case 3: s += sprintf(s, "MS<2>"); break;
			case 4: s += sprintf(s, "Z"); break;
			case 5: s += sprintf(s, "Q?"); break;
			case 6: s += sprintf(s, "C"); break;
			case 7: s += sprintf(s, "1?"); break;
			}
			s += sprintf(s, "]");
		}
	}

	switch(u->ai) {
	case 0: h = "MS"; break;
	case 1: h = "AH"; break;
	case 2: h = "AM"; break;
	case 3: h = "AL"; break;
	case 4: h = "DH"; break;
	case 5: h = "DL"; break;
	case 6: h = "MR"; break;
	case 7: h = "XR"; break;
	}

	switch(u->bi) {
	case 0: g = "0"; break;
	case 1: g = k; break;
	case 2: g = "mode0"; break;
	case 3: g = "mode1"; break;
	case 4: g = "DH"; break;
	case 5: g = "DL"; break;
	case 6: g = "MR"; break;
	case 7: g = "XR"; break;
	}

	if (!u->ac) h = "0"; // "15"? "0"? ???
	if (u->an) ops = "-----&^$";
	if (u->aop == 7) {
		sprintf(alu, "0");
	} else {
		s = alu;
		s += sprintf(s, "%s %c %s", h, ops[u->aop], g);
		switch (u->aop) {
		case 1:
		case 4:
			s += sprintf(s, " %c 1", ops[u->aop]);
			break;
		case 3:
			s += sprintf(s, " %c C", ops[u->aop]);
			break;
		}
		s += sprintf(s, " ->[Z");
		if (u->aop < 5) {
			s += sprintf(s, ",I");
			switch (u->aop) {
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
	if (u->st >=1 && u->st <= 8) {
		sprintf(acc, "MS<%d>=%d", (u->st - 1) & 3, ((u->st - 1) >> 2) ^ 1);
	} else {
		switch(u->st) {
		case 0: /* sprintf(mach, "NOP"); */ break;
		case 9: sprintf(mach, "RESET"); break;
		case 10: sprintf(acc, "MS<0>=!Z"); break;
		case 11: sprintf(acc, "MS<1>=Z"); break;
		case 12: sprintf(mach, "PE=1"); break;
		case 13: sprintf(acc, "MS=0"); break;
		case 14: sprintf(mach, "ME=1"); break;
		}
	}
	if (targ[0] && u->zo != 7) {
		t += sprintf(t, " = ");
	}
	switch(u->zo) {
	case 0:	if (u->st == 15) t += sprintf(t, "MS"); break;
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

	switch(u->mop) {
	case 1:	sprintf(opA, "mem(AH,AM,AL) = MR"); break;
	case 2:	sprintf(opA, "mem(15,%s,AL) = MR", k); break;
	case 3:	sprintf(opA, "mem(15,15,%s) = MR", k); break;
	case 4:	sprintf(opA, "MR = mem(AH,AM,AL), XR = rom(AH,AM,AL)"); break;
	case 5:	sprintf(opA, "MR = mem(15,%s,AL), XR = rom(15,%s,AL)", k, k); break;
	case 6:	sprintf(opA, "MR = mem(15,15,%s), XR = rom(15,15,%s)", k, k); break;
	case 7:	sprintf(opA, "PH += DL<0>"); break;
	case 8:	sprintf(opA, "PF=1"); break;
	case 9:	sprintf(opA, "<A9>"); break;
	case 10:	sprintf(opA, "DL<0> = TR"); break;
	case 11:	sprintf(opA, "TW = DL<0>"); break;
	case 12:	sprintf(opA, "DH=PD, DL<3>=PI, DL<1>=SB"); break;
	case 13:	sprintf(opA, "TM=1(%s)", u->bi & 1 ? "WR" : "RD"); break;
	case 14:	sprintf(opA, "TM=0%s", u->bi & 1 ? "(noreset)" : ""); break;
	case 15:	sprintf(opA, "XH=DH, XL=DL, XS=%s", k); break;
	}

	s = buf;
	//if (u->ac) *s++ = '*'; else *s++ = '_';
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
