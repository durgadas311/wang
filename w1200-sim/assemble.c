#include <stdio.h>
#include <stdint.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

uint64_t ucode[2048] = {0ULL};

int main(int argc, char **argv) {
	FILE *ifps[11];
	int ofd;
	int x, y;

	if (argc != 13) {
		fprintf(stderr, "Usage: %s <cm5910> <cm5920> ... <cm6010> <outfile>\n",
				argv[0]);
		exit(1);
	}

	for (x = 1; x <= 11; ++x) {
		ifps[x-1] = fopen(argv[x], "r");
		if (ifps[x-1] == NULL) {
			perror(argv[x]);
			exit(1);
		}
	}

	uint32_t adr = 0;
	uint64_t stripe[11];
	while (adr < 2048) {
		
		for (x = 0; x < 11; ++x) {
			uint32_t a;
			y = fscanf(ifps[x], "%x:%llx\n", &a, &stripe[x]);
			if (y != 2) {
				fprintf(stderr, "yikes! %d\n", y);
				exit(1);
			}
			if (a != adr) {
				fprintf(stderr, "oops! %03x %03x\n", adr, a);
				exit(1);
			}
		}
		for (x = 0; x < 11; ++x) {
			int zz = ((10 - x) * 4);
			for (y = 0; y < 16; ++y) {
				int z = (15 - y) * 4;
				ucode[adr + y] |= (((stripe[x] >> z) & 0x0f) << zz);
			}
		}
		adr += 16;
	}

	ofd = open(argv[12], O_WRONLY | O_CREAT | O_TRUNC, 0666);
	if (ofd < 0) {
		perror(argv[12]);
		exit(1);
	}
	x = write(ofd, ucode, sizeof(ucode));
	if (x < 0) {
		perror(argv[12]);
	}
	close(ofd);

	return 0;
}
