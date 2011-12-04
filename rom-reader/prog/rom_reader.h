#ifndef __wang_rom_reader_h__
#define __wang_rom_reader_h__

#ident "$Id: rom_reader.h,v 1.1 2011/12/04 02:56:33 drmiller Exp $"

#include "pport.h"

extern int rom_cmd(uint8_t c, uint8_t d);
extern int rom_setaddr(uint16_t adr);
extern int rom_setmux(uint8_t mux);
extern int rom_strobe();
extern int rom_read_700(uint16_t adr, uint64_t *rom);

#endif /* __wang_rom_reader_h__ */
