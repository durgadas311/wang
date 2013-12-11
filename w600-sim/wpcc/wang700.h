/*
 * Main include file for WPCC: Wang Programmable Calculator Compiler
 *
 *
 */

#ifndef __wpcc_wang700_h__
#define __wpcc_wang700_h__

#include "wang700internals.h"

#define E(dig)		_opcode(E ## dig)
#define SET_EXP()	_opcode(SET_EXP)
#define CHANGE_SIGN()	_opcode(CHANGE_SIGN)
#define DP()		_opcode(DP)
#define X_2()		_opcode(X_2)
#define RE_RES()	_opcode(RE_RES)
#define CLR_X()		_opcode(CLR_X)

#define ADD_DIR(longreg)	_oplongreg(ADD_DIR,longreg)
#define SUB_DIR(longreg)	_oplongreg(SUB_DIR,longreg)
#define MULT_DIR(longreg)	_oplongreg(MULT_DIR,longreg)
#define DIV_DIR(longreg)	_oplongreg(DIV_DIR,longreg)
#define ST_DIR(longreg)		_oplongreg(ST_DIR,longreg)
#define RE_DIR(longreg)		_oplongreg(RE_DIR,longreg)
#define EXCHG_DIR(longreg)	_oplongreg(EXCHG_DIR,longreg)
#define _SEARCH(label)		_opcode(SEARCH) _bytecode(label)
#define SEARCH(label)		_oplabel(_search_,label)
#define _MARK(label)		_opcode(MARK) _bytecode(label)
#define MARK(label)		_opcode(MARK) _bytecode(label)
#define GROUP1(func)		_opcode(GROUP1) _bytecode(func)
#define GROUP2(func)		_opcode(GROUP2) _bytecode(func)
#define WRITE(func)		_opcode(WRITE) _bytecode(func)
#define WRITE_ALPHA(func)	_opcode(WRITE_ALPHA) _bytecode(func)
#define END_ALPHA(func)		_opcode(END_ALPHA) _bytecode(func)
#define ST_Y_DIR(longreg)	_oplongreg(ST_Y_DIR,longreg)
#define RE_Y_DIR(longreg)	_oplongreg(RE_Y_DIR,longreg)

#define ADD_IND()		_opcode(ADD_IND)
#define SUB_IND()		_opcode(SUB_IND)
#define MULT_IND()		_opcode(MULT_IND)
#define DIV_IND()		_opcode(DIV_IND)
#define ST_IND()		_opcode(ST_IND)
#define RE_IND()		_opcode(RE_IND)
#define EXCHG_IND()		_opcode(EXCHG_IND)
#define SK_IF_GE()		_opcode(SK_IF_GE)
#define SK_IF_LT()		_opcode(SK_IF_LT)
#define SK_IF_EQ()		_opcode(SK_IF_EQ)
#define SK_IF_ERR()		_opcode(SK_IF_ERR)
#define RETURN()		_opcode(RETURN)
/* NOTE: not actual END PROG, but SEARCH to end_prog */
#define END_PROG()		_oplabel(_search_,end_prog)
#define LOAD_PROG()		_opcode(LOAD_PROG)
#define GO()			_opcode(GO)
#define STOP()			_opcode(STOP)

#define ADD_Y()			_opcode(ADD_Y)
#define SUB_Y()			_opcode(SUB_Y)
#define MULT_Y()		_opcode(MULT_Y)
#define DIV_Y()			_opcode(DIV_Y)
#define ST_Y()			_opcode(ST_Y)
#define RE_Y()			_opcode(RE_Y)
#define EXCHG_Y()		_opcode(EXCHG_Y)
#define ABS()			_opcode(ABS)
#define INT()			_opcode(INT)
#define PI()			_opcode(PI)
#define LOG_10_X()		_opcode(LOG_10_X)
#define LOG_E_X()		_opcode(LOG_E_X)
#define SQRT()			_opcode(SQRT)
#define E10_X()			_opcode(E10_X)
#define E_X()			_opcode(E_X)
#define INV()			_opcode(INV)

// Can these be handled automatically?
#define ADD_DIR100(longreg)	_oplongreg(ADD_DIR100,longreg)
#define SUB_DIR100(longreg)	_oplongreg(SUB_DIR100,longreg)
#define MULT_DIR100(longreg)	_oplongreg(MULT_DIR100,longreg)
#define DIV_DIR100(longreg)	_oplongreg(DIV_DIR100,longreg)
#define ST_DIR100(longreg)	_oplongreg(ST_DIR100,longreg)
#define RE_DIR100(longreg)	_oplongreg(RE_DIR100,longreg)
#define EXCHG_DIR100(longreg)	_oplongreg(EXCHG_DIR100,longreg)
#define ST_Y_DIR100(longreg)	_oplongreg(ST_Y_DIR100,longreg)
#define RE_Y_DIR100(longreg)	_oplongreg(RE_Y_DIR100,longreg)

#define ALPHA(cmd)	_opcode(WRITE_ALPHA) _opcode(cmd)

#define FCALL(label)	_bytecode(_subr_ ## label)

/* this should only be used internally? */
#define _SUBR(x,y)	_opcode(SUBR_ ## x ## _ ## y)
#define _ALPHA(code)	_opcode(WRITE_ALPHA) _bytecode(code)

// exist on 700?
//#define KTRACE_ON()	ALPHA(PRINT)
//#define KTRACE_OFF()	ALPHA(ALPHA)
//#define PTRACE_ON()	ALPHA(LOG_E_X)
//#define PTRACE_OFF()	ALPHA(E_X)
//#define LOAD_REGS()	ALPHA(RECALL)
//#define REC_REGS()	ALPHA(STORE)
//#define JUMP(reg)	INDIR(E(reg))

#define POW10(n)	_ALPHA(0x7 ## n)
#define POW_10(n)	_ALPHA(0x4 ## n)
#define SK_IF_YP()	_ALPHA(0x4a)
#define SK_IF_Y0()	_ALPHA(0x4b)
#define SK_IF_YM()	_ALPHA(0x5a)
#define SK_IF_YN0()	_ALPHA(0x5b)
#define SK_IF_XP()	_ALPHA(0x6a)
#define SK_IF_X0()	_ALPHA(0x6b)
#define SK_IF_XM()	_ALPHA(0x7a)
#define SK_IF_XN0()	_ALPHA(0x7b)
#define PAUSE()		_ALPHA(0x6f)
#define D180PI()	_ALPHA(0x5e)
#define DPI180()	_ALPHA(0x5f)

#endif /* __wpcc_wang700_h__ */
