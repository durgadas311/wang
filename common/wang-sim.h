// Copyright (c) 2011 Douglas Miller

#ifndef __wang_sim_h__
#define __wang_sim_h__

#ident "$Id: wang-sim.h,v 1.3 2011/11/14 04:24:47 drmiller Exp $"

#define TRACE

#ifdef TRACE
#include <stdio.h>
#endif // TRACE
#include <stdint.h>

#define SYS_START_GUI	1
#define SYS_BACK_END	2
#define SYS_WEB_BACKEND	4

#define SYS_MODEL_SHIFT	12
#define SYS_MODEL_NUM	16
#define SYS_MODEL_MASK	((SYS_MODEL_NUM - 1) << SYS_MODEL_SHIFT)

// common to all CPUs
typedef struct {
	uint16_t jam;
	uint16_t next;
	uint16_t pc;
	uint64_t cycles;
	uint64_t cylimit;
} wang_cpu_t;

#ifdef __wang600__

#include "w600_sys.h"

#endif // __wang600__

#ifdef __wang700__

#include "w700_sys.h"

#endif // __wang700__

extern void sys_init(wang_sys_t *sys);
extern void sys_start(wang_sys_t *sys);
extern void sys_loaducode(wang_sys_t *sys, char *exe, uint16_t adr, uint16_t entry);
extern void sys_loadpgm(wang_sys_t *sys, char *pgm);
extern void sys_loadram(wang_sys_t *sys, char *ram);
extern void sys_loadcass(wang_sys_t *sys, char *cass);
extern void sys_setcass(wang_sys_t *sys, char *cass);
extern int sys_go(wang_sys_t *sys, uint16_t entry);
extern void sys_stop(wang_sys_t *sys);

char *get_mach_str(wang_sys_t *sys);
char *get_psw_str(wang_sys_t *sys);

extern void ill_instr(wang_sys_t *sys, uint16_t pc);
extern void sys_exec(wang_sys_t *sys);

#endif // __wang_sim_h__
