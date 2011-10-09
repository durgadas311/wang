SECTIONS
{
	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	. = 0x00;
	.wang600label : {*(.wang600label) }
	. = 0xa0;
	.wang600flabel : {*(.wang600flabel) }
	. = 0x100;
	.wang600code : {*(.wang600code) }
	.wang600desc : {*(.wang600name) *(.wang600author) *(.wang600title) *(.wang600help) }
}
