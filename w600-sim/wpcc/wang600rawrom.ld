/* OUTPUT_FORMAT(binary); */

MEMORY {
	labels     : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels    : ORIGIN = 0xa0, LENGTH = 0x20 /* for ROM use 0xc0 */
	search     : ORIGIN = 0x80, LENGTH = 1
	call       : ORIGIN = 0xf7, LENGTH = 1
	subr       : ORIGIN = 0xa0, LENGTH = 1
	rom_labels : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	rom_flabels : ORIGIN = 0xc0, LENGTH = 0x20
	rom_search : ORIGIN = 0xf3, LENGTH = 1
	rom_call   : ORIGIN = 0xfc, LENGTH = 1
	rom_subr   : ORIGIN = 0xc0, LENGTH = 1
	regs       : ORIGIN = 0x00, LENGTH = 0xe6 /* not used for ROM */
	hdr        : ORIGIN = 0x00, LENGTH = 0x10
	code       : ORIGIN = 0x00, LENGTH = 0x0e70 >> 1 /* 0x100-0xf6f */
	dummy      : ORIGIN = 0x1000, LENGTH = 0x3fff
	rom_code   : ORIGIN = 0x00, LENGTH = 0x1000 >> 1
	data       : ORIGIN = 0x00, LENGTH = 0xf70 >> 1 /* not used for ROM */
	notes      : ORIGIN = 0x00, LENGTH = 0x1000000
}
SECTIONS
{
	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	.wang600build-id (NOLOAD) : { *(.note.gnu.build-id) }

	/* some files might use rom_*labels/rom_search/rom_call... */
	.wang600label (NOLOAD) : {
		*(.wang600label);
	} >rom_labels
	.wang600flabels (NOLOAD) : {
		*(.wang600flabel);
	} >rom_flabels
	.wang600search (NOLOAD) : {
		*(.wang600search);
	} >rom_search
	.wang600call (NOLOAD) : {
		*(.wang600call);
	} >rom_call
	.wang600subr (NOLOAD) : {
		*(.wang600subr);
	} >rom_subr
	/DISCARD/ : { *(.wang600dummy); }
	/DISCARD/ : { *(.wang600regs); }
	/DISCARD/ : { *(.wang600data); }

	__curr = 0x0000;
	.wang600rom : AT (__curr) {
		*(.wang600code);
		. = ORIGIN(rom_code) + LENGTH(rom_code);
	} >rom_code = 0x93939393
}
