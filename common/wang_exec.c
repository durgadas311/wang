// Copyright (c) 2011, 2012 Douglas Miller

#ident "$Id: wang_exec.c,v 1.4 2012/01/08 15:32:23 drmiller Exp $"

#include <stdlib.h>

#include "wang-sim.h"

extern int instr_exec(wang_sys_t *sys);

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
