#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

long verify_prog(uint8_t *buf, int len) {
	uint8_t *s = buf;
	int n = len;
	long vp = 0;
	int two_step = 0;
	while (n > 0) {
		// must ignore END_PROG except at very end...
		uint8_t c = *s;
		if (two_step) {
			if (two_step < 2 || (c & 0x70) >= 0x40) {
				two_step = 0;
			}
		} else if (c == 0x5c) {
			break;	// 700 does NOT count END PROG
		} else if ((c & 0x70) == 0x40) {
			// might be WRITE_ALPHA multi-step
			two_step = 1 + (c == 0x4c);
		}
		uint8_t a = (c >> 4) & 0x0f;
		uint8_t b = c & 0x0f;
		vp += a + b;
		++s;
		--n;
	}
	return vp;
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
		long vp = verify_prog(buf, (int)stb.st_size);
		printf("%s: %ld\n", argv[x], vp);
go_next:
		free(buf);
	}
}
