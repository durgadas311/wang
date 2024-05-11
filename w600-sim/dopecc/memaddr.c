// cycle through all memory addresses (T,U,V separately).
#include "ucode.h"

/*

000: 0 + 0 ->[Zo,CC]; RESET; jump 001
001: 0 + 0 ->[Zo,CC]; CA = mem(T,U,V), CB = rom(T,U,V); jump 002
002: T = T + 0 + 1 ->[Zo,CC]; jump 003
003: U = U + 0 + 1 ->[Zo,CC]; jump 004
004: V = V + 0 + 1 ->[Zo,CC]; jump 001

*/

ucword ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,0,0,0,0,0,0,9,0,0x000,0,1),
[0x001]=UCODE(0,0,0,0,0,0,4,0,0,0,0x000,1,0), // loop here
[0x002]=UCODE(1,0,1,1,1,0,0,0,0,0,0x000,1,1),
[0x003]=UCODE(2,0,2,1,1,0,0,0,0,0,0x004,0,0),
[0x004]=UCODE(3,0,3,1,1,0,0,0,0,0,0x000,0,1), // loop back

};
