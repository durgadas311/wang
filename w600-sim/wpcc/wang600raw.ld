OUTPUT_FORMAT(binary);

MEMORY {
	labels     : ORIGIN = 0x00, LENGTH = 0x9e /* must not use END PROG */
	flabels    : ORIGIN = 0xa0, LENGTH = 0x20 /* for ROM use 0xc0 */
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
	PROVIDE(last_regno_100 = (__last_reg / 100));
	PROVIDE(last_regno_10 = (__last_reg / 10) % 10);
	PROVIDE(last_regno_1 = (__last_reg % 10));

	__curr = 0x0000;
	.wang600prog : AT (__curr) {
		*(.wang600code);
		__curr = .;
	} >code = 0x93939393

	__curr = (__curr + 7) & 0xfffffff8;
	.wang600prog : AT (__curr) {
		*(.wang600data);
		/* must ensure register data doesn't mess up SEARCH end_prog */
		BYTE(0); /* a better value? */
		__curr = .;
	} >code = 0x93939393

	/* only if needed? how? not for ROM images... */
	.wang600prog : AT (__curr) {
		BYTE(0x90);
		BYTE(end_prog);
		BYTE(0x9e);
		BYTE(0x9e);
		__curr = .;
	} >code = 0x93939393
	_prog_len = __curr;
}
