#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

/*
 * File format:
 *	<chr> <a><b><c><d><e><f><g>[<g2><h><i>...]<dp>[<dpl>]
 *
 *	'chr' is glyph code (0xHH, DD, 0OOO)
 *	segments are binary values (1 = on)
 *
 */

#include "genfontseg.h"

static float _x, _y;

void move_to(int x, int y) {
	_x = x;
	_y = y;
	printf("%g %g m 0\n", _x, _y);
}

void draw_to(int x, int y) {
	_x += x;
	_y += y;
	printf(" %g %g l 1\n", _x, _y);
}

//        q=0  |  q=1
//             |
//      -------+-------
//             |
//        q=3  |  q=2
// ccw, (x,y) already placed (move_to, etc)
static float quads[4][6] = {
	{ 0,0.552,	0.448,1,	1,1 },
	{ 0.552,0,	1,-0.448,	1,-1 },
	{ 0,-0.552,	-0.448,-1,	-1,-1 },
	{ -0.552,0,	-1,0.448,	-1,1 },
};
void arc_to(int q, int r) {
	printf(" %g %g %g %g %g %g c 0\n",
		_x + r * quads[q][0],
		_y + r * quads[q][1],
		_x + r * quads[q][2],
		_y + r * quads[q][3],
		_x + r * quads[q][4],
		_y + r * quads[q][5]);
	_x += r * quads[q][4];
	_y += r * quads[q][5];
}


struct seg_chars {
	int chr;
	unsigned int segs;
};

void do_char(int cn, struct seg_chars *sc, struct display *dsp) {
	printf("StartChar: uni%04X\n", sc->chr);
	printf("Encoding: %d %d %d\n", sc->chr, sc->chr, cn); // what is 3rd number?
	printf("Width: %d\n", dsp->cell[0]);
	printf("VWidth: 0\n");
	printf("Flags: HW\n");
	printf("LayerCount: 2\n");
	printf("Fore\n");
	printf("SplineSet\n");
	enum segments seg = seg_a;
	for (seg = seg_a; seg < NSEGS; ++seg) {
		if ((sc->segs & (1 << seg)) == 0) {
			continue;
		}
		dsp->drawseg(dsp, sc->segs, seg);
	}
	printf("EndSplineSet\n");
	printf("EndChar\n");
}

int load_char(char **line, struct seg_chars *sc) {
	char *next;
	unsigned long c = strtoul(*line, &next, 0);
	if (next == *line || !isblank(*next)) {
		return -1;
	}
	while (isblank(*next)) ++next;
	int s = seg_a;
	unsigned int segs = 0;
	while (*next == '0' || *next == '1') {
		enum segments seg = s++;
		// if (!9seg && (seg == seg_h || seg == seg_i)) continue;
		char n = *next++;
		if (n != '1') {
			continue;
		}
		segs |= (1 << seg);
	}
	while (*next && *next++ != '\n');
	*line = next;
	sc->chr = c;
	sc->segs = segs;
	return 0;
}

struct seg_chars *load_chars(char *buf, int *_nc, int *_mx) {
	int nc = 0;
	int mx = 0;
	char *s = buf;
	struct seg_chars *sc = (struct seg_chars *)buf;
	while (*s != 0) {
		if (load_char(&s, sc) == 0) {
			if (sc->chr > mx) {
				mx = sc->chr;
			}
			++sc;
			++nc;
		}
	}
	if (_nc != NULL) {
		*_nc = nc;
	}
	if (_mx != NULL) {
		*_mx = mx;
	}
	return (struct seg_chars *)buf;
}

int main(int argc, char **argv) {
	int c;
	int x, y;
	int fd = -1;
	struct stat stb;

	extern int optind;
	extern char *optarg;

	while ((x = getopt(argc, argv, "")) != EOF) {
		switch(x) {
		}
	}
	x = optind;

	if (x < argc) {
		fd = open(argv[x], O_RDONLY);
		if (fd >= 0) {
			fstat(fd, &stb);
		}
	}
	if (fd < 0) {
		fprintf(stderr,
			"Usage: %s [options] <decoder-image>\n"
			"Options:\n"
			, argv[0]);
		exit(1);
	}
	char *buf = malloc(stb.st_size + 1);
	read(fd, buf, stb.st_size);
	close(fd);
	buf[stb.st_size] = 0;

	int nc = 0;
	int mx = 0;
	struct seg_chars *sc = load_chars(buf, &nc, &mx);
	if (nc < 1) {
		return 0; // print something?
	}

	extern struct display display_ppx9;
	printf("Ascent: %d\n"
		"Descent: %d\n"
		"BeginChars: %d %d\n",
		display_ppx9.ascent, display_ppx9.descent, mx, nc);
	for (x = 0; x < nc; ++x) {
		do_char(x, &sc[x], &display_ppx9);
	}
	
	printf("\nEndChars\n"
		"EndSplineFont\n");

	return 0;
}
