/*
 * Dump the "test area" of wire-weave ROM images (0x7fc-0x7ff) in binary.
 */
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>
#include <strings.h>
#include <string.h>

#define BIT0	2	// first bit in word
#define BITN	43	// last bit in word

typedef unsigned long u64;

extern u64 xcode_1[]; /* terminated by (u64)-1 */
extern u64 xcode_2[]; /* terminated by (u64)-1 */

int main(int argc, char **argv) {
	int x;
	int adr;
	int begin = 0x7fc;
	int end = 0x7ff;
	extern char *optarg;

	while ((x = getopt(argc, argv, "b:e:")) != EOF) {
		switch (x) {
		case 'b':
			begin = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		case 'e':
			end = strtoul(optarg, NULL, 0) & 0x7ff;
			break;
		}
	}

	adr = 0;
	while ((xcode_1[adr] >> 52) < begin) ++adr;
	while ((xcode_1[adr] >> 52) <= end) {
		printf("%03lx: ", xcode_1[adr] >> 52);
		for (x = BITN; x >= BIT0; --x) {
			putchar((xcode_1[adr] & (1 << x)) ? '1' : '0');
		}
		putchar('\n');
		++adr;
	}
	adr = 0;
	while ((xcode_2[adr] >> 52) < begin) ++adr;
	while ((xcode_2[adr] >> 52) <= end) {
		printf("%03lx: ", xcode_2[adr] >> 52);
		for (x = BITN; x >= BIT0; --x) {
			putchar((xcode_2[adr] & (1 << x)) ? '1' : '0');
		}
		putchar('\n');
		++adr;
	}

	return 0;
}
