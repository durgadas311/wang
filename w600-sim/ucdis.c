// Copyright (c) 2024, Douglas Miller <durgadas311@gmail.com>

// disassemble all/portions of a microcode image

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#define ROM_SIZE	(2 * 1024)	/* number of words in ROM */

#include "w600_ucode.h"

extern int loaducode_txt(int fd, uint64_t *m, int len);
extern void diwang(char *buf, uint64_t *v);

char buf[1024];

int main(int argc, char **argv) {
	int rc;
	uint64_t *ucode;
	w600_ucode_t *u;
	size_t ucodez = ROM_SIZE * sizeof(*ucode);
	int x;
	int verbose = 0;
	int nop = 0;
	int autoend = 0;
	int begin = 0;
	int end = 0x7ff;
	extern char *optarg;
	extern int optind;

	while ((x = getopt(argc, argv, "ab:e:v")) != EOF) {
		switch(x) {
		case 'a':
			++autoend;
			break;
		case 'b':
			begin = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		case 'e':
			end = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		case 'v':
			++verbose;
			break;
		}
	}

	if (optind + 1 != argc) {
		fprintf(stderr, "Usage: %s [-v][-b adr][-e adr] ucode-file\n", argv[0]);
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

	for (x = begin; x < end; ++x) {
		if (!ucode[x]) {
			++nop;
		} else {
			nop = 0;
		}
		if (autoend && nop > 4) {
			break;
		}
		u = (w600_ucode_t *)&ucode[x];
		diwang(buf, &ucode[x]);
		printf("%03x: ", x);
		if (verbose) {
			printf("[%x%x%x%x%x%x%x%x%x%x%03x%x%x] ",
				u->ai, u->bi, u->zo, u->aop, u->ac, u->bc,
				u->mop, u->kk, u->st,
				u->sub, u->jad << 2, u->jh, u->jl);
		}
		printf("%s\n", buf);
	}

	free(ucode);
	return 0;
}
