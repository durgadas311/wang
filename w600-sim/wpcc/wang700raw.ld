OUTPUT_FORMAT(binary);

MEMORY {
	labels     : ORIGIN = 0x60, LENGTH = 0x100 /* must not use END PROG */
	flabels    : ORIGIN = 0x00, LENGTH = 0x40
	search     : ORIGIN = 0x47, LENGTH = 1
	subr       : ORIGIN = 0x00, LENGTH = 0x40
	regs       : ORIGIN = 0x00, LENGTH = 0xffff
	hdr        : ORIGIN = 0x00, LENGTH = 0x10
	code       : ORIGIN = 0x00, LENGTH = 0x07c0 /* 0x000-0x7bf */
	dummy      : ORIGIN = 0x1000, LENGTH = 0x3fff
	data       : ORIGIN = 0x00, LENGTH = 0x7c0
	notes      : ORIGIN = 0x00, LENGTH = 0x1000000
}
SECTIONS
{
	/DISCARD/ : { *(.text) }
	/DISCARD/ : { *(.data) }
	/DISCARD/ : { *(.bss) }
	.wang700build-id (NOLOAD) : { *(.note.gnu.build-id) }

	/* some files might use rom_*labels/rom_search/rom_call... */
	.wang700label (NOLOAD) : {
		*(.wang700label);
	} >labels
	PROVIDE(_search_end_prog = 0x47);
	PROVIDE(end_prog = 0x5f); /* hard-code to avoid conflict with reg data */
	.wang700flabels (NOLOAD) : {
		*(.wang700flabel);
	} >flabels
	.wang700search (NOLOAD) : {
		*(.wang700search);
	} >search
	.wang700subr (NOLOAD) : {
		*(.wang700subr);
	} >subr
	/* need program code length in order to compute register base,
	 * but can't get program code length without relocating registers */
	__reg_base = 0;
	.wang700dummy (NOLOAD) : {
		__org_dummy = .;
		*(.wang700dummy);
		__reg_base = .;
	} >dummy
	__reg_base = __reg_base - __org_dummy;
	__reg_base = (__reg_base + 7) / 8;
	__reg_base = 248 - __reg_base;
	.wang700regs (NOLOAD) : {
		. = __reg_base;
		PROVIDE(longreg_base = .);
		*(.wang700regs);
		__last_reg = __reg_base - (. - __reg_base);
	} >regs
	PROVIDE(last_regno_100 = (__last_reg / 100));
	PROVIDE(last_regno_10 = (__last_reg / 10) % 10);
	PROVIDE(last_regno_1 = (__last_reg % 10));

	__curr = 0x0000;
	.wang700prog : AT (__curr) {
		*(.wang700code);
		__curr = .;
	} >code = 0x5f5f5f5f

	__curr = (__curr + 7) & 0xfffffff8;
	.wang700prog : AT (__curr) {
		*(.wang700data);
		/* must ensure register data doesn't mess up SEARCH end_prog */
		BYTE(0); /* a better value? */
		__curr = .;
	} >code = 0x5f5f5f5f

	/* only if needed? how? not for ROM images... */
	.wang700prog : AT (__curr) {
		BYTE(0x48);
		BYTE(end_prog);
		BYTE(0x5c);
		BYTE(0x5c);
		__curr = .;
	} >code = 0x5f5f5f5f
	_prog_len = __curr;
}
