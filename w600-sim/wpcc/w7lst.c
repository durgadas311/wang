#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

void dump(uint8_t *buf, int len) {
	int step = 0;
	uint8_t *s = buf;
	int n = len;
	while (n > 0) {
		uint8_t c = *s;
		uint8_t a = (c >> 4) & 0x0f;
		uint8_t b = c & 0x0f;
		++s;
		--n;
		printf(" %04d  %02d %02d\n", step, a, b);
		++step;
	}
}

int main(int argc, char **argv) {
	int x;
	int fd;
	struct stat stb;
	uint8_t *buf;

	for (x = 1; x < argc; ++x) {
		fd = open(argv[x], O_RDONLY);
		if (fd == -1) {
			perror(argv[x]);
			continue;
		}
		fstat(fd, &stb);
		buf = malloc(stb.st_size);
		if (buf == NULL) {
			perror("malloc");
			close(fd);
			continue;
		}
		int n = read(fd, buf, stb.st_size);
		if (n != stb.st_size) {
			perror(argv[x]);
			close(fd);
			goto go_next;
		}
		close(fd);
		printf("%s\n", argv[x]);
		dump(buf, (int)stb.st_size);
go_next:
		free(buf);
	}
}
