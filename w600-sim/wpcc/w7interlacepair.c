#include <stdint.h>
#include <string.h>

// 'reg2' may be NULL, 'out' must be uint8_t[16].
void w7interlacepair(uint8_t *out, uint8_t *reg1, uint8_t *reg2) {
	uint8_t new_pair[16];
	int x;
	for (x = 0; x < 8; ++x) {
		uint8_t a1 = ((reg1[x] >> 4) & 0x0f);
		uint8_t a2 = (reg1[x] & 0x0f);
		uint8_t b1;
		uint8_t b2;
		if (reg2 == NULL) {
			b1 = 0;
			b2 = 0;
		} else {
			b1 = ((reg2[x] >> 4) & 0x0f);
			b2 = (reg2[x] & 0x0f);
		}
		new_pair[x * 2 + 0] = (b1 << 4) | (a1);
		new_pair[x * 2 + 1] = (b2 << 4) | (a2);
	}
	memcpy(out, new_pair, sizeof(new_pair));
}
