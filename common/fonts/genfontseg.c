#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

/*
 * File format:
 *	<chr> <a><b><c><d><e><f><g>[<h><i>]<dp>[<dpl>]
 *
 *	'chr' is glyph code (0xHH, DD, 0OOO)
 *	segments are binary values (1 = on)
 *
 *          --a--               --a--
 *       f /    / b          f / /h / b
 *         --g--               --g--
 *      e /    / c          e / /i / c
 *        --d--              --d--
 *              dp                 dp
 *
 * segment splines:
 *
 *    B +---------------------+ C
 *     /      728x120 (+16)  / 
 *  A +---------------------+ D
 *
 *     A =    0,    0 (start)
 *     B =  +16, +120
 *     C = +728,   +0
 *     D =  -16, -120
 *         -728,   -0 (back to A)
 *
 * Origin, d: 174,0
 * Spacing, d-g or g-a (pt A): +100,+718
 * ---
 *          F +----+ G
 *            /    /
 *           /    /
 *          /    /
 *         /    /
 *        /    /
 *       /    /
 *      /    /
 *    E +----+ H
 *
 *     E =     0,    0 (start)
 *     F =  +101, +728
 *     G =  +120,   +0
 *     H =  -101, -728
 *          -120,   -0 (back to E)
 *
 * Origin (e): 46,42
 * Spacing e-f, i-h, or c-b:      +106,+754
 *         e-i, i-c, f-h, or h-b: +436,+0
 * ---
 * dp: (0,0) (+0,+51.888);(+22.1,+94);(+94,+0)c0
 */

enum segments {
	seg_a = 0,
	seg_b,
	seg_c,
	seg_d,
	seg_e,
	seg_f,
	seg_g,
	seg_h,
	seg_i,
	seg_dp,
	NSEGS,
	seg_xh = NSEGS,
	seg_xi,
};
#define HORIZ_SEGS	((1 << seg_a) | (1 << seg_d) | (1 << seg_g))

// original absolute char origin is 46,0
// original char cell width is 1450
int cell[2] = { 1450, 2048 };
int ascent = 1556;
int descent = 492;
int origins[][2] = {
[seg_a] = { 374,1436 },
[seg_b] = { 1024,788 },
[seg_c] = { 918,42 },
[seg_d] = { 174,0 },
[seg_e] = { 46,42 },
[seg_f] = { 152,788 },
[seg_g] = { 274,718 },
[seg_h] = { 600,856 },
[seg_i] = { 500,140 },
[seg_dp] = { 1056,-100 },
[seg_xh] = { 588,788 },	// { 600,856 } = { +12,+68 }
[seg_xi] = { 482,42 },	// { 500,140 } = { +18,+98 }
};

int horiz_seg[4][2] = {
	{ 16,120 },
	{ 728,0 },
	{ -16,-120 },
	{ -728,0 },
};

int vert_seg[4][2] = {
	{ 101,728 },	// { 78,562 } / { 76,558 } = { -23,-166 } / { -25,-170 }
	{ 120,0 },
	{ -101,-728 },	// {-78,-562 } / { -76,-558 }
	{ -120,0 },
};

int fix_seg_h[4][2] = {
	{ 78,562 },
	{ 120,0 },
	{ -78,-562 },
	{ -120,0 },
};

int fix_seg_i[4][2] = {
	{ 76,558 },
	{ 120,0 },
	{ -76,-558 },
	{ -120,0 },
};

float dp_seg[4][6] = {
	{ 0,55.2,  44.8,100,  100,100 }, // next = start + last
	{ 55.2,0,  100,-44.8, 100,-100 },
	{ 0,-55.2, -44.8,-100, -100,-100 },
	{ -55.2,0, -100,+44.8, -100,100 },
};

void do_dp(int x, int y) {
	float fx, fy;
	fx = x;
	//y += 100;
	fy = y;
	int c;
	printf("%d %d m 0\n", x, y);
	for (c = 0; c < 4; ++c) {
		printf(" %g %g %g %g %g %g c 0\n",
			fx + dp_seg[c][0],
			fy + dp_seg[c][1],
			fx + dp_seg[c][2],
			fy + dp_seg[c][3],
			fx + dp_seg[c][4],
			fy + dp_seg[c][5]);
		fx += dp_seg[c][4];
		fy += dp_seg[c][5];
	}
}

void do_seg(int x, int y, int seg[4][2]) {
	int c;
	printf("%d %d m 0\n", x, y);
	for (c = 0; c < 4; ++c) {
		printf(" %d %d l 1\n", x + seg[c][0], y + seg[c][1]);
		x += seg[c][0];
		y += seg[c][1];
	}
}

void do_segment(enum segments seg) {
	int x = origins[seg][0];
	int y = origins[seg][1];
	switch(seg) {
	case seg_a:
	case seg_d:
	case seg_g:
		do_seg(x, y, horiz_seg);
		break;
	case seg_b:
	case seg_c:
	case seg_e:
	case seg_f:
	case seg_xh:
	case seg_xi:
		do_seg(x, y, vert_seg);
		break;
	case seg_h:
		do_seg(x, y, fix_seg_h);
		break;
	case seg_i:
		do_seg(x, y, fix_seg_i);
		break;
	case seg_dp:
		do_dp(x, y);
		break;
	}
}

struct seg_chars {
	int chr;
	unsigned int segs;
};

void do_char(int cn, struct seg_chars *sc) {
	printf("StartChar: uni%04X\n", sc->chr);
	printf("Encoding: %d %d %d\n", sc->chr, sc->chr, cn); // what is 3rd number?
	printf("Width: %d\n", cell[0]);
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
		if (seg == seg_h && (sc->segs & HORIZ_SEGS) == 0) {
			do_segment(seg_xh);
		} else if (seg == seg_i && (sc->segs & HORIZ_SEGS) == 0) {
			do_segment(seg_xi);
		} else {
			do_segment(seg);
		}
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

	printf("Ascent: %d\n"
		"Descent: %d\n"
		"BeginChars: %d %d\n",
		ascent, descent, mx, nc);
	for (x = 0; x < nc; ++x) {
		do_char(x, &sc[x]);
	}
	
	printf("\nEndChars\n"
		"EndSplineFont\n");

	return 0;
}
