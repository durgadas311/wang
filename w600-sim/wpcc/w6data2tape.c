#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

char buf[256];
char str[256];
char lab[256];

int __line__;
char *__file__;

void write_reg(int fd, uint8_t reg[16]) {
	int y;
	for (y = 16; y > 0; y -= 2) {
		uint8_t byte = (reg[y - 1] << 4) | reg[y - 2];
		write(fd, &byte, 1);
	}
}

void do_data(int fd, int num) {
	uint8_t data[16];
	double d = num;
	w6_do_data_d(d, data);
	write_reg(fd, data);
}

uint8_t data[256][16]; // there will never be more that 246 registers...

int main(int argc, char **argv) {
	int x;
	int c;
	char *t;
	__line__ = 0;
	__file__ = "stdin";
	FILE *fp = stdin;
	if (argc > 1) {
		fp = fopen(argv[1], "r");
		if (fp == NULL) {
			perror(argv[1]);
			exit(1);
		}
		__file__ = argv[1];
	}
	x = 0;
	while (x < 246 && fgets(buf, sizeof(buf), fp) != NULL) {
		double d;
		++__line__;
		c = sscanf(buf, " %lf\n", &d);
		if (c == 1) {
			if (w6_do_data_d(d, data[x]) != 0) {
				exit(1);
			}
		} else {
			// get description?
		}
		++x;
	}
	if (x < 246) {
		c = x;
		uint8_t end_tape[] = { 0x9e, 0xff };
		do_data(1, c);
		write(1, end_tape, sizeof(end_tape));
		while (x > 0) {
			--x;
			write_reg(1, data[x]);
		}
#if 0	// not needed, it is not an error for less data to exist.
		// TODO: fill-in the 16 base registers.
		// data to fill TBD...
		for (x = 16; x > 0; --x) {
			double d = 0.0;
			if (x == 2) d = c + 16 - 1;
			else if (x == 1) d = 16;
			do_data(1, d);
		}
#endif
		write(1, end_tape, sizeof(end_tape));
	}
	return !(x < 246);
}
