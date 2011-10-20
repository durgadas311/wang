// Copyright (c) 2011 Douglas Miller

#ifndef __w700_exec_h__
#define __w700_exec_h__

#ident "$Id: w700_exec.h,v 1.1 2011/10/20 17:18:07 drmiller Exp $"

#include "w700_sys.h"

char *get_mach_str(w700_sys_t *sys);
char *get_psw_str(w700_sys_t *sys);

extern void ill_instr(w700_sys_t *sys);
extern void sys_exec(w700_sys_t *sys);

#endif // __w700_exec_h__
