// microcode to update display when key pressed
typedef unsigned long u64;

#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |	\
	(mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |	\
	((jad >> 2) << 8) | (jh << 5) | (jl << 2)

/*
Display register 15-15. If key pressed, put code in first 2 digits.

000: V = 0 - 0 - 1 ->[Zo,CC]; RESET; jump 007
001: V = V + 0 + 1 ->[Zo,CC]; jump 002
002: 0 + 0 ->[Zo,CC]; jump 008[KBD:]
003: 0 + 0 ->[Zo,CC]; CA = mem(15,15,V), CB = rom(15,15,V); jump 004
004: U = U + 0 + 1 ->[Zo,CC,SC]; jump 004[CC:]
005: U = U + 0 + 1 ->[Zo,CC,SC]; jump 004[CC:]
006: T = T + 0 + 1 ->[Zo,CC,SC]; jump 005[CC:]
007: 0 + 0 ->[Zo,CC]; jump 001
// no key pressed
008: CA = 0 + D1 ->[Zo,CC]; mem(15,15,4) = CA; jump 009
009: CA = 0 + D2 ->[Zo,CC]; mem(15,15,5) = CA; jump 003
// key pressed
00a: CA = KA + 0 ->[Zo,CC]; mem(15,15,1) = CA; jump 00b
00b: CA = KB + 0 ->[Zo,CC]; mem(15,15,2) = CA; jump 00c
00c: 0 + 0 ->[Zo,CC]; CA = mem(15,15,3), CB = rom(15,15,3); jump 00d
00d: CA = CA + 0 + 1 ->[Zo,CC,SC]; mem(15,15,3) = CA; jump 003
// end
00e: 0 + 0 ->[Zo,CC]; jump 000
00f: 0 + 0 ->[Zo,CC]; jump 000
*/

u64 ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,3,1,0,1,0,0,9,0,0x004,1,1),
[0x001]=UCODE(3,0,3,1,1,0,0,0,0,0,0x000,1,0),
[0x002]=UCODE(0,0,0,0,0,0,0,0,0,0,0x008,6,0),
[0x003]=UCODE(0,0,0,0,0,0,5,15,0,0,0x004,0,0),
[0x004]=UCODE(2,0,2,4,1,0,0,0,0,0,0x004,5,0),
[0x005]=UCODE(2,0,2,4,1,0,0,0,0,0,0x004,5,0),
[0x006]=UCODE(1,0,1,4,1,0,0,0,0,0,0x004,5,1),
[0x007]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,1),
[0x008]=UCODE(0,2,6,0,0,0,3,4,0,0,0x008,0,1),
[0x009]=UCODE(0,3,6,0,0,0,3,5,0,0,0x000,1,1),
[0x00a]=UCODE(4,0,6,0,1,0,3,1,0,0,0x008,1,1),
[0x00b]=UCODE(5,0,6,0,1,0,3,2,0,0,0x00c,0,0),
[0x00c]=UCODE(0,0,0,0,0,0,6,3,0,0,0x00c,0,1),
[0x00d]=UCODE(6,0,6,4,1,0,3,3,0,0,0x000,1,1),
[0x00e]=0,
[0x00f]=0,
};
