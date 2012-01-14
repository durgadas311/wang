// Copyright (c) 2011,2012 Douglas Miller
#ifndef __w600_cpu_h__
#define __w600_cpu_h__

#ident "$Id: w600_cpu.h,v 1.14 2012/01/14 21:48:42 drmiller Exp $"

#include <stdio.h>
#include <stdint.h>

typedef union {
	struct {
		uint16_t ov:1;	// prog err
		uint16_t err:1;	// mach err
		uint16_t _unused:14;
	} ind;
	uint16_t word;
} w600_ind_t;
		

typedef struct {
	uint8_t s;
	uint8_t t;
	uint8_t u;
	uint8_t v;
	uint8_t ca;
	uint8_t cb;
	uint8_t ka;
	uint8_t kb;

	uint8_t l;
	uint8_t m;
	uint8_t n;
	uint8_t rb;
	uint8_t gioa;
	uint8_t giob;
	uint8_t iob;

	// flags...
	uint8_t zo;
	uint8_t cc;
	uint8_t sc;

	w600_ind_t ind;
	uint8_t kbd;	// any key down
	uint8_t d1;	// mode switch inputs 1
	uint8_t d2;	// mode switch inputs 2

	uint16_t stk1;
	uint16_t stk2;

	wang_sys_cpu_t sys;	// common elements
} w600_cpu_t;

#define D10_FP		0x01	// Floating Point (!Sci)
#define D11_LST_L_P	0x02	// List or Learn+Print
#define D12_LRN_L_P	0x04	// Learn or Learn+Print
#define D13_STEP	0x08	// STEP key pressed

#define D20_DEGREES	0x01	// Degrees (!Radians)
#define D21_PRT_ON	0x02	// Printer On

#endif // __w600_cpu_h__
