#ifndef __w600_cpu_h__
#define __w600_cpu_h__

// $Id: w600_cpu.h,v 1.3 2011/05/01 14:35:23 drmiller Exp $
#include <stdio.h>
#include <stdint.h>

typedef struct {
	uint8_t ah;
	uint8_t am;
	uint8_t al;
	uint8_t mr;
	uint8_t acc;
	uint8_t dh;
	uint8_t dl;
	uint8_t xh;
	uint8_t xl;
	uint8_t xs;
	uint8_t xr;
	// flags...
	uint8_t z;
	uint8_t i;
	uint8_t c;

	uint8_t pe;	// prog err
	uint8_t kp;	// any key down
	uint8_t me;	// mach err
	uint8_t mode0;
	uint8_t mode1;

	uint16_t pc;
	uint16_t stk1;
	uint16_t stk2;

	// --------------------

	uint64_t cycles;
	uint64_t cylimit;
} w600_cpu_t;

#endif // __w600_cpu_h__
