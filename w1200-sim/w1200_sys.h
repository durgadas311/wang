// Copyright (c) 2011 Douglas Miller

#ifndef __w1200_sys_h__
#define __w1200_sys_h__

#ident "$Id: w1200_sys.h,v 1.2 2011/11/12 18:11:56 drmiller Exp $"

#include "w1200_ucode.h"
#include "w1200_cpu.h"

typedef struct w1200_sys_s {
	w1200_cpu_t cpu;
	uint64_t ucode[2048];
	uint8_t ram[256]; // 256x8
	void (*fault)(struct w1200_sys_s *sys, const char *str);
	int (*intr)(struct w1200_sys_s *sys, int sig);	// return != 0 if signal not handled
	void (*display)(struct w1200_sys_s *sys, int on);
	void (*keyboard)(struct w1200_sys_s *sys, uint16_t *kc, int ack);
	void (*printer)(struct w1200_sys_s *sys, int col, int drum);
	uint8_t (*tape)(struct w1200_sys_s *sys, int wr, uint8_t nibble);
	void (*dev)(struct w1200_sys_s *sys, uint8_t chr, uint8_t sts);
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} w1200_sys_t;
typedef w1200_sys_t wang_sys_t;

#define SYS_MODEL1200	0
#define SYS_MODEL1220	1
#define SYS_MODEL1222	2

#define WANG_SERIES	1200
#define WANG_SIM	"w1200-sim"
#define WANG_DEF_MODEL	"1200"
#define WANG_DEF_ROM	"wang1200.rom"
#define WANG_GUI_NAME	"w1200_fe"

#define WANG_END_PROG	0x9e

#endif // __w600_sys_h__
