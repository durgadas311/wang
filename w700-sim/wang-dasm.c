#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include "w700_ucode.h"

extern int loaducode_txt(int fd, uint64_t *m, int len);
extern void diwang(char *buf, uint64_t *v);

char buf[1024];
uint64_t ucode[2048] = {0};

int main(int argc, char **argv) {
	int fd;
	int n;
	w700_ucode_t *u;

	if (argc != 2) {
		fprintf(stderr, "Usage: %s <ucode.txt>\n", argv[0]);
		exit(1);
	}
	fd = open(argv[1], O_RDONLY);
	if (fd < 0) {
		perror(argv[1]);
		exit(1);
	}
	n = loaducode_txt(fd, ucode, sizeof(ucode));
	close(fd);
	if (n < 0) {
		fprintf(stderr, "Scan error: %s\n", argv[1]);
		exit(1);
	}
	for (n = 0; n < sizeof(ucode) / sizeof(ucode[0]); ++n) {
		u = (w700_ucode_t *)&ucode[n];
		diwang(buf, &ucode[n]);
		printf("%03x: [%x%x%x%x%x%x%x%x%x%x%03x%x%x] %s\n",n,
			u->ai, u->bi, u->zo, u->aop, u->ac, u->bc, u->bd,
			u->mop, u->kk, u->st,
			u->jad << 2, u->jh, u->jl,
			buf);
	}
	return 0;
}
