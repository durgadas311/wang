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
	NSEGS,
};
#define HORIZ_SEGS	((1 << seg_a) | (1 << seg_d) | (1 << seg_g))

struct display {
	int nseg;
	int xseg; /* dp, other special "segments" */
	int cell[2];
	int ascent;
	int descent;
	void (*drawseg)(struct display *, unsigned int, enum segments);
};

extern void go_to(int x, int y);
extern void move_to(int dx, int dy);
extern void draw_to(int dx, int dy);
extern void close_to();
extern void arc_to(int q, int r);
extern void circ_at(int x, int y, int r);
extern void poly_at(int x, int y, int poly[][2], int n);
