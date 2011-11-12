// Copyright (c) 2011 Douglas Miller

#ident "$Id: dw1200.c,v 1.1 2011/11/12 00:27:28 drmiller Exp $"

#include <stdio.h>
#include "w600_ucode.h"

void diwang(char *buf, uint64_t *v) {
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
			case 2: s += sprintf(s, "S<1>"); break;
			case 3: s += sprintf(s, "S<3>"); break;
			case 4: s += sprintf(s, "OV"); break;
			case 5: s += sprintf(s, "CC"); break;
			case 6: s += sprintf(s, "KBD"); break;
			}
			s += sprintf(s, ":");
			switch(u->jl) {
			case 2: s += sprintf(s, "S<0>"); break;
			case 3: s += sprintf(s, "S<2>"); break;
			case 4: s += sprintf(s, "Zo"); break;
			case 5: s += sprintf(s, "Q?"); break;
			case 6: s += sprintf(s, "SC"); break;
			case 7: s += sprintf(s, "1?"); break;
			}
			s += sprintf(s, "]");
		}
	}

	switch(u->ai) {
	case 0: h = "S"; break;
	case 1: h = "T"; break;
	case 2: h = "U"; break;
	case 3: h = "V"; break;
	case 4: h = "KA"; break;
	case 5: h = "KB"; break;
	case 6: h = "CA"; break;
	case 7: h = "CB"; break;
	}

	switch(u->bi) {
	case 0: g = "0"; break;
	case 1: g = k; break;
	case 2: g = "D1"; break;
	case 3: g = "D2"; break;
	case 4: g = "KA"; break;
	case 5: g = "KB"; break;
	case 6: g = "CA"; break;
	case 7: g = "CB"; break;
	}

	if (!u->ac) h = "0"; // "15"? "0"? ???
	if (u->bc) ops = "-----&^$";
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
			s += sprintf(s, " %c SC", ops[u->aop]);
			break;
		}
		s += sprintf(s, " ->[Zo");
		if (u->aop < 5) {
			s += sprintf(s, ",CC");
			switch (u->aop) {
			case 2:
			case 3:
			case 4:
				s += sprintf(s, ",SC");
				break;
			}
		}
		s += sprintf(s, "]");
	}
	char *t = targ;
	if (u->st >=1 && u->st <= 8) {
		sprintf(acc, "S<%d>=%d", (u->st - 1) & 3, ((u->st - 1) >> 2) ^ 1);
	} else {
		switch(u->st) {
		case 0: /* sprintf(mach, "NOP"); */ break;
		case 9: sprintf(mach, "RESET"); break;
		case 10: sprintf(acc, "S<0>=!Z"); break;
		case 11: sprintf(acc, "S<1>=Z"); break;
		case 12: sprintf(mach, "OV=1"); break;
		case 13: sprintf(acc, "S=0"); break;
		case 14: sprintf(mach, "ERR=1"); break;
		}
	}
	if (targ[0] && u->zo != 7) {
		t += sprintf(t, " = ");
	}
	switch(u->zo) {
	case 0:	if (u->st == 15) t += sprintf(t, "S"); break;
	case 1:	t += sprintf(t, "T"); break;
	case 2:	t += sprintf(t, "U"); break;
	case 3:	t += sprintf(t, "V"); break;
	case 4:	t += sprintf(t, "KA"); break;
	case 5:	t += sprintf(t, "KB"); break;
	case 6:	t += sprintf(t, "CA"); break;
	}
	if (targ[0]) {
		t += sprintf(t, " = ");
	}

	switch(u->mop) {
	case 1:	sprintf(opA, "mem(T,U,V) = CA"); break;
	case 2:	sprintf(opA, "mem(15,%s,V) = CA", k); break;
	case 3:	sprintf(opA, "mem(15,15,%s) = CA", k); break;
	case 4:	sprintf(opA, "CA = mem(T,U,V), CB = rom(T,U,V)"); break;
	case 5:	sprintf(opA, "CA = mem(15,%s,V), CB = rom(15,%s,V)", k, k); break;
	case 6:	sprintf(opA, "CA = mem(15,15,%s), CB = rom(15,15,%s)", k, k); break;
	case 7:	sprintf(opA, "KBP <<+ KB<0>"); break;
	case 8:	sprintf(opA, "PPF=1"); break;
	case 9:	sprintf(opA, "<A9>"); break;
	case 10:	sprintf(opA, "KB<0> = MHG/MHO"); break;
	case 11:	sprintf(opA, "WDT = KB<0>"); break;
	case 12:	sprintf(opA, "KA=PC0-3, KB<3>=PC4, KB<1>=RBS"); break;
	case 13:	sprintf(opA, "TMR=1(%s)", u->bi & 1 ? "WR" : "RD"); break;
	case 14:	sprintf(opA, "TMR=0%s", u->bi & 1 ? "(noreset)" : ""); break;
	case 15:	sprintf(opA, "GIOA=KA, GIOB=KB, IOB=%s", k); break;
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
