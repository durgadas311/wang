/* OUTPUT_FORMAT(binary); */

MEMORY {
	labels     : ORIGIN = 0x60, LENGTH = 0x100 /* must not use END PROG */
	flabels    : ORIGIN = 0x10, LENGTH = 0x30	/* leave some "reserved" */
	subr       : ORIGIN = 0x10, LENGTH = 0x30
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

	.wang700label (NOLOAD) : {
		*(.wang700label);
	} >labels
	PROVIDE(end_prog = 0x5f); /* hard-code to avoid conflict with reg data */
	.wang700flabels (NOLOAD) : {
		*(.wang700flabel);
	} >flabels
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
		/* this includes all program code, but not the postamble... */
		/* must arrange for uninitialized registers to skip over postamble */
	} >dummy
	/* register data is interleaved, so must align on 16-bytes */
	/* however, registers are number as if 8-bytes each... */
	__reg_base = (__reg_base + 15) & 0xfffffff0;
	__reg_base = __reg_base - __org_dummy;
	__reg_base = __reg_base / 8;
	__reg_base = 248 - __reg_base;
	.wang700regs (NOLOAD) : {
		. = __reg_base;
		PROVIDE(longreg_base = .);
		*(.wang700regs);
		__last_reg = __reg_base - (. - __reg_base) - 1;
		/* last register is always odd */
	} >regs

	__curr = 0x0000;
	.wang700prog : AT (__curr) {
		*(.wang700code);
		__curr = .;
	} >code = 0x5f5f5f5f

	/* register data is interleaved, so must align on 16-bytes */
	/* need to avoid alignment if no data... */
	/* seems as though alignment is controlled by .wang700data anyway? */
	/*__curr = (__curr + 15) & 0xfffffff0; */
	/* "AT" seems to do nothing at all, alignment is only controlled */
	/* by 'as' directives in the ".wang700data" section itself. */
	.wang700prog : AT (__curr) {
		/*
		 * section will auto-align, but we can't define __start_regs in modules
		 * without multiple defginition errors here. So, do the same alignment when
		 * computing __start_regs here.
		 *
		 */
		__start_regs = ((. + 15) & 0xfffffff0);
		*(.wang700data);
		/* must make certain an even number of registers are allocated */
		/* it seems that ld forces a minimum size == alignment, so
		 * one register takes 16 bytes, filled with STOP commands.
		 * must handle that in post-processing.
		 */
		. = ((. + 15) & 0xfffffff0);
		__init_space = ABSOLUTE(. - __start_regs) / 8;
		__curr = .;
	} >code = 0x00000000
	/* for some reason, can't control the fill byte above. Still uses 5f... */

	/* only if needed? how? */
	/* this will occupy 2 (interleaved) registers after any initialized ones... */
	/*
	 * since this always "wastes" 16 bytes, we add info about register data
	 * that needs interleaving here.
	 */
	.wang700prog : AT (__curr) {
		/* must ensure register data doesn't mess up SEARCH end_prog */
		BYTE(_op_GO);	/* this is never used as a label */
		BYTE(__init_space);
		BYTE(_op_MARK);
		BYTE(end_prog);
		BYTE(_op_END_PROG);
		BYTE(_op_END_PROG);
		__curr = .;
	} >code = 0x5f5f5f5f
	_prog_len = __curr;
}
