// Copyright (c) 2011 Douglas Miller

#ident "$Id: wang_exec.c,v 1.1 2011/11/06 01:08:25 drmiller Exp $"

#include <stdlib.h>

#include "wang-sim.h"

extern int instr_exec(wang_sys_t *sys);

char *get_mach_str(wang_sys_t *sys) {
	static char buf[32];
	char *s = buf;

#ifdef __wang600__
	s += sprintf(s, "mode0=%01x", sys->cpu.d1);
	s += sprintf(s, "|mode1=%01x", sys->cpu.d2);
	if (sys->cpu.ov) s += sprintf(s, "|Prog Err");
#endif // __wang600__
#ifdef __wang700__
	s += sprintf(s, "mode0=%01x", sys->cpu.d);
	if (sys->cpu.ofl) s += sprintf(s, "|Prog Err");
#endif // __wang700__
	if (sys->cpu.err) s += sprintf(s, "|Mach Err");
	if (sys->cpu.kbd) s += sprintf(s, "|Key Pressed");

	*s = '\0';
	return buf;
}

char *get_psw_str(wang_sys_t *sys) {
	static char buf[32];
	char *s = buf;

#ifdef __wang600__
	if (sys->cpu.zo) *s++ = 'Z';
	else *s++ = 'z';
#endif // __wang600__
#ifdef __wang700__
	if (sys->cpu.alu) *s++ = 'Z';
	else *s++ = 'z';
#endif // __wang700__
	if (sys->cpu.cc) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.sc) *s++ = 'C';
	else *s++ = 'c';
#ifdef __wang700__
	if (sys->cpu.q) *s++ = 'Q';
	else *s++ = 'q';
#endif // __wang700__

	*s = '\0';
	return buf;
}

void ill_instr(wang_sys_t *sys, uint16_t pc) {
	fprintf(stderr, "Illegal instruction %03x: %016llx\n", pc,
		sys->ucode[pc]);
	fprintf(stderr, "Terminating at cycle %lld\n", sys->cpu.sys.cycles);
	// exit(1);
	fflush(sys->trc_fp);
	sys->run = 0;
}

void sys_exec(wang_sys_t *sys) {
	uint16_t wang_pc = sys->cpu.sys.pc;
	int rc = instr_exec(sys);
	if (rc) {
		ill_instr(sys, wang_pc);
	}
#ifdef TRACE
	if (sys->trace) {
		if (rc) fprintf(sys->trc_fp, "Illegal instruction break (%d)\n", rc);
	}
#endif // TRACE
}
