// Copyright (c) 2011 Douglas Miller

#ifndef __w600_gui_h__
#define __w600_gui_h__

#ident "$Id: w600_gui.h,v 1.3 2011/05/15 23:10:44 drmiller Exp $"

#include "w600_sys.h"

extern int start_fe(w600_sys_t *sys);
extern void stop_fe(w600_sys_t *sys);
extern void setup_fe(w600_sys_t *sys, int ops);

#endif // __w600_gui_h__
