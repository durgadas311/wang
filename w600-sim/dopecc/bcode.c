/*
 * Dump the ROM range of wire-weave ROM images (0x7fc-0x7ff) in binary.
 */
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>

typedef unsigned long u64;

u64 rom600[2048] = {0};

int main(int argc, char **argv) {
	int x;
	int fd;
	char *rom = "wang600.rom";
	u64 base;
	int adr;
	int begin = 0x7fc;
	int end = 0x7ff;
	int bit0 = 2;	// Wang 500/600
	int bitn = 43;	// Wang 500/600
	extern char *optarg;

	while ((x = getopt(argc, argv, "7b:e:r:")) != EOF) {
		switch (x) {
		case '7':
			bit0 = 1;
			bitn = 43;
			break;
		case 'b':
			begin = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		case 'e':
			end = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		case 'r':
			rom = optarg;
			break;
		}
	}
	fd = open(rom, O_RDONLY);
	if (fd < 0) {
		perror(rom);
		exit(1);
	}
	x = read(fd, rom600, sizeof(rom600));
	if (x < 0) {
		perror(rom);
		exit(1);
	}
	if (x != sizeof(rom600)) {
		fprintf(stderr, "%s: wrong size\n", rom);
		exit(1);
	}
	close(fd);

	for (adr = begin; adr <= end; ++adr) {
		printf("%03x: ", adr);
		base = rom600[adr];
		// printf("%012lx ", base);
		for (x = bitn; x >= bit0; --x) {
			putchar((base & (1UL << x)) ? '1' : '0');
		}
		putchar('\n');
	}
	return 0;
}
