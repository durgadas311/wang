// Copyright (c) 2011 Douglas Miller

#ifndef __w700_sys_h__
#define __w700_sys_h__

#ident "$Id: w700_sys.h,v 1.6 2012/01/04 15:24:45 drmiller Exp $"

#include "w700_ucode.h"
#include "w700_cpu.h"

typedef w700_cpu_t wang_cpu_t;

#define cpu_overflow	cpu.ofl

#define WANG_UCODE_SIZE	2048
#define WANG_RAM_SIZE	2048
#define WANG_HAS_DISPLAY
#define WANG_HAS_TAPE
#define	WANG_HAS_DEV
#define WANG_SYS_INIT	w700_init

#define SYS_MODEL700A	0
#define SYS_MODEL700B	1
#define SYS_MODEL700C	2
#define SYS_MODEL720A	3
#define SYS_MODEL720B	4
#define SYS_MODEL720C	5

#define WANG_SERIES	700
#define WANG_TYPE	"Programmable Calculator"
#define WANG_SIM	"w700-sim"
#define WANG_DEF_MODEL	"720C"
#define WANG_DEF_ROM	"ROM720C.txt"
#define WANG_GUI_NAME	"w700_fe"

#define WANG_END_PROG	0x5c

#endif // __w700_sys_h__
