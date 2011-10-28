// Copyright (c) 2011 Douglas Miller

#ident "$Id: w700_exec.c,v 1.3 2011/10/28 01:12:50 drmiller Exp $"

#include <stdlib.h>

#include "w700_cpu.h"
#include "w700_decode.h"

char *get_mach_str(w700_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%01x", sys->cpu.d);
	if (sys->cpu.ofl) s += sprintf(s, "|Prog Err");
	if (sys->cpu.err) s += sprintf(s, "|Mach Err");
	if (sys->cpu.kbd) s += sprintf(s, "|Key Pressed");

	*s = '\0';
	return buf;
}

char *get_psw_str(w700_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	if (sys->cpu.alu) *s++ = 'Z';
	else *s++ = 'z';
	if (sys->cpu.cc) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.sc) *s++ = 'C';
	else *s++ = 'c';
	if (sys->cpu.q) *s++ = 'Q';
	else *s++ = 'q';

	*s = '\0';
	return buf;
}

void ill_instr(w700_sys_t *sys, uint16_t pc) {
	fprintf(stderr, "Illegal instruction %03x: %011llx\n", pc,
		(sys->ucode[pc] >> 2) & 0x000003ffffffffffULL);
	fprintf(stderr, "Terminating at cycle %lld\n", sys->cpu.cycles);
	// exit(1);
	fflush(sys->trc_fp);
	sys->run = 0;
}

void sys_exec(w700_sys_t *sys) {
	uint16_t w700_pc = sys->cpu.pc;
	int rc = instr_exec(sys);
	if (rc) {
		ill_instr(sys, w700_pc);
	}
#ifdef TRACE
	if (sys->trace) {
		if (rc) fprintf(sys->trc_fp, "Illegal instruction break (%d)\n", rc);
	}
#endif // TRACE
}
