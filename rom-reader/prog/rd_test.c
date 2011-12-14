#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include "pport.h"

int main(int argc, char **argv) {
	int x;

	if (ppdev_setup("/dev/parport0") < 0) {
		exit(1);
	}

	x = 0;
	while (1) {
		++x;
		//if ((x & 0x0ff) == 0) putchar('.');
		pport_recv_byte(&x);
	}
	ppdev_close();
	return 0;
}
