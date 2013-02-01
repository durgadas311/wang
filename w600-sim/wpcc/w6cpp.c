#include <stdio.h>
#include <stdlib.h>

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

void do_enter(char *s) {
	int x;
	while(*s) {
		x = *s++;
		if (isdigit(x)) {
			printf("E(%c)\n", x);
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
	char buf[32], *e = NULL;
	double d;
	d = strtod(s, &e);
	if (e == s || *e != '\0') {
		fprintf(stderr, "%s: %d: Not a floating point number: \"%s\"\n",
			"stdin", __line__, s);
		return;
	}
	sprintf(buf + 1, "%17.11e", d);
	e = buf + 1;
	if (*e != '-') {
		--e;
		*e = '+';
	}
	printf("\t_regdata(%s", l);
	x = 0;
	while (*e && x < 16) {
		int n = 0;
		int c = *e++;
		if (isdigit(c)) {
			n = c - '0';
		} else if (c == '.') {
			//n = 10;
			continue;
		} else if (c == '+') {
			n = 0;
		} else if (c == '-') {
			n = 1;
		} else if (c == 'e' || c == 'E') {
			while (x < 13) {
				printf(",0");
				++x;
			}
			continue;
		} else {
			continue;
		}
		printf(",%d", n);
		++x;
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
			case '^':	// plotter command for move
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
			default:
				if (isdigit(x) && isdigit(s[0]) && isdigit(s[1])) {
					c = ((x & 3) << 6) |
						((s[0] & 7) << 3) |
						(s[1] & 7);
					x = -1;
					s += 2;
					break;
				}
				continue;
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
			printf("_opcode(0x92);\n");
		}
		if (x >= 0) {
			if ((c & SHIFT) && !shift) {
				shift = 1;
				printf("_opcode(0x13);\n");
			}
			if (!(c & SHIFT) && shift) {
				shift = 0;
				printf("_opcode(0x12);\n");
			}
			c &= ~SHIFT;
			if (plot) {
				c |= PLOT;
				plot = 0;
			}
		}
		printf("_opcode(0x%x);\n", c);
	}
	if (start) {
		start = 0;
		printf("_opcode(0x22);\n");
	}
}

int main(int argc, char **argv) {
	int x;
	int c;
	char *t;
	__line__ = 0;
	while (fgets(buf, sizeof(buf), stdin) != NULL) {
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
