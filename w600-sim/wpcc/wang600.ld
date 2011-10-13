OUTPUT_FORMAT(binary);
_loc_search = 0x80;
_loc_call = 0xf7;

MEMORY {
	labels  : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels : ORIGIN = 0xa0, LENGTH = 0x20 /* for ROM use 0xc0 */
	regs    : ORIGIN = 0x00, LENGTH = 0xe6 /* not used for ROM */
	hdr     : ORIGIN = 0x00, LENGTH = 0x10
	code    : ORIGIN = 0x00, LENGTH = 0xe70 >> 1 /* 0x100-0xf6f */
	data    : ORIGIN = 0x00, LENGTH = 0xf70 >> 1 /* not used for ROM */
	notes   : ORIGIN = 0x00, LENGTH = 0x1000000
}
SECTIONS
{
	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	.wang600labels (NOLOAD) : {
		*(.wang600label);
		/* only if needed? how? */
		PROVIDE(_end_prog = .);
		BYTE(0);
	} >labels
	.wang600flabels (NOLOAD) : {
		*(.wang600flabel);
	} >flabels
	.wang600regs (NOLOAD) : {
		*(.wang600regs);
	} >regs
	.wang600hdr : AT (0x0000) {
		BYTE(0x77);
		BYTE(0x36);
		BYTE(0x30);
		BYTE(0x30);
		SHORT(_desc_len);
		SHORT(_prog_len);
		SHORT(_data_len);
		SHORT(0);
		SHORT(0);
		SHORT(0);
	} >hdr
	__curr = LOADADDR(.wang600hdr) + SIZEOF(.wang600hdr);
	.wang600desc : AT (__curr) {
		*(.wang600name);
		*(.wang600author);
		*(.wang600title);
		*(.wang600help);
	} >notes
	__curr = LOADADDR(.wang600desc) + SIZEOF(.wang600desc);
	_desc_len = __curr;
	.wang600prog : AT (__curr) {
		*(.wang600code);
		/* only if needed? how? */
		BYTE(0x90);
		BYTE(_end_prog);
		BYTE(0x9e);
		BYTE(0x9e);
	} >code
	__curr = LOADADDR(.wang600prog) + SIZEOF(.wang600prog);
	_prog_len = __curr;
	.wang600data : AT (__curr) {
		*(.wang600data);
		/* only if needed? how? */
		BYTE(0x9e);
		BYTE(0xff);
	} >data
	__curr = LOADADDR(.wang600data) + SIZEOF(.wang600data);
	_data_len = __curr;
}
