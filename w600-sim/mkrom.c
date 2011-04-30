#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>

int main(int argc, char **argv) {
	int x;
	int rc;
	int roms[11];
	int fd;
	uint64_t w;
	uint8_t b;

	if (argc != 11+1+1) {
		fprintf(stderr, "Usage: %s cm*.rom <out-file>\n", argv[0]);
		return 1;
	}

	for (x = 0; x < 11; ++x) {
		roms[x] = open(argv[x+1], O_RDONLY);
		if (roms[x] < 0) {
			perror(argv[x+1]);
			return 1;
		}
	}
	fd = open(argv[12], O_WRONLY|O_CREAT|O_TRUNC, 0666);
	if (fd < 0) {
		perror(argv[12]);
		return 1;
	}
	int eof = 0;
	while (1) {
		w = 0;
		for (x = 0; x < 11; ++x) {
			rc = read(roms[x], &b, 1);
			if (rc < 0) {
				perror(argv[x+1]);
				return 1;
			}
			if (rc == 0) {
				++eof;
				continue;
			}
			w <<= 4;
			w |= (b & 0x0f);
		}
		if (eof) {
			if (eof != 11) {
				fprintf(stderr, "ERROR: premature EOF\n");
			}
			break;
		}
		write(fd, &w, sizeof(w));
	}
	close(fd);

	return (eof != 11);
}
