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
// WRITE_ALPHA prefixed codes...
[12] = {
	"\u00D7 1e-10","\u00D7 1e-1", "\u00D7 1e-2", "\u00D7 1e-3", "\u00D7 1e-4",
	"\u00D7 1e-5", "\u00D7 1e-6", "\u00D7 1e-7", "\u00D7 1e-8", "\u00D7 1e-9",
	"SK IF Y+",
	"SK IF Y=0",
	NULL,
	"END ALPHA",
	NULL,
	NULL
},
[13] = {
	NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL,
	"SK IF Y-",
	"SK IF Y\u22600",
	NULL,
	NULL,
	"180/\u03C0",
	"\u03C0/180"
},
[14] = {
	NULL, NULL, NULL, NULL, NULL,
	NULL, NULL, NULL, NULL, NULL,
	"SK IF X+",
	"SK IF X=0",
	NULL,
	NULL,
	NULL,
	"PAUSE"
},
[15] = {
	"\u00D7 1e10","\u00D7 1e1", "\u00D7 1e2", "\u00D7 1e3", "\u00D7 1e4",
	"\u00D7 1e5", "\u00D7 1e6", "\u00D7 1e7", "\u00D7 1e8", "\u00D7 1e9",
	"SK IF X-",
	"SK IF X\u22600",
	NULL,
	NULL,
	NULL,
	NULL
},
};

static char *decode(uint8_t a, uint8_t b) {
	static char buf[128];
	static int reg = 0;

	buf[0] = '\0';
	// detect DIRECT codes and list register number differently?
	if (reg) {
		switch(reg - 1) {
		case 12:	// WRITE_ALPHA
			if (a < 4) {
				sprintf(buf, "PRINT %d-%d", a, b);
			} else if (a < 8) {
				if (op[a + 8][b] == NULL) {
					sprintf(buf, "?? %02d%02d", a, b);
				} else {
					if (a == 4 && b == 13) { // END ALPHA
						sprintf(buf, "%s", op[a + 8][b]);
					} else {
						sprintf(buf, "%s // %s", op[a][b], op[a + 8][b]);
					}
				}
				reg = 0;
			} else if (a < 12) {
				sprintf(buf, "PLOT %d-%d", a & 7, b);
			} else {
				sprintf(buf, "?? %02d%02d", a, b);
				reg = 0;
			}
			break;
		case 7:		// SEARCH
			sprintf(buf, "SEARCH %02d%02d", a, b);
			reg = 0;
			break;
		case 8:		// MARK
			sprintf(buf, "MARK %02d%02d", a, b);
			reg = 0;
			break;
		case 9:		// GRP1
			sprintf(buf, "GRP1 %02d%02d", a, b);
			reg = 0;
			break;
		case 10:	// GRP2
			sprintf(buf, "GRP2 %02d%02d", a, b);
			reg = 0;
			break;
		case 11:	// WRITE
			sprintf(buf, "WRITE %02d%02d", a, b);
			reg = 0;
			break;
		default:
			sprintf(buf, "REG %02d%02d", a, b);
			reg = 0;
			break;
		}


	} else if (a < 4) {
		sprintf(buf, "SR %02d%02d", a, b);
	} else if (a == 12) {
		if (b < 7 || b > 13) {
			sprintf(buf, "%s+100", op[4][b]);
		}
	} else if (a < 8) {
		if (a == 4) {
			reg = b + 1;
		}
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
