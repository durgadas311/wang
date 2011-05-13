// Copyright (c) 2011 Douglas Miller

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#ident "$Id: w600_fe.c,v 1.3 2011/05/13 12:40:17 drmiller Exp $"

char buf[128];

int main(int argc, char **argv) {
	char *s, *t;

	if (argc != 2) {
		exit(1);
	}

	int fd = atoi(argv[1]);

	while (1) {
		printf("> ");
		fflush(stdout);
		s = fgets(buf, sizeof(buf), stdin);
		if (!s) break;

		for (; (t = strtok(s, " \t")) != NULL; s = NULL) {
			int n = strtoul(t, NULL, 10);
			int h = n / 100;
			int l = n % 100;
			if (h > 15 || l > 15) {
				fprintf(stderr, "Invalid: \"%s\"\n", t);
				continue;
			}
			uint16_t b = (h << 4) | l;
			write(fd, &b, sizeof(b));
		}
	}
	while (1);
	exit(0);
}
