// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_exec.c,v 1.11 2011/11/05 00:51:32 drmiller Exp $"

#include <stdlib.h>

#include "w600_cpu.h"
#include "w600_decode.h"

char *get_mach_str(w600_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%01x", sys->cpu.d1);
	s += sprintf(s, "|mode1=%01x", sys->cpu.d2);
	if (sys->cpu.ov) s += sprintf(s, "|Prog Err");
	if (sys->cpu.err) s += sprintf(s, "|Mach Err");
	if (sys->cpu.err) s += sprintf(s, "|Key Pressed");

	*s = '\0';
	return buf;
}

char *get_psw_str(w600_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	if (sys->cpu.zo) *s++ = 'Z';
	else *s++ = 'z';
	if (sys->cpu.cc) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.sc) *s++ = 'C';
	else *s++ = 'c';

	*s = '\0';
	return buf;
}

void ill_instr(w600_sys_t *sys, uint16_t pc) {
	fprintf(stderr, "Illegal instruction %03x: %011llx\n", pc,
		(sys->ucode[pc] >> 2) & 0x000003ffffffffffULL);
	fprintf(stderr, "Terminating at cycle %lld\n", sys->cpu.cycles);
	// exit(1);
	fflush(sys->trc_fp);
	sys->run = 0;
}

void sys_exec(w600_sys_t *sys) {
	uint16_t w600_pc = sys->cpu.pc;
	int rc = instr_exec(sys);
	if (rc) {
		ill_instr(sys, w600_pc);
	}
#ifdef TRACE
	if (sys->trace) {
		if (rc) fprintf(sys->trc_fp, "Illegal instruction break (%d)\n", rc);
	}
#endif // TRACE
}
