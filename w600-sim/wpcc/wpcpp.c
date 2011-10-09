#include <stdio.h>

char xlat[256] = {
['-'] = 0x00,
['y'] = 0x01,
[' '] = 0x02,
['\b'] = 0x03,
['q'] = 0x04,
['p'] = 0x05,
['='] = 0x06,
['j'] = 0x07,
// [' '] = 0x08,      // no op
['/'] = 0x09,
//[' '] = 0x0a,       // no op
//[' '] = 0x0b,       // no op
[','] = 0x0c,
[';'] = 0x0d,
['f'] = 0x0e,
['g'] = 0x0f,

['w'] = 0x10,
['s'] = 0x11,
//[''] = 0x12,        // shift dn
//[''] = 0x13,        // shift up
['i'] = 0x14,
['\''] = 0x15,
['.'] = 0x16,
//['\001'] = 0x17,      // 1/2...
['\n'] = 0x18,
['o'] = 0x19,
//['\n'] = 0x1a,
//['\n'] = 0x1b,      // rev index
['a'] = 0x1c,
['r'] = 0x1d,
['v'] = 0x1e,
['m'] = 0x1f,

['b'] = 0x20,
['h'] = 0x21,
//['+'] = 0x22,       // step x+
//['+'] = 0x23,       // step x-
['k'] = 0x24,
['e'] = 0x25,
['n'] = 0x26,
['t'] = 0x27,
//[''] = 0x28,        // print mode
['1'] = 0x29,
//['+'] = 0x2a,       // step y+
//['+'] = 0x2b,       // step y-
['c'] = 0x2c,
['d'] = 0x2d,
['u'] = 0x2e,
['x'] = 0x2f,

['9'] = 0x30,
['0'] = 0x31,
//[''] = 0x32,        // step x+y+
//[''] = 0x33,        // step x-y+
['6'] = 0x34,
['5'] = 0x35,
['2'] = 0x36,
['z'] = 0x37,
//[''] = 0x38,        // plot mode
['4'] = 0x39,
//[''] = 0x3a,        // step x+y-
//[''] = 0x3b,        // step x-y-
['8'] = 0x3c,
['7'] = 0x3d,
['3'] = 0x3e,
['l'] = 0x3f,

// shifted versions...
['_'] = 0x40,
['Y'] = 0x41,
[' '] = 0x42,
//['\b'] = 0x43,
['Q'] = 0x44,
['P'] = 0x45,
['+'] = 0x46,
['J'] = 0x47,
['?'] = 0x49,
[','] = 0x4c,
[':'] = 0x4d,
['F'] = 0x4e,
['G'] = 0x4f,

['W'] = 0x50,
['S'] = 0x51,
['I'] = 0x54,
['"'] = 0x55,
['.'] = 0x56,
//['\002'] = 0x57,      // 1/4
//['\n'] = 0x58,
['O'] = 0x59,
//['\n'] = 0x5a,
//['\n'] = 0x5b,      // rev index
['A'] = 0x5c,
['R'] = 0x5d,
['V'] = 0x5e,
['M'] = 0x5f,

['B'] = 0x60,
['H'] = 0x61,
['K'] = 0x64,
['E'] = 0x65,
['N'] = 0x66,
['T'] = 0x67,
['!'] = 0x69,
['C'] = 0x6c,
['D'] = 0x6d,
['U'] = 0x6e,
['X'] = 0x6f,

['('] = 0x70,
[')'] = 0x71,
//['\003'] = 0x74,      // cent
['%'] = 0x75,
['@'] = 0x76,
['Z'] = 0x77,
['$'] = 0x79,
['*'] = 0x7c,
['&'] = 0x7d,
['#'] = 0x7e,
['L'] = 0x7f,
};

char buf[256];
char str[256];

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

void do_alpha(char *s) {
	int start = 0;
	int shift = 0;
	int esc = 0;
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
			default: continue; break;
			}
		}
		if (x == '\\') {
			esc = 1;
			continue;
		}
		c = xlat[x];
		if (!start) {
			start = 1;
			printf("_opcode(0x92);\n");
		}
		if ((c & 0x40) && !shift) {
			shift = 1;
			printf("_opcode(0x13);\n");
		}
		if (!(c & 0x40) && shift) {
			shift = 0;
			printf("_opcode(0x12);\n");
		}
		printf("_opcode(0x%x);\n", c & 0x3f);
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
	while (fgets(buf, sizeof(buf), stdin) != NULL) {
		t = buf;
		while (isspace(*t)) ++t;
		x = sscanf(t, "ALPHA_STRING(\"%[^\"]\")", str);
		if (x == 1) {
			do_alpha(str);
		} else {
			x = sscanf(t, "ENTER(%[^)])", str);
			if (x == 1) {
				do_enter(str);
			} else {
				printf(buf);
			}
		}
	}
	return 0;
}
