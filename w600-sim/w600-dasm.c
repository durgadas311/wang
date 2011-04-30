#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#include "w600_ucode.h"
extern void diw600(char *buf, uint64_t *t);

#define FLAG	0

char buf[4096];

#if FLAG
void flip_all(uint64_t *ucode, size_t ucodez) {
#if 0
	uint8_t *m = (uint8_t *)ucode;
	uint8_t b, c;
	size_t x = ucodez * sizeof(*ucode);
	int i;

	while (x) {
		b = *m;
		c = 0;
		for (i = 0; i < 8; ++i) {
			c <<= 1;
			c |= ((b & 0x80) >> 7);
			b <<= 1;
		}
		*m++ = c;
		--x;
	}
#else
	uint64_t a, b;
	int x, i;

	for (x = 0; x < ucodez; ++x) {
		a = ucode[x];
		b = 0;
		for (i = 0; i < 8; ++i) {
			b <<= 8;
			b |= (a & 0xff);
			a >>= 8;
		}
		ucode[x] = b;
	}
#endif
}
#endif

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
#if 0
	printf("sizeof(w600_ucode_t) = %d\n", sizeof(w600_ucode_t));
	*ucode = 0x20000000ffffffffULL;
	printf("%016llx: H=%x ... F=%x\n", *ucode, ((w600_ucode_t *)ucode)->h, ((w600_ucode_t *)ucode)->f);
#endif
	rc = read(fd, ucode, stb.st_size);
	if (rc != stb.st_size) {
		perror(argv[1]);
		return 1;
	}
	close(fd);

	ucodez = stb.st_size / sizeof(*ucode);

#if FLAG
	flip_all(ucode, ucodez);

	sprintf(buf, "%s.new", argv[1]);
	fd = open(buf, O_WRONLY|O_CREAT|O_TRUNC, 0666);
	if (fd < 0) {
		perror(buf);
		return 1;
	}

	rc = write(fd, ucode, stb.st_size);
	if (rc != stb.st_size) {
		perror(buf);
		return 1;
	}
	rc = close(fd);
	if (rc < 0) {
		perror(buf);
		return 1;
	}
#else
	for (x = 0; x < ucodez; ++x) {
		s = buf;
		s += sprintf(s, "%03x: ", x);
		diw600(s, ucode + x);
		printf("%s\n", buf);
	}
#endif
	return 0;
}
