// Copyright (c) 2011 Douglas Miller

#ifndef __w700_sys_h__
#define __w700_sys_h__

#ident "$Id: w700_sys.h,v 1.3 2011/11/06 01:04:12 drmiller Exp $"

#include "w700_ucode.h"
#include "w700_cpu.h"

typedef struct w700_sys_s {
	w700_cpu_t cpu;
	uint64_t ucode[2048];
	uint8_t ram[2048];
	void (*fault)(struct w700_sys_s *sys, const char *str);
	int (*intr)(struct w700_sys_s *sys, int sig);	// return != 0 if signal not handled
	void (*display)(struct w700_sys_s *sys, int on);
	void (*keyboard)(struct w700_sys_s *sys, uint16_t *kc, int ack);
	uint8_t (*tape)(struct w700_sys_s *sys, int wr, uint8_t nibble);
	void (*dev)(struct w700_sys_s *sys, uint8_t chr, uint8_t sts);
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} w700_sys_t;
typedef w700_sys_t wang_sys_t;

#define SYS_MODEL700A	0
#define SYS_MODEL700B	1
#define SYS_MODEL700C	2
#define SYS_MODEL720A	3
#define SYS_MODEL720B	4
#define SYS_MODEL720C	5

#define WANG_SERIES	700
#define WANG_SIM	"w700-sim"
#define WANG_DEF_MODEL	"720C"
#define WANG_DEF_ROM	"ROM720C.txt"
#define WANG_GUI_NAME	"w700_fe"

#define WANG_END_PROG	0x5c

#endif // __w700_sys_h__
