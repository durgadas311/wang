/*
 * Internals of warping Wang 600 programs through GCC
 *
 *
 */

#ifndef __wpcc_wang600internals_h__
#define __wpcc_wang600internals_h__

asm(".ident \"Wang 600 Compiler over GCC $Revision: 1.9 $ \"");
asm(".include \"wang600opcodes.s\"");

asm(	".section .wang600code, \"a\";"
	".pushsection .wang600regs,\"a\";"
	".subsection 0;"
	"longreg_base:;"
	".subsection 1;"
	".byte 0;"
	".popsection"
);

#define _shadow_code(n)	 asm( \
			".pushsection .wang600dummy,\"a\";" \
			".rept " # n "; .byte 0; .endr;" \
			".popsection");

#define _bytecode(byte)		asm(".byte (" # byte ")" ); _shadow_code(1)

#define _opcode(op)		asm(".byte (_op_" # op ")" ); _shadow_code(1)
#define _opreg(op,reg)		asm(".byte (_op_" #op #reg ")" ); _shadow_code(1)

#define _oplabel(prefix,label)	asm(".byte (" # prefix # label "),(" # label ")"); \
					_shadow_code(2)

#define _reg(reg)		asm(".pushsection .wang600regs,1,\"a\";" \
					".global " #reg ";"	\
					#reg ": .byte 0;"	\
					".popsection");

#define _regdata(reg,b0,b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,b11,b12,b13,b14,b15)	\
				asm(".pushsection .wang600regs,\"a\";" \
					".subsection 0;" \
					".global " #reg ";" \
					#reg ": .byte 0;"	\
					".section .wang600data,\"a\";" \
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

#define _oplongreg(op,reg)	_opcode(op) \
				_bytecode(longreg_base+(longreg_base-reg))

/***************************************************************************/

#define RES_EXTERN(label, const)	\
				asm(".pushsection .wang600label,\"a\";" \
					".global " #label ";" \
					".set " #label ", res_label" #const ";" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					".set _search_" #label ", 0x80;" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					".set _call_" #label ", 0xf7;" \
					".popsection");

#define BEGIN()
#define END()

// Define a label for use with SEARCH/MARK
#define LABEL(label)		asm(".pushsection .wang600label,\"a\";" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					"_search_" #label ":;" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					"_call_" #label ":;" \
					".popsection");

// Define a label for use with SEARCH/MARK
#define LLABEL(label)		asm(".pushsection .wang600label,\"a\";" \
					#label ":  .byte 0;" \
					".section .wang600search,\"a\";" \
					"_search_" #label ":;" \
					".section .wang600call,\"a\";" \
					"_call_" #label ":;" \
					".popsection");

// Define a label for use with FCALL/MARK
#define FLABEL(label)		asm(".pushsection .wang600flabel,\"a\";" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					"_search_" #label ":;" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					"_call_" #label ":;" \
					".section .wang600subr,\"a\";" \
					".global _subr_" #label ";" \
					".set _subr_" #label "," #label ";" \
					".popsection");

// Define a label for use with FCALL/MARK
#define FLLABEL(label)		asm(".pushsection .wang600flabel,\"a\";" \
					#label ":  .byte 0;" \
					".section .wang600search,\"a\";" \
					"_search_" #label ":;" \
					".section .wang600call,\"a\";" \
					"_call_" #label ":;" \
					".section .wang600subr,\"a\";" \
					".set _subr_" #label "," #label ";" \
					".popsection");

// A label from another module for use with SEARCH/CALL
#define EXTERNAL(label)		asm(".pushsection .wang600label,\"a\";" \
					".global " #label ";" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					".popsection");

// A label from another module for use with FCALL
#define FEXTERNAL(label)	asm(".pushsection .wang600flabel,\"a\";" \
					".global " #label ";" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					".section .wang600subr,\"a\";" \
					".global _subr_" #label ";" \
					".popsection");

// Reserve an un-initialize long register
#define UREG(name)		_reg(name)

/* These should be pre-rpocessed and never exist when gcc invoked */
#define ENTER(num)		asm(".error \"run w6cpp preprocessor for ENTER()\"");
#define IREG_DATA(reg,num)	asm(".error \"run w6cpp preprocessor for IREG_DATA()\"");
#define ALPHA_STRING(str)	asm(".error \"run w6cpp preprocessor for ALPHA_STRING()\"");
#define ALPHA_PLOT(str)		asm(".error \"run w6cpp preprocessor for ALPHA_PLOT()\"");

#define ENTER_LAST_REGNO()	_bytecode(last_regno_100) \
				_bytecode(last_regno_10) \
				_bytecode(last_regno_1)

// Pseudo constructs for embedding data (future plan)
#define NAME(prog_name)		asm(".pushsection .wang600name,\"a\",@note;" \
					".string \"" prog_name "\";" \
					".popsection");

#define AUTHOR(name)		asm(".pushsection .wang600author,\"a\",@note;" \
					".string \"" name "\";" \
					".popsection");

#define TITLE(title)		asm(".pushsection .wang600title,\"a\",@note;" \
					".string \"" title "\";" \
					".popsection");

#define HELP_BEGIN(string)	asm(".pushsection .wang600help,\"a\",@note;" \
					".string \"" #string "\";"\
					".popsection");

#endif /* __wpcc_wang600internals_h__ */
