// Copyright (c) 2011, 2013 Douglas Miller

#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

#ident "$Id: w700ucode2rom.c,v 1.2 2013/12/09 15:36:32 drmiller Exp $"

#include "w700_ucode.h"

char buf[4096];

int main(int argc, char **argv) {
	int rc;
	uint64_t *ucode;
	size_t ucodez = 2*1024*8;

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
	// assume text format...
	extern int loaducode_txt(int fd, uint64_t *m, int len);
	ucode = (uint64_t *)malloc(ucodez);
	rc = loaducode_txt(fd, ucode, ucodez);
	// what's a good error check?
	if (rc != ucodez) {
		fprintf(stderr, "loaducode_txt() returned %d\n", rc);
	}
	close(fd);

	char *out = malloc(strlen(argv[1]) + 16);
	strcpy(out, argv[1]);
	char *t = strrchr(out, '.');
	if (t && strcmp(t, ".txt") == 0) {
		strcpy(t, ".rom");
	} else {
		strcat(out, ".rom");
	}
	fd = open(out, O_WRONLY | O_TRUNC | O_CREAT, 0666);
	if (fd < 0) {
		perror(out);
		exit(1);
	}
	rc = write(fd, ucode, ucodez);
	if (rc != ucodez) {
		perror(out);
		return 1;
	}
	close(fd);
	return 0;
}
