
// Copyright (c) 2011 Douglas Miller

#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#ident "$Id: w600_ld.c,v 1.1 2011/06/11 02:33:58 drmiller Exp $"

#define W6_ENDPROG	0x9e
#define W6_MARK		0x90
#define W6_SEARCH	0x80
#define W6_RECALL	0x91
#define W6_STORE	0x81
#define W6_PRINT	0x92
#define W6_alpha	0x82
#define W6_RECALL2	0xf0
#define W6_PRINT2	0xf1
#define W6_IO		0xf2
#define W6_SEARCH_ROM	0xf3
#define W6_SEARCH_ROM2	0xf4
#define W6_SEARCH_ROM3	0xf5
#define W6_SEARCH_ROM4	0xf6
#define W6_CALL		0xf7
#define W6_MARK2	0xf8
#define W6_STORE	0xf9
#define W6_alpha2	0xfa
#define W6_INDIR	0xfb
#define W6_CALL_ROM	0xfc
#define W6_GROUP1	0xfd
#define W6_GROUP2	0xfe
#define W6_SEARCH2	0xff
#define W6_fx_m		0xe0
#define W6_fx		0xa0
#define W6_Fx		(W6_fx + 16)
#define W6_fx_ROM	0xc0
#define W6_Fx_ROM	(W6_fx_ROM + 16)

uint8_t cmds[256] = {
[W6_RECALL]	= TWO_STEP,
[W6_STORE]	= TWO_STEP,
[W6_PRINT]	= TWO_STEP,
[W6_alpha]	= TWO_STEP | ALPHA,
#warning alpha is not just two steps...

[W6_MARK]	= TWO_STEP | LABEL,
[W6_SEARCH]	= TWO_STEP | LABEL,

[W6_fx + 0]	= LABEL,
[W6_fx + 1]	= LABEL,
[W6_fx + 2]	= LABEL,
[W6_fx + 3]	= LABEL,
[W6_fx + 4]	= LABEL,
[W6_fx + 5]	= LABEL,
[W6_fx + 6]	= LABEL,
[W6_fx + 7]	= LABEL,
[W6_fx + 8]	= LABEL,
[W6_fx + 9]	= LABEL,
[W6_fx + 10]	= LABEL,
[W6_fx + 11]	= LABEL,
[W6_fx + 12]	= LABEL,
[W6_fx + 13]	= LABEL,
[W6_fx + 14]	= LABEL,
[W6_fx + 15]	= LABEL,
[W6_Fx + 0]	= LABEL,
[W6_Fx + 1]	= LABEL,
[W6_Fx + 2]	= LABEL,
[W6_Fx + 3]	= LABEL,
[W6_Fx + 4]	= LABEL,
[W6_Fx + 5]	= LABEL,
[W6_Fx + 6]	= LABEL,
[W6_Fx + 7]	= LABEL,
[W6_Fx + 8]	= LABEL,
[W6_Fx + 9]	= LABEL,
[W6_Fx + 10]	= LABEL,
[W6_Fx + 11]	= LABEL,
[W6_Fx + 12]	= LABEL,
[W6_Fx + 13]	= LABEL,
[W6_Fx + 14]	= LABEL,
[W6_Fx + 15]	= LABEL,
[W6_fx_ROM + 0]	= LABEL,
[W6_fx_ROM + 1]	= LABEL,
[W6_fx_ROM + 2]	= LABEL,
[W6_fx_ROM + 3]	= LABEL,
[W6_fx_ROM + 4]	= LABEL,
[W6_fx_ROM + 5]	= LABEL,
[W6_fx_ROM + 6]	= LABEL,
[W6_fx_ROM + 7]	= LABEL,
[W6_fx_ROM + 8]	= LABEL,
[W6_fx_ROM + 9]	= LABEL,
[W6_fx_ROM + 10]= LABEL,
[W6_fx_ROM + 11]= LABEL,
[W6_fx_ROM + 12]= LABEL,
[W6_fx_ROM + 13]= LABEL,
[W6_fx_ROM + 14]= LABEL,
[W6_fx_ROM + 15]= LABEL,
[W6_Fx_ROM + 0]	= LABEL,
[W6_Fx_ROM + 1]	= LABEL,
[W6_Fx_ROM + 2]	= LABEL,
[W6_Fx_ROM + 3]	= LABEL,
[W6_Fx_ROM + 4]	= LABEL,
[W6_Fx_ROM + 5]	= LABEL,
[W6_Fx_ROM + 6]	= LABEL,
[W6_Fx_ROM + 7]	= LABEL,
[W6_Fx_ROM + 8]	= LABEL,
[W6_Fx_ROM + 9]	= LABEL,
[W6_Fx_ROM + 10]= LABEL,
[W6_Fx_ROM + 11]= LABEL,
[W6_Fx_ROM + 12]= LABEL,
[W6_Fx_ROM + 13]= LABEL,
[W6_Fx_ROM + 14]= LABEL,
[W6_Fx_ROM + 15]= LABEL,

[W6_RECALL2]	= TWO_STEP,
[W6_PRINT2]	= TWO_STEP,
[W6_IO]		= TWO_STEP,
[W6_SEARCH_ROM]	= TWO_STEP | LABEL,
[W6_SEARCH_ROM2]= TWO_STEP | LABEL,
[W6_SEARCH_ROM3]= TWO_STEP | LABEL,
[W6_SEARCH_ROM4]= TWO_STEP | LABEL,
[W6_CALL]	= TWO_STEP | LABEL,
[W6_MARK2]	= TWO_STEP,
[W6_STORE]	= TWO_STEP,
[W6_alpha2]	= TWO_STEP | ALPHA,
[W6_INDIR]	= TWO_STEP,
[W6_CALL_ROM]	= TWO_STEP | LABEL,
[W6_GROUP1]	= TWO_STEP,
[W6_GROUP2]	= TWO_STEP,
[W6_SEARCH2]	= TWO_STEP | LABEL,
};

typedef struct {
	off_t off;
} w6_sym_t;

typedef struct {
	char *name;	// came from argv[]...
	uint8_t *img;
	off_t siz;
	w6_sym_t *sym;
	int nsym;
} w6_file_t;

uint8_t get_lab(uint8_t *step) {
	// assert((cmds[*step] & LABEL));
	if ((cmds[step[0]] & TWO_STEP)) {
		return step[1];
	} else {
		return step[0];
	}
}

int next_step(w6_file_t *f, int x) {
	if ((cmds[f->img[x]] & TWO_STEP)) {
		if ((cmds[f->img[x]] & ALPHA) && f->img[x+1] < 0x80) {
			while (x < f->siz && f->img[x] != 0x22) ++x;
		} else {
			++x;
		}
	}
	return x;
}

void bld_symtab(w6_file_t *f) {
	int x, n;

	for (x = 0; x < f->siz; ++x) {
		if ((cmds[f->img[x]] & LABEL)) {
			++n;
		}
		x = next_step(f, x);
	}
	f->nsym = n;
	if (n == 0) {
		return;
	}
	f->sym = malloc(n * sizeof(w6_sym_t));
	for (x = 0; x < f->siz; ++x) {
		if ((cmds[f->img[x]] & LABEL)) {
			f->sym[n++].off = x;
		}
		x = next_step(f, x);
	}
	// now sort by label...
}

int main(int argc, char **argv) {
	char *s, *t;
	int ram = 1;
	char *outfile = NULL;

	while ((x = getopt(argc, argv, "o:R")) != EOF) {
		switch(x) {
		case 'o':	// output file
			outfile = optarg;
			break;
		case 'R':	// target ROM
			ram = 0;
			break;
		}
	}
	argv += optind;
	argc -= optind; // right?

	w6_file_t *f = malloc(argc * sizeof(w6_file_t));

	for (x = 0; x < argc; ++x) {
		f[x].name = argv[x];
		fd = open(f[x].name, O_RDONLY);
		fstat(fd, &stb);
		f[x].siz = stb.st_size;
		f[x].img = malloc(f[x].siz);
		read(fd, f[x].img, f[x].siz);
		close(fd);
		bld_symtab(&f[x]);
	}

	exit(0);
}
