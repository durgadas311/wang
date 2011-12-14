#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "rom_reader.h"

int main(int argc, char **argv) {
	int x, e;
	uint8_t b;

	if (ppdev_setup("/dev/parport0") < 0) {
		exit(1);
	}

	x = 0;
	while (1) {
		++x;
		//if ((x & 0x0ff) == 0) putchar('.');
		rom_setaddr(x);
	}
	ppdev_close();
	return 0;
}
