"object" file sections:

	.wang600label
		Abstract. Allocation of labels for nun-subroutine (non-f(x)) use.
		00 00 - 09 13, to prevent accidental use of END PROG (09 14).

	.wang600flabel
		Abstract. Allocation of labels for subroutine (f(x)) use.
		10 00 - 11 15 for RAM code, 12 00 - 13 15 for ROM code.

	.wang600data
		Loaded. Register data. Defines symbols used for "long registers"
		(INDIR and RECALL/STORE), but also provides initialization data.
		Aligned on 8-byte boundaries.

	.wang600prog
		Loaded. Program codes, and references to labels and data (long registers).
		Also may reference _loc_search and _loc_call which are
		defined as 08 00, 15 07 (respectively) for RAM code, and
		15 03, 15 12 for ROM code.


"executable" file layout (binary):

	+----------------+
	| Header         |
	+----------------+
	| (optional)     |
	| Program Notes  |
	|                |
	+----------------+
	|                |
	| Program Code   |
	|                |
	+- - - - - - - - +
	| MARK _end_prog |
	| END-PROG *     |
	+----------------+
	| (optional)     |
	| Register Data  |
	|                |
	+- - - - - - - - +
	| END-PROG 15-15 |
	+----------------+
	| (optional)     |
	| ROM code       |
	|                |
	+- - - - - - - - +
	| END-PROG *     |
	+----------------+
	| (optional)     |
	| W630 Image     |
	|                |
	+- - - - - - - - +
	| END-PROG *     |
	+----------------+
	| EOF            |
	+----------------+

Header:
	struct {
		char id[4] = "w600";
		short code_offset;
		short data_offset;
		short rom_offset;
		short w630_offset;
		short _reserved1;
		short _reserved2;
	};

Linking targets:

	RAM Program

	RAM Program + Register Data

	ROM Image

	ROM Image + Register Data

	RAM Program + ROM Image

	RAM Program + Register Data + ROM Image

	m630 Disk Image

	m630 Disk Image + ROM Image


Labels:

	15 15 xx xx	Search RAM	15 07 xx xx	Call RAM
	15 04 xx xx	Search ROM	15 12 xx xx	Call ROM

	Call = Search ^ 00 80 00 00

setup three sections for each label:

			Label prefix	RAM ORG		ROM ORG
	.wang600search	_search_	0x8000		0xf300
	.wang600call	_call_		0xf700		0xfc00
	.wang600mark	_mark_		0x9000		0x9000

asm:

	declare:
		.pushsection .wang600search,"a"
		.global _search_#label
		_search_#label:	.byte 0
		.section .wang600call
		.global _call_#label
		_call_#label:	.byte 0
		.section .wang600mark
		.global _mark_#label
		_mark_#label:	.byte 0
		.popsection

	mark:
		.short _mark_#label

	search: (and END())
		.short _search_#label

	call:
		.short _call_#label

	!!! need fix for endianess !!!

linker script:

	MEMORY {
        	mark       : ORIGIN = 0x9000, LENGTH = 0x9e /* must not use END PROG */
        	search     : ORIGIN = 0x8000, LENGTH = 0x9e /* must not use END PROG */
        	call       : ORIGIN = 0xf700, LENGTH = 0x9e /* must not use END PROG */
        	rom_mark   : ORIGIN = 0x9000, LENGTH = 0x9e /* must not use END PROG */
        	rom_search : ORIGIN = 0xf300, LENGTH = 0x9e /* must not use END PROG */
        	rom_call   : ORIGIN = 0xfc00, LENGTH = 0x9e /* must not use END PROG */
		...

	.wang600mark (NOLOAD) : {
		*(.wang600mark);
		PROVIDE(_mark_end_prog = .);
		BYTE(0);
	} >mark
	.wang600search (NOLOAD) : {
		*(.wang600search);
		PROVIDE(_search_end_prog = .);
		BYTE(0);
	} >search
	.wang600call (NOLOAD) : {
		*(.wang600call);
	} >call
	...

	.wang600prog : AT (__curr) {
		*(.wang600code);
                /* only if needed? how? */
                SHORT(_mark_end_prog);
                BYTE(0x9e);
                BYTE(0x9e);
        } >code
	...
