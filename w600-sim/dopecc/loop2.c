// program to refresh display using register 15-12

typedef unsigned long u64;

#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |	\
	(mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |	\
	((jad >> 2) << 8) | (jh << 5) | (jl << 2)

/*
000: V = 0 - 0 - 1 ->[Zo,CC]; RESET; jump 007
001: V = V + 0 + 1 ->[Zo,CC]; jump 002
002: 0 + 0 ->[Zo,CC]; jump 003
003: 0 + 0 ->[Zo,CC]; CA = mem(15,12,V), CB = rom(15,12,V); jump 004
004: U = U + 0 + 1 ->[Zo,CC,SC]; jump 004[CC:]
005: U = U + 0 + 1 ->[Zo,CC,SC]; jump 004[CC:]
006: T = T + 0 + 1 ->[Zo,CC,SC]; jump 005[CC:]
007: 0 + 0 ->[Zo,CC]; jump 001
*/

u64 ucode[2048] = {
UCODE(0,0,3,1,0,1,0,0,9,0,0x004,1,1),
UCODE(3,0,3,1,1,0,0,0,0,0,0x000,1,0),
UCODE(0,0,0,0,0,0,0,0,0,0,0x000,1,1),
UCODE(0,0,0,0,0,0,5,12,0,0,0x004,0,0),
UCODE(2,0,2,4,1,0,0,0,0,0,0x004,5,0),
UCODE(2,0,2,4,1,0,0,0,0,0,0x004,5,0),
UCODE(1,0,1,4,1,0,0,0,0,0,0x004,5,1),
UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,1),
};
