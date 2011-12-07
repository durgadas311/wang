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

	e = 0;
	x = 0;
	while (x < 16*1024*1024) {
		++x;
		//if ((x & 0x0ff) == 0) putchar('.');
		rom_setaddr(x);
		rom_setmux(0);	// also clears latches...
		rom_strobe();	// needed?
		rom_rdlat(&b);
		if (b != (x & 0x0ff)) {
			++e;
		}
		if ((x & 0x0ff) == 0 && e) {
			printf("%d errors at 0x%03x\n", e, x & 0x0fff);
			e = 0;
		}
	}
	ppdev_close();
	return 0;
}
