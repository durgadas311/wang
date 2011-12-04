#ident "$Id: rom_reader.c,v 1.1 2011/12/04 02:56:33 drmiller Exp $"

#include "pport.h"

int rom_cmd(uint8_t c, uint8_t d) {
	return send_byte((c << 4) | (d & 0x0f));
}

int rom_setaddr(uint16_t adr) {
	int x;

	x = rom_cmd(2, (adr >> 8));
	if (x < 0) return x;
	x = rom_cmd(1, (adr >> 4));
	if (x < 0) return x;
	x = rom_cmd(0, (adr >> 0));
	return x;
}

int rom_setmux(uint8_t mux) {
	int x;

	x = rom_cmd(3, mux);
	return x;
}

int rom_strobe() {
	int x;

	x = rom_cmd(7, 0);
	return x;
}

// always reads 44 bits, only 43 have microcode data
int rom_read_700(uint16_t adr, uint64_t *rom) {
	int x, y;
	uint64_t u = 0;
	uint8_t byte;

	x = rom_setaddr(adr);
	if (x < 0) return x;
	// hi 8 bits of 44 are mux=0
	for (y = 0; y < 6; ++y) {
		x = rom_setmux(y);
		if (x < 0) return x;
		x = rom_strobe();
		if (x < 0) return x;
		x = recv_byte(&byte);
		if (x < 0) return x;
		u = (u << 8) | byte;
	}
	// now un-shift 4 bits...
	u >>= 4;
	*rom = u;
	return 0;
}
