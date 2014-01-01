/*
 *	Copyright (c) 2013 Douglas Miller
 *	$Id: w6ldfix.c,v 1.1 2014/01/01 18:00:21 drmiller Exp $
 *
 * Do post-ld processing (fixup) of Wang 600 Register addresses.
 * Adds ability to enter indirect register addresses by symbol name.
 *
 * The undefined (illegal) op-code sequence 15-08 15-08 RR-RR is used to prefix (escape)
 * a register address, where RR-RR is the 8-bit binary register address (easily generated
 * by ld). The escape is converted into the appropriate codes to enter the register
 * (3-digit) number into the display, for indirect addressing purposes (the register
 * is always accessible by RECALL/STORE instructions). This is also necessary for arrays.
 * Note, it should not be possible for 15-08 15-08 to be a legitimate sequence in a program
 * (even considering ALPHA or 2-step sequences).
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>

#include "wang600opcodes.h"

static void w6ldfixup(uint8_t *buf, int len) {
	uint8_t *end = buf + len;
	while (buf < end) {
		len = end - buf;
		if (len >= 3 && buf[0] == _tag_last_reg && buf[1] == _tag_last_reg) {
			int reg = buf[2];
			buf[0] = _pre_E | (reg / 100);
			buf[1] = _pre_E | ((reg / 10) % 10);
			buf[2] = _pre_E | (reg % 10);
			buf += 3;
		} else {
			buf += 1;
		}
	}
}

int main(int argc, char **argv) {
	int x;
	char *t;
	if (argc < 2) {
		fprintf(stderr, "Usage: %s <file> [...]\n", argv[0]);
		exit(1);
	}
	for (x = 1; x < argc; ++x) {
		t = strrchr(argv[x], '.');
		if (t == NULL || strcmp(t, ".w6$") != 0) {
			fprintf(stderr, "%s: filename must match *.w6$\n", argv[x]);
			exit(1);
		}
		int fd = open(argv[x], O_RDONLY);
		if (fd < 0) {
			perror(argv[x]);
			exit(1);
		}
		struct stat stb;
		fstat(fd, &stb);
		uint8_t *buf = malloc(stb.st_size);
		if (buf == NULL) {
			perror("malloc");
			exit(1);
		}
		int n = read(fd, buf, stb.st_size);
		if (n != stb.st_size) {
			if (n < 0) perror(argv[x]);
			else fprintf(stderr, "%s: incomplete read\n", argv[x]);
			exit(1);
		}
		close(fd);
		w6ldfixup(buf, stb.st_size);
		strcpy(t, ".w6t");
		fd = open(argv[x], O_WRONLY | O_CREAT | O_TRUNC, 0666);
		if (fd < 0) {
			perror(argv[x]);
			exit(1);
		}
		n = write(fd, buf, stb.st_size);
		if (n != stb.st_size) {
			if (n < 0) perror(argv[x]);
			else fprintf(stderr, "%s: incomplete write\n", argv[x]);
			exit(1);
		}
		free(buf);
		n = close(fd);
		if (n < 0) {
			perror(argv[x]);
			exit(1);
		}
	}
	return 0;
}
