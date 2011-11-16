// Copyright (c) 2011 Douglas Miller

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#ident "$Id: wang-dasm.c,v 1.3 2011/11/16 21:49:15 drmiller Exp $"

#define TRACE_RAW_UCODE

#ifdef __wang600__
#include "w600_ucode.h"
#endif // __wang600__
#ifdef __wang700__
#include "w700_ucode.h"
#endif // __wang700__
#ifdef __wang1200__
#include "w1200_ucode.h"
#endif // __wang1200__

extern void diwang(char *buf, uint64_t *t);

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
	if (stb.st_size > 2*1024*8) { // must be text format...
		extern int loaducode_txt(int fd, uint64_t *m, int len);
		ucode = (uint64_t *)malloc(2*1024*8);
		rc = loaducode_txt(fd, ucode, 2*1024*8);
		// what's a good error check?
	} else {
		ucode = (uint64_t *)malloc(stb.st_size);
		rc = read(fd, ucode, stb.st_size);
		if (rc != stb.st_size) {
			perror(argv[1]);
			return 1;
		}
	}
	close(fd);

	ucodez = rc / sizeof(*ucode);

#ifdef CALL_COUNT
	for (x = 0; x < ucodez; ++x) {
		int h = 2, l = 2;
		int a,b;
		uint64_t *m = ucode + x;
		wang_ucode_t *u = (wang_ucode_t *)(m);
#if defined(__wang600__) || defined(__wang1200__)
		if (u->jl == 7) continue; // return... no address to count
		if (u->sub) {	// call, implies there will be a return...
			if (++calls[x|1] == 0) calls[x|1] = 255;
		}
#endif // __wang600__ || __wang1200__
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
		diwang(buf, m);

#ifdef TRACE_RAW_UCODE
		wang_ucode_t *u = (wang_ucode_t *)(m);
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
#if defined(__wang600__) || defined(__wang1200__)
			u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->mop, u->kk, u->st,
			u->sub, u->jad << 2, u->jh, u->jl,
#endif // __wang600__ || __wang1200__
#ifdef __wang700__
			u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->bd,
			u->mop, u->kk, u->st,
			u->jad << 2, u->jh, u->jl,
#endif // __wang700__
#endif // TRACE_RAW_UCODE
			buf);
	}
	return 0;
}
