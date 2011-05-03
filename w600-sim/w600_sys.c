// $Id: w600_sys.c,v 1.6 2011/05/03 22:53:17 drmiller Exp $

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <signal.h>

#include "w600_sys.h"
#include "w600_exec.h"

extern int diw600(char *buf, uint64_t *t);

//
// NOTE: microcode image is aligned in 64-bit ints as follows:
//
// uint64_t bits: | 63...44 | 43...           ...            ...2 | 1 0 |
// ucode fields:  | x ... x | h h h | g g g | ... | e e e | f f f | x x |
//
// Use w600_ucode_t from w600_ucode.h to decode from a uint64_t.
// All microcode image files pad each instruction to 64 bits.
//

static void sysdisplay(w600_sys_t *sys, int on) {
	if (!on) {
		// fputc('\b', stdout);
		// fputc(' ', stdout);
		// fflush(stdout);
		return;
	}
	int c = ' ';
	if (sys->cpu.pe || sys->cpu.me) c = '!';
	uint8_t ds = sys->cpu.al;
	uint8_t dc = sys->cpu.mr;
	if (ds == 0) {
		fputc('\r', stdout);
		fputc(c, stdout);
	}
	if (ds == 0 || ds == 13) {
		c = "+-+-+-+-+-+-+-+ "[dc];
	} else {
		c = "0123456789.>u<L "[dc];
	}
	fputc(c, stdout);
	fflush(stdout);
}

static void syskeyboard(w600_sys_t *sys) {
	if (sys->klen && !sys->cpu.kp) {
		--sys->klen;
		sys->cpu.dh = sys->keyb[sys->keyp] >> 4;
		sys->cpu.dl = sys->keyb[sys->keyp] & 0x0f;
		++sys->keyp;
		sys->cpu.kp = 1;
		return;
	}
}

static int intr(w600_sys_t *sys, int sig) {
	if (sig == SIGINT && sys->run) {
		fflush(sys->trc_fp);
		sys->run = 0;
		return 0;
	}
	return 1;
}

static void dump(w600_sys_t *sys) {
	char buf[1024];
	char *s = buf;
	fprintf(stderr, "at cycle %lld\n", sys->cpu.cycles);

	// todo: call w600_sim_cmd.c:_dump()...
	uint64_t *pc = &sys->ucode[sys->cpu.pc];

	diw600(buf, pc);
	fprintf(stderr, "PC = %03x [ %s ]\n", sys->cpu.pc, buf);
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n", sys->cpu.stk1, sys->cpu.stk2);
	fprintf(stderr, "AH = %01x AM = %01x AL = %01x MR = %01x\n",
				sys->cpu.ah, sys->cpu.am, sys->cpu.al, sys->cpu.mr);
	fprintf(stderr, "ACC = %01x Z = %d I = %d C = %d\n",
				sys->cpu.acc, sys->cpu.z, sys->cpu.i, sys->cpu.c);
	fprintf(stderr, "DH = %01x DL = %01x XH = %01x XL = %01x XR = %01x\n",
			sys->cpu.dh, sys->cpu.dl, sys->cpu.xh, sys->cpu.xl, sys->cpu.xr);
	// more...
}

static void iofault(void *v, uint8_t port) {
	w600_sys_t *sys = (w600_sys_t *)v;
	fprintf(stderr, "I/O Fault at port %01x\n", port);
	dump(sys);
	exit(1);
}

static void segfault(void *v, uint16_t adr) {
	w600_sys_t *sys = (w600_sys_t *)v;
	fprintf(stderr, "Seg Fault at address %03x\n", adr);
	dump(sys);
	exit(1);
}

static void sysfault(w600_sys_t *sys, const char *str) {
	fprintf(stderr, "%s\n", str);
	dump(sys);
	exit(1);
}

void sys_init(w600_sys_t *sys) {
	memset(sys, 0, sizeof(*sys));
	sys->fault = sysfault;
	sys->display = sysdisplay;
	sys->keyboard = syskeyboard;
	//cpu_init(&sys->cpu);
	sys->cpu.cylimit = (uint64_t)-1;

	// need to get from "keyboard"...
	sys->cpu.mode0 = 0;
	sys->cpu.mode1 = 0;

	// already done by memset above...
	//memset(sys->ucode, 0, sizeof(sys->ucode));
	//memset(sys->ram, 0, sizeof(sys->ram));

	sys->intr = intr;
	sys->trace = 0;
	sys->trc_fp = stderr;

	// now install all devices and peripherals...
}

static void sys_interact(w600_sys_t *sys) {
	extern void sys_command(w600_sys_t *sys);

	printf("break at %03x\n", sys->cpu.pc);
	do {
		sys_command(sys);
	} while (!sys->run);
}

// ! This loads a microcode image!
void sys_loadpgm(w600_sys_t *sys, char *exe, uint16_t adr, uint16_t entry) {
	int fd;
	uint32_t max = 2 * 1024;
	adr &= 0x0fff;
	entry &= 0x0fff;
	int len = (max - adr) * sizeof(sys->ucode[0]);

	fd = open(exe, O_RDONLY);
	if (fd < 0) {
		perror(exe);
		exit(1);
	}
	uint64_t *m = &sys->ucode[adr];
	int rc = read(fd, m, len);
	close(fd);
}

static void run_some(w600_sys_t *sys, uint16_t entry) {
	sys->cpu.pc = entry;
	do {
		while (!sys->run) {
			sys_interact(sys);
		}
		sys_exec(sys); // single-step
		if (sys->cpu.cycles >= sys->cpu.cylimit) {
			sys->cpu.cylimit = (uint64_t)-1;
			sys->run = 0;
		}
	} while (1);
}


void sys_go(w600_sys_t *sys, uint16_t entry) {
	entry &= 0x0fff;
	run_some(sys, entry);
	printf("exit at cycle %lld\n", sys->cpu.cycles);
#ifdef TRACE
	if (sys->trace) {
		dump(sys);
	}
#endif // TRACE
}
