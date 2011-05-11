#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

#define TRACE_RAW_UCODE

#include "w600_ucode.h"
extern void diw600(char *buf, uint64_t *t);

char buf[4096];

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

	for (x = 0; x < ucodez; ++x) {
		uint64_t *m = ucode + x;
		diw600(buf, m);
#ifdef TRACE_RAW_UCODE
		w600_ucode_t *u = (w600_ucode_t *)(m);
#endif // TRACE_RAW_UCODE
		printf("%03x: "
#ifdef TRACE_RAW_UCODE
			"[%x%x%x%x%x%x%x%x%x%x%03x%x%x] "
#endif // TRACE_RAW_UCODE
			"%s\n", x,
#ifdef TRACE_RAW_UCODE
			u->h, u->g, u->c, u->d, u->l, u->dd, u->a, u->k, u->b, u->j,
			u->next << 2, u->e, u->f,
#endif // TRACE_RAW_UCODE
			buf);
	}
	return 0;
}
