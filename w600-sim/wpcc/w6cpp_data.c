#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

extern int __line__;
extern char *__file__;

int w6_do_data_d(double d, uint8_t out[16]) {
	int x;
	char buf[32], *e;

	sprintf(buf + 1, "%17.11e", d);
	e = buf + 1;
	if (*e != '-') {
		--e;
		*e = '+';
	}
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
				out[x] = 0;
				++x;
			}
			continue;
		} else {
			continue;
		}
		out[x] = n;
		++x;
	}
	while (x < 16) {
		out[x] = 0;
		++x;
	}
	return 0;
}

int w6_do_data(char *s, uint8_t out[16]) {
	int x;
	char buf[32], *e = NULL;
	double d;
	d = strtod(s, &e);
	if (e == s || (*e != '\0' && *e != '\n')) {
		fprintf(stderr, "%s: %d: Not a floating point number: \"%s\"\n",
			__file__, __line__, s);
		return -1;
	}
	return w6_do_data_d(d, out);
}
