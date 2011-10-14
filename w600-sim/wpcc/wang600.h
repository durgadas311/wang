/*
 * Main include file for WPCC: Wang Programmable Calculator Compiler
 *
 *
 */

#ifndef __wpcc_wang600_h__
#define __wpcc_wang600_h__

asm(".ident \"Wang 600 Compiler over GCC\"");

asm(	".section .wang600code, \"a\";"
	".pushsection .wang600search,\"a\";"
	".global _search_base;"
	".global _search_end_prog;"
	".set _search_end_prog, _search_base;"
	".section .wang600call,\"a\";"
	".global _call_base;"
	" .popsection"
);

#define BEGIN()
#define END()			_oplabel(_search_,end_prog);

#define _opcode(byte)		asm(" .byte (" # byte ")" );
#define _oplabel(prefix,label)	asm(" .byte (" # prefix # label ")," \
					"(" # label ")" );
#define _regop(cmd, reg)	_opcode((cmd << 4) | (reg & 0x0f))
#define _regdata(reg,b0,b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,b11,b12,b13,b14,b15)	\
				asm(" .pushsection .wang600regs,\"a\";" \
					".global _reg_base;" \
					".global " #reg ";" \
					#reg ": .byte 0;"	\
					".section .wang600data,\"a\";" \
					".align 8;"		\
					".byte ((" #b1 ") << 4) | (" #b0 ");"	\
					".byte ((" #b3 ") << 4) | (" #b2 ");"	\
					".byte ((" #b5 ") << 4) | (" #b4 ");"	\
					".byte ((" #b7 ") << 4) | (" #b6 ");"	\
					".byte ((" #b9 ") << 4) | (" #b8 ");"	\
					".byte ((" #b11 ") << 4) | (" #b10 ");"	\
					".byte ((" #b13 ") << 4) | (" #b12 ");"	\
					".byte ((" #b15 ") << 4) | (" #b14 ");"	\
					" .popsection");

#define LABEL(label)\
				asm(".pushsection .wang600label,\"a\";" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					".set _search_" #label ", _search_base;" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					".set _call_" #label ", _call_base;" \
					" .popsection");

#define FLABEL(label)		asm(" .pushsection .wang600flabel,\"a\";" \
					".global " #label ";" \
					#label ":  .byte 0;" \
					" .popsection");

#define EXTERNAL(label)	\
				asm(".pushsection .wang600label,\"a\";" \
					".global " #label ";" \
					".section .wang600search,\"a\";" \
					".global _search_" #label ";" \
					".set _search_" #label ", _search_base;" \
					".section .wang600call,\"a\";" \
					".global _call_" #label ";" \
					".set _call_" #label ", _call_base;" \
					" .popsection");

#define FEXTERNAL(label)	asm(" .pushsection .wang600flabel,\"a\";" \
					".global " #label ";" \
					" .popsection");

#define NAME(prog_name)		asm(".pushsection .wang600name,\"a\",@note;" \
					".string \"" prog_name "\";" \
					" .popsection");

#define AUTHOR(name)		asm(".pushsection .wang600author,\"a\",@note;" \
					".string \"" name "\";" \
					" .popsection");

#define TITLE(title)		asm(".pushsection .wang600title,\"a\",@note;" \
					".string \"" title "\";" \
					" .popsection");

#define HELP_BEGIN()		asm(".pushsection .wang600help,\"a\",@note;" \
					".string \"");
#define HELP_END()		asm("\";"\
					" .popsection");

/* util macros for printer formating */
#define tag_X	0
#define tag_Y	1
#define tag_Z	2
#define tag_A	3
#define tag_B	4
#define tag_C	5
#define tag_D	6
#define tag_E	7
#define tag_F	8
#define tag_G	9
#define tag_H	10
#define tag_I	11
#define tag_J	12
#define tag_K	13
#define tag_L	14
#define tag_M	15
#define dp_sci	11
#define dp_blank 15

#define E(dig)		_regop(0, dig)
#define DP()		_opcode(0x0a)
#define SET_EXP()	_opcode(0x0b)
#define CHANGE_SIGN()	_opcode(0x0c)
#define CLEAR()		_opcode(0x0e)
#define CLR_DISP()	_opcode(0x0f)
#define T(reg)		_regop(1, reg)
#define ADD(reg)	_regop(2, reg)
#define SUB(reg)	_regop(3, reg)
#define MULT(reg)	_regop(4, reg)
#define DIV(reg)	_regop(5, reg)
#define ST(reg)		_regop(6, reg)
#define RE(reg)		_regop(7, reg)

#define SEARCH(label)	_oplabel(_search_,label)
#define RECALL(longreg)	_opcode(0x81) \
				asm(" .pushsection .wang600regs,\"a\";" \
					".global " #longreg ";" \
					" .popsection");	\
			_opcode(longreg)
#define PRINT(dp,tag)	_opcode(0x82) _regop(tag,dp)
#define GO()		_opcode(0x83)
#define J_IF_0()	_opcode(0x84)
#define J_IF_P()	_opcode(0x85)
#define SIN()		_opcode(0x86)
#define COS()		_opcode(0x87)
#define TAN()		_opcode(0x88)
#define RAD_DEG()	_opcode(0x89)
#define LOG_E_X()	_opcode(0x8a)
#define E_X()		_opcode(0x8b)
#define X_2()		_opcode(0x8c)
#define SQRT()		_opcode(0x8d)
#define LOAD_PROG()	_opcode(0x8e)
#define INV()		_opcode(0x8f)

#define MARK(label)	_opcode(0x90) _opcode(label)
#define STORE(longreg)	_opcode(0x91) _opcode(longreg)
#define ALPHA(cmd)	_opcode(0x92) cmd
#define STOP()		_opcode(0x93)
#define J_IF_N0()	_opcode(0x94)
#define J_IF_ERR()	_opcode(0x95)
#define SIN_1()		_opcode(0x96)
#define COS_1()		_opcode(0x97)
#define TAN_1()		_opcode(0x98)
#define DEG_RAD()	_opcode(0x99)
#define LOG_10_X()	_opcode(0x9a)
#define E10_X()		_opcode(0x9b)
#define INT()		_opcode(0x9c)
#define ABS()		_opcode(0x9d)
/* NOTE: tape format wants double-END PROG */
#define END_PROG()	_opcode(0x9e) _opcode(0x9e)
#define RETURN()	_opcode(0x9f)

#define f(x)		_regop(10, x)
#define F(x)		_regop(11, x)

#define ROM_f(x)	_regop(12, x)
#define ROM_F(x)	_regop(13, x)

#define EXCHG(reg)	_regop(14, reg)

#define IO(func)	_opcode(0xf2) _opcode(func)
#define ROM_SEARCH(label) _opcode(0xf3) _opcode(label)
#define CALL(label)	_oplabel(_call_,label)
#define INDIR(regop)	_opcode(0xfb) regop
#define ROM_CALL(label)	_opcode(0xfc) _opcode(label)
#define GROUP1(func)	_opcode(0xfd) _opcode(func)
#define GROUP2(func)	_opcode(0xfe) _opcode(func)

#define J_IF_EQ()	IO(0xa0)
#define J_IF_NE()	IO(0xb0)

#define KTRACE_ON()	ALPHA(_opcode(0x82))
#define KTRACE_OFF()	ALPHA(_opcode(0x92))
#define PTRACE_ON()	ALPHA(LOG_E_X())
#define PTRACE_OFF()	ALPHA(E_X())
#define PAUSE()		ALPHA(STOP())
#define PI()		ALPHA(f(0))
#define POW10(n)	ALPHA(f(n))
#define POW_10(n)	ALPHA(F(n))
#define J_IF_EQUAL()	ALPHA(J_IF_0())
#define J_IF_GT		ALPHA(J_IF_P())
#define J_IF_LT		ALPHA(SIN())
#define JUMP(reg)	INDIR(E(reg))

/* These should be pre-rpocessed and never exist when gcc invoked */
#define ENTER(num)		asm(".error \"run wpcpp preprocessor for ENTER()\"");
#define ALPHA_STRING(num)	asm(".error \"run wpcpp preprocessor for ALPHA_STRING()\"");

#endif /* __wpcc_wang600_h__ */
