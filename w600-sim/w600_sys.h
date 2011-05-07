#ifndef __w600_sys_h__
#define __w600_sys_h__

// $Id: w600_sys.h,v 1.6 2011/05/07 16:51:02 drmiller Exp $

#include "w600_ucode.h"
#include "w600_cpu.h"

#define TRACE

#ifdef TRACE
#include <stdio.h>
#endif // TRACE

typedef struct w600_sys_s {
	w600_cpu_t cpu;
	uint64_t ucode[2048];
	uint8_t ram[2048]; // 4096x4
	void (*fault)(struct w600_sys_s *sys, const char *str);
	int (*intr)(struct w600_sys_s *sys, int sig);	// return != 0 if signal not handled
	void (*display)(struct w600_sys_s *sys, int on);
	void (*keyboard)(struct w600_sys_s *sys, uint8_t *kc);
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} w600_sys_t;

#define SYS_START_GUI	1

extern void sys_init(w600_sys_t *sys);
extern void sys_start(w600_sys_t *sys, int ops);
extern void sys_loadpgm(w600_sys_t *sys, char *exe, uint16_t adr, uint16_t entry);
extern int sys_go(w600_sys_t *sys, uint16_t entry);

#endif // __w600_sys_h__
