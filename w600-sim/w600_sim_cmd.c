// $Id: w600_sim_cmd.c,v 1.2 2011/05/01 00:33:02 drmiller Exp $

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

static char *get_mach_str(w600_sys_t *sys) {
	static char buf[32];
	char *s = buf;

	s += sprintf(s, "mode0=%02x", sys->cpu.mode0);
	s += sprintf(s, "|mode1=%02x", sys->cpu.mode1);
	if (sys->cpu.pe) s += sprintf(s, "|Prog Err");
	if (sys->cpu.me) s += sprintf(s, "|Mach Err");
	if (sys->cpu.me) s += sprintf(s, "|Key Pressed");

	*s = '\0';
	return buf;
}

static char *get_psw_str(w600_sys_t *sys) {
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
	sys->run = 1;
	printf("resuming at %03x\n", sys->cpu.pc);
	return 0;
}

static int _help(w600_sys_t *sys, char *line) {
	printf(	"W600-SIM Commands:\n"
		"\tquit\tEnd simulation\n"
		"\ttrace\tToggle trace on/off\n"
		"\tdump\tDump processor state/registers\n"
		"\tgo\tResume program at current PC\n"
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
