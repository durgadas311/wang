// Copyright (c) 2011 Douglas Miller

#ifndef __w600_sys_h__
#define __w600_sys_h__

#ident "$Id: w600_sys.h,v 1.17 2011/11/06 00:48:45 drmiller Exp $"

#include "w600_ucode.h"
#include "w600_cpu.h"

typedef struct w600_sys_s {
	w600_cpu_t cpu;
	uint64_t ucode[2048];
	uint8_t ram[2048]; // 4096x4
	uint8_t rom[2048]; // 4096x4
	void (*fault)(struct w600_sys_s *sys, const char *str);
	int (*intr)(struct w600_sys_s *sys, int sig);	// return != 0 if signal not handled
	void (*display)(struct w600_sys_s *sys, int on);
	void (*keyboard)(struct w600_sys_s *sys, uint16_t *kc, int ack);
	void (*printer)(struct w600_sys_s *sys, int col, int drum);
	uint8_t (*tape)(struct w600_sys_s *sys, int wr, uint8_t nibble);
	void (*dev)(struct w600_sys_s *sys, uint8_t chr, uint8_t sts);
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} w600_sys_t;
typedef w600_sys_t wang_sys_t;

#define SYS_MODEL600_2TP	0
#define SYS_MODEL600_6TP	1
#define SYS_MODEL600_14TP	2

#define WANG_SERIES	600
#define WANG_SIM	"w600-sim"
#define WANG_DEF_MODEL	"600-14TP"
#define WANG_DEF_ROM	"wang600.rom"
#define WANG_GUI_NAME	"w600_fe"

#define WANG_END_PROG	0x9e

#endif // __w600_sys_h__
