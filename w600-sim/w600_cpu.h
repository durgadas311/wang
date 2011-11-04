// Copyright (c) 2011 Douglas Miller
#ifndef __w600_cpu_h__
#define __w600_cpu_h__

#ident "$Id: w600_cpu.h,v 1.11 2011/11/04 22:33:00 drmiller Exp $"

#include <stdio.h>
#include <stdint.h>

typedef struct {
	uint8_t s;
	uint8_t t;
	uint8_t u;
	uint8_t v;
	uint8_t ca;
	uint8_t cb;
	uint8_t ka;
	uint8_t kb;

//	uint8_t l;
//	uint8_t m;
//	uint8_t n;
//	uint8_t rb;
	uint8_t gioa;
	uint8_t giob;
	uint8_t iob;

	// flags...
	uint8_t zo;
	uint8_t cc;
	uint8_t sc;

	uint8_t ov;	// prog err
	uint8_t kbd;	// any key down
	uint8_t err;	// mach err
	uint8_t d1;
	uint8_t d2;

	uint16_t next;
	uint16_t pc;
	uint16_t stk1;
	uint16_t stk2;

	// --------------------

	uint64_t cycles;
	uint64_t cylimit;
} w600_cpu_t;

#define D10_FP		0x01
#define D11_LST_L_P	0x02
#define D12_LRN_L_P	0x04
#define D13_STEP	0x08

#define D20_DEGREES	0x01
#define D21_PRT_ON	0x02

#endif // __w600_cpu_h__
