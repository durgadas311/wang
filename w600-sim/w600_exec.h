// Copyright (c) 2011 Douglas Miller

#ifndef __w600_exec_h__
#define __w600_exec_h__

#ident "$Id: w600_exec.h,v 1.4 2011/05/13 12:40:17 drmiller Exp $"

#include "w600_sys.h"

char *get_mach_str(w600_sys_t *sys);
char *get_psw_str(w600_sys_t *sys);

extern void ill_instr(w600_sys_t *sys);
extern void sys_exec(w600_sys_t *sys);

#endif // __w600_exec_h__
