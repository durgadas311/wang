# CC2001-L01.txt
# 000  [003101E00 007F 00] 00 0 3 11 E 0 0  J 07F 0 0  1FC         
# 001  [001101E0D 007F 10] 00 0 1 11 E 0 D  J 07F 1 0  1FE         
# 002  [003000000 017F 01] 00 0 3 00 0 0 0  J 17F 0 1  5FD         
# 003  [000200627 0180 00] 00 0 0 02 6 2 7  J 180 0 0  600         
# 004  [000000000 0181 10] 00 0 0 00 0 0 0  J 181 1 0  606         
# 005  [01300002D 0007 10] 00 1 3 00 0 2 D  J 007 1 0  01E         
# 006  [015000030 008B 00] 00 1 5 00 0 3 0  J 08B 0 0  22C         
# 007  [004000000 008C 00] 00 0 4 00 0 0 0  J 08C 0 0  230         
# 008  [016000070 0000 07] 00 1 6 00 0 7 0  J 000 0 7  000    RT   
# 009  [060010000 00D4 14] 10 6 0 00 0 0 0  J 0D4 1 4  352    Z0   
# ...
# CC2001-S16-xx.txt
# ...
# 051  [01000001F 0013 10]:16  
# 052  [640510623 0124 14]:15  [640500623 0124 14]:01  ...
# ...
BEGIN{
	print "typedef unsigned long u64;";
	print "#define UCODE(ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl) \\";
	print " ((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |   \\";
	print " ((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |  \\";
	print " (mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |   \\";
	print " (jad << 8) | (jh << 5) | (jl << 2)";
	print "u64 ucode[2048] = {";
}
{
	r1=$2;
	r2=$3;
	r3=$4;

	ai=substr(r1,2,1);
	bi=substr(r1,3,1);
	zo=substr(r1,4,1);
	aop=substr(r1,5,1);
	ac=substr(r1,6,1);
	bc=substr(r1,7,1);
	mop=substr(r1,8,1);
	kk=substr(r1,9,1);
	st=substr(r1,10,1);
	sr=substr(r2,1,1);
	jad=substr(r2,2);
	jh=substr(r3,1,1);
	jl=substr(r3,2,1);

	printf "UCODE(0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s),\n",\
		ai,bi,zo,aop,ac,bc,mop,kk,st,sr,jad,jh,jl;
}
END{
	print "};";
}
