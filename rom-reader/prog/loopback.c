#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "rom_reader.h"

#define MAX_TEST 16*1024*1024
//#define MAX_TEST 256

int main(int argc, char **argv) {
	int x, e;
	uint8_t b;

	if (ppdev_setup("/dev/parport0") < 0) {
		exit(1);
	}

	e = 0;
	x = 0;
	while (x < MAX_TEST) {
		++x;
		//if ((x & 0x0ff) == 0) putchar('.');
		rom_setaddr(x);
		//rom_setmux(0);	// also clears latches...
		//rom_strobe();	// needed?
		rom_rdlat(&b);
		if (b != (x & 0x0ff)) {
#if 0
			printf("error at 0x%03x (%02x::%02x)\n",
				x & 0x0fff, b, x & 0x0ff);
#endif
			++e;
		}
sleep(1);
		if ((x & 0x0ff) == 0 && e) {
			printf("%d errors at 0x%03x (%02x::%02x)\n",
				e, x & 0x0fff, b, x & 0x0ff);
			e = 0;
		}
	}
	ppdev_close();
	return 0;
}
