#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

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
			uint8_t b = (h << 4) | l;
			write(fd, &b, 1);
		}
	}
	while (1);
	exit(0);
}
