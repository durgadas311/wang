/*
 * Internals of warping Wang 700 programs through GCC
 *
 *
 */

#ifndef __wpcc_wang700internals_h__
#define __wpcc_wang700internals_h__

asm(".ident \"Wang 700 Compiler over GCC $Revision: 1.2 $ \"");

asm(	".section .wang700code, \"a\";"
	".include \"wang700opcodes.s\";"
	".pushsection .wang700regs,\"a\";"
	".subsection 0;"
	".type longreg_base STT_OBJECT;"
	"longreg_base:;"
	".subsection 1;"
	".byte 0;"
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
					".align 8;"		\
					".byte ((" #b15 ") << 4) | (" #b14 ");"	\
					".byte ((" #b13 ") << 4) | (" #b12 ");"	\
					".byte ((" #b11 ") << 4) | (" #b10 ");"	\
					".byte ((" #b9 ") << 4) | (" #b8 ");"	\
					".byte ((" #b7 ") << 4) | (" #b6 ");"	\
					".byte ((" #b5 ") << 4) | (" #b4 ");"	\
					".byte ((" #b3 ") << 4) | (" #b2 ");"	\
					".byte ((" #b1 ") << 4) | (" #b0 ");"	\
					".popsection");

#define _oplongreg(op,reg)	_opcode(op) _bytecode(_longreg_ ##reg)

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

// reg must be stored in BCD... todo: handle >100 case
// (((reg / 100) << 4) | (reg % 100))
#define RES_REG(label,reg)	asm(\
					".type _longreg_" #label " STT_OBJECT;" \
					".global _longreg_" #label ";" \
					".set _longreg_" #label ",(((" #reg " / 10) << 4) | (" #reg " % 10))");

// Reserve an un-initialize long register
#define UREG(name)		_longreg(name)

/* These should be pre-rpocessed and never exist when gcc invoked */
#define ENTER(num)		asm(".error \"run w7cpp preprocessor for ENTER()\"");
#define IREG_DATA(reg,num)	asm(".error \"run w7cpp preprocessor for IREG_DATA()\"");
#define ALPHA_STRING(str)	asm(".error \"run w7cpp preprocessor for ALPHA_STRING()\"");
#define ALPHA_PLOT(str)		asm(".error \"run w7cpp preprocessor for ALPHA_PLOT()\"");

#define ENTER_LAST_REGNO()	_bytecode(0x70+last_regno_100) \
				_bytecode(0x70+last_regno_10) \
				_bytecode(0x70+last_regno_1)

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
