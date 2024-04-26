# CC2001-S16-xx.txt:
# ...
# 051  [01000001F 0013 10]:16  
# 052  [640510623 0124 14]:15  [640500623 0124 14]:01  ...
# ...
BEGIN{
	print "typedef unsigned long u64;";
	print "#define XCODE(ad,cnt,ai,bi,zo,aop,ac,bc,mop,kk,st,sub,jad,jh,jl) \\";
	print " ((u64)ad << 52) | ((u64)cnt << 44) | \\";
	print " ((u64)ai << 41) | ((u64)bi << 38) | ((u64)zo << 35) |   \\";
	print " ((u64)aop << 32) | ((u64)ac << 31) | ((u64)bc << 30) |  \\";
	print " (mop << 26) | (kk << 22) | (st << 18) | (sub << 17) |   \\";
	print " (jad << 8) | (jh << 5) | (jl << 2)";
	print "u64 " tag "[] = {";
}
{
	ad=$1
	x=2;
	for (x=2; x<NF; x+=3) {
		r1=$x;
		r2=$(x+1);
		r3=$(x+2);
		if (r1!~/^\[/) break;

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
		cnt=substr(r3,5);

		printf "XCODE(0x%s,%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s,0x%s),\n",\
			ad,cnt+0,ai,bi,zo,aop,ac,bc,mop,kk,st,sr,jad,jh,jl;
	}
}
END{
	print " (u64)-1,";
	print "};";
}
