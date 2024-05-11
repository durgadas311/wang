// microcode to update display when key pressed

#include "ucode.h"

/*
Display register 15-15 after filling digits with seven "1".

000: CA = 0 + 0 + 1 ->[Zo,CC]; mem(15,15,1) = CA; jump 001
001: 0 + 0 ->[Zo,CC]; mem(15,15,2) = CA; jump 002
002: 0 + 0 ->[Zo,CC]; mem(15,15,3) = CA; jump 003
003: 0 + 0 ->[Zo,CC]; mem(15,15,4) = CA; jump 004
004: 0 + 0 ->[Zo,CC]; mem(15,15,5) = CA; jump 005
005: 0 + 0 ->[Zo,CC]; mem(15,15,6) = CA; jump 006
006: 0 + 0 ->[Zo,CC]; mem(15,15,7) = CA; jump 00a
// display refresh
00a: V = V + 0 + 1 ->[Zo,CC]; jump 00b
00b: 0 + 0 ->[Zo,CC]; CA = mem(15,15,V), CB = rom(15,15,V); jump 00c
00c: U = U + 0 + 1 ->[Zo,CC]; jump 00c[CC:]
00d: U = U + 0 + 1 ->[Zo,CC]; jump 00c[CC:]
00e: T = T + 0 + 1 ->[Zo,CC]; jump 00d[CC:]
00f: 0 + 0 ->[Zo,CC]; jump 00a

*/

u64 ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,6,1,0,0,3,1,0,0,0x000,0,1),
[0x001]=UCODE(0,0,0,0,0,0,3,2,0,0,0x000,1,0),
[0x002]=UCODE(0,0,0,0,0,0,3,3,0,0,0x000,1,1),
[0x003]=UCODE(0,0,0,0,0,0,3,4,0,0,0x004,0,0),
[0x004]=UCODE(0,0,0,0,0,0,3,5,0,0,0x004,0,1),
[0x005]=UCODE(0,0,0,0,0,0,3,6,0,0,0x004,1,0),
[0x006]=UCODE(0,0,0,0,0,0,3,7,0,0,0x008,1,0),

[0x00a]=UCODE(3,0,3,1,1,0,0,0,0,0,0x008,1,1), // refresh loop
[0x00b]=UCODE(0,0,0,0,0,0,5,15,0,0,0x00c,0,0),
[0x00c]=UCODE(2,0,2,1,1,0,0,0,0,0,0x00c,5,0),
[0x00d]=UCODE(2,0,2,1,1,0,0,0,0,0,0x00c,5,0),
[0x00e]=UCODE(1,0,1,1,1,0,0,0,0,0,0x00c,5,1),
[0x00f]=UCODE(0,0,0,0,0,0,0,0,0,0,0x008,1,0), // loop back
};
