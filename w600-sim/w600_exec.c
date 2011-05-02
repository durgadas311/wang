// $Id: w600_exec.c,v 1.4 2011/05/02 00:05:51 drmiller Exp $

#include <stdlib.h>

#include "w600_cpu.h"
#include "w600_decode.h"

#undef TRACE_CYCLES

#ifdef TRACE
inline uint64_t tsc() {
	uint64_t tsc;
	__asm__ __volatile__ ("rdtsc" : "=A" (tsc));
	return tsc;
}

extern int diw600(char *buf, uint64_t *t);
#endif // TRACE

char *get_mach_str(w600_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%01x", sys->cpu.mode0);
	s += sprintf(s, "|mode1=%01x", sys->cpu.mode1);
	if (sys->cpu.pe) s += sprintf(s, "|Prog Err");
	if (sys->cpu.me) s += sprintf(s, "|Mach Err");
	if (sys->cpu.me) s += sprintf(s, "|Key Pressed");

	*s = '\0';
	return buf;
}

char *get_psw_str(w600_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	if (sys->cpu.z) *s++ = 'Z';
	else *s++ = 'z';
	if (sys->cpu.i) *s++ = 'I';
	else *s++ = 'i';
	if (sys->cpu.c) *s++ = 'C';
	else *s++ = 'c';

	*s = '\0';
	return buf;
}

void rd_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->cpu.mr = b;
}

void wr_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t a = sys->cpu.mr;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		a <<= 4;
		b &= 0x0f;
	} else {
		b &= 0xf0;
	}
	sys->ram[adr >> 1] = b | a;
}

void rd_ram(w600_sys_t *sys) {
	rd_ram_i(sys, sys->cpu.ah, sys->cpu.am, sys->cpu.al);
}

void wr_ram(w600_sys_t *sys) {
	wr_ram_i(sys, sys->cpu.ah, sys->cpu.am, sys->cpu.al);
}

void ill_instr(w600_sys_t *sys) {
	uint16_t pc = sys->cpu.pc;
	fprintf(stderr, "Illegal instruction %03x: %011llx\n", pc,
		(sys->ucode[pc] >> 2) & 0x000003ffffffffffULL);
	fprintf(stderr, "Terminating at cycle %lld\n", sys->cpu.cycles);
	// exit(1);
	fflush(sys->trc_fp);
	sys->run = 0;
}

void sys_exec(w600_sys_t *sys) {
#ifdef TRACE
	uint16_t w600_pc = sys->cpu.pc;
#ifdef TRACE_CYCLES
	uint64_t nat_tsc = 0;
	if (sys->trace) {
		nat_tsc = tsc();
	}
#endif // TRACE_CYCLES
#endif // TRACE
	int rc = instr_exec(sys);
	if (rc) {
		ill_instr(sys);
	}
#ifdef TRACE
	if (sys->trace) {
		if (rc) fprintf(sys->trc_fp, "Illegal instruction break (%d)\n", rc);
		uint64_t *m;
		int c, x;
#ifdef TRACE_CYCLES
		nat_tsc = tsc() - nat_tsc;
#endif // TRACE_CYCLES
		char buf[128];
		m = &sys->ucode[w600_pc];
		diw600(buf, m);
#ifdef TRACE_CYCLES
		fprintf(sys->trc_fp, "TRACE: %03x: (%s) "
				"%5lld native cycles\n",
				w600_pc, buf, nat_tsc);
#else // !TRACE_CYCLES
		fprintf(sys->trc_fp, "TRACE: %03x: "
				"[%03x %03x %03x] %01x %01x %01x %01x "
				"[%s] %01x %01x %01x : "
				"%s\n",
				w600_pc,
				sys->cpu.pc,
				sys->cpu.stk1,
				sys->cpu.stk2,
				sys->cpu.ah,
				sys->cpu.am,
				sys->cpu.al,
				sys->cpu.mr,
				get_psw_str(sys),
				sys->cpu.acc,
				sys->cpu.dh,
				sys->cpu.dl,
				buf);

#endif // !TRACE_CYCLES
	}
#endif // TRACE
}
