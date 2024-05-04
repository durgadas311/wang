// microcode to perform simple memory test. Results displayed
// as zero, 3-digit addr of last good location, zero, pass number.
// A pass is started by pressing PRIME.

typedef unsigned long u64;

#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl)	\
	((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |	\
	((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |	\
	(mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |	\
	((jad >> 2) << 8) | (jh << 5) | (jl << 2)

/*
Use register 15-15 for results.

000: 0 + 0 ->[Zo,CC]; RESET; jump 02c

// set test results in 15-15...
bad:
001: CA = 0 + 0 - 1 ->[Zo,CC]; mem(15,15,1) = CA; jump 002 // "_"
002: CA = T + 0 ->[Zo,CC]; mem(15,15,2) = CA; jump 003 // adr hi
003: CA = U + 0 ->[Zo,CC]; mem(15,15,3) = CA; jump 004 // adr mid
004: CA = V + 0 ->[Zo,CC]; mem(15,15,4) = CA; jump 005 // adr lo
005: CA = 0 + 0 - 1 ->[Zo,CC]; mem(15,15,5) = CA; jump 006 // "_"
006: CA = KB + 0 ->[Zo,CC]; mem(15,15,6) = CA; jump 007 // seed
007: CA = 0 + 0 - 1 ->[Zo,CC]; mem(15,15,7) = CA; jump 008 // "_"
008: CA = S + 0 ->[Zo,CC]; mem(15,15,8) = CA; jump 009 // syndrome
009: CA = 0 + 0 - 1 ->[Zo,CC]; mem(15,15,9) = CA; jump 00a // "_"
// display refresh code... until PRIME
00a: V = V + 0 + 1 ->[Zo,CC]; jump 00b
00b: 0 + 0 ->[Zo,CC]; CA = mem(15,15,V), CB = rom(15,15,V); jump 00c
// continue display refresh
00c: U = U + 0 + 1 ->[Zo,CC,SC]; jump 00c[CC:]
00d: U = U + 0 + 1 ->[Zo,CC,SC]; jump 00c[CC:]
00e: T = T + 0 + 1 ->[Zo,CC,SC]; jump 00d[CC:]
00f: 0 + 0 ->[Zo,CC]; jump 00a
//
010: KA = KB + 0 ->[Zo,CC]; call 026 // zero
011: 0 + 0; jump 012
012: CA = KA + 0; mem(T,U,V) = CA; call 020 // subr <---+
013: 0 + 0; jump 014[:SC]                        |
014: 0 + 0; jump 012 ----------------------------+
015: KA = KB + 0; jump 016 // verify using seed from KB
016: 0 + 0; call 026 // zero
017: 0 + 0; CA = mem(T,U,V); jump 018 <----------+
018: S = KA ^ CA ->[Zo,CC]; jump 01a[:Zo]        |
019:                                             |
01a: 0 + 0; jump 001                             |
01b: 0 + 0; jump 01c                             |
01c: 0 + 0; call 020 // subr // increment        |
01d: jump 01e[:SC]                               |
01e: jump 017 -----------------------------------+
01f: 0 + 0; jump 001	// done with pass

subr: increments TUV, and (bcd) KA. SC if TUV wrapped.
020: V = V + 0 + 1 ->[Zo,CC,SC]; jump 021
021: U = U + 0 + SC ->[Zo,CC,SC]; jump 022
022: T = T + 0 + SC ->[Zo,CC,SC]; jump 023
023: T + 8 ->[Zo,CC,SC]; jump 024 // 2Kx4 RAM size, end => SC
subr2: BCD-increment KA
024: KA = KA + 0 + 1 ->[Zo,CC]; jump 025
025: KA + 6 ->[Zo,CC]; jump 028[CC:]
zero:
026: T = 0 + 0 ->[Zo,CC]; jump 027	*
027: U = 0 + 0 ->[Zo,CC]; jump 029	*
028: 0 + 0; return
029: V = 0 + 0 ->[Zo,CC]; return	*
02a: KA = 0 + 0; return
02b:
02c: KA = KB + 0; call 024 // subr2 // next seed #
02d: KB = KA + 0; jump 010
*/

u64 ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,0,0,0,0,0,0,9,0,0x02c,0,0),

[0x001]=UCODE(0,0,6,1,0,1,3,1,0,0,0x000,1,0), // blank
[0x002]=UCODE(1,0,6,0,1,0,3,2,0,0,0x000,1,1),
[0x003]=UCODE(2,0,6,0,1,0,3,3,0,0,0x004,0,0),
[0x004]=UCODE(3,0,6,0,1,0,3,4,0,0,0x004,0,1),
[0x005]=UCODE(0,0,6,1,0,1,3,5,0,0,0x004,1,0), // blank
[0x006]=UCODE(5,0,6,0,1,0,3,6,0,0,0x004,1,1),
[0x007]=UCODE(0,0,6,1,0,1,3,7,0,0,0x008,0,0), // blank
[0x008]=UCODE(0,0,6,0,1,0,3,8,0,0,0x008,0,1),
[0x009]=UCODE(0,0,6,1,0,1,3,9,0,0,0x008,1,0), // blank
[0x00a]=UCODE(3,0,3,1,1,0,0,0,0,0,0x008,1,1), // refresh loop
[0x00b]=UCODE(0,0,0,0,0,0,5,15,0,0,0x00c,0,0),
[0x00c]=UCODE(2,0,2,1,1,0,0,0,0,0,0x00c,5,0),
[0x00d]=UCODE(2,0,2,1,1,0,0,0,0,0,0x00c,5,0),
[0x00e]=UCODE(1,0,1,1,1,0,0,0,0,0,0x00c,5,1),
[0x00f]=UCODE(0,0,0,0,0,0,0,0,0,0,0x008,1,0), // loop back

//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x010]=UCODE(5,0,4,0,1,0,0,0,0,1,0x024,1,0),
[0x011]=UCODE(0,0,0,0,0,0,0,0,0,0,0x010,1,0),
[0x012]=UCODE(4,0,6,0,1,0,1,0,0,1,0x020,0,0),
[0x013]=UCODE(0,0,0,0,0,0,0,0,0,0,0x014,0,6),
[0x014]=UCODE(0,0,0,0,0,0,0,0,0,0,0x010,1,0),
[0x015]=UCODE(5,0,4,0,1,0,0,0,0,0,0x014,1,0),
[0x016]=UCODE(0,0,0,0,0,0,0,0,0,1,0x024,1,0),
[0x017]=UCODE(0,0,0,0,0,0,4,0,0,0,0x018,0,0), // loop
[0x018]=UCODE(4,6,0,6,1,1,0,0,15,0,0x018,1,4),
[0x019]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,0),
[0x01a]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,1),
[0x01b]=UCODE(0,0,0,0,0,0,0,0,0,0,0x01c,0,0),
[0x01c]=UCODE(0,0,0,0,0,0,0,0,0,1,0x020,0,0),
[0x01d]=UCODE(0,0,0,0,0,0,0,0,0,0,0x01c,1,6),
[0x01e]=UCODE(0,0,0,0,0,0,0,0,0,0,0x014,1,1), // loop back
[0x01f]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,1),

//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x020]=UCODE(3,0,3,4,1,0,0,0,0,0,0x020,0,1),
[0x021]=UCODE(2,0,2,3,1,0,0,0,0,0,0x020,1,0),
[0x022]=UCODE(1,0,1,3,1,0,0,0,0,0,0x020,1,1),
[0x023]=UCODE(1,1,0,2,1,0,0,8,0,0,0x024,0,0),
[0x024]=UCODE(4,0,4,1,1,0,0,0,0,0,0x024,0,1),
[0x025]=UCODE(4,1,0,0,1,0,0,6,0,0,0x028,5,0), // skip...
// zero
[0x026]=UCODE(0,0,1,0,0,0,0,0,0,0,0x024,1,1),
[0x027]=UCODE(0,0,2,0,0,0,0,0,0,0,0x028,0,1),
// ...skip
[0x028]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,7),
[0x029]=UCODE(0,0,3,0,0,0,0,0,0,0,0x000,0,7),
[0x02a]=UCODE(0,0,4,0,0,0,0,0,0,0,0x000,0,7),

[0x02b]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,0),
// start test:
[0x02c]=UCODE(5,0,4,0,1,0,0,0,0,1,0x024,0,0),
[0x02d]=UCODE(4,0,5,0,1,0,0,0,0,0,0x010,0,0),

[0x02e]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,0),

};
