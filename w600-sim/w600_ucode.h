// $Id: w600_ucode.h,v 1.1 2011/04/30 21:36:10 drmiller Exp $

#ifndef __w600_ucode_h__
#define __w600_ucode_h__

#include <stdint.h>

typedef struct {
	uint64_t _pad:2;
	uint64_t f:3;
	uint64_t e:3;
	uint64_t next:9;
	uint64_t j:1;
	uint64_t b:4;
	uint64_t k:4;
	uint64_t a:4;
	uint64_t dd:1;
	uint64_t l:1;
	uint64_t d:3;
	uint64_t c:3;
	uint64_t g:3;
	uint64_t h:3;
	uint64_t _unused: 2;
} w600_ucode_t;

#endif // __w600_ucode_h__
