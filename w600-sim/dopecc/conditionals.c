// microcode to test conditional jumps

#include "ucode.h"

/*
Display register 15-15 after filling digits with test results

000: KA = 0 + 0 ->[Zo,CC]; jump 014[:Zo]
001: KA = 0 + 15 + 1 ->[Zo,CC]; jump 018[CC:]
002: KA = 0 + 15 + 1 ->[Zo,CC,SC]; jump 003
003: 0 + 0 ->[Zo,CC]; jump 016[:SC]
004: S = 0 + 8 ->[Zo,CC]; jump 005
005: 0 + 0 ->[Zo,CC]; jump 019[S<3>:]
006: S = 0 + 4 ->[Zo,CC]; jump 007
007: 0 + 0 ->[Zo,CC]; jump 01c[:S<2>]
008: S = 0 + 2 ->[Zo,CC]; jump 009
009: 0 + 0 ->[Zo,CC]; jump 020[S<1>:]
00a: S = 0 + 1 ->[Zo,CC]; jump 00b
00b: 0 + 0 ->[Zo,CC]; jump 01e[:S<0>]
00c: 0 + 0 ->[Zo,CC]; jump 000
00d: 0 + 0 ->[Zo,CC]; jump 000
00e: V = V + 0 + 1 ->[Zo,CC]; jump 00f
00f: 0 + 0 ->[Zo,CC]; CA = mem(15,15,V), CB = rom(15,15,V); jump 010
010: U = U + 0 + 1 ->[Zo,CC]; jump 010[CC:]
011: U = U + 0 + 1 ->[Zo,CC]; jump 010[CC:]
012: T = T + 0 + 1 ->[Zo,CC]; jump 011[CC:]
013: 0 + 0 ->[Zo,CC]; jump 00e
014: CA = 0 + 0 ->[Zo,CC]; mem(15,15,1) = CA; jump 001
015: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,1) = CA; jump 001
016: CA = 0 + 0 ->[Zo,CC]; mem(15,15,3) = CA; jump 004
017: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,3) = CA; jump 004
018: CA = 0 + 0 ->[Zo,CC]; mem(15,15,2) = CA; jump 002
019: CA = 0 + 0 ->[Zo,CC]; mem(15,15,4) = CA; jump 006
01a: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,2) = CA; jump 002
01b: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,4) = CA; jump 006
01c: CA = 0 + 0 ->[Zo,CC]; mem(15,15,5) = CA; jump 008
01d: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,5) = CA; jump 008
01e: CA = 0 + 0 ->[Zo,CC]; mem(15,15,7) = CA; jump 00e
01f: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,7) = CA; jump 00e
020: CA = 0 + 0 ->[Zo,CC]; mem(15,15,6) = CA; jump 00a
021: 0 + 0 ->[Zo,CC]; jump 000
022: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,6) = CA; jump 00a
023: 0 + 0 ->[Zo,CC]; jump 000
024: 0 + 0 ->[Zo,CC]; jump 000
025: 0 + 0 ->[Zo,CC]; jump 000
026: 0 + 0 ->[Zo,CC]; jump 000
*/

ucword ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
// Zo test
[0x000]=UCODE(0,0,4,0,0,0,0,0,0,0,0x014,0,4),
// CC test
[0x001]=UCODE(0,1,4,1,0,0,0,15,0,0,0x018,5,0),
// SC test
[0x002]=UCODE(0,1,4,4,0,0,0,15,0,0,0x000,1,1),
[0x003]=UCODE(0,0,0,0,0,0,0,0,0,0,0x014,1,6),
// S<3> test
[0x004]=UCODE(0,1,0,0,0,0,0,8,15,0,0x004,0,1),
[0x005]=UCODE(0,0,0,0,0,0,0,0,0,0,0x018,3,1),
// S<2> test
[0x006]=UCODE(0,1,0,0,0,0,0,4,15,0,0x004,1,1),
[0x007]=UCODE(0,0,0,0,0,0,0,0,0,0,0x01c,0,3),
// S<1> test
[0x008]=UCODE(0,1,0,0,0,0,0,2,15,0,0x008,0,1),
[0x009]=UCODE(0,0,0,0,0,0,0,0,0,0,0x020,2,0),
// S<0> test
[0x00a]=UCODE(0,1,0,0,0,0,0,1,15,0,0x008,1,1),
[0x00b]=UCODE(0,0,0,0,0,0,0,0,0,0,0x01c,1,2),

[0x00e]=UCODE(3,0,3,1,1,0,0,0,0,0,0x00c,1,1), // refresh loop
[0x00f]=UCODE(0,0,0,0,0,0,5,15,0,0,0x010,0,0),
[0x010]=UCODE(2,0,2,1,1,0,0,0,0,0,0x010,5,0),
[0x011]=UCODE(2,0,2,1,1,0,0,0,0,0,0x010,5,0),
[0x012]=UCODE(1,0,1,1,1,0,0,0,0,0,0x010,5,1),
[0x013]=UCODE(0,0,0,0,0,0,0,0,0,0,0x00c,1,0), // loop back

// Zo results
[0x014]=UCODE(0,0,6,0,0,0,3,1,0,0,0x000,0,1),
[0x015]=UCODE(0,0,6,1,0,0,3,1,0,0,0x000,0,1),
// SC results
[0x016]=UCODE(0,0,6,0,0,0,3,3,0,0,0x004,0,0),
[0x017]=UCODE(0,0,6,1,0,0,3,3,0,0,0x004,0,0),
// CC results
[0x018]=UCODE(0,0,6,0,0,0,3,2,0,0,0x000,1,0),
[0x01a]=UCODE(0,0,6,1,0,0,3,2,0,0,0x000,1,0),
// S<3> results
[0x019]=UCODE(0,0,6,0,0,0,3,4,0,0,0x004,1,0),
[0x01b]=UCODE(0,0,6,1,0,0,3,4,0,0,0x004,1,0),
// S<2> results
[0x01c]=UCODE(0,0,6,0,0,0,3,5,0,0,0x008,0,0),
[0x01d]=UCODE(0,0,6,1,0,0,3,5,0,0,0x008,0,0),
// S<0> results
[0x01e]=UCODE(0,0,6,0,0,0,3,7,0,0,0x00c,1,0),
[0x01f]=UCODE(0,0,6,1,0,0,3,7,0,0,0x00c,1,0),
// S<1> results
[0x020]=UCODE(0,0,6,0,0,0,3,6,0,0,0x008,1,0),
[0x022]=UCODE(0,0,6,1,0,0,3,6,0,0,0x008,1,0),

};
