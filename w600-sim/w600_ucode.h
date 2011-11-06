// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_ucode.h,v 1.5 2011/11/06 00:48:45 drmiller Exp $"

#ifndef __w600_ucode_h__
#define __w600_ucode_h__

#include <stdint.h>

typedef struct {
	uint64_t _pad:2;
	uint64_t jl:3;
	uint64_t jh:3;
	uint64_t jad:9;
	uint64_t jc:1;
	uint64_t st:4;
	uint64_t kk:4;
	uint64_t mop:4;
	uint64_t bc:1;
	uint64_t ac:1;
	uint64_t aop:3;
	uint64_t zo:3;
	uint64_t bi:3;
	uint64_t ai:3;
	uint64_t _unused: 20;
} w600_ucode_t;

#endif // __w600_ucode_h__
