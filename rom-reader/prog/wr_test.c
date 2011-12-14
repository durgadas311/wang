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

	x = 0;
	while (1) {
		++x;
		//if ((x & 0x0ff) == 0) putchar('.');
		pport_send_byte(x & 0x00f);
	}
	ppdev_close();
	return 0;
}
