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
int ones[NBITS] = {0};

u64 rom600[2048];
u64 chkrom[2048];

char *bit_names[NBITS] = {
[0]  = "  ?0",
[1]  = "  ?1",
#ifdef WIRE_WEAVE
[2]  = " jl0 L16B L37D L37A",
[3]  = " jl1 L16A L37D L37A",
[4]  = " jl2 L17B L37D L37A",
[5]  = " jh0 L17A L37D L37A",
[6]  = " jh1 L18B L37D L37A",
[7]  = " jh2 L18A L37D L37A",
[8]  = "jad0 L19B L37D L37A",
[9]  = "jad1 L19A L37E L37B",
[10] = "jad2 L20B L37E L37B",
[11] = "jad3 L20A L37E L37B",
[12] = "jad4 L21B L37E L37B",
[13] = "jad5 L21A L37E L37B",
[14] = "jad6 L22B L37E L37B",
[15] = "jad7 L22A L37E L37B",
[16] = "jad8 L23B L37F L37C",
[17] = " sub L23A L37F L37C",
[18] = " st0 L24B L37F L37C",
[19] = " st1 L24A L37F L37C",
[20] = " st2 L25B L37F L37C",
[21] = " st3 L25A L37F L37C",
[22] = " kk0 L26B L37F L37C",
[23] = " kk1 L26A L38D L38C",
[24] = " kk2 L27B L38D L38C",
[25] = " kk3 L27A L38D L38C",
[26] = "mop0 L28B L38D L38C",
[27] = "mop1 L28A L38D L38C",
[28] = "mop2 L29B L38D L38C",
[29] = "mop3 L29A L38D L38C",
[30] = "  bc L30B L38E L38A",
[31] = "  ac L30A L38E L38A",
[32] = "aop0 L31B L38E L38A",
[33] = "aop1 L31A L38E L38A",
[34] = "aop2 L32B L38E L38A",
[35] = " zo0 L32A L38E L38A",
[36] = " zo1 L33B L38E L38A",
[37] = " zo2 L33A L38F L38B",
[38] = " bi0 L34B L38F L38B",
[39] = " bi1 L34A L38F L38B",
[40] = " bi2 L35B L38F L38B",
[41] = " ai0 L35A L38F L38B",
[42] = " ai1 L36B L38F L38B",
[43] = " ai2 L36A L38F L38B",
#else
[2]  = " jl0 L18",
[3]  = " jl1 L18",
[4]  = " jl2 L19",
[5]  = " jh0 L19",
[6]  = " jh1 L19",
[7]  = " jh2 L19",
[8]  = "jad0 L17",
[9]  = "jad1 L17",
[10] = "jad2 L17",
[11] = "jad3 L17",
[12] = "jad4 L20",
[13] = "jad5 L20",
[14] = "jad6 L20",
[15] = "jad7 L20",
[16] = "jad8 L21",
[17] = " sub L21",
[18] = " st0 L21",
[19] = " st1 L21",
[20] = " st2 L22",
[21] = " st3 L22",
[22] = " kk0 L22",
[23] = " kk1 L22",
[24] = " kk2 L23",
[25] = " kk3 L23",
[26] = "mop0 L23",
[27] = "mop1 L23",
[28] = "mop2 L24",
[29] = "mop3 L24",
[30] = "  bc L24",
[31] = "  ac L24",
[32] = "aop0 L25",
[33] = "aop1 L25",
[34] = "aop2 L25",
[35] = " zo0 L25",
[36] = " zo1 L26",
[37] = " zo2 L26",
[38] = " bi0 L26",
[39] = " bi1 L26",
[40] = " bi2 L27",
[41] = " ai0 L27",
[42] = " ai1 L27",
[43] = " ai2 L27",
#endif
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
	int verbose = 0;
	int nobad = 0;
	int noskip = 0;
	char *rom = "wang600.rom";
	char *chk = NULL;
	u64 base, check;
	int ad1, ad2;
	u64 m;
	extern char *optarg;
	extern int optind;

	while ((x = getopt(argc, argv, "br:sv")) != EOF) {
		switch (x) {
		case 'b':
			++nobad;
			break;
		case 'r':
			rom = optarg;
			break;
		case 's':
			++noskip;
			break;
		case 'v':
			++verbose;
			break;
		}
	}
	if (optind >= argc) {
		fprintf(stderr, "Usage: %s [-bsv] [-r base-rom] check-rom\n", argv[0]);
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
#ifdef WIRE_WEAVE
		skip = (!noskip && (x == 0x008 || /* memory size subroutine */
			x == 0x474 || x == 0x49c ||
			x == 0x4d2 || x == 0x4fe || x == 0x4ff ||
			x == 0x7bd)) || x >= 0x7fc;
		bad = (skip || (!nobad && (x == 0x055 || x == 0x248 || x == 0x352 || x == 0x512)));
#else
		skip = 0;
		bad = 0;
#endif
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
		/* count 1's... */
		while (base) {
			b = ffsl(base) - 1;
			m = 1ull << b;
			++ones[b];
			base &= ~m;
		}
		for (b = 0; !bad && b < NBITS; ++b) {
			m = 1ull << b;
			if (plus[b]) {
				if (!saw) {
					printf("%03x: %011lx %.4s +\n", x, m, bit_names[b]);
				} else {
					printf("     %011lx %.4s +\n", m, bit_names[b]);
				}
				++saw;
				all_plus[b] += plus[b];
			}
			if (minus[b]) {
				if (!saw) {
					printf("%03x: %011lx %.4s -\n", x, m, bit_names[b]);
				} else {
					printf("     %011lx %.4s -\n", m, bit_names[b]);
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
	for (b = 2; b < NBITS; ++b) {
		double d;
		m = 1ull << b;
		saw = 0;
		if (all_plus[b]) {
			d = all_plus[b] * 100;
			d /= ones[b];
			printf("     %011lx %s %4d %4d %.2f%% +\n",
				m, bit_names[b], ones[b], all_plus[b], d);
			++saw;
		}
		if (all_minus[b]) {
			d = all_minus[b] * 100;
			d /= ones[b];
			if (saw) {
				printf("     " "           " " "
					"                   "
					"      %4d %.2f%% -\n",
					all_minus[b], d);
			} else {
				printf("     %011lx %s %4d %4d %.2f%% -\n",
					m, bit_names[b], ones[b], all_minus[b], d);
			}
			++saw;
		}
		if (!saw && verbose) {
			printf("     %011lx %s %4d\n",
				m, bit_names[b], ones[b]);
		}
	}
	return 0;
}
