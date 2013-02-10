#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

#define EX_PLOT		'\001'
#define EX_MOVE		'\002'
#define EX_CHRSIZE	'\003'
#define EX_CHRSPC	'\004'
#define EX_HOME		'\005'

#define MAP(c,t)	[c] = t,	// normal characters
#define REMAP(c,t)	MAP(c,t)	// overlap conversion (e.g. force uppercase)
#define SPMAP(c,t)	MAP(c,t)	// special (non-standard)
#define EXMAP(c,t)	MAP(c,t)	// extensions
#define PLMAP(c,t)			// plotting characters (not needed in fwd xlat)

#define NONZERO		0x80
#define SHIFT		0x40

#define PLOT		0	// don't want PLOT bit in this table...
unsigned char xlat_plot[256] = {
#include "xlat_plotter_x.h"
};
unsigned char xlat_ow[256] = {
#include "xlat_outputwriter_x.h"
};

#undef PLOT
#define PLOT		0x40	// note, possible conflict with SHIFT

char buf[256];
char str[256];
char lab[256];

int __line__;
char *__file__;

void do_enter(char *s) {
	int x;
	// negative mantissa requires special processing...
	int neg = (*s == '-');
	if (neg) ++s;
	while (*s) {
		x = *s++;
		if (isdigit(x)) {
			printf("E(%c)\n", x);
			if (neg) {
				printf("CHANGE_SIGN()\n");
				neg = 0;
			}
		} else if (x == '-') {
			printf("CHANGE_SIGN()\n");
		} else if (x == 'e') {
			printf("SET_EXP()\n");
		} else if (x == '.') {
			printf("DP()\n");
		} else if (x == '+') {
		} else {
			printf("ENTER(%c)\n", x);
		}

	}
}

void do_data(char *l, char *s) {
	int x;
	uint8_t reg[16];

	extern int w6_do_data(char *s, uint8_t out[16]);

	if (w6_do_data(s, reg) != 0) {
		return;
	}
	printf("\t_regdata(%s", l);
	for (x = 0; x < 16; ++x) {
		printf(",%d", reg[x]);
	}
	printf(");\n");
}

void do_data_string(char *l, char *s) {
	int x;
	printf("\t_regdata(%s", l);
	x = 0;
	while (*s && x < 16) {
		int n = 0;
		int c = *s++;
		if (isdigit(c)) {
			n = c - '0';
		} else if (isalpha(c)) {
			n = toupper(c) - 'A' + 10;
		} else {
			continue;
		}
		printf(",%d", n);
		++x;
	}
	printf(");\n");
}

void do_alpha(char *s, unsigned char *xlat) {
	int start = 0;
	int shift = 0;
	int esc = 0;
	int plot = 0;
	int x;
	int c;

	while (*s) {
		x = *s++;
		if (esc) {
			esc = 0;
			switch(x) {
			case 'n': x = '\n'; break;
			case 'b': x = '\b'; break;
			case 'r': x = '\r'; break;
			case 'v': x = '\v'; break;
			case '%':	// plotter command for draw
				x = EX_PLOT;
				plot = 1;
				break;
			case '/':	// plotter command for move
				x = EX_MOVE;
				plot = 1;
				break;
			case 'z':	// plotter command for char size
				x = EX_CHRSIZE;
				plot = 1;
				break;
			case 's':	// plotter command for char spacing
				x = EX_CHRSPC;
				plot = 1;
				break;
			case 'h':	// plotter command for home
				x = EX_HOME;
				plot = 1;
				break;
			case '^':	// selectric code for cent-sign
				x = -1;
				shift = 1;
				c = 0x34;
				break;
			case '[':	// selectric code for one-half
				x = -1;
				shift = 0;
				c = 0x17;
				break;
			case '{':	// selectric code for one-forth
				x = -1;
				shift = 1;
				c = 0x17;
				break;
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
				if (isdigit(s[0]) && isdigit(s[1])) {
					c = ((x & 3) << 6) |
						((s[0] & 7) << 3) |
						(s[1] & 7);
					x = -1;
					s += 2;
					break;
				} else {
					fprintf(stderr, "malformed octal character on line %d\n", __line__);
				}
				continue;
				break;
			default:
				fprintf(stderr, "unknown character escape '\\%c' on line %d\n", x, __line__);
				break;
			}
		}
		if (x == '\\') {
			esc = 1;
			continue;
		}
		if (x == '|') {
			plot = 1;
			continue;
		}
		if (x >= 0) {
			c = xlat[x];
			if (c == 0) { // invalid character
				// warning message?
				continue;
			}
			c &= ~NONZERO; // don't need bit anymore
		}
		if (!start) {
			start = 1;
			printf("_opcode(ALPHA);\n");
		}
		if (x >= 0) {
			if ((c & SHIFT) && !shift) {
				shift = 1;
				printf("_bytecode(0x13);\n");
			}
			if (!(c & SHIFT) && shift) {
				shift = 0;
				printf("_bytecode(0x12);\n");
			}
			c &= ~SHIFT;
			if (plot) {
				c |= PLOT;
				plot = 0;
			}
		}
		printf("_bytecode(0x%x);\n", c);
	}
	if (start) {
		start = 0;
		if (shift) {
			shift = 0;
			printf("_bytecode(0x12);\n");
		}
		printf("_opcode(END_ALPHA);\n");
	}
}

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
		fprintf(stdout, "#line 1 \"%s\"\n", argv[1]);
		fprintf(stdout, "asm(\".file \\\"%s\\\" ; .line 1\\n\");\n", argv[1]);
	}
	while (fgets(buf, sizeof(buf), fp) != NULL) {
		++__line__;
		t = buf;
		while (isspace(*t)) ++t;
		x = sscanf(t, "ALPHA_STRING(\"%[^\"]\")", str);
		if (x == 1) {
			do_alpha(str, xlat_ow);
			continue;
		}
		x = sscanf(t, "ALPHA_PLOT(\"%[^\"]\")", str);
		if (x == 1) {
			do_alpha(str, xlat_plot);
			continue;
		}
		x = sscanf(t, "ENTER(%[^)])", str);
		if (x == 1) {
			do_enter(str);
			continue;
		}
		x = sscanf(t, "IREG_DATA(%[^,],\"%[a-fA-F0-9]\")", lab, str);
		if (x == 2) {
			do_data_string(lab, str);
			continue;
		}
		x = sscanf(t, "IREG_DATA(%[^,],%[^)])", lab, str);
		if (x == 2) {
			do_data(lab, str);
			continue;
		}
		printf(buf);
	}
	return 0;
}
