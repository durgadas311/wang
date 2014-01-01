OUTPUT_FORMAT(binary);

MEMORY {
	labels     : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels    : ORIGIN = 0xa4, LENGTH = 0x1c /* leave some "reserved" */
	search     : ORIGIN = 0x80, LENGTH = 1
	call       : ORIGIN = 0xf7, LENGTH = 1
	subr       : ORIGIN = 0xa0, LENGTH = 0x20
	rom_labels : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	rom_flabels : ORIGIN = 0xc0, LENGTH = 0x20
	rom_search : ORIGIN = 0xf3, LENGTH = 1
	rom_call   : ORIGIN = 0xfc, LENGTH = 1
	rom_subr   : ORIGIN = 0xc0, LENGTH = 0x20
	regs       : ORIGIN = 0x00, LENGTH = 0xffff /* not used for ROM */
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
	} >labels
	PROVIDE(_search_end_prog = 0x80);
	PROVIDE(end_prog = 0xee); /* hard-code to avoid conflict with reg data */
	.wang600flabels (NOLOAD) : {
		*(.wang600flabel);
	} >flabels
	.wang600search (NOLOAD) : {
		*(.wang600search);
	} >search
	.wang600call (NOLOAD) : {
		*(.wang600call);
	} >call
	.wang600subr (NOLOAD) : {
		*(.wang600subr);
	} >subr
	/* need program code length in order to compute register base,
	 * but can't get program code length without relocating registers */
	__reg_base = 0;
	.wang600dummy (NOLOAD) : {
		__org_dummy = .;
		*(.wang600dummy);
		__reg_base = .;
	} >dummy
	__reg_base = __reg_base - __org_dummy;
	__reg_base = (__reg_base + 7) / 8;
	__reg_base = 246 - __reg_base;
	.wang600regs (NOLOAD) : {
		. = __reg_base;
		PROVIDE(longreg_base = .);
		*(.wang600regs);
		__last_reg = __reg_base - (. - __reg_base);
	} >regs

	__curr = 0x0000;
	.wang600prog : AT (__curr) {
		*(.wang600code);
		__curr = .;
	} >code = 0x93939393

	/* it appears to actually be ".align 8" that aligns this... */
	__curr = (__curr + 7) & 0xfffffff8;
	.wang600prog : AT (__curr) {
		*(.wang600data);
		__curr = .;
	} >code = 0x93939393

	/* only if needed? how? not for ROM images... */
	.wang600prog : AT (__curr) {
		/* must ensure register data doesn't mess up SEARCH end_prog */
		BYTE(0); /* a better value? */
		BYTE(_op_MARK);
		BYTE(end_prog);
		BYTE(_op_END_PROG);
		BYTE(_op_END_PROG);
		__curr = .;
	} >code = 0x93939393
	_prog_len = __curr;
}
