// Copyright (c) 2011 Douglas Miller

#ifndef __w700_gui_h__
#define __w700_gui_h__

#ident "$Id: w700_gui.h,v 1.1 2011/10/20 17:18:07 drmiller Exp $"

#include "w700_sys.h"

extern int start_fe(w700_sys_t *sys, int ops);
extern void stop_fe(w700_sys_t *sys);
extern void setup_fe(w700_sys_t *sys, int ops);

#endif // __w700_gui_h__
