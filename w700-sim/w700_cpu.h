// Copyright (c) 2011 Douglas Miller
#ifndef __w700_cpu_h__
#define __w700_cpu_h__

#ident "$Id: w700_cpu.h,v 1.4 2011/11/06 01:04:12 drmiller Exp $"

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
	uint8_t d;	// mode switch inputs

	wang_cpu_t sys;	// common elements
} w700_cpu_t;

#define D11_LST_L_P	0x02	// List or Learn+Print
#define D12_LRN_L_P	0x04	// Learn or Learn+Print
#define D13_STEP	0x08	// STEP key pressed

#endif // __w700_cpu_h__
