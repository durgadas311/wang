// program to refresh display using register 15-15 X/Y
#include "ucode.h"

/*
Basic refresh loop
*/

ucword ucode[2048] = {
//                  a       m
//            a b z o a b b o k  s       j j
//            i i o p c c d p k  t   jad h l
[0x000]=UCODE(0,1,4,6,0,1,0,8, 0,0,0x010,0,0),  // PRIME
[0x001]=UCODE(0,1,4,6,0,1,0,8, 1,0,0x010,0,0),  // VERIF PROG
[0x002]=UCODE(0,1,4,6,0,1,0,8, 2,0,0x010,0,0),  // SET P.C.
[0x003]=UCODE(0,1,4,6,0,1,0,8, 3,0,0x010,0,0),  // REC PROG
[0x004]=UCODE(0,1,4,6,0,1,0,8, 4,0,0x010,0,0),  // S.M.
[0x005]=UCODE(0,1,4,6,0,1,0,8, 5,0,0x010,0,0),  // B.S.
[0x006]=UCODE(0,1,4,6,0,1,0,8, 6,0,0x010,0,0),  // INS
[0x007]=UCODE(0,1,4,6,0,1,0,8, 7,0,0x010,0,0),  // DEL

//                  a       m
//            a b z o a b b o k s       j j
//            i i o p c c d p k t   jad h l
[0x008]=UCODE(1,0,1,6,1,0,0,4,15,0,0x008,0,1),
[0x009]=UCODE(7,0,7,6,1,0,0,1, 0,0,0x008,1,0),
[0x00a]=UCODE(2,0,2,4,1,0,0,8, 0,0,0x008,1,1), // U=U+1 [SC]
[0x00b]=UCODE(1,0,1,3,1,0,0,8, 0,0,0x00c,5,0), // T=T+SC; did (T,U)++ carry?
[0x00c]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x008,1,0), // no carry, loop back
[0x00d]=UCODE(0,1,1,6,0,1,0,8, 7,0,0x020,6,0),
[0x00e]=UCODE(3,0,3,0,1,2,0,8, 0,0,0x00c,5,1), // carry: did V-- borrow?
[0x00f]=UCODE(0,1,1,6,0,1,0,8, 7,0,0x008,0,0), // no borrow; T=7; loop back 008

// init
[0x010]=UCODE(0,0,0,1,0,0,0,8, 0,0,0x010,0,1), // S = 1 for FLDX/FLDY
[0x011]=UCODE(0,0,3,6,0,2,0,8, 0,0,0x00c,1,1), // V = 15

};
