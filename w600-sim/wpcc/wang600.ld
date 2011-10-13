OUTPUT_FORMAT(binary);
_loc_search = 0x80;
_loc_call = 0xf7;

MEMORY {
	labels  : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels : ORIGIN = 0xa0, LENGTH = 0x20 /* for ROM use 0xc0 */
	regs    : ORIGIN = 0x00, LENGTH = 0xe6 /* not used for ROM */
	code    : ORIGIN = 0x00, LENGTH = 0xe70 >> 1 /* 0x100-0xf6f */
}
SECTIONS
{
	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	.wang600labels (NOLOAD) : {
		*(.wang600label);
		PROVIDE(_end_prog = .);
		BYTE(0);
	} >labels
	.wang600flabels (NOLOAD) : {
		*(.wang600flabel);
	} >flabels
	.wang600hdr : AT (0x0000) {
		BYTE(0x77);
		BYTE(0x36);
		BYTE(0x30);
		BYTE(0x30);
		SHORT(_prog_len);
		SHORT(0);
	} >code
	.wang600prog : {
		*(.wang600code);
		_reg_base = ALIGN(8) >> 4;
		*(.wang600data);
		BYTE(0x90);
		BYTE(_end_prog);
		BYTE(0x9e);
		BYTE(0x9e);
		_prog_len = . - ORIGIN(code);
	} >code
	.wang600regs (NOLOAD) : {
		*(.wang600regs);
	} >regs
	.wang600desc : {
		*(.wang600name);
		*(.wang600author);
		*(.wang600title);
		*(.wang600help);
	} >code
}
