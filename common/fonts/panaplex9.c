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

#include "genfontseg.h"

static void draw_ppx9(struct display *, unsigned int, enum segments);
struct display display_ppx9 = {
.nseg = 9,
.cell = { 1450, 2048 },
.ascent = 1556,
.descent = 492,
.drawseg = draw_ppx9,
};

static int origins[][2] = {
[seg_a] = { 374,1436 },
[seg_b] = { 1024,788 },
[seg_c] = { 918,42 },
[seg_d] = { 174,0 },
[seg_e] = { 46,42 },
[seg_f] = { 152,788 },
[seg_g] = { 274,718 },
[seg_h] = { 588,788 },
[seg_i] = { 482,42 },
[seg_dp] = { 1056,-100 },
};

static int horiz_seg[4][2] = {
	{ 16,120 },
	{ 728,0 },
	{ -16,-120 },
	{ -728,0 },
};

static int vert_seg[4][2] = {
	{ 101,728 },	// { 78,562 } / { 76,558 } = { -23,-166 } / { -25,-170 }
	{ 120,0 },
	{ -101,-728 },	// {-78,-562 } / { -76,-558 }
	{ -120,0 },
};

static int fix_org_h[2] = { 600,856 };
static int fix_seg_h[4][2] = {
	{ 78,562 },
	{ 120,0 },
	{ -78,-562 },
	{ -120,0 },
};

static int fix_org_i[2] = { 500,140 };
static int fix_seg_i[4][2] = {
	{ 76,558 },
	{ 120,0 },
	{ -76,-558 },
	{ -120,0 },
};

static void do_dp(int x, int y) {
	int c;
	move_to(x, y);
	for (c = 0; c < 4; ++c) {
		arc_to(c, 100);
	}
}

static void do_seg(int x, int y, int seg[4][2]) {
	int c;
	move_to(x, y);
	for (c = 0; c < 4; ++c) {
		draw_to(seg[c][0], seg[c][1]);
	}
}

static void do_segment(enum segments seg) {
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
	case seg_h:
	case seg_i:
		do_seg(x, y, vert_seg);
		break;
	case seg_dp:
		do_dp(x, y);
		break;
	}
}

static void draw_ppx9(struct display *dsp, unsigned int segs, enum segments seg) {
	if (seg == seg_h) {
		if ((segs & HORIZ_SEGS) == 0) {
			do_segment(seg);
		} else {
			do_seg(fix_org_h[0], fix_org_h[1], fix_seg_h);
		}
	} else if (seg == seg_i) {
		if ((segs & HORIZ_SEGS) == 0) {
			do_segment(seg);
		} else {
			do_seg(fix_org_i[0], fix_org_i[1], fix_seg_i);
		}
	} else {
		do_segment(seg);
	}
}
