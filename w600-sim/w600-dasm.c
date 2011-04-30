#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#include "w600_ucode.h"
extern void diw600(char *buf, uint64_t *t);

char buf[4096];

int main(int argc, char **argv) {
	int x;
	int rc;
	uint64_t *ucode;
	size_t ucodez;
	char *s;

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

	for (x = 0; x < ucodez; ++x) {
		s = buf;
		s += sprintf(s, "%03x: ", x);
		diw600(s, ucode + x);
		printf("%s\n", buf);
	}
	return 0;
}
