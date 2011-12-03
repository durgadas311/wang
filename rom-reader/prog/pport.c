#ident "$Id: pport.c,v 1.2 2011/12/03 20:31:35 drmiller Exp $"

#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <stdint.h>

#include "pport.h"

static int dev = -1;

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
	x = ioctl(ppfd, PPWDATA, &pps);
	if (x < 0) {
		perror("PPWDATA");
		close(ppfd);
		return -1;
	}
	dev = ppfd;
	return ppfd;
}

int ppdev_stat() {
	int fd, x;
	uint32_t pps = 0;

	x = ioctl(dev, PPRSTATUS, &pps);
	if (x < 0)
		return -1;
	return (pps & 0xff);
}

void ppdev_close() {
	if (dev >= 0)
		close(dev);
}

int send_byte(uint8_t byte) {
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
	x = ioctl(dev, PPWDATA, &pps);
	if (x < 0) {
		perror("PPWDATA");
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

int recv_byte(uint8_t *byte) {
	uint8_t pps;
	int ret, x;

	x = ioctl(dev, PPRDATA, &pps);
	if (x < 0) {
		perror("PPRDATA");
		return -1;
	}
	*byte = pps;
	return 0;
}

