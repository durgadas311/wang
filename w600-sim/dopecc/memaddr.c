// cycle through all memory addresses (T,U,V separately).

typedef unsigned long u64;

#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |	\
	(mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |	\
	((jad >> 2) << 8) | (jh << 5) | (jl << 2)

/*

000: 0 + 0 ->[Zo,CC]; RESET; jump 001
001: 0 + 0 ->[Zo,CC]; CA = mem(T,U,V), CB = rom(T,U,V); jump 002
002: T = T + 0 + 1 ->[Zo,CC]; jump 003
003: U = U + 0 + 1 ->[Zo,CC]; jump 004
004: V = V + 0 + 1 ->[Zo,CC]; jump 001

*/

u64 ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,0,0,0,0,0,0,9,0,0x000,0,1),
[0x001]=UCODE(0,0,0,0,0,0,4,0,0,0,0x000,1,0), // loop here
[0x002]=UCODE(1,0,1,1,1,0,0,0,0,0,0x000,1,1),
[0x003]=UCODE(2,0,2,1,1,0,0,0,0,0,0x004,0,0),
[0x004]=UCODE(3,0,3,1,1,0,0,0,0,0,0x000,0,1), // loop back

};
