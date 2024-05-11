#ifndef __UCODE_H
#define __UCODE_H

typedef unsigned long u64;

#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |	\
	(mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |	\
	((jad >> 2) << 8) | (jh << 5) | (jl << 2)

#endif /* __UCODE_H */
