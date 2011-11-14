// Copyright (c) 2011 Douglas Miller

#ifndef __wang_sim_h__
#define __wang_sim_h__

#ident "$Id: wang-sim.h,v 1.4 2011/11/14 17:18:10 drmiller Exp $"

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
} wang_sys_cpu_t;

#ifdef __wang600__
#include "w600_sys.h"
#endif // __wang600__

#ifdef __wang700__
#include "w700_sys.h"
#endif // __wang700__

#ifdef __wang1200__
#include "w1200_sys.h"
#endif // __wang1200__

typedef struct wang_sys_s {
	wang_cpu_t cpu;
	char * (*get_psw_str)(struct wang_sys_s *sys);
	char * (*get_mach_str)(struct wang_sys_s *sys);
	void   (*get_reg_str)(struct wang_sys_s *sys, char *buf);

	uint64_t ucode[WANG_UCODE_SIZE];
	void (*ucode_override)(struct wang_sys_s *sys);

	uint8_t ram[WANG_RAM_SIZE];
#ifdef WANG_ROM_SIZE
	uint8_t rom[WANG_ROM_SIZE];
#endif
	void (*fault)(struct wang_sys_s *sys, const char *str);
	int (*intr)(struct wang_sys_s *sys, int sig);  // return != 0 if signal not handled
	void (*display)(struct wang_sys_s *sys, int on);
	void (*keyboard)(struct wang_sys_s *sys, uint16_t *kc, int ack);
	int (*special_key)(struct wang_sys_s *sys, uint16_t b);
#ifdef WANG_HAS_PRINTER
	void (*printer)(struct wang_sys_s *sys, int col, int drum);
#endif
#ifdef WANG_HAS_TAPE
	uint8_t (*tape)(struct wang_sys_s *sys, int wr, uint8_t data);
#endif
#ifdef WANG_HAS_DEV
	void (*dev)(struct wang_sys_s *sys, uint8_t chr, uint8_t sts);
#endif

	int ops;
	int run;
	int cmd;	// is command mode allowed?
#ifdef TRACE
	int trace;
	FILE *trc_fp;
#endif // TRACE
} wang_sys_t;


extern void sys_init(wang_sys_t *sys);
extern void sys_start(wang_sys_t *sys);
extern void sys_loaducode(wang_sys_t *sys, char *exe, uint16_t adr, uint16_t entry);
extern void sys_loadpgm(wang_sys_t *sys, char *pgm);
extern void sys_loadram(wang_sys_t *sys, char *ram);
extern void sys_loadcass(wang_sys_t *sys, char *cass);
extern void sys_setcass(wang_sys_t *sys, char *cass);
extern int sys_go(wang_sys_t *sys, uint16_t entry);
extern void sys_stop(wang_sys_t *sys);

extern void ill_instr(wang_sys_t *sys, uint16_t pc);
extern void sys_exec(wang_sys_t *sys);

#endif // __wang_sim_h__
