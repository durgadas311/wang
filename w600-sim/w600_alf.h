#ifndef __w600_alf_h__
#define __w600_alf_h__

#ident "$Id: w600_alf.h,v 1.2 2011/06/12 15:34:55 drmiller Exp $ Copyright (c) 2008-2011 Alf Urban"

typedef struct
{
	char magic[8], date[9], author[15], title[24];
	short order, block, bytes, verify;
} w600_alf_t;

#define ALF_MAGIC	"WANG600\0"

#define ALF_ORDER_OK	0xabba
#define ALF_ORDER_SWAP	0xbaab

#endif // __w600_alf_h__
