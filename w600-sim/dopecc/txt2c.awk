/^#/{ next; }
{
	aa=$6;
	bi=$7;
	zo=$8;
	ab=$9;
	mop=$10;
	kk=$11;
	st=$12;
	ssb=$13;
	adr=$14;
	jh=$15;
	jl=$16;

	ac=substr(aa,1,1);
	ai=substr(aa,2,1);
	aop=substr(ab,2,1);
	bc=substr(ab,1,1);

	if (ssb == "J") {
		ssb=0;
	} else {
		ssb=1;
	}

	printf("UCODE(%s,%s,%s,%s,%s,%s,0x%s,0x%s,0x%s,%s,0x%s,%s,%s),\n",
		ai,bi,zo,aop,ac,bc,mop,kk,st,ssb,adr,jh,jl);
}
