// Copyright (c) 2011,2012 Douglas Miller

#ifndef __w600_sys_h__
#define __w600_sys_h__

#ident "$Id: w600_sys.h,v 1.21 2012/01/14 21:48:42 drmiller Exp $"

#include "w600_ucode.h"
#include "w600_cpu.h"

typedef w600_cpu_t wang_cpu_t;

#define cpu_overflow  cpu.ov

#define WANG_UCODE_SIZE	2048
#define WANG_RAM_SIZE	(4096>>1)
#define WANG_ROM_SIZE	(4096>>1)
#define WANG_HAS_DISPLAY
#define WANG_HAS_PRINTER
#define WANG_HAS_TAPE
#define WANG_HAS_DEV
#define WANG_SYS_INIT	w600_init

#define SYS_MODEL600_2TP	0
#define SYS_MODEL600_6TP	1
#define SYS_MODEL600_14TP	2

#define WANG_SERIES	600
#define WANG_TYPE	"Programmable Calculator"
#define WANG_SIM	"w600-sim"
#define WANG_DEF_MODEL	"600-14TP"
#define WANG_DEF_ROM	"wang600.rom"
#define WANG_GUI_NAME	"w600_fe"

#define WANG_END_PROG	0x9e

#endif // __w600_sys_h__
