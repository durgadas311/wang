#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

char *simp_op[16] = {
[0] = "E",
[1] = "T",
[2] = "ADD",
[3] = "SUB",
[4] = "MULT",
[5] = "DIV",
[6] = "ST",
[7] = "RE",
[10] = "_f",
[11] = "_F",
[12] = "_ROM_f",
[13] = "_ROM_F",
[14] = "EXCHG"
};

char *prog_op[32] = {
[3] = "GO",
[4] = "J_IF_0",
[5] = "J_IF_P",
[6] = "SIN",
[7] = "COS",
[8] = "TAN",
[9] = "RAD_DEG",
[10] = "LOG_E_X",
[11] = "E_X",
[12] = "X_2",
[13] = "SQRT",
[14] = "LOAD_PROG",
[15] = "INV",

[19] = "STOP",
[20] = "J_IF_N0",
[21] = "J_IF_ERR",
[22] = "SIN_1",
[23] = "COS_1",
[24] = "TAN_1",
[25] = "DEG_RAD",
[26] = "LOG_10_X",
[27] = "E10_X",
[28] = "INT",
[29] = "ABS",
[30] = "END_PROG",
[31] = "RETURN",
};

char *simp_ext[16] = {
[10] = "DP",
[11] = "SET_EXP",
[12] = "CHANGE_SIGN",
[13] = "E13",
[14] = "CLEAR",
[15] = "CLR_DISP"
};

#define EX_PLOT		'\001'
#define EX_MOVE		'\002'
#define EX_CHRSIZE	'\003'
#define EX_CHRSPC	'\004'
#define EX_HOME		'\005'

#define MAP(c,t)	[t] = c,
#define REMAP(c,t)
#define SPMAP(c,t)	MAP(c,t)
#define EXMAP(c,t)	MAP(c,t)
#define PLMAP(c,t)	MAP(c,t)

#define NONZERO		0
#define SHIFT		0x40
#define PLOT		0x40	// needed in rev table for uniq vals

char revxlat_ow[256] = {
#include "xlat_outputwriter_x.h"
};
char revxlat_plot[256] = {
#include "xlat_plotter_x.h"
};
#undef NONZERO
#define NONZERO		0x80

void dump_simp(uint8_t a, uint8_t b, char *pre, char *suf) {
	if (a == 0 && b > 9) {
		printf("%s%s()%s", pre, simp_ext[b], suf);
	} else {
		printf("%s%s(%d)%s", pre, simp_op[a], b, suf);
	}
}

int dump_string(uint8_t *buf, int len) {
	printf("\tALPHA_STRING(\"");
	char *s = buf;
	int n = len;
	int shift = 0;
	for (;n > 0 && *s != 0x22; --n,++s) {
		int a = *s;
		if (a == 0x12) {
			shift = 0;
			continue;
		}
		if (a == 0x13) {
			shift = 1;
			continue;
		}
		if (shift) {
			a |= SHIFT;
		}
		int c = revxlat_ow[a];
		if (isprint(c)) {
			printf("%c", c);
		} else {
			switch(c) {
			case '\n':
				printf("\\n");
				break;
			case '\r':
				printf("\\r");
				break;
			case '\b':
				printf("\\b");
				break;
			case '\v':
				printf("\\v");
				break;
			default:
				// more decoding later...
				printf("\\%03o", a & ~SHIFT);
				break;
			}
		}
	}
	if (n > 0) {
		--n;
		++s;
	}
	printf("\")\n");
	return len - n;
}

int dump_plot(uint8_t *buf, int len) {
	printf("//\tALPHA_PLOT(\"");
	char *s = buf;
	int n = len;
	for (;n > 0 && *s != 0x22; --n,++s) {
		int a = *s;
		if (a & PLOT) {
			printf("|");
		}
		int c = revxlat_plot[a];
		if (isprint(c)) {
			printf("%c", c);
		} else {
			switch(c) {
			case '\n':
				printf("\\n");
				break;
			case '\r':
				printf("\\r");
				break;
			case '\v':
				printf("\\v");
				break;
			case EX_PLOT:
				printf("\\%%");
				break;
			case EX_MOVE:
				printf("\\^");
				break;
			case EX_CHRSIZE:
				printf("\\z");
				break;
			case EX_CHRSPC:
				printf("\\s");
				break;
			case EX_HOME:
				printf("\\h");
				break;
			default:
				// more decoding later...
				printf("\\%03o", a);
				break;
			}
		}
	}
	if (n > 0) {
		--n;
		++s;
	}
	printf("\")\n");
	return len - n;
}

void dump_alpha(uint8_t c) {
	switch(c) {
	case 0x80:
	case 0x90:
	case 0xa0:
	case 0xb0:
		printf("\tPI()\n");
		break;
	case 0xa1:
	case 0xa2:
	case 0xa3:
	case 0xa4:
	case 0xa5:
	case 0xa6:
	case 0xa7:
	case 0xa8:
	case 0xa9:
	case 0xaa:
	case 0xab:
	case 0xac:
	case 0xad:
	case 0xae:
	case 0xaf:
		printf("\tPOW10(%d)\n", c & 0x0f);
		break;
	case 0xb1:
	case 0xb2:
	case 0xb3:
	case 0xb4:
	case 0xb5:
	case 0xb6:
	case 0xb7:
	case 0xb8:
	case 0xb9:
	case 0xba:
	case 0xbb:
	case 0xbc:
	case 0xbd:
	case 0xbe:
	case 0xbf:
		printf("\tPOW_10(%d)\n", c & 0x0f);
		break;
	case 0x81:
		printf("tALPHA(RECALL())\n");
		break;
	case 0x91:
		printf("tALPHA(STORE())\n");
		break;
	case 0x82:
		printf("\tKTRACE_ON()\n");
		break;
	case 0x84:
		printf("\tJ_IF_EQ()\n");
		break;
	case 0x85:
		printf("\tJ_IF_GT()\n");
		break;
	case 0x86:
		printf("\tJ_IF_LT()\n");
		break;
	case 0x92:
		printf("\tKTRACE_OFF()\n");
		break;
	case 0x93:
		printf("\tPAUSE()\n");
		break;
	case 0x8a:
	case 0x9a:
		printf("\tPTRACE_ON()\n");
		break;
	case 0x8b:
	case 0x9b:
		printf("\tPTRACE_OFF()\n");
		break;
	default:
		printf("\tALPHA(0x%02x)\n", c);
		break;
	}
}

void dump_two(uint8_t prefix, uint8_t **buf, int *len) {
	int n = *len;
	uint8_t *s = *buf;
	uint8_t p = prefix;
	int x = 0;

	while (p) {
		uint8_t c = *s;
		uint8_t a = (c >> 4) & 0x0f;
		uint8_t b = c & 0x0f;
		++s;
		--n;
		p = 0;
		switch(prefix) {
		case 0x80:
		case 0xff:
			printf("\tSEARCH(L%d)\n", c);
			break;
		case 0x81:
		case 0xf0:
			printf("\tRECALL(R%d)\n", c);
			break;
		case 0x82:
		case 0xf1:
			printf("\tPRINT(%d,%d)\n", b, a);
			break;
		case 0x90:
			printf("MARK(L%d)\n", c);
			break;
		case 0x91:
		case 0xf9:
			printf("\tSTORE(R%d)\n", c);
			break;
		case 0x92:
		case 0xfa:
			if (c < 0x80) {
				--s;
				++n;
				int N = dump_string(s, n);
				dump_plot(s, n);
				n -= N;
				s += N;
			} else {
				dump_alpha(c);
			}
			break;
		case 0xf2:
			if (a == 10 || a == 14) {
				printf("\tJ_IF_E()\n");
			} else if (a == 11 || a == 15) {
				printf("\tJ_IF_NE()\n");
			} else {
				printf("\tIO(0x%02x)\n", c);
			}
			break;
		case 0xf3:
		case 0xf4:
		case 0xf5:
		case 0xf6:
			printf("\t_ROM_SEARCH(LL%d)\n", c);
			break;
		case 0xf7:
			printf("\tCALL(L%d)\n", c);
			break;
		case 0xf8:
			printf("(MARK)(L%d)\n", c);
			break;
		case 0xfb:
			if (a == 0) {
				printf("\tJUMP(R%d)\n", b);
			} else {
				dump_simp(a, b, "\tINDIR(", ")\n");
			}
			break;
		case 0xfc:
			printf("\t_ROM_CALL(LL%d)\n", c);
			break;
		case 0xfd:
			printf("\tGROUP1(%d)\n", c);
			break;
		case 0xfe:
			printf("\tGROUP2(%d)\n", c);
			break;
		default:
			break;
		}
	}
	*buf = s;
	*len = n;
}

void dump(uint8_t *buf, int len) {
	uint8_t *s = buf;
	int n = len;
	while (n > 0) {
		uint8_t c = *s;
		uint8_t a = (c >> 4) & 0x0f;
		uint8_t b = c & 0x0f;
		++s;
		--n;
		if (simp_op[a]) {
			dump_simp(a, b, "\t", "\n");
			continue;
		}
		if (a == 15 || b < 3) {
			if (n < 1) {
				printf("/* dangling %02d %02d */\n", a, b);
				return;
			}
			dump_two(c, &s, &n);
		} else {
			printf("\t%s()\n", prog_op[c & 0x1f]);
		}
	}
}

#if 0
void labels(uint8_t *buf, int len) {
	uint8_t *s = buf;
	int n = len;
	while (n > 0) {
		uint8_t c = *s;
		uint8_t a = (c >> 4) & 0x0f;
		uint8_t b = c & 0x0f;
		++s;
		--n;
		if (a == 15 || ((a == 8 || a == 9) && b < 3)) {
			if (n < 1) return;
			if (c == 0x90) {
				label_add(...);
			} else {
			}
		}
	}
}
#endif

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
		printf("/* %s */\n\n#include \"wang600.h\"\n\nBEGIN()\n", argv[x]);
		//labels(buf, (int)stb.st_size);
		dump(buf, (int)stb.st_size);
		printf("END()\n");
go_next:
		free(buf);
	}
}
