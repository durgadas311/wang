#ifndef __w600_exec_h__
#define __w600_exec_h__

// $Id: w600_exec.h,v 1.2 2011/05/01 03:49:35 drmiller Exp $
#include "w600_sys.h"

char *get_mach_str(w600_sys_t *sys);
char *get_psw_str(w600_sys_t *sys);

extern void rd_ram(w600_sys_t *sys);
extern void wr_ram(w600_sys_t *sys);
extern void rd_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al);
extern void wr_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al);
extern void ill_instr(w600_sys_t *sys);
extern void sys_exec(w600_sys_t *sys);

#endif // __w600_exec_h__
