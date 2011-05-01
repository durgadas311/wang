// $Id: w600_sim_cmd.c,v 1.5 2011/05/01 18:18:48 drmiller Exp $

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include "w600_sys.h"
#include "w600_exec.h"

extern int diw600(char *buf, uint64_t *t);

static int _dump(w600_sys_t *sys, char *line) {
	char buf[1024];

	uint64_t *pc = &sys->ucode[sys->cpu.pc];
	diw600(buf, pc);
	fprintf(stderr, "PC = %03x %s\n", sys->cpu.pc, buf);
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n",
			sys->cpu.stk1, sys->cpu.stk2);

	fprintf(stderr, "ACC  = %02x [ %s ]\n", sys->cpu.acc, get_psw_str(sys));
	fprintf(stderr, "AH = %02x AM = %02x AL = %02x MR = %02x\n",
		sys->cpu.ah, sys->cpu.am, sys->cpu.al, sys->cpu.mr);
	fprintf(stderr, "DH = %02x DL = %02x XH = %02x XL = %02x XR = %02x\n",
		sys->cpu.dh, sys->cpu.dl, sys->cpu.xh, sys->cpu.xl, sys->cpu.xr);

	fprintf(stderr, "[%s]\n", get_mach_str(sys));

	return 0;
}

static int _disas(w600_sys_t *sys, char *line) {
	char buf[1024];
	char *s;
	uint16_t adr = sys->cpu.pc;
	int len = 16;
	uint16_t max = 2 * 1024;

	s = strtok(NULL, " \t");
	if (s) {
		if (strcmp(s, ".") != 0) {
			adr = strtoul(s, NULL, 16);
		}
		s = strtok(NULL, " \t");
		if (s) {
			len = strtoul(s, NULL, 0);
		}
	}
	adr &= 0x0fff;
	if (max - adr < len) len = max - adr;

	uint64_t *pc = &sys->ucode[adr];
	while (len > 0) {
		diw600(buf, pc);
		printf("%03x: [%011llx] %s\n", adr, (*pc >> 2), buf);
		--len;
		++adr;
		++pc;
	}
	return 0;
}

static int _exam(w600_sys_t *sys, char *line) {
	char *s;
	uint16_t adr = (sys->cpu.ah << 8) | (sys->cpu.am << 4) | sys->cpu.al;
	int len = 16;
	s = strtok(NULL, " \t");
	if (s) {
		if (strcmp(s, ".") != 0) {
			adr = strtoul(s, NULL, 16);
		}
		s = strtok(NULL, " \t");
		if (s) {
			len = strtoul(s, NULL, 0);
		}
	}

	adr >>= 1;
	len = (len + 1) & ~1;
	int x, y;
	for (x = 0; x < len;) {
		printf("%03x:", adr << 1);
		for (y = 0; x + y < len && y < 16; y += 2) {
			uint8_t b = sys->ram[adr];
			printf(" %02u-%02u", (b >> 4), (b & 0x0f));
			++adr;
		}
		printf("\n");
		x += y;
	}
	return 0;
}

static int _trace(w600_sys_t *sys, char *line) {
	char *s;
	s = strtok(NULL, " \t");
	if (s) {
		if (strcmp(s, "on") == 0) {sys->trace = 1;}
		else if (strcmp(s, "off") == 0) {sys->trace = 0;}
		else {
			FILE *fp = fopen(s, "a");
			if (!fp) {
				perror(s);
				return 0;
			}
			sys->trace = 1;
			sys->trc_fp = fp;
		}
	} else {
		sys->trace = !sys->trace;
	}
	if (!sys->trace && sys->trc_fp != stderr) {
		fclose(sys->trc_fp);
		sys->trc_fp = stderr;
	}
	if (sys->trace) {
		printf("trace is on (%s)\n",
			sys->trc_fp == stderr ? "stderr" : "file");
	} else {
		printf("trace is off\n");
	}
	return 0;
}

static int _go(w600_sys_t *sys, char *line) {
	char *s;
	s = strtok(NULL, " \t");
	if (s) {
		if (*s == '+') {
			uint64_t n = strtoul(s + 1, NULL, 0);
			sys->cpu.cylimit = sys->cpu.cycles + n;
			printf("breakpoint at %lld cycles (now + %lld)\n",
						sys->cpu.cylimit, n);
		}
	}
	sys->run = 1;
	printf("resuming at %03x\n", sys->cpu.pc);
	return 0;
}

static int _help(w600_sys_t *sys, char *line) {
	printf(	"W600-SIM Commands:\n"
		"\tquit\tEnd simulation\n"
		"\ttrace [file]\tToggle trace on/off\n"
		"\tdump\tDump processor state/registers\n"
		"\texam [addr [words]]\tExamine RAM at AH,AM,AL [ or hex addr]\n"
		"\tdisas [addr [instrs]]\tDisassemble ROM at PC [ or hex addr]\n"
		"\tgo [+cycles]\tResume program at current PC [break after <cycles>]\n"
		"\thelp\tDisplay this help\n"
		);
	return 0;
}

struct {
	char *cmd;
	int (*func)(w600_sys_t *sys, char *line);
} commands[] = {
	{ "quit", NULL },
	{ "trace", _trace },
	{ "dump", _dump },
	{ "disas", _disas },
	{ "exam", _exam },
	{ "go", _go },
	{ "help", _help },
};
#define NUM_CMDS	(sizeof(commands) / sizeof(commands[0]))

void sys_command(w600_sys_t *sys) {
	char buf[128];
	int x;

	printf("%% ");
	fflush(stdout);
	char *s = fgets(buf, sizeof(buf), stdin);
	if (!s) {
		printf("w600-sim done.\n");
		exit(0);
	}
	x = strlen(s);
	if (!x) return;
	--x;
	if (s[x] == '\n') s[x] = '\0';
	s = strtok(buf, " \t");
	if (!s) return;
	for (x = 0; x < NUM_CMDS; ++x) {
		if (strcmp(s, commands[x].cmd) == 0) {
			break;
		}
	}
	if (!(x < NUM_CMDS)) {
		printf("%s ?\n", s);
		return;
	}
	if (!commands[x].func) {
		printf("w600-sim done.\n");
		exit(0);
	}
	int rc = commands[x].func(sys, buf);
	if (rc) {
		// what is this for?
		exit(1);
	}
}
