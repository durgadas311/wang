#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdlib.h>

/*
 */

#include "genfontseg.h"

#define seg_am	(NSEGS + 0)	/* "AM" */
#define seg_pm	(NSEGS + 1)	/* "PM" */
#define seg_co	(NSEGS + 2)	/* ":" */

static void draw_ftb5(struct display *, unsigned int, enum segments);
struct display display_ftb5 = {
.nseg = 7,
.xseg = 3, /* TODO: special AM, PM, : "segments" */
.cell = { 1450, 2048 },
.ascent = 1556,
.descent = 492,
.drawseg = draw_ftb5,
};

static int origins[][2] = {
[seg_a] = { 0,1854 },
[seg_b] = { 1170, 1120 },
[seg_c] = { 1170,30 },
[seg_d] = { 170,0 },
[seg_e] = { 0,220 },
[seg_f] = { 85,1036 },
[seg_g] = { 110,1024 },
[seg_am] = { 0,0 },
[seg_pm] = { 0,0 },
[seg_co] = { 0,0 },
};

static void draw_a() {
	go_to(origins[seg_a][0], origins[seg_a][1]);
	draw_to(0, 20);
	arc_to(0, 170);
	draw_to(813, 0);
	draw_to(0, -170);
	draw_to(-793, 0);
	/* draw_to(-20, -20); */
	arc_to(4, 20);
	close_to();
}

static void draw_b() {
	go_to(origins[seg_b][0], origins[seg_b][1]);
	draw_to(-85, -85);
	draw_to(-85, 85);
	draw_to(0, 925);
	arc_to(1, 170);
	close_to();
}

static void draw_c() {
	go_to(origins[seg_c][0], origins[seg_c][1]);
	draw_to(-170, 170);
	draw_to(0, 719);
	draw_to(85, 85);
	draw_to(85, -85);
	close_to();
}

static void draw_d() {
	go_to(origins[seg_d][0], origins[seg_d][1]);
	arc_to(3, 170);
	draw_to(0, 20);
	draw_to(170, 0);
	/* draw_to(20, -20); */
	arc_to(7, 20);
	draw_to(790, 0);
	draw_to(170, -170);
	close_to();
}

static void draw_f() {
	go_to(origins[seg_f][0], origins[seg_f][1]);
	draw_to(-85, 85);
	draw_to(0, 719);
	draw_to(170, 0);
	draw_to(0, -719);
	close_to();
}

static void draw_e() {
	go_to(origins[seg_e][0], origins[seg_e][1]);
	draw_to(0, 719);
	draw_to(85, 85);
	draw_to(85, -85);
	draw_to(0, -719);
	close_to();
}

static void draw_g() {
	go_to(origins[seg_g][0], origins[seg_g][1]);
	draw_to(85, 85);
	draw_to(790, 0);
	draw_to(85, -85);
	draw_to(-85, -85);
	draw_to(-790, 0);
	close_to();
}

static void draw_am() {
}

static void draw_pm() {
}

static void draw_co() {
}

static void draw_ftb5(struct display *dsp, unsigned int segs, enum segments seg) {
	switch(seg) {
	case seg_a:
		draw_a();
		break;
	case seg_d:
		draw_d();
		break;
	case seg_g:
		draw_g();
		break;
	case seg_b:
		draw_b();
		break;
	case seg_c:
		draw_c();
		break;
	case seg_e:
		draw_e();
		break;
	case seg_f:
		draw_f();
		break;
	case seg_am:
		draw_am();
		break;
	case seg_pm:
		draw_pm();
		break;
	case seg_co:
		draw_co();
		break;
	}
}
