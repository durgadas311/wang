#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

/*
 * columns 16 and 17 are justified such that, when both are printed,
 * the characters abutt.
 */
char *col_16[16] = {
[0]  = " E",
[1]  = " T",
[2]  = " +",
[3]  = " -",
[4]  = " \u00D7",
[5]  = " \u00F7",
[6]  = "ST",
[7]  = "RE",
[8]  = " *",
[9]  = " *",
[10] = " f",
[11] = " F",
[12] = " A",
[13] = " B",
[14] = " C",
[15] = " D"
};

char *col_17[16] = {
[0]  = "0 ",
[1]  = "1 ",
[2]  = "2 ",
[3]  = "3 ",
[4]  = "4 ",
[5]  = "5 ",
[6]  = "6 ",
[7]  = "7 ",
[8]  = "8 ",
[9]  = "9 ",
[10] = "10",
[11] = "11",
[12] = "12",
[13] = "13",
[14] = "14",
[15] = "15"
};

/*
 * columns 18 and 19 are never printed at the same time...
 * so we squeeze/overlap them to avoid using up too much space.
 */
char *col_18[16] = {
[0] =  "S   ",
[1] =  "RE  ",
[2] =  "W   ",
[3] =  "GO  ",
[4] =  "Jo  ",
[5] =  "J+  ",
[6] =  "SN  ",
[7] =  "CS  ",
[8] =  "TN  ",
[9] =  "RD  ",
[10] = "LN  ",
[11] = "e\u207F  ",
[12] = "x\u00B2  ",
[13] = "\u221AX  ",
[14] = "LP  ",
[15] = "1/x ",
};

char *col_19[16] = {
[0] =  "  M ",
[1] =  "  ST",
[2] =  "  \u03B1 ",
[3] =  "  SP",
[4] =  "  J\u00F8",
[5] =  "  Je",
[6] =  "  S\u00B9",
[7] =  "  C\u00B9",
[8] =  "  T\u00B9",
[9] =  "  DR",
[10] = "  LG",
[11] = " 10\u207F",
[12] = "  I ",
[13] = " |x|",
[14] = "  EP",
[15] = "  RT",
};

char *col_20[16] = {
[0] =  "X",
[1] =  "Y",
[2] =  "Z",
[3] =  "A",
[4] =  "B",
[5] =  "C",
[6] =  "D",
[7] =  "E",
[8] =  "F",
[9] =  "G",
[10] = "H",
[11] = "I",
[12] = "J",
[13] = "K",
[14] = "L",
[15] = "M"
};

char _overflow_[] = {"....OVERFLOW...."};

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
		printf(" %04d  %02d %02d %2s", step, a, b, col_16[a]);
		if (a == 8) {
			printf("  %s\n", col_18[b]);
		} else if (a == 9) {
			printf("  %s\n", col_19[b]);
		} else {
			printf("%s\n", col_17[b]);
		}
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
