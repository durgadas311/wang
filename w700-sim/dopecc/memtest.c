// program to test memory
#include "ucode.h"

#ifndef MEMSIZE
#define MEMSIZE	2048
#endif

/*
000: // PRIME:
001: // V P:
002: // SET PC:
003: // R P:
004: // S M:
005: // B S:
006: // INS:
007: // DEL:

*/

// 720 (2K): X=8, 700 (1K): X=12
#define X (16 - (MEMSIZE >> 8))

// KA = (BCD) current pass seed
// KB = (BCD) current nibble pattern

ucword ucode[2048] = {
//                  a       m
//            a b z o a b b o k  s       j j
//            i i o p c c d p k  t   jad h l
[0x000]=UCODE(0,0,4,6,0,0,0,8, 0,9,0x014,1,0),	// PRIME
[0x001]=UCODE(1,0,1,6,1,0,0,8, 1,9,0x014,1,0),	// VERIF PROG
[0x002]=UCODE(1,0,1,6,1,0,0,8, 2,9,0x014,1,0),	// SET P.C.
[0x003]=UCODE(1,0,1,6,1,0,0,8, 3,9,0x014,1,0),	// REC PROG
[0x004]=UCODE(1,0,1,6,1,0,0,8, 4,9,0x014,1,0),	// S.M.
[0x005]=UCODE(1,0,1,6,1,0,0,8, 5,9,0x014,1,0),	// B.S.
[0x006]=UCODE(1,0,1,6,1,0,0,8, 6,9,0x014,1,0),	// INS
[0x007]=UCODE(1,0,1,6,1,0,0,8, 7,9,0x014,1,0),	// DEL

// one refresh cycle, then another memory pass
[0x008]=UCODE(1,0,1,6,1,0,0,4,15,0,0x008,0,1),
[0x009]=UCODE(7,0,7,6,1,0,0,1, 0,0,0x008,1,0),
[0x00a]=UCODE(2,0,2,4,1,0,0,8, 0,0,0x008,1,1), // U=U+1 [SC]
[0x00b]=UCODE(1,0,1,3,1,0,0,8, 0,0,0x00c,5,0), // T=T+SC; did (T,U)++ carry?
[0x00c]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x008,1,0), // no carry, loop back
[0x00d]=UCODE(0,1,1,6,0,1,0,8, 7,0,0x014,0,3), // borrow: go back to test [:S2]
[0x00e]=UCODE(3,0,3,0,1,2,0,8, 0,0,0x00c,5,1), // carry: did V-- borrow?
[0x00f]=UCODE(0,1,1,6,0,1,0,8, 7,0,0x008,0,0), // no borrow; T=7; loop back 008 (entry)

// 010: entry to refresh cycle from success
[0x010]=UCODE(0,0,0,1,0,0,0,8, 0,0,0x010,0,1), // S = 1 for FLDX/FLDY
[0x011]=UCODE(0,0,3,6,0,2,0,8, 0,0,0x00c,1,1), // V = 15
// 012: entry to refresh cycle from error
[0x012]=UCODE(0,1,0,0,0,1,0,8, 5,0,0x010,0,1), // S = 5 for FLDX/FLDY/error

//                  a       m
//            a b z o a b b o k  s       j j
//            i i o p c c d p k  t   jad h l
// PRIME - init all and start test
[0x013]=UCODE(0,0,4,6,0,0,0,8, 0,0,0x014,1,0), // KA=0; goto start pass

// break in refresh loop, continue or next test pass
[0x014]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x014,1,0),	// S2=0: goto next pass
[0x015]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x00c,1,1),	// S2=1: stay in refresh

// [0x000]=UCODE(0,0,0,0,0,0,0,8, 0,0,0x000,0,0),
//                  a       m
//            a b z o a b b o k  s       j j
//            i i o p c c d p k  t   jad h l
// 016: start test, write pattern to memory
[0x016]=UCODE(4,0,5,6,1,0,0,8, 0,0,0x014,1,1), // KB=KA
[0x017]=UCODE(0,0,1,6,0,0,0,8, 0,0,0x018,0,0), // T=0
[0x018]=UCODE(0,0,2,6,0,0,0,8, 0,0,0x018,0,1), // U=0
[0x019]=UCODE(0,0,3,6,0,0,0,8, 0,0,0x018,1,1), // V=0
[0x01a]=UCODE(4,0,5,6,1,0,0,8, 0,0,0x020,1,1), //// done: KB=KA
[0x01b]=UCODE(1,0,1,6,1,0,0,3, 0,0,0x01c,0,0), // read T,U,V <---------------+
[0x01c]=UCODE(5,0,5,1,1,0,1,1, 0,0,0x01c,0,1), // write RB=alu; KB=BCD(KB+1) |
[0x01d]=UCODE(1,0,1,6,1,0,0,3, 0,0,0x01c,1,0), // read T,U,V                 |
[0x01e]=UCODE(5,0,5,1,1,0,1,0, 0,0,0x01c,1,1), // write RA=alu; KB=BCD(KB+1) |
[0x01f]=UCODE(3,0,3,4,1,0,0,8, 0,0,0x020,0,0), // V=V+1[SC]                  |
[0x020]=UCODE(2,0,2,3,1,0,0,8, 0,0,0x020,0,1), // U=U+SC[SC]                 |
[0x021]=UCODE(1,0,1,3,1,0,0,8, 0,0,0x020,1,0), // T=T+SC[SC]                 |
[0x022]=UCODE(1,1,6,5,1,1,0,8, X,0,0x018,1,4), // CA=T&(X); jump [:Zo] ------+
// now read and compare, by definition U,V are 0
[0x023]=UCODE(0,0,1,6,0,0,0,8, 0,0,0x024,0,1), // T=0
// TODO: do not preserve contents?
[0x024]=UCODE(4,0,4,1,1,0,1,8, 0,0,0x010,0,0), // pass: KA=BCD(KA+1); jump 
[0x025]=UCODE(1,0,1,6,1,0,0,2, 0,0,0x024,1,0), // read T,U,V <---------------+
[0x026]=UCODE(7,0,7,6,1,0,0,1, 0,0,0x024,1,1), // write RB=alu; CB=RB        |
[0x027]=UCODE(5,0,5,1,1,0,1,8, 0,0,0x028,0,0), // KB=BCD(KB+1)               |
[0x028]=UCODE(7,5,7,6,1,1,0,8, 0,0,0x028,0,1), // CB=CB^KB;                  |
[0x029]=UCODE(5,0,5,1,1,0,1,8, 0,0,0x028,1,0), // KB=BCD(KB+1)               |
[0x02a]=UCODE(6,5,6,6,1,1,0,8, 0,0,0x028,1,1), // CA=CA^KB;                  |
[0x02b]=UCODE(6,0,6,6,1,0,0,8, 0,0,0x02c,0,4), // CA=CA; jump [:Zo]
[0x02c]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x030,1,1), // error
[0x02d]=UCODE(7,0,7,6,1,0,0,8, 0,0,0x02c,1,4), // OK... CB=CB; jump [:Zo]
[0x02e]=UCODE(1,0,1,6,1,0,0,8, 0,0,0x030,1,1), // error
// OK...
[0x02f]=UCODE(3,0,3,4,1,0,0,8, 0,0,0x030,0,0), // V=V+1[SC]                  |
[0x030]=UCODE(2,0,2,3,1,0,0,8, 0,0,0x030,0,1), // U=U+SC[SC]                 |
[0x031]=UCODE(1,0,1,3,1,0,0,8, 0,0,0x030,1,0), // T=T+SC[SC]                 |
[0x032]=UCODE(1,1,6,5,1,1,0,8, X,0,0x024,0,4), // CA=T&(X); jump [:Zo] ------+

// error - CA,CB is syndrome, T,U,V is address
// TODO: preserve KA so same test can be run again...
//                  a       m
//            a b z o a b b o k  s       j j
//            i i o p c c d p k  t   jad h l
// store syndrome first, free up CA,CB. in X display last digits
[0x033]=UCODE(3,0,0,6,1,0,0,8, 0,12,0x034,0,0), // S=V; OFL on
[0x034]=UCODE(0,1,3,6,0,1,0,8,12,0,0x034,0,1), // V=12
[0x035]=UCODE(3,0,3,0,1,2,0,5,15,0,0x034,1,0), // read 15,15,V--
[0x036]=UCODE(7,0,7,2,1,0,1,1, 0,0,0x034,1,1), // write BCD(CB+0)[SC]
[0x037]=UCODE(3,0,3,0,1,2,0,5,15,0,0x038,0,0), // read 15,15,V--
[0x038]=UCODE(0,0,7,3,0,0,0,1, 0,0,0x038,0,1), // write 0+0+SC
[0x039]=UCODE(3,0,3,0,1,2,0,5,15,0,0x038,1,0), // read 15,15,V--
[0x03a]=UCODE(0,1,7,0,0,1,0,1,15,0,0x038,1,1), // write 15
[0x03b]=UCODE(3,0,3,0,1,2,0,5,15,0,0x03c,0,0), // read 15,15,V--
[0x03c]=UCODE(6,0,6,2,1,0,1,1, 0,0,0x03c,0,1), // write BCD(CA+0)[SC]
[0x03d]=UCODE(3,0,3,0,1,2,0,5,15,0,0x03c,1,0), // read 15,15,V--
[0x03e]=UCODE(0,0,7,3,0,0,0,1, 0,0,0x03c,1,1), // write 0+0+SC
[0x03f]=UCODE(3,0,3,0,1,2,0,5,15,0,0x040,0,0), // read 15,15,V--
[0x040]=UCODE(0,1,7,0,0,1,0,1,15,0,0x040,0,1), // write 15
// store addr T,U,S in Y display last digits as BCD
[0x041]=UCODE(0,1,3,6,0,1,0,8,12,0,0x040,1,0), // V=12
[0x042]=UCODE(3,0,3,0,1,2,0,5,15,0,0x040,1,1), // read 15,15,V--
[0x043]=UCODE(0,0,7,2,1,0,1,0, 0,0,0x044,0,0), // write BCD(S+0)[SC]
[0x044]=UCODE(3,0,3,0,1,2,0,5,15,0,0x044,0,1), // read 15,15,V--
[0x045]=UCODE(0,0,7,3,0,0,0,0, 0,0,0x044,1,0), // write 0+0+SC
[0x046]=UCODE(3,0,3,0,1,2,0,5,15,0,0x044,1,1), // read 15,15,V--
[0x047]=UCODE(0,1,7,0,0,1,0,0,15,0,0x048,0,0), // write 15
[0x048]=UCODE(3,0,3,0,1,2,0,5,15,0,0x048,0,1), // read 15,15,V--
[0x049]=UCODE(2,0,7,2,1,0,1,0, 0,0,0x048,1,0), // write BCD(U+0)[SC]
[0x04a]=UCODE(3,0,3,0,1,2,0,5,15,0,0x048,1,1), // read 15,15,V--
[0x04b]=UCODE(0,0,7,3,0,0,0,0, 0,0,0x04c,0,0), // write 0+0+SC
[0x04c]=UCODE(3,0,3,0,1,2,0,5,15,0,0x04c,0,1), // read 15,15,V--
[0x04d]=UCODE(0,1,7,0,0,1,0,0,15,0,0x04c,1,0), // write 15
[0x04e]=UCODE(3,0,3,0,1,2,0,5,15,0,0x04c,1,1), // read 15,15,V--
[0x04f]=UCODE(1,0,7,2,1,0,1,0, 0,0,0x050,0,0), // write BCD(T+0)[SC]
[0x050]=UCODE(3,0,3,0,1,2,0,5,15,0,0x050,0,1), // read 15,15,V--
[0x051]=UCODE(0,0,7,3,0,0,0,0, 0,0,0x050,1,0), // write 0+0+SC
[0x052]=UCODE(3,0,3,0,1,2,0,5,15,0,0x050,1,1), // read 15,15,V--
[0x053]=UCODE(0,1,7,0,0,1,0,0,15,0,0x010,1,0), // write 15; jump 012
};
