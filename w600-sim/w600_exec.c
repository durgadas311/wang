// $Id: w600_exec.c,v 1.1 2011/05/01 00:05:39 drmiller Exp $

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

extern int diw600(char *buf, uint8_t *t);
#endif // TRACE

void rd_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t b = sys->ram[adr >> 1];
	if (adr & 1) {
		b >>= 4;
	} else {
		b &= 0x0f;
	}
	sys->mr = b;
}

void wr_ram_i(w600_sys_t *sys, uint8_t ah, uint8_t am, uint8_t al) {
	uint16_t adr = (ah << 8) | (am << 4) | al;
	uint8_t a = sys->mr;
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
	rd_ram_i(sys, sys->ah, sys->am, sys->al);
}

void wr_ram(w600_sys_t *sys) {
	wr_ram_i(sys, sys->ah, sys->am, sys->al);
}

void ill_instr(w600_sys_t *sys) {
	uint16_t pc = sys->cpu.pc;
	fprintf(stderr, "Illegal instruction %03x: %011llx\n", pc,
		(sys->ucode[pc] >> 2) & 0x000003ffffffffffULL);
	fprintf(stderr, "Terminating at cycle %lld\n", sys->cpu.cycles);
	exit(1);
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
		uint64_t *m;
		int c, x;
#ifdef TRACE_CYCLES
		nat_tsc = tsc() - nat_tsc;
#endif // TRACE_CYCLES
		char buf[128];
		m = &sys->ucode[w600_pc];
		diw600(buf, m);
		fprintf(sys->trc_fp, "TRACE: %03x: (%s) "
#ifdef TRACE_CYCLES
				"%5lld native cycles\n",
				w600_pc, buf, nat_tsc);
#else // !TRACE_CYCLES
				"[%03x %03x %03x] %02x %02x %02x %02x %02x %02x %02x\n",
				w600_pc, buf,
				sys->cpu.pc,
				sys->cpu.stk1,
				sys->cpu.stk2,
				sys->cpu.ah,
				sys->cpu.am,
				sys->cpu.al,
				sys->cpu.mr,
				sys->cpu.acc,
				sys->cpu.dh,
				sys->cpu.dl);

#endif // !TRACE_CYCLES
	}
#endif // TRACE
}
