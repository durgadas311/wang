/* $Id: txt2wng.c,v 1.2 2012/01/08 00:28:02 drmiller Exp $ */

#include <stdio.h>
#include <stdint.h>
#include <string.h>

struct buffer {
	uint8_t hdr[8];
	uint8_t line[100];
};

char inbuf[256];

int l;
int eod;

static int cvt_line(char *inb, struct buffer *buf) {
	int y, c, t;
	int code;
	char *in = inb;
	extern uint8_t cn24_revxlate[256];
	extern uint8_t cn24_codes[256];
	extern uint8_t escapes[256];

	memset(buf->hdr, 0, sizeof(buf->hdr));
	memset(buf->line, 0xaa, sizeof(buf->line));
	y = 0;
	while (y < sizeof(buf->line)) {
		eod = 0;
		c = *in++;
		code = (c == '['); // CODE characters...
		if (code) {
			c = *in++;
			if (*in == '\\') {
				// ASCII escapes...
				++in;
				c = escapes[tolower(*in)];
				if (c == '\0') {
					fprintf(stderr, "%d: invalid escape character '%c'\n", l, *in);
					return 1;
				}
				++in;
			}
			if (*in != ']') {
				fprintf(stderr, "%d: invalid code string for '%c' @%d:\"%s\"\n", l, c, in - inb, inb);
				return 1;
			}
			t = cn24_codes[c];
			if (c == '/') {	// EOD - strip off \n
				if (*in == '\n') ++in;
				eod = 1;
				c = '\n';
			}
			if (t == '\0') {
				fprintf(stderr, "%d: unknown code character '%c'\n", l, c);
				return 1;
			}
		} else {
			t = cn24_revxlate[c];
		}
		buf->line[y++] = t;
		if (c == '\n' || t == 0x0a) break;
	}
	if (c != '\n') {
		fprintf(stderr, "%d: line overflow\n", l);
		return 1;
	}
	return 0;
}

// converts stdin to stdout... stdout is binary 108-block formatted...
int main(int argc, char **argv) {
	int e;
	struct buffer buf;

	l = 0;
	while (fgets(inbuf, sizeof(inbuf), stdin) != NULL) {
		++l;
		e = cvt_line(inbuf, &buf);
		if (e) break;
		write(1, &buf, sizeof(buf));
	}
	if (!eod) {
		++l;
		strcpy(inbuf, "[/]");
		cvt_line(inbuf, &buf);
		write(1, &buf, sizeof(buf));
	}
	return 0;
}

uint8_t	cn24_revxlate[256] = {
['-'] = 0x00,
['y'] = 0x01,
[' '] = 0x02,
['\b'] = 0x13,
['q'] = 0x04,
['p'] = 0x05,
['='] = 0x06,
['j'] = 0x07,
[' '] = 0x03,
['/'] = 0x09,
[','] = 0x0c,
[','] = 0x0d,
['f'] = 0x0e,
['g'] = 0x0f,

['w'] = 0x10,
['s'] = 0x11,
['i'] = 0x14,
['\''] = 0x15,
['.'] = 0x16,
['['] = 0x17,	// 1/2...
['\n'] = 0x18,
['o'] = 0x19,
['\n'] = 0x33,
['\t'] = 0x23,
['a'] = 0x1c,
['r'] = 0x1d,
['v'] = 0x1e,
['m'] = 0x1f,

['b'] = 0x20,
['h'] = 0x21,
['k'] = 0x24,
['e'] = 0x25,
['n'] = 0x26,
['t'] = 0x27,
['l'] = 0x29,
['c'] = 0x2c,
['d'] = 0x2d,
['u'] = 0x2e,
['x'] = 0x2f,

['9'] = 0x30,
['0'] = 0x31,
['6'] = 0x34,
['5'] = 0x35,
['2'] = 0x36,
['z'] = 0x37,
['4'] = 0x39,
['8'] = 0x3c,
['7'] = 0x3d,
['3'] = 0x3e,
['1'] = 0x3f,

		// shifted versions...
['_'] = 0x80,
['Y'] = 0x81,
['Q'] = 0x84,
['P'] = 0x85,
['+'] = 0x86,
['J'] = 0x87,
['?'] = 0x89,
[','] = 0x8c,
[':'] = 0x8d,
['F'] = 0x8e,
['G'] = 0x8f,

['W'] = 0x90,
['S'] = 0x91,
['|'] = 0x92,	// Set Tab
['I'] = 0x94,
['"'] = 0x95,
['.'] = 0x96,
['{'] = 0x97,	// 1/4
['O'] = 0x99,
['A'] = 0x9c,
['R'] = 0x9d,
['V'] = 0x9e,
['M'] = 0x9f,

['B'] = 0xa0,
['H'] = 0xa1,
['K'] = 0xa4,
['E'] = 0xa5,
['N'] = 0xa6,
['T'] = 0xa7,
['L'] = 0xa9,
['C'] = 0xac,
['D'] = 0xad,
['U'] = 0xae,
['X'] = 0xaf,

['('] = 0xb0,
[')'] = 0xb1,
['^'] = 0xb4,	// cent
['%'] = 0xb5,
['@'] = 0xb6,
['Z'] = 0xb7,
['$'] = 0xb9,
['*'] = 0xbc,
['&'] = 0xbd,
['#'] = 0xbe,
['!'] = 0xbf,
};

uint8_t	cn24_codes[256] = {
[' '] = 0x02,
['\b'] = 0x12,
['\t'] = 0x22,
['\n'] = 0x32,

['-'] = 0x08,
['w'] = 0x18,
['b'] = 0x28,
['9'] = 0x38,

['/'] = 0x0a,
['o'] = 0x1a,
['l'] = 0x2a,
['4'] = 0x3a,

['g'] = 0x0b,
['m'] = 0x1b,
['x'] = 0x2b,
['1'] = 0x3b,
};

uint8_t	escapes[256] = {
['b'] = '\b',
['n'] = '\n',
['t'] = '\t',
};
