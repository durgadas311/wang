// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class CardDeck extends JPanel {
	static final Color buff1 = new Color(243, 226, 182);

	private static final int bdw = 3;	// width of BevelBorder
	private Color clr;
	private boolean topDown;
	private boolean leftRight;
	private int width;
	private int height;
	private int rule;
	private int stack;

	private int cards = 0; // number of cards present
	private int total = 0; // total number of cards
	private String count = "";

	public CardDeck(int wid, int hit, boolean topDown) {
		super();
		this.topDown = topDown;
		if (wid < 0) {
			leftRight = true;
			wid = -wid;
			rule = wid;
		} else {
			rule = hit;
		}
		width = wid;
		height = hit;
		setPreferredSize(new Dimension(wid + 2 * bdw, hit + 2 * bdw));
		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		setBackground(Color.white);
		clr = buff1;
		initDeck(0, 0);
	}

	public void initDeck(int cds, int max) {
		if (max <= 0) {
			max = cds = 0;
		}
		if (cds < 0) cds = 0;
		if (cds > max) cds = max;
		cards = cds;
		total = max;
		updateDeck();
	}

	public void updateCards(int cds) {
		if (cds < 0) {
			cds = 0;
		} else if (cds >= total) {
			cds = total;
		}
		if (cards == cds) return;
		cards = cds;
		updateDeck();
	}

	public int getNumCards() { return cards; }

	private void updateDeck() {
		if (cards > 0) { // implies total > 0 (?)
			count = String.format("%d", cards);
			stack = (int)Math.round(((double)cards / total) * rule);
		} else {
			count = "";
			stack = 0;
		}
		repaint();
	}

	private void horiz(Graphics2D g2d, int n) {
		boolean max = false;
		if (n > width) {
			max = true;
			n = width;
		}
		if (n > 0) {
			if (topDown) {	// i.e. right-fill
				g2d.fillRect(width - n, 0, width + 1, height + 1);
				if (max) {
					g2d.setColor(Color.red);
					g2d.drawLine(0, 0, 0, height);
				}
			} else {	// i.e. left-fill
				g2d.fillRect(0, 0, n + 1, height + 1);
				if (max) {
					g2d.setColor(Color.red);
					g2d.drawLine(width, 0, width, height);
				}
			}
		}
	}

	private void vert(Graphics2D g2d, int n) {
		boolean max = false;
		if (n > height) {
			max = true;
			n = height;
		}
		if (n > 0) {
			if (topDown) {
				g2d.fillRect(0, 0, width + 1, n + 1);
				if (max) {
					g2d.setColor(Color.red);
					g2d.drawLine(0, height, width, height);
				}
			} else {
				g2d.fillRect(0, height - n, width + 1, n + 1);
				if (max) {
					g2d.setColor(Color.red);
					g2d.drawLine(0, 0, width, 0);
				}
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (total == 0) return;
		Graphics2D g2d = (Graphics2D)g;
		g2d.translate(bdw, bdw);	// based on border width
		g2d.setColor(clr);
		if (leftRight) {
			horiz(g2d, stack);
		} else {
			vert(g2d, stack);
		}
		g2d.setColor(Color.black);
		if (cards > 0) {
			g2d.drawString(count, width / 2 - 5, height / 2 + 5);
		}
	}
}
