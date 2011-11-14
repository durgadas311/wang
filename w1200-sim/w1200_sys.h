// Copyright (c) 2011 Douglas Miller

#ifndef __w1200_sys_h__
#define __w1200_sys_h__

#ident "$Id: w1200_sys.h,v 1.3 2011/11/14 17:18:10 drmiller Exp $"

#include "w1200_ucode.h"
#include "w1200_cpu.h"

typedef w1200_cpu_t wang_cpu_t;

#define WANG_UCODE_SIZE	2048
#define WANG_RAM_SIZE	256
#define WANG_HAS_TAPE
#define WANG_SYS_INIT	w1200_init

#define SYS_MODEL1200	0
#define SYS_MODEL1220	1
#define SYS_MODEL1222	2

#define WANG_SERIES	1200
#define WANG_SIM	"w1200-sim"
#define WANG_DEF_MODEL	"1200"
#define WANG_DEF_ROM	"wang1200.rom"
#define WANG_GUI_NAME	"w1200_fe"

#define WANG_END_PROG	0x9e

#endif // __w1200_sys_h__
