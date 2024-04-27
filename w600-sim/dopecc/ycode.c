/*
 * read in wang600.rom plus additional ROM and compare, tabulating
 * bits that differ.
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

int plus[NBITS], minus[NBITS];
int all_plus[NBITS] = {0};
int all_minus[NBITS] = {0};

u64 rom600[2048];
u64 chkrom[2048];

char *bit_names[NBITS] = {
[0] = "?0",
[1] = "?1",
[2] = "jl0",
[3] = "jl1",
[4] = "jl2",
[5] = "jh0",
[6] = "jh1",
[7] = "jh2",
[8] = "jad0",
[9] = "jad1",
[10] = "jad2",
[11] = "jad3",
[12] = "jad4",
[13] = "jad5",
[14] = "jad6",
[15] = "jad7",
[16] = "jad8",
[17] = "sub",
[18] = "st0",
[19] = "st1",
[20] = "st2",
[21] = "st3",
[22] = "kk0",
[23] = "kk1",
[24] = "kk2",
[25] = "kk3",
[26] = "mop0",
[27] = "mop1",
[28] = "mop2",
[29] = "mop3",
[30] = "bc",
[31] = "ac",
[32] = "aop0",
[33] = "aop1",
[34] = "aop2",
[35] = "zo0",
[36] = "zo1",
[37] = "zo2",
[38] = "bi0",
[39] = "bi1",
[40] = "bi2",
[41] = "ai0",
[42] = "ai1",
[43] = "ai2",
};

int get_rom(u64 *buf, int len, char *rom) {
	int fd;
	int x;

	fd = open(rom, O_RDONLY);
	if (fd < 0) {
		perror(rom);
		return -1;
	}
	x = read(fd, buf, len);
	if (x < 0) {
		perror(rom);
		return -1;
	}
	if (x != len) {
		fprintf(stderr, "%s: wrong size\n", rom);
		return -1;
	}
	close(fd);
	return 0;
}

int main(int argc, char **argv) {
	int x, b;
	int fd;
	int saw, bad, skip, count;
	char *rom = "wang600.rom";
	char *chk = NULL;
	u64 base, check;
	int ad1, ad2;
	u64 m;
	extern char *optarg;
	extern int optind;

	while ((x = getopt(argc, argv, "r:")) != EOF) {
		switch (x) {
		case 'r':
			rom = optarg;
			break;
		}
	}
	if (optind >= argc) {
		fprintf(stderr, "Usage: %s [-r base-rom] check-rom\n", argv[0]);
		exit(1);
	}
	chk = argv[optind];

	x = get_rom(rom600, sizeof(rom600), rom);
	if (x < 0) {
		exit(1);
	}
	x = get_rom(chkrom, sizeof(chkrom), chk);
	if (x < 0) {
		exit(1);
	}

	ad1 = ad2 = 0;
	for (x = 0; x < sizeof(rom600) / sizeof(rom600[0]); ++x) {
		base = rom600[x];
		memset(plus, 0, sizeof(plus));
		memset(minus, 0, sizeof(minus));
		saw = 0;
		skip = (x == 0x008 || /* memory size subroutine */
			x == 0x474 || x == 0x49c ||
			x == 0x4d2 || x == 0x4fe || x == 0x4ff ||
			x == 0x7bd || x >= 0x7fc);
		bad = (skip || x == 0x055 || x == 0x248 || x == 0x352 || x == 0x512);
		check = (base ^ chkrom[x]) & 0x0fffffffffffull;
		while (!bad && check) {
			b = ffsl(check) - 1;
			m = 1ull << b;
			if (base & m) {
				++minus[b];
			} else {
				++plus[b];
			}
			check &= ~m;
		}
		for (b = 0; !bad && b < NBITS; ++b) {
			m = 1ull << b;
			if (plus[b]) {
				if (!saw) {
					printf("%03x: %011lx %4s +\n", x, m, bit_names[b]);
				} else {
					printf("     %011lx %4s +\n", m, bit_names[b]);
				}
				++saw;
				all_plus[b] += plus[b];
			}
			if (minus[b]) {
				if (!saw) {
					printf("%03x: %011lx %4s -\n", x, m, bit_names[b]);
				} else {
					printf("     %011lx %4s -\n", m, bit_names[b]);
				}
				++saw;
				all_minus[b] += minus[b];
			}
		}
		if (skip) {
			printf("%03x: SKIP\n", x);
		} else if (bad) {
			printf("%03x: BAD\n", x);
		}
	}
	printf("Summary:\n");
	for (b = 0; b < NBITS; ++b) {
		m = 1ull << b;
		if (all_plus[b]) {
			printf("     %011lx %4s (%d) +\n", m, bit_names[b], all_plus[b]);
		}
		if (all_minus[b]) {
			printf("     %011lx %4s (%d) -\n", m, bit_names[b], all_minus[b]);
		}
	}
	return 0;
}
