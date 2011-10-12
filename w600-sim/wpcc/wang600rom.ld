_loc_search = 0xf3;
_loc_call = 0xfc;

MEMORY {
	labels : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels : ORIGIN = 0xc0, LENGTH = 0x20
	code : ORIGIN = 0x1000, LENGTH = 0x1000 >> 1 /* 0x000-0xfff */
}
SECTIONS
{

	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	.wang600label : {*(.wang600label) } >labels
	.wang600flabel : {*(.wang600flabel) } >flabels
	.wang600code : {*(.wang600code) } >code
	.wang600desc : {*(.wang600name) *(.wang600author) *(.wang600title) *(.wang600help) }
}
