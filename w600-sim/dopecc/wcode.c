/*
 * read in xcode-X.o arrays that contain read attempts from wire-weave ROM.
 * AND together all samples for each location, write resulting ROM image.
 */
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>

#define NBITS	44

typedef unsigned long u64;

extern u64 xcode_1[]; /* terminated by (u64)-1 */
extern u64 xcode_2[]; /* terminated by (u64)-1 */

u64 rom600[2048] = {0};

int main(int argc, char **argv) {
	int x;
	int fd;
	char *rom = "wcode-1-2.rom";
	u64 base;
	int ad1, ad2;
	extern char *optarg;

	while ((x = getopt(argc, argv, "r:")) != EOF) {
		switch (x) {
		case 'r':
			rom = optarg;
			break;
		}
	}

	ad1 = ad2 = 0;
	for (x = 0; x < sizeof(rom600) / sizeof(rom600[0]); ++x) {
		base = ~0ul;
		while ((xcode_1[ad1] >> 52) == x) {
			base &= (xcode_1[ad1] & 0x0ffffffffffful);
			++ad1;
		}
		while ((xcode_2[ad2] >> 52) == x) {
			base &= (xcode_2[ad2] & 0x0ffffffffffful);
			++ad2;
		}
		rom600[x] = base;
	}
	fd = open(rom, O_WRONLY | O_CREAT | O_TRUNC, 0666);
	if (fd < 0) {
		perror(rom);
		exit(1);
	}
	x = write(fd, rom600, sizeof(rom600));
	if (x < 0) {
		perror(rom);
		exit(1);
	}
	x = close(fd);
	if (x < 0) {
		perror(rom);
	}
	return 0;
}
