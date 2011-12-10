#ident "$Id: pport.c,v 1.6 2011/12/10 02:51:41 drmiller Exp $"

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <stdint.h>

#include "pport.h"

static int dev = -1;

int pport_send_byte(uint8_t byte) {
	uint8_t pps;
	int x;

#if 0
	x = ioctl(dev, PPRSTATUS, &pps);
	if (x < 0) {
		perror("PPRSTATUS");
		return -1;
	}
	ret = ((pps & SSI_ST_DO) != 0);
#endif
	pps = byte;
	x = write(dev, &pps, 1);
	if (x < 0) {
		perror("write");
		return -1;
	}
#if 0
	pps |= STROBE;
	x = ioctl(dev, PPWDATA, &pps);
	if (x < 0) {
		perror("PPWDATA");
		return -1;
	}
	pps &= ~STROBE;
	x = ioctl(dev, PPWDATA, &pps);
	if (x < 0) {
		perror("PPWDATA");
		return -1;
	}
#endif
	return 0;
}

int pport_recv_byte(uint8_t *byte) {
	uint8_t pps;
	int ret, x;

	x = read(dev, &pps, 1);
	if (x < 0) {
		perror("read");
		return -1;
	}
	*byte = pps;
	return 0;
}


int ppdev_setup(char *path) {
	int x, ppfd;
	int ppmode;
	uint8_t pps;

	ppfd = open(path, O_RDWR);
	if (ppfd < 0) {
		perror(path);
		return -1;
	}
	x = ioctl(ppfd, PPCLAIM, 0);
	if (x < 0) {
		perror("PPCLAIM");
		close(ppfd);
		return -1;
	}
	ppmode = IEEE1284_MODE_EPP;
	x = ioctl(ppfd, PPSETMODE, &ppmode);
	if (x < 0) {
		perror("PPSETMODE IEEE1284_MODE_EPP");
		close(ppfd);
		return -1;
	}
	pps = 0;
	x = ioctl(ppfd, PPDATADIR, &pps);
	if (x < 0) {
		perror("PPDATADIR");
		close(ppfd);
		return -1;
	}

	dev = ppfd;
	x = pport_send_byte(0);
	if (x < 0) {
		perror("PPWDATA 48");
		close(ppfd);
		dev = -1;
		return -1;
	}
	return ppfd;
}

void ppdev_close() {
	int x;
	if (dev >= 0) {
		x = ioctl(dev, PPRELEASE, 0);
		if (x < 0) {
			perror("PPRELEASE");
		}
		close(dev);
	}
}
