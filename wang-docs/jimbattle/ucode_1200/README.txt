11/24/09 --

I really should be working, but the machine may be disappearing shortly,
so I spent the evening on this project.

The ROMs from the Wang 1220 were carefully removed and inserted on a
BASIC STAMP II experiment board (INEX-1000).  The program dump.bs2
located in this directory was used to capture the ROM contents.  Comments
in the program indicate ROM pinout.

Each ROM was read twice and compared (there were never any mismatches).

The file names match the part location numbers on the schematic 6293.pdf

The Wang 700 has a very similar microword, but there are a few differences.
See Rick Bensene's great site:

    http://www.oldcalculatormuseum.com/t-w700microcode.html

Actually, the 1200 really seems to be more closely related to the
Wang 500 -- the ucode fields are identical, although the meanings of
the field encodings might be different.

==============================================================================
			      6152 Schematic notes
==============================================================================

L5 and L12 (both 7495s) are clocked on the falling edge of CK1, capturing
MAV[3:0] and MAU[3:0], respectively.  These drive the 8b RAM address to the
two 256x4 RAMs.

Data is read and written from these RAMs through L6 and L13.  L6 is known as "CA"
in the microcode, and L13 is "CB".

The input "MC" is "mode control", and comes from the 6192 board.
The mode control input on the 7495 has this behavior:

    mc=0: shifts QA toward QD when clk1 falls
    mc=1: parallel load when clk2 falls

These controls are driven on the 6152 like this:

    mc   = RS && (MOP==4 || MOP==5 || MOP==6)
    clk1 = CK1234
    clk2 = CK5

CA and CB will load a byte from RAM on the falling edge of CK5
when (MOP==4 || MOP==5 || MOP==6).

CA will load the output of the ALU when ZO=6.
CB will load the output of the ALU when ZO=7.

!(R/M/W) comes from 6195.

==============================================================================
			      6174 Schematic notes
==============================================================================
The upper left contains the clock/timing logic.  An 8 MHz crystal is
immediately divided by two by 1/2 of L9, producing a 4MHz clock, known as MK0.

MK0 goes into a ring counter formed by the other half of L9, L8, and L18.
This divides by 10 with a phase pattern that looks like this:

    L9.12, L8.9, L8.12, L18.9, L18.12
          -- start --
    0      0     0      0      0
    1      0     0      0      0
    1      1     0      0      0
    1      1     1      0      0
    1      1     1      1      0
    1      1     1      1      1
    0      1     1      1      1
    0      0     1      1      1
    0      0     0      1      1
    0      0     0      0      1
          -- repeat --
    0      0     0      0      0
    1      0     0      0      0
              etc.

L29 divides MK0 by two and combines with the ring counter outputs in a
bunch of NAND gates (L29, L17, L7, L6, L5, L4) to create different clock
phases.  The ring counter is clocked on the positive edge of MK0, while
L29 toggles on the negative edge of MK0.

Some of these signals persist for a full 250 ns (marked as 0 or 1), but
others are gated by MK0 to produce a ~125 ns pulse.

L29 toggles 1/2 clock period out of phase wrt the ring counter.  !CK6 clears
L29 in the 2nd phase of the ring counter state 00001 to guarantee proper phase
alignment.  Things gated by the L30.8 and L30.10 outputs pulse in the first
half of the clock period wrt the ring counter phase.  Things gated by L19.6
pulse during the second half of the clock period.

The phase is listed in the order as above.  p1 means pulses in 1st half of
phase; p2 means pulses in 2nd half of the phase.  Items are marked as "p" for
positive pulse, and "!p" for negative pulse.

    CUP = !tap2 && !tap5
    CL  = !tap1 && !tap5
    RS  = !tap3 && tap5
    ROS = !tap3 && tap4 && (L29.!Q9 && 1st_half_of_phase)
    ST  = tap1 && !tap2 && (L29.Q9 && 1st_half_of_phase)
    CK1 = tap2 && !tap3 && (L29.!Q9 && 1st_half_of_phase)
    CK2 = tap3 && !tap4 && (2nd_half_of_phase)
    CK3 = tap1 &&  tap5 && (L29.Q9 && 1st_half_of_phase)
    CK4 = tap2 && !tap1 && (2nd_half_of_phase)
    CK5 = tap5 && !tap4 && (L29.Q9 && 1st_half_of_phase)
    CK6 = tap5 && !tap4 && (2nd_half_of_phase)
    TP1 = tap1 && !tap3 && L29.!Q
    TP2 = tap3 && !tap4
    TP3 = tap1 &&  tap4 && L29.Q
    TP4 = !tap3
    !R/!W = tap5

           |========================================= one ucode cycle =========================================|
        __      ____      ____      ____      ____      ____      ____      ____      ____      ____      ____      ____
MK0       \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    
        ___ _________ _________ _________ _________ _________ _________ _________ _________ _________ _________ ________
phase   ___X__00000__X__10000__X__11000__X__11100__X__11110__X__11111__X__01111__X__00111__X__00011__X__00001__X__00000_
                 _________           _________           _________           _________           _________           ___
L29.9   ________/         \_________/         \_________/         \_________/         \_________/         \_________/   
        ____                     _______________________________________________________________________________
!CUP        \___________________/                                                                               \_______
             _________                                                                                           _______
CL      ____/         \_________________________________________________________________________________________/
             __________________________________________________________________________        _________________________
gate L16.8*                                                                            \______/
        ____                                                                                 ___________________
RS          \_______________________________________________________________________________/ \_________________\_______
                                                                                             ____
ROS     ____________________________________________________________________________________/ \__\______________________
        ______________      ____________________________________________________________________________________________
!ST                   \____/
                                 ____
CK1     ________________________/    \__________________________________________________________________________________
                                                ____
CK2     _______________________________________/    \___________________________________________________________________
                                                               ____
CK3     ______________________________________________________/    \____________________________________________________
                                                                              ____
CK4     _____________________________________________________________________/    \_____________________________________
                                 ____           ____           ____           ____
CK1234  ________________________/    \_________/    \_________/    \_________/    \_____________________________________
                                                                                                       ____
CK5     ______________________________________________________________________________________________/    \____________
        ___________________________________________________________________________________________________      _______
!CK6                                                                                                       \____/
                            _________
TP1     ___________________/         \__________________________________________________________________________________
                                           _________
TP2     __________________________________/         \___________________________________________________________________
                                                          _________
TP3     _________________________________________________/         \____________________________________________________
                                                                         _________
L15.2   ________________________________________________________________/         \_____________________________________
                                           _________________________________________________
TP234   __________________________________/                                                 \___________________________
        ____                                                   _________________________________________________
!(R/W)      \_________________________________________________/                                                 \_______
        ___ _________ _________ _________ _________ _________ _________ _________ _________ _________ _________ ________
phase   ___X__00000__X__10000__X__11000__X__11100__X__11110__X__11111__X__01111__X__00111__X__00011__X__00001__X__00000_
        __      ____      ____      ____      ____      ____      ____      ____      ____      ____      ____      ____
MK0       \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    \____/    

                                     1         2                                            2                           

1 = falling edge of CK1, MAU[3:0] and MAV[3:0] are clocked into the RAM address
    driver.
2 = rising edge of CK2, JAD is flopped and drives (script) jad when JL[2:0]=7
    normally, ROV=ROS, but ROV is suppressed if certain keys are pressed.
    when JL[2:0]<>7, then JAD is flopped by ROV and drives (script) jad.

* L16.8 was added to a later revision of the schematic and includes a dodgy cap
  and pair of inverters to delay the decode to shape the timing of RS and ROS.
  the diagram above shows two forms of RS and ROS; the version with the longer
  pulse high is from the earlier revision, and the short pulse is the later
  revision where L16.8 cuts off the pulses.

state  phase   -> !CUP, CL, RS, ROS, !ST, CK1234, CK1, CK2, CK3, CK4, CK5, !CK6, TP1, TP2, TP3, TP234, !R/!W
  0    00000.0 -> 0     1   0   0    1    0       0    0    0    0    0    1     0    0    0    0      0
  1    10000.1 -> 0     0   0   0    p1   0       0    0    0    0    0    1     p2   0    0    0      0
  2    11000.0 -> 1     0   0   0    1    p1      p1   0    0    0    0    1     p1   0    0    0      0
  3    11100.1 -> 1     0   0   0    1    p2      0    p2   0    0    0    1     0    1    0    1      0
  4    11110.0 -> 1     0   0   0    1    0       0    0    0    0    0    1     0    0    p2   1      0
  5    11111.1 -> 1     0   0   0    1    p1      0    0    p1   0    0    1     0    0    p1   1      1
  6    01111.0 -> 1     0   0   0    1    p2      0    0    0    p2   0    1     0    0    0    1      1
  7    00111.1 -> 1     0   0   0    1    0       0    0    0    0    0    1     0    0    0    1      1
  8    00011.0 -> 1     0   1   p1   1    0       0    0    0    0    0    1     0    0    0    0      1
  9    00001.0 -> 1     0   1   0    1    0       0    0    0    0    p1   !p2   0    0    0    0      1

TP1 and TP3 look like two back to back pulses in the table above, but what it
really means is that it is a one clock long pulse that starts half way through
one phase and into the first half of the next phase.

AI[2:0]  meaning
-------  -----------------------------------------------
  000    S
  001    T
  010    U
  011    V
  100    KAP (KA0-KA3, ???) look up the function of L22.  Loads either KA or SKA, and can accumulate Z output
  101    KBP (KB0-KB3, ???) look up the function of L23.  similar
  110    CA 
  111    CB 

BI[2:0]  meaning
-------  -----------------------------------------------
  000    0
  001    KK
  010    D register (D10,D11,D12,D13 -- driven by 6259 board, cabling connector)
  011    D20        (not sure who drives this)
  100    KAP
  101    KBP
  110    CA 
  111    CB 

D1x comes from the board sitting between the 6170 and 6237, but is not labeled
on the 6165 schematic.  In my system, it is the 6259 cabling connector.

AC0=0 forces A output to 0
BC0 doesn't do that.  See ALU page (6192).

If JH=110, then L3.2 is high; when this is high and the RS strobe is high
(states 8 and 9), and they keyboard is pressed (presumably; KBD is high),
then the KB0-KB3 signals are latched into L23.

==============================================================================
			      6177 schematic notes
==============================================================================

The JH/JL logic is in the lower right corner of the 6177 schematic.

JH2 JH1 JH0  meaning
--- --- ---  -----------------------------------------------
 0   0   0   0
 0   0   1   1
 0   1   0   L24's D output (S3)
 0   1   1   L24's B output (S1)
 1   0   0   0
 1   0   1   CC
 1   1   0   KBD (keyboard strobe latch)
 1   1   1   Z0  (the ALU op produced a zero result nibble)

JL2 JL1 JL0  meaning
--- --- ---  -----------------------------------------------
 0   0   0   0
 0   0   1   1
 0   1   0   L24's A output (S0)
 0   1   1   L24's C output (S2)
 1   0   0   Z0
 1   0   1   CC
 1   1   0   L24's E output (TBD)
 1   1   1   1; subroutine return

L24 is a 5b shift register with async reset and load.  Weird.
It is not used as a shift register, as CLK is tied to ground.
It is cleared on the CK5 phase, and set on the CL phase.
    A = S0, B = S1, C = S2, D = S3, E = SC
I'm not sure why this clear/load thing happens.

SUB means "subroutine".  The CPU has a two level subroutine stack.
If SUB is set, L9 clocks (CK3) before L19 (CK4).  This is a call.
If JL=7, L19 clocks (CK3) before L9 (CK4).  This is a return.

On a call, {JAD,JH} are pushed to the two level stack.  For unknown reasons,
JL is not saved.  Upon return, {JAD,JH,1} is the jump address.

So far it is pretty closely following the Wang 700 microcode behavior.
But it really is much more closely related to the Wang 500 ucode.
Both the Wang 500 and the 1200 used a bit serial rather than the 4-bit
parallel Alu of the Wang 700.

8266 is a weird part:

    Control
    -------
     S0  S1   fn
    --- ---  ----
     0   x    Bn
     1   0    ~An
     1   1    1

or, f = (!S0 & B) | (S0 & !S1 & !A) | (S0 & S1)

       16  15  14  13  12  11  10  9
    |----------------------------------|
    | Vcc  A3  B3  f3  f2  B2  A2  S0  |
     >         Wang 8266 pinout        |
    |  A0  B0  f0  f1  B1  A1  S1  GND |
    |----------------------------------|
       1   2   3   4   5   6   7   8

The rom address, producing (script) jad[8:0], JH, JL, is normally clocked by
ROS, which is active in state 8.  On subroutine call (SUB=1), it is clocked
on phase CK2 (state 3).  This doesn't make sense to me that the two would be
so far apart in the microcycle, so come back and look at this with fresh eyes.

(script) jL, jH, and jad0 can be twiddled by force by L21, L22, L12 NOR gates.
Look into the details.  Actually, the other (script) jad bits can be forced too.
If any of RES, RWD, FWD, or STOP buttons are pressed, L13.8 goges high, and "BOX"
(or is it "BDX"?) goes high.  When this happens, L4.3 goes low the next CK5,
forcing (script) jad[8:1] low.  Based on which button is pressed, the three lsbs
are jammed.

How can this possibly work?  For reset, OK, but for RWD, FWD, STOP, the current
uaddr isn't saved so there is no way to return, and I don't see any way to allow
this jam only in certain contexts.

In more detail:
!RES  active forces BDX=1; jad[8:1]=0, jad[0]=*, jh=0, jl=0
!RWD  active forces BDX=1; jad[8:1]=0, jad[0]=*, jh=0, jl=1
!FWD  active forces BDX=1; jad[8:1]=0, jad[0]=*, jh=1, jl=0
!STOP active forces BDX=1; jad[8:1]=0, jad[0]=*, jh=1, jl=1

*With !APR active, jad[0]=1; with !APR inactive, jad[0]=0.
!APR comes from the 6164 (aka F564) board; it is an async power reset strobe.
!APR in effect is the same as !RES active, with one slight difference.

!RES active coincident with CK5 also drives "PRIME".
This bit of circuitry is prety hideous -- RES, RWD, FWD, STOP can be pressed
at any time, such that the dependent logic has unpredictable timing margin,
with unpredictable results.  They appear to be debounced via caps (except
STOP, which perhaps has a cap somewhere else on the line).

==============================================================================
			      6192 schematic notes
==============================================================================

This contains the bit serial ALU.  The output of th ALU is !Z.

The latch at the left side made of the cross coupled NAND2 gates of L16
is cleared on the CL pulse (state 0), and is set any time the Z output
is high for any phase CLK1234.  That is, the output Z0 is high when all
four bits of the operation were zero, otherwise Z0 is low.

If AOP=0..4, the ALU carry out bit is saved in L20.9, aka "CC".
If AOP=2..4, the ALU carry out bit is saved in L20.5, aka "SC".

The three NAND gates in L21 mux in SC on the first bit of an addition,
and CC on the next three bits.  I don't see a way to clear carry, other
than to perform an addition with save carry which is known not to produce
a carry out.

L10, the L21.11, and L22.8 select the carry in for the first bit.  It is forced
to 1 for AOP=1 and AOP=4, and it comes from SC (stored carry) for AOP=3.

The bit serial ALU logic is
   L18.3  = A ^ BC0
   L18.11 = A ^ B
   L18.6  = !A
   L18.8  = L18.11 ^ carry_in = A ^ B ^ carry_in
   L9.4   = !B
   L8.13  = !BC0
   L8.1   = !(L18.6 || L9.4) = !(!A || !B) = A && B
   L11.11 = !(L18.6 && L9.4) = !(!A && !B) = A || B
   L8.4   = !(L8.13 || (AOP!=6)) = !(!BC0 || (AOP!=6)) =  BC0 && (AOP==6)
   L8.10  = !(BC0   || (AOP!=6)) = !( BC0 || (AOP!=6)) = !BC0 && (AOP==6)

   Z = (L18.8 && ((AOP==4) || (AOP<4))
    || (L8.1  && (AOP==5)
    || (L18.11 && L8.4)
    || (L11.11 && L8.10)
or
   Z = ((A ^ B ^ carry_in) && (AOP<=4))
    || ((A && B)           && (AOP==5)
    || ((A ^  B) &&  BC0   && (AOP==6))
    || ((A || B) && !BC0   && (AOP==6))

BC0 also affects the carry logic, via L18.3.

    L19.1  = ![ B         && carry_in ]
    L19.4  = ![ (A ^ BC0) && carry_in ]
    L19.10 = ![ (A ^ BC0) && B        ]

    carry = (B         && carry_in)
	 || ((A ^ BC0) && carry_in)
	 || ((A ^ BC0) && B)

Case analysis, BC=0:

    carry = (B && carry_in)
	 || (A && carry_in)
	 || (A && B)

This is normal carry logic.

Case analysis, BC=1:

    carry = ( B && carry_in)
	 || (!A && carry_in)
	 || (!A && B)

Note that either way, the carry flag gets twiddled for all ops, even the
logical ops (AOP>4).

What does this mean?

    AOP BC  meaning
    --- --  ---------------
     0  0   A+B     CC = carry from A+B
     1  0   A+B+1   CC = carry from A+B+1
     2  0   A+B,    CC = carry from A+B;    SC from A+B
     3  0   A+B+SC, CC = carry from A+B+SC; SC from A+B+SC
     4  0   A+B+1,  CC = carry from A+B+1;  SC from A+B+1
     5  0   A&B     CC = carry from A+B
     6  0   A|B     CC = carry from A+B
     7  0   unused; probably just Z=0
     0  1   A+B,    CC = carry from ~A+B
     1  1   A+B+1,  CC = carry from ~A+B+1
     2  1   A+B,    CC = carry from ~A+B;    SC from ~A+B
     3  1   A+B+SC, CC = carry from ~A+B+SC; SC from ~A+B+SC
     4  1   A+B+1,  CC = carry from ~A+B+1;  SC from ~A+B+1
     5  1   A&B        = carry from ~A+B
     6  1   A^B        = carry from ~A+B
     7  1   unused; probably just Z=0

AOP=7 is not used.  if it was used, I believe the Z output would just be 0.

L2 and L4 form the S0-S3 register.  Normally, these four bits just recirculate
during CK1234, but if ZO=0, it loads the four bits of the ALU result.  Then
during CK5, the microcode has the opportunity to set or reset some or all of
the bits.

One oddness is that ZO=0 loads S with the ALU result, as just stated, unless
ST[3:0]=0xF.  Looking at the actual ucode, whenever ST=F, ZO=0, but there are
many cases where ZO=0 and ST!=F.  It seems the Wang 700 didn't do this
qualification by ST thing.  I guess the difference is this: the 700 way,
the Z output always had to be stored somewhere.  The 500/1200 way, it is
possible to say the ALU output is not saved.

L1 and L12 are 74145 are open collector bcd->decade decoders.  Since outputs
8 and 9 are not used, in effect the D input is used to enable the selected
output.  In some cases the open collector output is used to wire-or, but I'm
baffled why some of them don't have pull-ups.

L1  strobes during CK5 for ST=0..7.
L12 strobes during CK5 for ST=8..15.
CK5 happens at the very end of the micro cycle, after the ALU op is done.

  ST  function
 ---  ---------------
   0  (not used)
   1  sets S0
   2  sets S1
   3  sets S2
   4  sets S3
   5  clear S0
   6  clear S1
   7  clear S2
   8  clear S3
   9  strobe !ZK
  10  set S0 high if !Z0, low if Z0   (ie, S0=!Z0)
  11  set S1 high if  Z0, low if !Z0  (ie, S1=Z0)
  12  strobe !OV                      (not seen in the microcode)
  13  clear S0,S1,S2,S3
  14  strobe !ERR                     (not seen in the microcode)
  15  interacts with ZO field         (see note above)

L13 does the Z Output (ZO) decoding.

ZO2 ZO1 ZO0  meaning
--- --- ---  --------------------
 0   0   0   store ALU result to S, unless ST=F (in which case output is discarded)
 0   0   1   store ALU result to T
 0   1   0   store ALU result to U
 0   1   1   store ALU result to V
 1   0   0   store ALU result to KA
 1   0   1   store ALU result to KB
 1   1   0   store ALU result to CA
 1   1   1   store ALU result to CB

CK(RB) = CK5 && RS && MOP=any_of(4,5,6)
It goes out on connector 2S.  however, looking at the backplane diagram
(6165 schematic), there is no label on that pin and so far I don't see
any other card consuming the signal.

On the Wang 500 schematic, CK(RB) clocks the output of the DRAM chips
at the end of the read cycle.  Also, D10,D20,D30,D40 come from the
unclocked DRAM outputs; D20 is a mystery signal on the 6174 board.
Looking at 6119 schematic (that is for the 500), !(R/W/N) active causes
the DRAM (R/W) line to pulse low.  Thus I think this is a R/!W input.
Also, I think (R/W/N) is a typo (although drafted by hand).

==============================================================================
			      6195 schematic notes
==============================================================================

L2 is the "T" register.
L4 is the "U" register.
L6 is the "V" register.
All are clocked on CK1234; based on ZO, the serial ALU output may
be loaded into none or one of these registers.

"MAT" is 0xF if MOP= any_of(2,3,5,6)
"MAT" is "T" if MOP=!any_of(2,3,5,6)

"MAU" is KK  if MOP= any_of(2,5)
"MAU" is 0xF if MOP= any_of(3,6)
"MAU" is "U" if MOP=!any_of(2,3,5,6)

"MAV" is KK  if MOP= any_of(3,6)
"MAV" is "U" if MOP=!any_of(3,6)

MAU and MAV go to the 6152 and drive RAM address bits [7:4] and [3:0],
respectively.  MAT is unused, and is probably there because the
calculator this logic is derived from had a larger SRAM, and MAT
supplied the top 4 bits of the 4KB address.

For most mop values, the ram address MA is (T,U,V) (T is unused).
mop2,mop5: MA=(0xF,KK, V )
mop3,mop6: MA=(0xF,0xF,KK)

L18 and L20 decode the MOP field into 16 one-hot active low outputs.

L19.8 means MOP=any_of(2,5)
L19.6 means MOP=any_of(3,6)
L9.4  means MOP=!any_of(2,3,5,6)
L9.13 means MOP= L19.8 || L9.4 = any_of(2,5) || !any_of(2,3,5,6) = !any_of(3,6) (why not just invert L19.6?)

L10.6 = !RWC = !any_of(1,2,3,4,5,6)

MOP  users           meaning
---  -------------   -------------------------------------
 0                   nop                                [most common]
 1                   write RAM[U,V] from data=CA,CB
 2                   write RAM[KK,V] from data=CA,CB
 3                   write RAM[0xF,KK] from data=CA,CB  [very common]
 4   6192            CA,CB=read RAM[U,V]
 5   6192            CA,CB=read RAM[KK,V]
 6   6192            CA,CB=read RAM[0xF,KK]             [very common]
 7             6178  set/clear tape input controls CSL,ELN,ERN,NAN
 8   6172,     6178  set/clear tape input controls; ring BEL
 9   6174,6237,6504  6174:(KKm9) KA/KB load SKA inputs; 6237: uart control; 6504:keyboard status
10   6174            load KB with status from serial comm board
11   6173            set DIN1=KAP, DIN0=KBP
12   6174            load KB with status from serial comm board
13   6173,6176,6178  modify tape input controls
14   6173,6176,6178   "      "    "     "
15   6237            never used by ucode; all others are

On 6172 (typewriter output control),
    mop8 -> on TP1 && KK[2],
                L13.8=!BUSY latches 0
                L23.8, "TIMING" flop latches L23.8=1
                L22 flops are cleared
                L29,L30,L31,L21,L10 counters are cleared (RO(1)=RO(2)=1)
                    (it takes 80000 MK0 clocks before L10.11 goes high == 20ms)
    mop8 -> on TP1 && KK[2] && BI[0],
                L3.12=0, "SP FUNCTION" flop latches L23.3=0
    mop8 -> on TP1 && KK[2] && !BI[0],
                L3.12=0 and "NORMAL" flop latches L23.3=1

On 6173 (KA/KB code converter),
    mop11 -> on CK5, DIN0 latches KBP and DIN1 latches KAP
    mop13 -> on CK5, DIN0 and DIN1 are cleared
    if mop13 or mop14, on CK5 !RC latches BI0    (the alu arg-B selector lsb)

On 6174 (timing, KA, KB registers),
    mop10 -> on RS, KB.A=ROP, KB.B=LOP, KB.C=PSKB1, KB.D=SKB0  (TBD: what are these?)
    mop12 -> on RS, KB.A=RHS, KB.B=LHS, KB.C=R/B,   KB.D=L/S   (TBD: what are these?)

On 6176,
    ... MXA through MXH go to 6231 and drive the four phase motor windings
    and a bunch of other things based on MOP and KK

    when either mop13 or mop14 strobes, L10.3 strobes high;
       on the falling edge of L10.3, L9 latches KK0, KK1, KK3.
       likewise, L11 clocks KK2 into Q1 (RHS) or Q2 (LHS) depending on KK0.
       thus it seems that KK[0]=1 selects the RHS tape drive;
                          KK[0]=0 selects the LHS tape drive

       it seems mop13 enables  the motor (either TMR or TML is high)
          while mop14 disables the motor (both TMR and TML are low)
       KK[1] sets the tape direction
       KK[3] sets the motor current (H+L) -- double check with 2200 schematics
       these same flop outputs are decoded somehow to produce FSR, RSR, FSL, RSL
            mop13/14 rs-flop is used to enable/suppress all of them

       mop13 sets   a flop, which enables  TML & TMR outputs
       mop14 clears a flop, which disables TML & TMR outputs
       if ((mop13 || mop14) &&  KK0), RHS=KK2
       if ((mop13 || mop14) && !KK0), LHS=KK2

On 6178 (tape input control),
    MOP[3:0]=7  -> if KK0=1, set CSL=!BI0
                   if KK1=1, set ELN=!BI0
                   if KK2=1, set ERN=!BI0
                   if KK3=1, set NAN=!BI0
    MOP[3:0]=8  -> if KK[0], set KBLO=BI0, !BKLK=!BI0
                   if KK[1], ring bell
    MOP[3:0]=13 -> create !mop13g on TP2
    MOP[3:0]=14 -> create  mop14g on TP2
          if ((mop13 || mop14) &&  KK0) select R tape inputs for DIL1, DIL0, DIR1, DIR0, !RCL0, !RCR0, !PSKB1
          if ((mop13 || mop14) && !KK0) select L tape inputs for same

On 6192,
    mop3,4,5 generate the "R" signal, but I don't see a consumer
    also generates the CK(RB) signal, but again, no consumer?

On 6195,
    the board generates mop and !mop, but also does some decoding.
    mop1,2,3,4,5,6

    "W" is from L19.12, and is (mop1 || mop2 || mop3).  It is used locally, and
    although it goes to the backplane, this pin isn't labeled on the schematic.

    !(R/M/W) (terrible name -- which is active low?) comes from L1.8, which is
       = !( !(R/W) && W)
       = !( !(R/W) && (mop1 || mop2 || mop3))
    !(R/W) comes from 6174 and is simply tap5
    !(R/M/W) goes to 6152's SRAM R/W line (again, no polarity indication)

On 6237 (serial communications board),
    mop9  - KK[2:0] decodes into various strobes; KA/KB provides data in
    mop15 - KK[2:0] decodes into various strobes; KA/KB provides data in
    output of UART controls come out serially as SKA[3:0]

On 6405 (keyboard control),
    mop9 drives keyboard status out on SKA[2:0]
        SKA[0] = ADJ
        SKA[1] = ORG
        SKA[2] = !(KR/B)

==============================================================================
			      6293 schematic notes
==============================================================================

ROM mapping.

    L17, OUT1: JAD0
    L17, OUT2: JAD1
    L17, OUT3: JAD2
    L17, OUT4: JAD3

    L18, OUT1: n/c
    L18, OUT2: n/c
    L18, OUT3: JL0
    L18, OUT4: JL1

    L19, OUT1: JL2
    L19, OUT2: JH0
    L19, OUT3: JH1
    L19, OUT4: JH2

    L20, OUT1: JAD4
    L20, OUT2: JAD5
    L20, OUT3: JAD6
    L20, OUT4: JAD7

    L21, OUT1: JAD8
    L21, OUT2: SUB
    L21, OUT3: ST0
    L21, OUT4: ST1

    L22, OUT1: ST2
    L22, OUT2: ST3
    L22, OUT3: KK0
    L22, OUT4: KK1

    L23, OUT1: KK2
    L23, OUT2: KK3
    L23, OUT3: MOP0
    L23, OUT4: MOP1

    L24, OUT1: MOP2
    L24, OUT2: MOP3
    L24, OUT3: BC0
    L24, OUT4: AC0

    L25, OUT1: AOP0
    L25, OUT2: AOP1
    L25, OUT3: AOP2
    L25, OUT4: ZO0

    L26, OUT1: ZO1
    L26, OUT2: ZO2
    L26, OUT3: BI0
    L26, OUT4: BI1

    L27, OUT1: BI2
    L27, OUT2: AI0
    L27, OUT3: AI1
    L27, OUT4: AI2

In the schematic, the printed "JAD*" are the address bits as stored in the
ucode, while the cursive JAD* are the address bits which are being driven to
the ucode address decoder.

6293 makes it clear that RS means "Read Strobe."  This is what causes the ROMs
to read the addressed word and latch the output.  However, I'm not sure if it
is the rising or falling edge, or if it is a transparent latch.  RS is delayed
by 8 inverters, so it is a delayed version of RS really.  The 7404 used has
timing of tPLH 12-22ns, and tPHL 8-15ns (typ-max).  For a pair of inverters,
this means 20-37ns, or 80ns typical and 148ns max for the whole chain.

A low on an L2 output means that patch word is selected.  That means the
corresponding address decoder line before the inverter must be high, meaning
none of the diode taps should pulled low.

word0 = !(!A0) && !(!A3) && !(A4) && !(!A5) && !(A6) && !(A7) && !(A8) && !(A9) && !(!A10)
      =    A0  &&    A3  &&  !A4  &&    A5  &&  !A6  &&  !A7  &&  !A8  &&  !A9  &&    A10
      = 0x429, 0x42B, 0x42D, 0x42F

This is not a fully decoded address.  Perhaps it is a subroutine thing where
not all the possible offsets within a group of eight are used.

The microword generated by the patch word is, according to the schematic,
    JL[2:0]=0
    JH[2:0]=1
    JAD[8:0]=0x17F
    SUB=1
    ST[3:0]=0
    KK[3:0]=0
    MOP[3:0]=0
    BC0=0
    AC0=0
    AOP[2:0]=0
    ZO[2:0]=0
    BI[2:0]=0
    AI[2:0]=0

The actual board seems to match.
