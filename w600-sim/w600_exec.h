#ifndef __w600_exec_h__
#define __w600_exec_h__

// $Id: w600_exec.h,v 1.3 2011/05/09 10:10:55 drmiller Exp $
#include "w600_sys.h"

char *get_mach_str(w600_sys_t *sys);
char *get_psw_str(w600_sys_t *sys);

extern void ill_instr(w600_sys_t *sys);
extern void sys_exec(w600_sys_t *sys);

#endif // __w600_exec_h__
