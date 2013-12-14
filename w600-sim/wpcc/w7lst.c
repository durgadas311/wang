#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

char *op[16][16] = {
[4] = {
	"+DIR", "-DIR", "\u00D7DIR", "\u00F7DIR", "ST DIR", "RE DIR", "EX DIR",
	"SEARCH",
	"MARK",
	"GRP1",
	"GRP2",
	"WRITE",
	"WRITE ALPHA",
	"END ALPHA",
	"ST Y DIR", "RE Y DIR"
},
[5] = {
	"+IND", "-IND", "\u00D7IND", "\u00F7IND", "ST IND", "RE IND", "EX IND",
	"SK IF Y\u2265X",
	"SK IF Y<X",
	"SK IF Y=X",
	"SK IF ERR",
	"RETURN",
	"END PROG",
	"LOAD PROG",
	"GO", "STOP"
},
[6] = {
	"+", "-", "\u00D7", "\u00F7", "ST", "RE", "EX",
	"|X|",
	"INT",
	"\u03C0",
	"log10X",
	"logX",
	"\u221AX",
	"10\u207F",
	"e\u207F",
	"1/X"
},
[7] = {
	"E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9",
	"SET EXP",
	"CHANGE SIGN",
	".",
	"X\u00B2",
	"RE RES",
	"CLEAR X"
},
};

static char *decode(uint8_t a, uint8_t b) {
	static char buf[128];

	buf[0] = '\0';
	// detect DIRECT codes and list register number differently?
	if (a < 4) {
		sprintf(buf, "SR %02d%02d", a, b);
	} else if (a == 12) {
		if (b < 7 || b > 13) {
			sprintf(buf, "%s+100", op[4][b]);
		}
	} else if (a < 8) {
		sprintf(buf, "%s", op[a][b]);
	}
	return buf;
}

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
		printf(" %04d  %02d %02d  %s\n", step, a, b, decode(a, b));
		++step;
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
