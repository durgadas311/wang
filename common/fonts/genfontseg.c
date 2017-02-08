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

/* TODO: how to import these? */
extern struct display display_ppx9;
extern struct display display_ftb5;

static float _x, _y;
static float _sx, _sy;

void go_to(int x, int y) {
	_sx = _x = x;
	_sy = _y = y;
	printf("%g %g m 0\n", _x, _y);
}

void move_to(int dx, int dy) {
	_x += dx;
	_y += dy;
	printf("%g %g m 0\n", _x, _y);
}

void draw_to(int dx, int dy) {
	_x += dx;
	_y += dy;
	printf(" %g %g l 1\n", _x, _y);
}

void close_to() {
	_x = _sx;
	_y = _sy;
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

void circ_at(int x, int y, int r) {
	int c;
	go_to(x, y);
	for (c = 0; c < 4; ++c) {
		arc_to(c, r);
	}
}

void poly_at(int x, int y, int poly[][2], int n) {
	int p;
	go_to(x, y);
	for (p = 0; p < n; ++p) {
		draw_to(poly[p][0], poly[p][1]);
	}
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
	for (seg = seg_a; seg < dsp->nseg; ++seg) {
		if ((sc->segs & (1 << seg)) == 0) {
			continue;
		}
		dsp->drawseg(dsp, sc->segs, seg);
	}
	for (seg = 0; seg < dsp->xseg; ++seg) {
		enum segments sg = seg + NSEGS;
		if ((sc->segs & (1 << sg)) == 0) {
			continue;
		}
		dsp->drawseg(dsp, sc->segs, sg);
	}
	printf("EndSplineSet\n");
	printf("EndChar\n");
}

int load_char(char **line, struct seg_chars *sc, struct display *dsp) {
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
		if (s == dsp->nseg) {
			s = NSEGS;
		}
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

struct seg_chars *load_chars(char *buf, int *_nc, int *_mx, struct display *dsp) {
	int nc = 0;
	int mx = 0;
	char *s = buf;
	struct seg_chars *sc = (struct seg_chars *)buf;
	while (*s != 0) {
		if (load_char(&s, sc, dsp) == 0) {
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

static void preamble(struct display *dsp) {
	printf(	"SplineFontDB: 3.0\n"
		"FontName: Untitled1\n"
		"FullName: Untitled1\n"
		"FamilyName: Untitled1\n"
		"Weight: Medium\n"
		"Copyright: Created by Douglas Miller,,, with FontForge 2.0 (http://fontforge.sf.net)\n"
		"UComments: \"2017-2-1: Created.\" \n"
		"Version: 001.000\n"
		"ItalicAngle: 0\n"
		"UnderlinePosition: -100\n"
		"UnderlineWidth: 50\n"
		"Ascent: %d\n"
		"Descent: %d\n"
		"LayerCount: 2\n"
		"Layer: 0 0 \"Back\"  1\n"
		"Layer: 1 0 \"Fore\"  0\n"
		"XUID: [1021 590 1989546996 919824]\n"
		"FSType: 0\n"
		"OS2Version: 0\n"
		"OS2_WeightWidthSlopeOnly: 0\n"
		"OS2_UseTypoMetrics: 1\n"
		"CreationTime: 1454106881\n"
		"ModificationTime: 1454113539\n"
		"OS2TypoAscent: 0\n"
		"OS2TypoAOffset: 1\n"
		"OS2TypoDescent: 0\n"
		"OS2TypoDOffset: 1\n"
		"OS2TypoLinegap: 90\n"
		"OS2WinAscent: 0\n"
		"OS2WinAOffset: 1\n"
		"OS2WinDescent: 0\n"
		"OS2WinDOffset: 1\n"
		"HheadAscent: 0\n"
		"HheadAOffset: 1\n"
		"HheadDescent: 0\n"
		"HheadDOffset: 1\n"
		"OS2Vendor: 'PfEd'\n"
		"DEI: 91125\n"
		"Encoding: ISO8859-1\n"
		"UnicodeInterp: none\n"
		"NameList: Adobe Glyph List\n"
		"DisplaySize: -24\n"
		"AntiAlias: 1\n"
		"FitToEm: 1\n"
		"WinInfo: 16 16 15\n",
		dsp->ascent, dsp->descent);
}

int main(int argc, char **argv) {
	int c;
	int x, y;
	int fd = -1;
	struct stat stb;
#if 0
	struct display *dsp = &display_ppx9;
#else
	struct display *dsp = &display_ftb5;
#endif

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
	struct seg_chars *sc = load_chars(buf, &nc, &mx, dsp);
	if (nc < 1) {
		return 0; // print something?
	}

	preamble(dsp);
	printf("\nBeginChars: %d %d\n\n", mx, nc);
	for (x = 0; x < nc; ++x) {
		do_char(x, &sc[x], dsp);
	}
	printf("\nEndChars\n"
		"EndSplineFont\n");

	return 0;
}
