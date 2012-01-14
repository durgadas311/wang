// Copyright (c) 2011,2012 Douglas Miller

#include <stdio.h>
#include <stdint.h>

#include "w1200_ucode.h"

int loaducode_txt(int fd, uint64_t *m, int len) {
	static char junk[4096];
	int x, n;
	union {
		uint64_t word;
		w1200_ucode_t flds;
	} u;

	FILE *fp = fdopen(fd, "r");
	if (fp == NULL) return -1;
	int _jad, _jh, _jl;
	int sub, jad, jh, jl;
	int ai, bi, zo, ac, bc;
	int aop, mop, kk, st;

	n = 0;
	while (n < len / sizeof(u)) {
		u.word = 0;
		x = fscanf(fp, "%x %x %x      %x %x %x %x %x %x %x %x %x %x %x %x %x\n",
			&_jad, &_jh, &_jl,
			&ac,
			&bc,
			&ai,
			&bi,
			&zo,
			&aop,
			&mop,
			&kk,
			&st,
			&sub,
			&jh,
			&jl,
			&jad);
		if (x == EOF) {
			break;
		}
		if (x != 16) {
			// not error, just "other" lines
			fgets(junk, sizeof(junk), fp);
			continue;
		}
		u.flds.ac = ac;
		u.flds.bc = bc;
		u.flds.ai = ai;
		u.flds.bi = bi;
		u.flds.zo = zo;
		u.flds.aop = aop;
		u.flds.mop = mop;
		u.flds.kk = kk;
		u.flds.st = st;
		u.flds.sub = sub;
		u.flds.jh = jh;
		u.flds.jl = jl;
		u.flds.jad = jad;
		*m++ = u.word;
if (0) fprintf(stderr, "got[%d] %x %x %x %x %x %x %x %x %x %x %x %x %x\n", n,
		u.flds.ac,
		u.flds.bc,
		u.flds.ai,
		u.flds.bi,
		u.flds.zo,
		u.flds.aop,
		u.flds.mop,
		u.flds.kk,
		u.flds.st,
		u.flds.sub,
		u.flds.jh,
		u.flds.jl,
		u.flds.jad
);

		++n;
	}

	fclose(fp);
	return n * sizeof(u.word);
}

/*
... or a different format? ...
#Wang 600 ROM
		          A M
		A B A B Z O O K S J J J 
		C C I I O P P K T C H L JAD

000 0 0		0 0 1 0 1 3 6 D F D 0 0 1BA
000 0 1		0 0 0 0 0 3 6 8 0 D 0 0 160
000 1 0		0 0 1 0 1 3 6 8 5 D 1 1 161
000 1 1		0 0 0 0 0 3 6 8 0 0 0 1 164
*/
