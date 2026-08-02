#ifndef __UCODE_H
#define __UCODE_H

typedef unsigned long u64;

/* Template for using the macros:
//                  a       m
//            a b z o a b b o k s       j j
//            i i o p c c d p k t   jad h l
[0x000]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,0),
*/

/* these may vary for different output formats */

typedef u64 ucword;

/* This is for the Wang 700 simulator binary ROM format */
#define UCODE(ai,bi,zo,aop,ac,bc,bd,mop,kk,st,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 29) |	((u64)bd << 28) | \
	(mop << 24) | (kk << 20) | (st << 16) | \
	((jad >> 2) << 7) | (jh << 4) | (jl << 1)

/* end of variable part */

#endif /* __UCODE_H */
