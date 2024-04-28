// Copyright (c) 2023, Douglas Miller <durgadas311@gmail.com>

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

/* look for orphaned code in Wang 600 ROM code (binary file) */

#define ROM_SIZE	(2 * 1024)	/* number of words in ROM */

#include "w600_ucode.h"

extern int loaducode_txt(int fd, uint64_t *m, int len);
extern void diwang(char *buf, uint64_t *v);

char calls[ROM_SIZE] = {0};
char buf[1024];

int show = 0;

int main(int argc, char **argv) {
	int rc;
	uint64_t *ucode;
	w600_ucode_t *u;
	size_t ucodez = ROM_SIZE * sizeof(*ucode);
	int adr;
	int x;
	int t;
	extern char *optarg;
	extern int optind;

	while ((x = getopt(argc, argv, "t:")) != EOF) {
		switch(x) {
		case 't':
			show = strtoul(optarg, NULL, 0);
			break;
		}
	}

	if (optind + 1 != argc) {
		fprintf(stderr, "Usage: %s [-t num] ucode-file\n", argv[0]);
		return 1;
	}

	int fd = open(argv[optind], O_RDONLY);
	if (fd < 0) {
		perror(argv[optind]);
		return 1;
	}
	ucode = (uint64_t *)malloc(ucodez);
	if (!ucode) {
		fprintf(stderr, "Out of memory\n");
		return 1;
	}
	x = read(fd, ucode, ucodez);
	if (x < 0) {
		perror(argv[optind]);
		return 1;
	}
	if (x < ucodez) {
		fprintf(stderr, "Image too small\n");
		return 1;
	}
	close(fd);

	/* these are called by hardware */
	calls[0x000]++;
	calls[0x001]++;
	calls[0x002]++;
	calls[0x003]++;
	calls[0x004]++;
	calls[0x005]++;
	calls[0x006]++;
	calls[0x007]++;

	for (x = 0; x < ROM_SIZE; ++x) {
		u = (w600_ucode_t *)&ucode[x];
		if (u->jl == 7) continue;
		if (u->sub) calls[x | 0x001]++;
		adr = u->jad << 2;
		if (u->jl == 1) adr |= 0x001;
		if (u->jh == 1) adr |= 0x002;
		calls[adr]++;
		if (u->jl > 1) calls[adr | 0x001]++;
		if (u->jh > 1) calls[adr | 0x002]++;
		if (u->jl > 1 && u->jh > 1) calls[adr | 0x003]++;
	}

	for (x = 0; x < ROM_SIZE; ++x) {
		if (ucode[x] != 0UL && calls[x] == 0) {
			t = 0;
			adr = x;
			do {
				u = (w600_ucode_t *)&ucode[adr];
				diwang(buf, &ucode[adr]);
				printf("%.*s%03x: [%x%x%x%x%x%x%x%x%x%x%03x%x%x] %s\n",
					t, "                            ", adr,
					u->ai, u->bi, u->zo, u->aop, u->ac, u->bc,
					u->mop, u->kk, u->st,
					u->sub, u->jad << 2, u->jh, u->jl,
					buf);
				if (u->jl > 1 || u->jh > 1) {
					break;
				}
				adr = u->jad << 2;
				if (u->jl == 1) adr |= 0x001;
				if (u->jh == 1) adr |= 0x002;
			} while (++t < show && calls[adr] < 2);
		}
	}

	free(ucode);
	return 0;
}
