/*
 * Internals of warping Wang 700 programs through GCC
 *
 *
 */

#ifndef __wpcc_wang700internals_h__
#define __wpcc_wang700internals_h__

asm(".ident \"Wang 700 Compiler over GCC $Revision: 1.5 $ \"");

asm(	".section .wang700code, \"a\";"
	".include \"wang700opcodes.s\";"
	".pushsection .wang700regs,\"a\";"
	".subsection 0;"
	".type longreg_base STT_OBJECT;"
	"longreg_base:;"
	".subsection 1;"
	".byte 0;"
	".section .wang700data,\"a\";" \
	".align 16;" \
	".popsection"
);

#define _shadow_code(n)	 asm( \
			".pushsection .wang700dummy,\"a\";" \
			".rept " # n "; .byte 0; .endr;" \
			".popsection");

#define _bytecode(byte)		asm(".byte (" # byte ")" ); _shadow_code(1)

#define _opcode(op)		asm(".byte (_op_" # op ")" ); _shadow_code(1)

#define _oplabel(prefix,label)	asm(".byte (" # prefix # label "),(" # label ")"); \
					_shadow_code(2)

#define _longreg(reg)		asm(".pushsection .wang700regs,1,\"a\";" \
					".type _longreg_" #reg " STT_OBJECT;"	\
					".global _longreg_" #reg ";"	\
					".set _longreg_" #reg ",longreg_base+(longreg_base-.);" \
					".byte 0;"	\
					".popsection");

#define _regdata(reg,b0,b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,b11,b12,b13,b14,b15)	\
				asm(".pushsection .wang700regs,\"a\";" \
					".subsection 0;" \
					".type _longreg_" #reg " STT_OBJECT;" \
					".global _longreg_" #reg ";" \
					".set _longreg_" #reg ",longreg_base+(longreg_base-.);" \
					".byte 0;"	\
					".section .wang700data,\"a\";" \
					".byte ((" #b15 ") << 4) | (" #b14 ");"	\
					".byte ((" #b13 ") << 4) | (" #b12 ");"	\
					".byte ((" #b11 ") << 4) | (" #b10 ");"	\
					".byte ((" #b9 ") << 4) | (" #b8 ");"	\
					".byte ((" #b7 ") << 4) | (" #b6 ");"	\
					".byte ((" #b5 ") << 4) | (" #b4 ");"	\
					".byte ((" #b3 ") << 4) | (" #b2 ");"	\
					".byte ((" #b1 ") << 4) | (" #b0 ");"	\
					".popsection");

#define _oplongreg(op,reg)	_bytecode(0xe0 | (_op_ ##op & 0x0f)); \
				_bytecode(_longreg_ ##reg)

/***************************************************************************/

#define BEGIN()
#define END()

#define RES_EXTERN(label, const)	\
				asm(".pushsection .wang700label,\"a\";" \
					".type " #label " STT_OBJECT;" \
					".global " #label ";" \
					".set " #label ", res_label" #const ";" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					".global _search_" #label ";" \
					".set _search_" #label ", 0x80;" \
					".popsection");

// Define a label for use with SEARCH/MARK
#define LABEL(label)		asm(".pushsection .wang700label,\"a\";" \
					".type " #label " STT_OBJECT;" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					".global _search_" #label ";" \
					"_search_" #label ":;" \
					".popsection");

// Define a label for use with SEARCH/MARK
#define LLABEL(label)		asm(".pushsection .wang700label,\"a\";" \
					".type " #label " STT_OBJECT;" \
					#label ":  .byte 0;" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					"_search_" #label ":;" \
					".popsection");

// Define a label for use with FCALL/MARK
#define FLABEL(label)		asm(".pushsection .wang700flabel,\"a\";" \
					".type " #label " STT_OBJECT;" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					".global _search_" #label ";" \
					"_search_" #label ":;" \
					".section .wang700subr,\"a\";" \
					".type _subr_" #label " STT_OBJECT;" \
					".global _subr_" #label ";" \
					"_subr_" #label ":  .byte 0;" \
					".popsection");

// Define a label for use with FCALL/MARK
#define FLLABEL(label)		asm(".pushsection .wang700flabel,\"a\";" \
					".type " #label " STT_OBJECT;" \
					#label ":  .byte 0;" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					"_search_" #label ":;" \
					".section .wang700subr,\"a\";" \
					".type _subr_" #label " STT_OBJECT;" \
					"_subr_" #label ":  .byte 0;" \
					".popsection");

// A label from another module for use with SEARCH/CALL
#define EXTERNAL(label)		asm(".pushsection .wang700label,\"a\";" \
					".type " #label " STT_OBJECT;" \
					".global " #label ";" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					".global _search_" #label ";" \
					".popsection");

// A label from another module for use with FCALL
#define FEXTERNAL(label)	asm(".pushsection .wang700flabel,\"a\";" \
					".type " #label " STT_OBJECT;" \
					".global " #label ";" \
					".section .wang700search,\"a\";" \
					".type _search_" #label " STT_OBJECT;" \
					".global _search_" #label ";" \
					".section .wang700subr,\"a\";" \
					".type _subr_" #label " STT_OBJECT;" \
					".global _subr_" #label ";" \
					".popsection");

// Does not prevent conflicts. Programmer must ensure "reg" has no
// conflicting uses (including program code) in the same program.
// Does not allocarte any space, simply sets up reference to the register
// number by the given label.
#define RES_REG(label,reg)	asm(\
					".type _longreg_" #label " STT_OBJECT;" \
					".global _longreg_" #label ";" \
					".set _longreg_" #label "," #reg );

// Reserve an un-initialize long register. This prevents conflicts
// between all UREG() definitions, and will not overwrite program code.
// However, RES_REG() can still conflict. This actually allocates "empty" space
// for the register data.
#define UREG(name)		_longreg(name)

/* These should be pre-rpocessed and never exist when gcc invoked */
#define ENTER(num)		asm(".error \"run w7cpp preprocessor for ENTER()\"");
#define IREG_DATA(reg,num)	asm(".error \"run w7cpp preprocessor for IREG_DATA()\"");
#define ALPHA_STRING(str)	asm(".error \"run w7cpp preprocessor for ALPHA_STRING()\"");
#define ALPHA_PLOT(str)		asm(".error \"run w7cpp preprocessor for ALPHA_PLOT()\"");

// Enter into X the register number associated with the symbol <label>
#define ENTER_REGNO(label)	_bytecode(0xed) \
				_bytecode(0xed) \
				_bytecode(_longreg_ #label )

// Enter into X the highest register number not occupied by program code
#define ENTER_LAST_REGNO()	_bytecode(0xed) \
				_bytecode(0xed) \
				_bytecode(__last_regno)

// Pseudo constructs for embedding data (future plan)
#define NAME(prog_name)		asm(".pushsection .wang700name,\"a\",@note;" \
					".string \"" prog_name "\";" \
					".popsection");

#define AUTHOR(name)		asm(".pushsection .wang700author,\"a\",@note;" \
					".string \"" name "\";" \
					".popsection");

#define TITLE(title)		asm(".pushsection .wang700title,\"a\",@note;" \
					".string \"" title "\";" \
					".popsection");

#define HELP_BEGIN(string)	asm(".pushsection .wang700help,\"a\",@note;" \
					".string \"" #string "\";"\
					".popsection");

#endif /* __wpcc_wang700internals_h__ */
