// Copyright (c) 2011 Douglas Miller

#ident "$Id: w700-sim.c,v 1.2 2011/10/27 21:39:09 drmiller Exp $"

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>

#include "w700_sys.h"

w700_sys_t sys;

static void set_intr(void) {
	struct sigaction sa;
	extern void sig_intr(int sig);
	sa.sa_handler = sig_intr;
	sa.sa_flags = SA_RESTART;
	sigemptyset(&sa.sa_mask);
	sigaction(SIGINT, &sa, NULL);
}

void sig_intr(int sig) {
	if (sys.intr && sys.intr(&sys, sig) == 0) {
		return;
	}
	sys.fault(&sys, "\nInterrupt");
	exit(1);
}

static char *argv0 = NULL;

static void usage() {
	fprintf(stderr, "Usage: "
		"%s [options]\n"
		"Options:\n"
		"\t-M model\tRun simulation for model, default \"700-14TP\"\n"
		"\t-b\tBack-end mode (for GUI FE)\n"
		"\t-g\tSpawn GUI (normally GUI spawns w700-sim)\n"
		"\t-i\tInteractive mode enable\n"
		"\t-c file\tUse file as a cassette tape\n"
		"\t-p file\tLoad initial contents of Program Space (conflicts with -m)\n"
		"\t-m file\tLoad initial contents of RAM. 2048 bytes, Lo nibble in [0]\n"
		"\t-r file\tLoad initial contents of ROM. 2048 bytes, Lo nibble in [0]\n"
		"\t\tNOTE: RAM/ROM contents are reverse order for program steps.\n"
		"\t\tROM addr FFF is program step 0000 (RAM F6F is step 0000)\n"
		"\t-t file\tTurn on TRACE to file, '-' for stderr (huge file alert!)\n"
		"\t-p file\tUse microcode image in 'file', default 'ROM720C.txt'\n"
		"\t-l addr\tLoad microcode into 'addr', default 000\n"
		"\t-e addr\tStart running at 'addr', default 000\n"
		,argv0);
}

int main(int argc, char **argv) {
	sys_init(&sys);
	int x;
	uint16_t entry = -1, load = -1;
	char *pgm = NULL;
	char *ucode = NULL;
	int interact = 0;
	int sys_ops = 0;
	char *ram = NULL;
	char *rom = NULL;
	char *cass = NULL;
	char *model = NULL;

	extern char *optarg;
	extern int optind, opterr, optopt;

	argv0 = argv[0];
	while ((x = getopt(argc, argv, "bc:e:gil:m:M:p:r:t:u:")) != EOF) {
		switch(x) {
		case 'b':
			sys_ops |= SYS_BACK_END;
			break;
		case 'c':
			cass = optarg;
			break;
		case 'e':
			entry = strtoul(optarg, NULL, 0);
			break;
		case 'g':
			sys_ops |= SYS_START_GUI;
			break;
		case 'i':
			interact = 1;
			break;
		case 'l':
			load = strtoul(optarg, NULL, 0);
			break;
		case 'm':
			ram = optarg;
			break;
		case 'M':
			model = optarg;
			break;
		case 'p':
			pgm = optarg;
			break;
		case 'r':
			rom = optarg;
			break;
#ifdef TRACE
		case 't':
			if (strcmp(optarg, "-") == 0) {
				sys.trc_fp = stderr;
			} else {
				sys.trc_fp = fopen(optarg, "w");
				if (!sys.trc_fp) {
					perror(optarg);
					exit(1);
				}
			}
			sys.trace = 1;
			break;
#endif // TRACE
		case 'u':
			ucode = optarg;
			break;
		default:
			usage();
			exit(1);
			break;
		}
	}
	if (model == NULL) model = "700-14TP";
	char *s = model;
	if (strncmp(s, "700-", 4) == 0) s += 4;
	/* for now, assume TP */
	x = strtoul(s, NULL, 10);
	switch(x) {
	case 14:
		sys_ops |= (SYS_MODEL700_14TP << SYS_MODEL_SHIFT);
		break;
	case 6:
		sys_ops |= (SYS_MODEL700_6TP << SYS_MODEL_SHIFT);
		break;
	case 2:
		sys_ops |= (SYS_MODEL700_2TP << SYS_MODEL_SHIFT);
		break;
	default:
		fprintf(stderr, "unknown model \"%s\"\n", model);
		exit(1);
	}
	sys_start(&sys, sys_ops);
	if (load == (uint16_t)-1) {
		load = 0x0000;
	}
	if (entry == (uint16_t)-1) {
		entry = load;
	}
	if (ucode == NULL) {
		ucode = "ROM720C.txt";
	}

	set_intr();

	if (ucode) {
		sys_loaducode(&sys, ucode, load, entry);
	}
	if (ram) {
		sys_loadram(&sys, ram);
	}
	if (rom) {
		sys_loadram(&sys, rom);
	}
	if (pgm) {
		sys_loadpgm(&sys, pgm);
	}
	if (cass) {
		sys_setcass(&sys, cass);
	}

	sys.cmd = (interact ? 1 : 0);
	sys.run = (interact ? 0 : 1);
	sys_go(&sys, entry);
	sys_stop(&sys, sys_ops);
	return 0;
}
