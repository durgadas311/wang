// Copyright (c) 2011 Douglas Miller

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#ident "$Id: w600-dasm.c,v 1.8 2011/11/03 12:57:33 drmiller Exp $"

#define TRACE_RAW_UCODE

#include "w600_ucode.h"
extern void diw600(char *buf, uint64_t *t);

char buf[4096];

#ifdef CALL_COUNT
uint8_t calls[2048] = {0};
#endif

int main(int argc, char **argv) {
	int x;
	int rc;
	uint64_t *ucode;
	size_t ucodez;

	if (argc != 2) {
		fprintf(stderr, "Usage: %s ucode-file\n", argv[0]);
		return 1;
	}

	int fd = open(argv[1], O_RDONLY);
	if (fd < 0) {
		perror(argv[1]);
		return 1;
	}
	struct stat stb;
	fstat(fd, &stb);
	ucode = (uint64_t *)malloc(stb.st_size);
	rc = read(fd, ucode, stb.st_size);
	if (rc != stb.st_size) {
		perror(argv[1]);
		return 1;
	}
	close(fd);

	ucodez = stb.st_size / sizeof(*ucode);

#ifdef CALL_COUNT
	for (x = 0; x < ucodez; ++x) {
		int h = 2, l = 2;
		int a,b;
		uint64_t *m = ucode + x;
		w600_ucode_t *u = (w600_ucode_t *)(m);
		if (u->jl == 7) continue; // return... no address to count
		if (u->jc) {	// call, implies return...
			if (++calls[x|1] == 0) calls[x|1] = 255;
		}
		uint16_t t = u->jad << 2;
		if (u->jl < 2) {
			t |= u->jl;
			l = 1;
		}
		if (u->jh < 2) {
			t |= (u->jh << 1);
			h = 1;
		}

		for (a = 0; a < h; ++a) {
		for (b = 0; b < l; ++b) {
			uint16_t g = t | (a << 1)|b;
			if (++calls[g] == 0) calls[g] = 255;
		}}
	}
#endif

	for (x = 0; x < ucodez; ++x) {
		uint64_t *m = ucode + x;
		diw600(buf, m);
#ifdef TRACE_RAW_UCODE
		w600_ucode_t *u = (w600_ucode_t *)(m);
#endif // TRACE_RAW_UCODE
		printf("%03x: "
#ifdef CALL_COUNT
			"(%d) "
#endif
#ifdef TRACE_RAW_UCODE
			"[%x%x%x%x%x%x%x%x%x%x%03x%x%x] "
#endif // TRACE_RAW_UCODE
			"%s\n", x,
#ifdef CALL_COUNT
			calls[x],
#endif
#ifdef TRACE_RAW_UCODE
			u->ai, u->bi, u->zo, u->aop, u->ac, u->an, u->mop, u->kk, u->st,
			u->jc, u->jad << 2, u->jh, u->jl,
#endif // TRACE_RAW_UCODE
			buf);
	}
	return 0;
}
