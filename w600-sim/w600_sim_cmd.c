// Copyright (c) 2011 Douglas Miller

#ident "$Id: w600_sim_cmd.c,v 1.16 2011/10/12 19:01:36 drmiller Exp $"

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

extern uint8_t __systrc[16];
extern uint8_t __keytrc;
extern uint8_t __keyb[32];
extern int __klen;
extern int __keyp;

static int _dump(w600_sys_t *sys, char *line) {
	char buf[1024];

	uint64_t *pc = &sys->ucode[sys->cpu.pc];
	diw600(buf, pc);
	fprintf(stderr, "PC = %03x %s\n", sys->cpu.pc, buf);
	fprintf(stderr, "STK1 = %03x STK2 = %03x\n",
			sys->cpu.stk1, sys->cpu.stk2);

	fprintf(stderr, "ACC  = %01x [ %s ]\n", sys->cpu.ms, get_psw_str(sys));
	fprintf(stderr, "AH = %01x AM = %01x AL = %01x MR = %01x\n",
		sys->cpu.ah, sys->cpu.am, sys->cpu.al, sys->cpu.mr);
	fprintf(stderr, "DH = %01x DL = %01x XH = %01x XL = %01x XR = %01x\n",
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
	int len = 256;
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
			printf(" %01x-%01x", (b & 0x0f), (b >> 4));
			++adr;
		}
		printf("\n");
		x += y;
	}
	return 0;
}

static int _rom(w600_sys_t *sys, char *line) {
	char *s;
	uint16_t adr = (sys->cpu.ah << 8) | (sys->cpu.am << 4) | sys->cpu.al;
	int len = 256;
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
			uint8_t b = sys->rom[adr];
			printf(" %01x-%01x", (b & 0x0f), (b >> 4));
			++adr;
		}
		printf("\n");
		x += y;
	}
	return 0;
}

static int _store(w600_sys_t *sys, char *line) {
	char *s;
	uint16_t adr = (sys->cpu.ah << 8) | (sys->cpu.am << 4) | sys->cpu.al;
	uint8_t b, v;

	s = strtok(NULL, " \t");
	if (!s) {
		return 0;
	}
	if (*s == '@') {
		adr = strtoul(s + 1, NULL, 16);
		s = strtok(NULL, " \t");
	}
	while (s != NULL) {
		v = strtoul(s, NULL, 16);
		v &= 0x0f;
		b = sys->ram[adr >> 1];
		if (adr & 1) {
			b &= 0x0f;
			v <<= 4;
		} else {
			b &= 0xf0;
		}
		sys->ram[adr >> 1] = b | v;
		++adr;
		s = strtok(NULL, " \t");
	}
	return 0;
}

static int _set(w600_sys_t *sys, char *line) {
	char *s;
	s = strtok(NULL, " \t");
	if (s) {
		int z;
		uint8_t *r;
		uint16_t v;
		char *t = strchr(s, '=');
		if (!t) {
			printf("Sytax error\n");
			return 0;
		}
		*t++ = '\0';
		z = 4;
		if (strcasecmp(s, "pc") == 0) {
			z = 11;
			r = (uint8_t *)&sys->cpu.pc;	// casted back later
		} else if (strcasecmp(s, "stk1") == 0) {
			z = 11;
			r = (uint8_t *)&sys->cpu.stk1;	// casted back later
		} else if (strcasecmp(s, "stk2") == 0) {
			z = 11;
			r = (uint8_t *)&sys->cpu.stk2;	// casted back later
		} else if (strcasecmp(s, "ah") == 0) {
			r = &sys->cpu.ah;
		} else if (strcasecmp(s, "am") == 0) {
			r = &sys->cpu.am;
		} else if (strcasecmp(s, "al") == 0) {
			r = &sys->cpu.al;
		} else if (strcasecmp(s, "ms") == 0) {
			r = &sys->cpu.ms;
		} else if (strcasecmp(s, "mr") == 0) {
			r = &sys->cpu.mr;
		} else if (strcasecmp(s, "dh") == 0) {
			r = &sys->cpu.dh;
		} else if (strcasecmp(s, "dl") == 0) {
			r = &sys->cpu.dl;
		} else if (strcasecmp(s, "xh") == 0) {
			r = &sys->cpu.xh;
		} else if (strcasecmp(s, "xl") == 0) {
			r = &sys->cpu.xl;
		} else if (strcasecmp(s, "xr") == 0) {
			r = &sys->cpu.xr;
		} else if (strcasecmp(s, "xs") == 0) {
			z = 3;
			r = &sys->cpu.xs;
		} else if (strcasecmp(s, "mode0") == 0) {
			r = &sys->cpu.mode0;
		} else if (strcasecmp(s, "mode1") == 0) {
			r = &sys->cpu.mode1;
		} else if (strcasecmp(s, "pe") == 0) {
			z = 1;
			r = &sys->cpu.pe;
		} else if (strcasecmp(s, "me") == 0) {
			z = 1;
			r = &sys->cpu.me;
		} else if (strcasecmp(s, "kp") == 0) {
			z = 1;
			r = &sys->cpu.kp;
		} else {
			printf("Unknown register name\n");
			return 0;
		}
		v = strtoul(t, NULL, 16);
		v &= ((1 << z) - 1);
		if (z > 8) {
			*((uint16_t *)r) = v;
		} else {
			*r = v;
		}
		printf("%s = %x\n", s, v);
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

static int _core(w600_sys_t *sys, char *line) {
	char *s;
	s = strtok(NULL, " \t");
	if (!s) {
		fprintf(stderr, "requires core-file name\n");
		return 0;
	}
	FILE *fp = fopen(s, "w");
	if (!fp) {
		perror(s);
		return 0;
	}
	size_t n = fwrite(sys->ram, sizeof(sys->ram), 1, fp);
	if (n != 1) {
		fprintf(stderr, "failed to dump core\n");
	}
	fclose(fp);
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

static int _dup(w600_sys_t *sys, char *line) {
	uint8_t *ram = &sys->ram[0xf6f >> 1];
	uint8_t *rom = &sys->rom[0xfff >> 1];
	int len = ram - &sys->ram[0x100 >> 1] + 1;
	printf("copying %d bytes from RAM to ROM...\n", len);
	while (len > 0) {
		*rom-- = *ram--;
		--len;
	}
	return 0;
}

static int _step(w600_sys_t *sys, char *line) {
	sys->cpu.cylimit = sys->cpu.cycles + 1;
	sys->run = 1;
	return 0;
}

static int _systrc(w600_sys_t *sys, char *line) {
	char *s;
	int x = 0;
	while ((s = strtok(NULL, " \t")) != NULL && x < sizeof(__systrc)) {
		if (strcmp(s, "off") == 0) {
			memset(__systrc, 0, sizeof(__systrc));
			x = 0;
		} else {
			int n = strtoul(s, NULL, 10);
			__systrc[x++] = (n != 0);
		}
	}
	printf("Tracing system words (15,15,0):"
		" %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %d\n",
		__systrc[0], __systrc[1], __systrc[2], __systrc[3],
		__systrc[4], __systrc[5], __systrc[6], __systrc[7],
		__systrc[8], __systrc[9], __systrc[10], __systrc[11],
		__systrc[12], __systrc[13], __systrc[14], __systrc[15]);
	return 0;
}

static int _keytrc(w600_sys_t *sys, char *line) {
	char *s;
	int x = 0;
	while ((s = strtok(NULL, " \t")) != NULL && x < sizeof(__systrc)) {
		if (strcmp(s, "off") == 0) {
			__keytrc = 0;
		} else if (strcmp(s, "on") == 0) {
			__keytrc = 1;
		} else {
		}
	}
	printf("Tracing keys is %s\n", __keytrc ? "on" : "off");
	return 0;
}

static int _keyboard(w600_sys_t *sys, char *line) {
	char *s;
	__keyp = 0;
	int x = 0;
	while ((s = strtok(NULL, " \t")) != NULL && x < sizeof(__keyb)) {
		int n = strtoul(s, NULL, 10);
		int h = n / 100;
		int l = n % 100;
		if (h > 15 || l > 15) {
			fprintf(stderr, "Invalid key code \"%s\"\n", s);
			return 0;
		}
		__keyb[x++] = (h << 4) | l;
	}
	__klen = x;
	return 0;
}

static int _help(w600_sys_t *sys, char *line);
struct {
	char *cmd;
	int (*func)(w600_sys_t *sys, char *line);
	char *arg_help;
	char *help;
} commands[] = {
	{ "quit", NULL,		NULL, "End simulation" },
	{ "trace", _trace,	"[file]", "Toggle trace on/off" },
	{ "dump", _dump,	NULL, "Dump processor state/registers" },
	{ "disas", _disas,	"[addr [instrs]]", "Disassemble ROM at PC [or hex addr]" },
	{ "exam", _exam,	"[addr [words]]", "Examine RAM at AH,AM,AL [or hex addr]" },
	{ "rom", _rom,		"[addr [words]]", "Examine ROM at AH,AM,AL [or hex addr]" },
	{ "set", _set,		"reg=value", "Set register" },
	{ "store", _store,	"[@addr] val...", "Store hex val(s) in RAM at AH,AM,AL [or hex addr]" },
	{ "step", _step,	NULL, "Single-step one instruction" },
	{ "keyboard", _keyboard,"", "Hack to provide input when no GUI" },
	{ "core", _core,	"file", "Dump all of RAM (2K) to <file>" },
	{ "dup", _dup,		"", "Copy program space into ROM" },
	{ "go", _go,		"[+cycles]", "Resume program at current PC [break after <cycles>]" },
	{ "systrc", _systrc,	"[[off] pattern]", "Enable tracing of sys mem bytes" },
	{ "keytrc", _keytrc,	"on/off", "Enable tracing of key presses" },
	{ "help", _help,	NULL, "Display this help" },
};
#define NUM_CMDS	(sizeof(commands) / sizeof(commands[0]))

static int _help(w600_sys_t *sys, char *line) {
	int x, m = 0, n;

	for (x = 0; x < NUM_CMDS; ++x) {
		n = strlen(commands[x].cmd);
		if (commands[x].arg_help) {
			n += strlen(commands[x].arg_help);
		}
		if (n > m) m = n;
	}
	printf(	"W600-SIM Commands:\n");
	for (x = 0; x < NUM_CMDS; ++x) {
		n = strlen(commands[x].cmd);
		printf("  %s %-*s %s\n", commands[x].cmd, m - n,
			commands[x].arg_help ? commands[x].arg_help : "",
			commands[x].help);
	}
	return 0;
}

int sys_command(w600_sys_t *sys) {
	char buf[128];
	int x;

	printf("%% ");
	fflush(stdout);
	char *s = fgets(buf, sizeof(buf), stdin);
	if (!s) {
		printf("w600-sim done.\n");
		return 1;
	}
	x = strlen(s);
	if (!x) return 0;
	--x;
	if (s[x] == '\n') s[x] = '\0';
	s = strtok(buf, " \t");
	if (!s) return 0;
	for (x = 0; x < NUM_CMDS; ++x) {
		if (strcmp(s, commands[x].cmd) == 0) {
			break;
		}
	}
	if (!(x < NUM_CMDS)) {
		printf("%s ?\n", s);
		return 0;
	}
	if (!commands[x].func) {
		printf("w600-sim done.\n");
		return 1;
	}
	int rc = commands[x].func(sys, buf);
	if (rc) {
		// what is this for?
		return rc;
	}
	return 0;
}
