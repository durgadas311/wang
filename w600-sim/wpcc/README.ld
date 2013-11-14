Internals of how GNU 'ld' (ELF file format) is used

"object" file sections:

	.wang600label
		Abstract. Allocation of labels for nun-subroutine (non-f(x)) use.
		00 00 - 09 13, to prevent accidental use of END PROG (09 14).

	.wang600flabel
		Abstract. Allocation of labels for subroutine (f(x)) use.
		10 00 - 11 15 for RAM code, 12 00 - 13 15 for ROM code.

	.wang600data
		Loaded. Register data. Aligned on 8-byte boundaries.

	.wang600code
		Loaded. Program codes, and references to labels and data (long registers).

	.wang600dummy
		Abstract. Used to calculate end of program codes to facilitate
		assignment of long register space.

	.wang600regs
		Abstract. Allocation of labels for long (extended) registers.

	.wang600search
	.wang600call
	.wang600subr
		Abstract. Used to delay assignment of actual SEARCH, CALL, and
		f(x)/F(x) command codes until link time, when it is known whether
		the target label is in ROM or RAM.

Special linker scripts process the above sections into executable Wang 600
program codes. There are currently two linker scripts, T_LDSCRIPT (wang600raw.ld)
for making Tape image programs, and X_LDSCRIPT (wang600rawrom.ld) for making
Expansion ROM (library) images.

Note, standard library "archives" (lib*.a) may be produced independent of the
final type of image. A program may be linked with a library (i.e. "static linking")
or may be linked against a ROM image (sort of "dynamic linking"). Note, however,
that there is no way for the Wang 600 to ensure that a program linked against
a ROM image is being run with the correct ROM image installed.
