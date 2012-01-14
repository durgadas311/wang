// Copyright (c) 2011,2012 Douglas Miller
#ifndef __w1200_cpu_h__
#define __w1200_cpu_h__

#ident "$Id: w1200_cpu.h,v 1.12 2012/01/14 21:48:32 drmiller Exp $"

#include <stdio.h>
#include <stdint.h>

typedef union {
	struct {
		uint16_t ern	:1;	// RECORD lamp
		uint16_t tmr	:1;	// TAPE MOVING (right) lamp
		uint16_t tml	:1;	// TAPE MOVING (left) lamp
		uint16_t eln	:1;	// END OF DOCUMENT lamp
		uint16_t nan	:1;	// NO ADJUST lamp
		uint16_t csl	:1;	// CHAR / STOP lamp
		uint16_t shl	:1;	// SEARCH lamp
		uint16_t skl	:1;	// SKIP lamp
	} ind;
	uint16_t word;
} w1200_ind_t;

typedef struct {
	uint8_t s;
	uint8_t t;
	uint8_t u;
	uint8_t v;
	uint8_t ca;
	uint8_t cb;
	uint8_t ka;
	uint8_t kb;

	uint8_t l; // for compat - never used
	uint8_t m;
	uint8_t n;
	uint8_t to;
	uint8_t ro;

	// flags...
	uint8_t zo;
	uint8_t cc;
	uint8_t sc;

	uint8_t function;	// MOP=8 etc
	uint8_t ls;		// L/S - Lock/Shift on keyboard...

	w1200_ind_t ind;

	uint8_t kbd;	// any key down
	uint8_t d1;	// mode0 switch inputs 1
	uint8_t d2;	// mode1 switch inputs 2
	uint8_t d3;	// mode2 switch inputs 3

	// tape control
	uint8_t right;	// tape device select
	uint8_t tm;	// tape motor control
	uint8_t rv;	// tape movement direction (reverse, forward)
	uint8_t rc;	// tape record
	uint8_t hl;	// tape seek control?
	uint8_t din0;	// left/right tape write clock bit
	uint8_t din1;	// left/right tape write data bit
	uint8_t tck;	// left/right tape read clock bit
	uint8_t dk;	// left/right tape read data bit
	uint8_t lhs;	// tape head engage, left
	uint8_t rhs;	// tape head engage, right
	uint8_t lop;	// tape protected, left (from GUI)
	uint8_t rop;	// tape protected, right (from GUI)

	uint16_t stk1;
	uint16_t stk2;

	wang_sys_cpu_t sys;	// common elements
} w1200_cpu_t;

#define D10_RIGHT	0x01	// Right Tape selected
#define D11_DOUBLE	0x02	// Double (!Single)
#define D12_RECORD	0x04	// RECORD depressed
#define D13_TRANSFER	0x08	// TRANSFER depressed

#define D20_UNK		0x01	// TBD

#endif // __w1200_cpu_h__
