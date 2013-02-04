/*
 * Main include file for WPCC: Wang Programmable Calculator Compiler
 *
 *
 */

#ifndef __wpcc_wang600_h__
#define __wpcc_wang600_h__

#include "wang600internals.h"

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

#define E(dig)		_opcode(dig)
#define DP()		_opcode(0x0a)
#define SET_EXP()	_opcode(0x0b)
#define CHANGE_SIGN()	_opcode(0x0c)
#define CLEAR()		_opcode(0x0e)
#define CLR_DISP()	_opcode(0x0f)
#define T(reg)		_opreg(1, reg)
#define ADD(reg)	_opreg(2, reg)
#define SUB(reg)	_opreg(3, reg)
#define MULT(reg)	_opreg(4, reg)
#define DIV(reg)	_opreg(5, reg)
#define ST(reg)		_opreg(6, reg)
#define RE(reg)		_opreg(7, reg)

#define _SEARCH(label)	_opcode(0x80) label
#define SEARCH(label)	_oplabel(_search_,label)
#define RECALL(longreg)	_oplongreg(0x81,longreg)
#define _RECALL(reg)	_opcode(0x81) _opcode(reg)
#define PRINT(dp,tag)	_opcode(0x82) _opreg(tag,dp)
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

#define _MARK(label)	_opcode(0x90) label
#define MARK(label)	_opcode(0x90) _opcode(label)
#define STORE(longreg)	_oplongreg(0x91,longreg)
#define _STORE(reg)	_opcode(0x91) _opcode(reg)
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
/* NOTE: not actual END PROG, but SEARCH to end_prog */
#define END_PROG()	_oplabel(_search_,end_prog)
#define RETURN()	_opcode(0x9f)

#define FCALL(fx)	_opcode(fx)

/* these should not be used? */
#define _f(x)		_opreg(10, x)
#define _F(x)		_opreg(11, x)
#define _ROM_f(x)	_opreg(12, x)
#define _ROM_F(x)	_opreg(13, x)

#define EXCHG(reg)	_opreg(14, reg)

#define IO(func)	_opcode(0xf2) _opcode(func)
#define _ROM_SEARCH(label) _opcode(0xf3) label
#define _CALL(label)	_opcode(0xf7) label
#define CALL(label)	_oplabel(_call_,label)
#define INDIR(regop)	_opcode(0xfb) regop
#define _ROM_CALL(label) _opcode(0xfc) label
#define GROUP1(func)	_opcode(0xfd) _opcode(func)
#define GROUP2(func)	_opcode(0xfe) _opcode(func)

#define J_IF_E()	IO(0xa0)
#define J_IF_NE()	IO(0xb0)

#define KTRACE_ON()	ALPHA(_opcode(0x82))
#define KTRACE_OFF()	ALPHA(_opcode(0x92))
#define PTRACE_ON()	ALPHA(LOG_E_X())
#define PTRACE_OFF()	ALPHA(E_X())
#define PAUSE()		ALPHA(STOP())
#define PI()		ALPHA(_f(0))
#define POW10(n)	ALPHA(_f(n))
#define POW_10(n)	ALPHA(_F(n))
#define J_IF_EQ()	ALPHA(J_IF_0())
#define J_IF_GT()	ALPHA(J_IF_P())
#define J_IF_LT()	ALPHA(SIN())
#define JUMP(reg)	INDIR(E(reg))

#endif /* __wpcc_wang600_h__ */
