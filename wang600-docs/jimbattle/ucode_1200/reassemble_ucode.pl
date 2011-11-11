# this is a small program to read the 11 ROM images
# and produce a unified ROM listing, with fields broken out
#
use strict;

# array of references to rom images.
# yeah, 0-16 contain nothing.
my @roms;

{
    my @ai_dec = qw( S T U V KA KB CA CB );
    my @bi_dec = qw( 0 KK D D20 KA KB CA CB );
    my @zo_dec = qw( S T U V KA KB CA CB );
    my @aop_dec = qw( A+B A+B+1 A+B,sc A+B+sc,sc A+B+1,sc A&B A|B ?? );
    my @st_dec = qw( --    S0=1  S1=1    S2=1
                     S3=1  S0=0  S1=0    S2=0
                     S3=0  !ZK   S0=!Z0  S1=Z0
                     !OV   S=0   !ERR    --   );
    my @jh_dec = qw( 0 1 S3 S1 0 CC KBD Z0 );
    my @jl_dec = qw( 0 1 S0 S2 Z0 CC SC ret );

    # read in the roms
    foreach my $n (17 .. 27) {
        $roms[$n] = &readrom( "L${n}.rom" );
    }

    # print out the roms
    foreach my $addr (0 .. 2047) {
        my $ai  = &bits($addr, 27, 1, 3);
        my $bi  = &bits($addr, 27, 0, 1) * 4 + &bits($addr, 26, 2, 2);
        my $zo  = &bits($addr, 26, 0, 2) * 2 + &bits($addr, 25, 3, 1);
        my $aop = &bits($addr, 25, 0, 3);
        my $ac  = &bits($addr, 24, 3, 1);
        my $bc  = &bits($addr, 24, 2, 1);
        my $mop = &bits($addr, 24, 0, 2) * 4 + &bits($addr, 23, 2, 2);
        my $kk  = &bits($addr, 23, 0, 2) * 4 + &bits($addr, 22, 2, 2);
        my $st  = &bits($addr, 22, 0, 2) * 4 + &bits($addr, 21, 2, 2);
        my $sub = &bits($addr, 21, 1, 1);
        my $jad = &bits($addr, 21, 0, 1) * 256
                + &bits($addr, 20, 0, 4) * 16
                + &bits($addr, 17, 0, 4);
        my $jh  = &bits($addr, 19, 1, 3);
        my $jl  = &bits($addr, 19, 0, 1) * 4 + &bits($addr, 18, 2, 2);

        # check some assumptions.
        # any time ac is 0, the a input is a don't care. make sure ai is 0.
        die "ai nonzero" if ($ai !=0 && $ac == 0);
        # st=F only makes sense when zo=S
        die "zo nonzero" if ($zo !=0 && $st == 15);
        # shouldn't have a call (sub=1) and return (jl=7) together
        die "sub/ret conflict" if ($sub && $jl==7);
        # not strictly necessary, but seems to be true: return always has jad=0,jh=0
        die "hmm" if ($jl==7 && ($jad!=0 || $jh!=0));

        if (0) {
            printf "%03X: ai:%X bi:%X zo:%X aop:%X ac:%X bc:%X mop:%X kk:%X st:%X sub:%X jad:%03X jh:%X jl:%X\n",
                    $addr, $ai, $bi, $zo, $aop, $ac, $bc, $mop, $kk, $st, $sub, $jad*4, $jh, $jl;
        } elsif (0) {
            # symbolic decoding, field by field
            my $ai_s = $ai_dec[$ai];
            my $bi_s = $bi_dec[$bi];
            my $zo_s = $zo_dec[$zo];
            my $aop_s = $aop_dec[$aop];
            my $st_s = $st_dec[$st];
            my $jh_s = $jh_dec[$jh];
            my $jl_s = $jl_dec[$jl];

            # zero out the a input
            if (!$ac) { $ai_s = '0'; }

            # bc=1 aop=6 is a special case
            if ($aop==6 && $bc==1) { $aop_s = "A^B"; }

            # kill the z store if zo=0 (S) and ST=0xf
            if ($zo==0 && $st==15) { $zo_s="--"; }

            printf "%03X: ai:%-2s bi:%-3s zo:%-2s aop:%-9s bc:%X mop:%X kk:%X st:%-6s sub:%X jad:%03X jh:%-3s jl:%-5s\n",
                    $addr, $ai_s, $bi_s, $zo_s, $aop_s, $bc,
                    $mop, $kk, $st_s, $sub, $jad*4, $jh_s, $jl_s;
        } else {
            # symbolic decoding, combining fields
            my $ai_s = $ai_dec[$ai];
            my $bi_s = $bi_dec[$bi];
            my $zo_s = $zo_dec[$zo];
            my $aop_s = $aop_dec[$aop];
            my $st_s = $st_dec[$st];
            my $jh_s = $jh_dec[$jh];
            my $jl_s = $jl_dec[$jl];

            # zero out the a input
            if (!$ac) { $ai_s = '0'; }

            # bc=1 aop=6 is a special case
            if ($aop==6 && $bc==1) { $aop_s = "A^B"; }

            # kill the z store if zo=0 (S) and ST=0xf
            if ($zo==0 && $st==15) { $zo_s="--"; }

            # replace 'A' and 'B' by operands
            $aop_s =~ s/\bA\b/$ai_s/;
            $aop_s =~ s/\bB\b/$bi_s/;
            # replace KK by constant value
            $aop_s =~ s/KK/#${kk}/;
            # collapse 0+, 0^, and 0|
            $aop_s =~ s/0[+^|]//;
            # collapse +0, ^0, and |0
            $aop_s =~ s/[+^|]0//;
            # collapse 0+ (which can happen if A+B+1 and A=0, B=0
            $aop_s =~ s/0[+^|]//;

            # next address calculation
            # fold in jh and jl when known
            $jad = $jad*4;

            if    ($jh==0) { $jh_s = ''; }
            elsif ($jh==4) { $jh_s = ''; }
            elsif ($jh==1) { $jh_s = ''; $jad += 2; }
            else           { $jh_s = ' jh:' . $jh_s; }

            if    ($jl==0) { $jl_s = ''; }
            elsif ($jl==1) { $jl_s = ''; $jad += 1; }
            else           { $jl_s = ' jl:' . $jl_s; }

            my $nextaddr = sprintf("%03X", $jad) . $jh_s . $jl_s;
            my $jad_s = ($jl==7) ? 'ret' :
                        ($jad == $addr+1) ? '' : # "[$nextaddr]" :  # just next instruction
                        ($sub ? 'sub:' : 'jmp:') . $nextaddr;

            # mop=0 is a nop
            my $mop_s = ($mop == 0) ? '-' : sprintf("%X", $mop);

            printf "%03X: %-16s bc:%X mop:%s kk:%X st:%-6s %-1s\n",
                    $addr, $zo_s.'='.$aop_s, $bc, $mop_s, $kk, $st_s, $jad_s;
        }

    } # foreach addr
}


# extract bits from a word
sub bits
{
    my $addr = shift;    # word to inspect
    my $romnum = shift;  # rom L-number
    my $lowbit = shift;  # which bit of nibble to start
    my $numbit = shift;  # number of bits to keep

    my $nib = $roms[$romnum][$addr];
    return ($nib >> $lowbit) & ((1 << $numbit)-1);
}


# return a rom as a reference to an array of bytes, one rom nibble per byte.
# no error checking.
sub readrom
{
    my $filename = shift;
    my $lineno = 0;
    my @rom;

    open(my $fh, '<', $filename) or die $!;
    while(my $line = <$fh>) {
        chomp $line;
        $line =~ s/^[0-9A-F]*://;  # drop the address
        die if length($line) != 16;
        for(my $i=0; $i<16; $i++) {
            $rom[16*$lineno + $i] = hex(substr($line,$i,1));
        }
        $lineno++;
    }

    return \@rom;
}
