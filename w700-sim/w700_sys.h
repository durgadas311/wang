// Copyright (c) 2011 Douglas Miller

#ifndef __w700_sys_h__
#define __w700_sys_h__

#ident "$Id: w700_sys.h,v 1.2 2011/10/29 14:41:11 drmiller Exp $"

#include "w700_ucode.h"
#include "w700_cpu.h"

#define TRACE

#ifdef TRACE
#include <stdio.h>
#endif // TRACE

typedef struct w700_sys_s {
	w700_cpu_t cpu;
	uint64_t ucode[2048];
	uint8_t ram[2048];
	void (*fault)(struct w700_sys_s *sys, const char *str);
	int (*intr)(struct w700_sys_s *sys, int sig);	// return != 0 if signal not handled
	void (*display)(struct w700_sys_s *sys, int on);
	void (*keyboard)(struct w700_sys_s *sys, uint16_t *kc, int ack);
	void (*printer)(struct w700_sys_s *sys, int col, int drum);
	uint8_t (*tape)(struct w700_sys_s *sys, int wr, uint8_t nibble);
	void (*dev)(struct w700_sys_s *sys, uint8_t chr, uint8_t sts);
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} w700_sys_t;

#define SYS_START_GUI		1
#define SYS_BACK_END		2
#define SYS_MODEL_SHIFT		12
#define SYS_MODEL_NUM		16
#define SYS_MODEL_MASK		((SYS_MODEL_NUM - 1) << SYS_MODEL_SHIFT)

#define SYS_MODEL700_2TP	0
#define SYS_MODEL700_6TP	1
#define SYS_MODEL700_14TP	2

extern void sys_init(w700_sys_t *sys);
extern void sys_start(w700_sys_t *sys, int ops);
extern void sys_loaducode(w700_sys_t *sys, char *exe, uint16_t adr, uint16_t entry);
extern void sys_loadpgm(w700_sys_t *sys, char *pgm);
extern void sys_loadram(w700_sys_t *sys, char *ram);
extern void sys_loadrom(w700_sys_t *sys, char *rom);
extern void sys_loadcass(w700_sys_t *sys, char *cass);
extern void sys_setcass(w700_sys_t *sys, char *cass);
extern int sys_go(w700_sys_t *sys, uint16_t entry);
extern void sys_stop(w700_sys_t *sys, int ops);

#endif // __w700_sys_h__
