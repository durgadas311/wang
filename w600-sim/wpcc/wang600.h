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

#define E(dig)		_opreg(E, dig)
#define DP()		_opcode(DP)
#define SET_EXP()	_opcode(SET_EXP)
#define CHANGE_SIGN()	_opcode(CHANGE_SIGN)
#define CLEAR()		_opcode(CLEAR)
#define CLR_DISP()	_opcode(CLR_DISP)
#define T(reg)		_opreg(T, reg)
#define ADD(reg)	_opreg(ADD, reg)
#define SUB(reg)	_opreg(SUB, reg)
#define MULT(reg)	_opreg(MULT, reg)
#define DIV(reg)	_opreg(DIV, reg)
#define ST(reg)		_opreg(ST, reg)
#define RE(reg)		_opreg(RE, reg)

#define _SEARCH(label)	_opcode(SEARCH) _bytecode(label)
#define SEARCH(label)	_oplabel(_search_,label)
#define RECALL(longreg)	_oplongreg(RECALL,longreg)
#define _RECALL(reg)	_opcode(RECALL) _bytecode(reg)
#define PRINT(dp,tag)	_opcode(PRINT) _bytecode((tag << 4) | dp)
#define GO()		_opcode(GO)
#define J_IF_0()	_opcode(J_IF_0)
#define J_IF_P()	_opcode(J_IF_P)
#define SIN()		_opcode(SIN)
#define COS()		_opcode(COS)
#define TAN()		_opcode(TAN)
#define RAD_DEG()	_opcode(RAD_DEG)
#define LOG_E_X()	_opcode(LOG_E_X)
#define E_X()		_opcode(E_X)
#define X_2()		_opcode(X_2)
#define SQRT()		_opcode(SQRT)
#define LOAD_PROG()	_opcode(LOAD_PROG)
#define INV()		_opcode(INV)

#define _MARK(label)	_opcode(MARK) _bytecode(label)
#define MARK(label)	_opcode(MARK) _bytecode(label)
#define STORE(longreg)	_oplongreg(STORE,longreg)
#define _STORE(reg)	_opcode(STORE) _bytecode(reg)
#define ALPHA(cmd)	_opcode(ALPHA) _opcode(cmd)
#define STOP()		_opcode(STOP)
#define J_IF_N0()	_opcode(J_IF_N0)
#define J_IF_ERR()	_opcode(J_IF_ERR)
#define SIN_1()		_opcode(SIN_1)
#define COS_1()		_opcode(COS_1)
#define TAN_1()		_opcode(TAN_1)
#define DEG_RAD()	_opcode(DEG_RAD)
#define LOG_10_X()	_opcode(LOG_10_X)
#define E10_X()		_opcode(E10_X)
#define INT()		_opcode(INT)
#define ABS()		_opcode(ABS)
/* NOTE: not actual END PROG, but SEARCH to end_prog */
#define END_PROG()	_oplabel(_search_,end_prog)
#define RETURN()	_opcode(RETURN)

#define FCALL(label)	_bytecode(_subr_ ## label)

/* these should not be used? */
#define _f(x)		_opreg(f, x)
#define _F(x)		_opreg(F, x)
#define _ROM_f(x)	_opreg(ROM_f, x)
#define _ROM_F(x)	_opreg(ROM_F, x)

#define EXCHG(reg)	_opreg(EXCHG, reg)

#define IO(func)	_opcode(IO) _bytecode(func)
#define _ROM_SEARCH(label) _opcode(SEARCH_ROM) _bytecode(label)
#define _CALL(label)	_opcode(CALL) _bytecode(label)
#define CALL(label)	_oplabel(_call_,label)
#define INDIR(regop)	_opcode(INDIR) regop
#define _ROM_CALL(label) _opcode(CALL_ROM) _bytecode(label)
#define GROUP1(func)	_opcode(GROUP1) _bytecode(func)
#define GROUP2(func)	_opcode(GROUP2) _bytecode(func)

#define J_IF_E()	IO(_op_f0)
#define J_IF_NE()	IO(_op_F0)

#define KTRACE_ON()	ALPHA(PRINT)
#define KTRACE_OFF()	ALPHA(ALPHA)
#define PTRACE_ON()	ALPHA(LOG_E_X)
#define PTRACE_OFF()	ALPHA(E_X)
#define PAUSE()		ALPHA(STOP)
#define LOAD_REGS()	ALPHA(RECALL)
#define REC_REGS()	ALPHA(STORE)
#define PI()		ALPHA(f0)
#define POW10(n)	ALPHA(f ## n)
#define POW_10(n)	ALPHA(F ## n)
#define J_IF_EQ()	ALPHA(J_IF_0)
#define J_IF_GE()	ALPHA(J_IF_P)
#define J_IF_LT()	ALPHA(SIN)
#define JUMP(reg)	INDIR(E(reg))

#endif /* __wpcc_wang600_h__ */
