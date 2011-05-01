// $Id: w600-sim.c,v 1.2 2011/05/01 03:49:35 drmiller Exp $

#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <signal.h>

#include "w600_sys.h"

w600_sys_t sys;

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

int main(int argc, char **argv) {
	sys_init(&sys);
	int x;
	uint16_t entry = -1, load = -1;
	char *pgm = NULL;
	int interact = 0;

	extern char *optarg;
	extern int optind, opterr, optopt;

	while ((x = getopt(argc, argv, "e:il:p:t")) != EOF) {
		switch(x) {
		case 'e':
			entry = strtoul(optarg, NULL, 0);
			break;
		case 'l':
			load = strtoul(optarg, NULL, 0);
			break;
		case 'i':
			interact = 1;
			break;
		case 'p':
			pgm = optarg;
			break;
#ifdef TRACE
		case 't':
			sys.trace = 1;
			break;
#endif // TRACE
		default:
			fprintf(stderr, "Usage: "
					"%s [-t] [-i] [-p program [-l load-addr] [-e entry]]\n",
					argv[0]);
			exit(1);
			break;
		}
	}
	if (load == (uint16_t)-1) {
		load = 0x0000;
	}
	if (entry == (uint16_t)-1) {
		entry = load;
	}
	if (pgm == NULL) {
		pgm = "wang600_ucode";
	}

	set_intr();

	if (pgm) {
		sys_loadpgm(&sys, pgm, load, entry);
	}

	sys.run = (interact ? 0 : 1);
	sys_go(&sys, entry);
	return 0;
}
