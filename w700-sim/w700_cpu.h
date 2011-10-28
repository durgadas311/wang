// Copyright (c) 2011 Douglas Miller
#ifndef __w700_cpu_h__
#define __w700_cpu_h__

#ident "$Id: w700_cpu.h,v 1.3 2011/10/28 01:12:50 drmiller Exp $"

#include <stdio.h>
#include <stdint.h>

typedef struct {
	uint8_t s;
	uint8_t t;
	uint8_t u;
	uint8_t v;
	uint8_t cb;
	uint8_t ca;
	uint8_t ka;
	uint8_t kb;

	uint8_t l;
	uint8_t m;
	uint8_t n;
	uint8_t ra;
	uint8_t rb;
	uint8_t giob;
	uint8_t gioa;
	uint8_t iob;
	// flags...
	uint8_t alu;
	uint8_t cc;
	uint8_t sc;
	uint8_t q;

	uint8_t ofl;	// prog err
	uint8_t kbd;	// any key down
	uint8_t err;	// mach err
	uint8_t d;

	uint16_t next;
	uint16_t pc;

	// --------------------

	uint64_t cycles;
	uint64_t cylimit;
} w700_cpu_t;

#define MODE0_LST_L_P	0x02
#define MODE0_LRN_L_P	0x04
#define MODE0_STEP	0x08

#endif // __w700_cpu_h__
