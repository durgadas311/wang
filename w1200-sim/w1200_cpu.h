// Copyright (c) 2011 Douglas Miller
#ifndef __w600_cpu_h__
#define __w600_cpu_h__

#ident "$Id: w1200_cpu.h,v 1.3 2011/11/14 04:24:47 drmiller Exp $"

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

	uint8_t m;
	uint8_t n;
	uint8_t to;
	uint8_t ro;

	// flags...
	uint8_t zo;
	uint8_t cc;
	uint8_t sc;

	uint8_t function;	// MOP=8 etc

	uint16_t ern	:1;	// RECORD lamp
	uint16_t tmr	:1;	// TAPE MOVING (right) lamp
	uint16_t tml	:1;	// TAPE MOVING (left) lamp
	uint16_t eln	:1;	// END OF DOCUMENT lamp
	uint16_t nan	:1;	// NO ADJUST lamp
	uint16_t csl	:1;	// CHAR / STOP lamp
	uint16_t shl	:1;	// SEARCH lamp
	uint16_t skl	:1;	// SKIP lamp
	uint16_t cdl	:1;	// CODE lamp/status *

	uint8_t kbd;	// any key down
	uint8_t d1;	// mode switch inputs 1
	uint8_t d2;	// mode switch inputs 2

	uint16_t stk1;
	uint16_t stk2;

	wang_cpu_t sys;	// common elements
} w600_cpu_t;

#define D10_RIGHT	0x01	// Right Tape selected
#define D11_DOUBLE	0x02	// Double (!Single)
#define D12_RECORD	0x04	// RECORD depressed
#define D13_TRANSFER	0x08	// TRANSFER depressed

#define D20_UNK		0x01	// TBD

#endif // __w600_cpu_h__
