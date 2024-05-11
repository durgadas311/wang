#include "ucode.h"

/* jad is full 11-bit addr, low 2 bits ignored */
ucword ucode[2048] = {
//                  a     m     s
//            a b z o a b o k s u       j j
//            i i o p c c p k t b   jad h l
[0x000]=UCODE(0,0,0,0,0,0,0,0,0,0,0x000,0,0),
};
