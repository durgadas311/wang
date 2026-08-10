// Copyright (c) 2026, Douglas Miller <durgadas311@gmail.com>

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* look for core wr-wr sequence in ROM720C */

#include "w700_ucode.h"

extern int loaducode_txt(int fd, uint64_t *m, int len);
extern void diwang(char *buf, uint64_t *v);

uint64_t *ucode;

static void check_wrwr(int adr1, w700_ucode_t *u, int adr2) {
	w700_ucode_t *v;

	v = (w700_ucode_t *)&ucode[adr2];
	if (v->mop != 0 && v->mop != 1) return;
	if (v->mop == u->mop) return;
	printf("%03x :: %03x\n", adr1, adr2);
}

int main(int argc, char **argv) {
	int rc;
	w700_ucode_t *u;
	size_t ucodez = 2 * 1024 * sizeof(*ucode);
	int adr;
	int x;
	int t;
	extern char *optarg;
	extern int optind;

	while ((x = getopt(argc, argv, "t:")) != EOF) {
		switch(x) {
		case 't':
			break;
		}
	}

	if (optind + 1 != argc) {
		fprintf(stderr, "Usage: %s ucode-file\n", argv[0]);
		return 1;
	}

	int fd = open(argv[optind], O_RDONLY);
	if (fd < 0) {
		perror(argv[optind]);
		return 1;
	}

	ucode = (uint64_t *)malloc(ucodez);
	x = read(fd, ucode, ucodez);
	if (x < 0) {
		perror(argv[optind]);
		return 1;
	}
	if (x != ucodez) {
		fprintf(stderr, "read() returned %d\n", x);
	}
	close(fd);

	for (x = 0; x < 2 * 1024; ++x) {
		u = (w700_ucode_t *)&ucode[x];
		if (u->mop != 0 && u->mop != 1) continue;
		adr = u->jad << 2;
		if (u->jl == 1) adr |= 0x001;
		if (u->jh == 1) adr |= 0x002;
		check_wrwr(x, u, adr);
		if (u->jl > 1) check_wrwr(x, u, adr | 1);
		if (u->jh > 1) check_wrwr(x, u, adr | 2);
		if (u->jl > 1 && u->jh > 1) check_wrwr(x, u, adr | 3);
	}

	free(ucode);
	return 0;
}
