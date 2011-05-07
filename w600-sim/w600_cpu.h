#ifndef __w600_cpu_h__
#define __w600_cpu_h__

// $Id: w600_cpu.h,v 1.6 2011/05/07 22:17:11 drmiller Exp $
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

#define MODE0_FP	0x01
#define MODE0_LST_L_P	0x02
#define MODE0_LRN_L_P	0x04
#define MODE0_STEP	0x08

#define MODE1_DEGREES	0x01
#define MODE1_PRT_OFF	0x02

#endif // __w600_cpu_h__
