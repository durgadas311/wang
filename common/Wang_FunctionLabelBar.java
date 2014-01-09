// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_FunctionLabelBar.java,v 1.2 2014/01/09 17:45:56 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang_FunctionLabelBar extends JPanel {

	static final long serialVersionUID = 311999692037L;

	private JLabel[] f = new JLabel[16];
	private JLabel[] corners = new JLabel[4];
	private int leftNudge = 0;

	private int fnPosition(int x) {
		return x * 50 + 87 + leftNudge;
	}

	private int cnWidth(int x) {
		int xw = 75;
		if ((x & 1) == 1) {
			xw -= leftNudge;
		} else {
			xw += leftNudge;
		}
		return xw;
	}

	private int cnPosition(int x) {
		int xp = 26;
		if ((x & 1) == 1) {
			xp = fnPosition(16);
		}
		return xp;
	}

	// horizontal layout:
	// 975 = full width
	// 1-24 = metal tab (0-25 reserved for hold-down)
	// 25-87            = corner[0] (width = 75 + Nudge)
	// 87 + Nudge + 0   = f(0)  (width = 50)
	// 87 + Nudge + 50  = f(1)  (width = 50)
	// ...
	// 87 + Nudge + 750 = f(15) (width = 50)
	// 87 + Nudge + 800 = corner[1)] (width = 75 - Nudge)
	// 951-974 metal tab (950-975 reserved for hold-down)

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);

		g2d.setColor(Color.black);
		for (int x = 0; x < 17; ++x) {
			int xp = fnPosition(x);
			g2d.drawLine(xp, 0, xp, 50);
			if (x < 16) f[x].setLocation(xp, 0);
		}
		for (int x = 0; x < 4; ++x) {
			int xp = cnPosition(x);
			int yp = ((x / 2) * 25);
			corners[x].setLocation(xp, yp);
		}
		g2d.drawLine(26, 25, 949, 25);
		g2d.setColor(Wang_Colors.white3);
		g2d.fillRect(1, 1, 23, 48);
		g2d.fillRect(951, 1, 23, 48);
		g2d.setColor(Color.black);
		g2d.fillOval(2, 14, 21, 21);
		g2d.fillOval(952, 14, 21, 21);
	}

	public Wang_FunctionLabelBar() {
		boolean w700 = (Wang_UI.getSeries().equals("7"));
		if (w700) {
			leftNudge += 23;
		}
		setPreferredSize(new Dimension(975, 50));
		setBackground(Wang_Colors.ivory);
		Dimension dim = new Dimension(50, 25);
		for (int x = 0; x < 16; ++x) {
			f[x] = new JLabel();
			f[x].setText("f(" + x + ")");
			f[x].setPreferredSize(dim);
			// fiddling with "visible", in order to hide initial
			// positions, seems to screw up final positioning.
			//f[x].setVisible(false);
			add(f[x]);
		}
		// +-------+   +------+
		// |   0   |   |  1   |
		// +-------+...+------+
		// |   2   |   |  3   |
		// +-------+   +------+
		for (int x = 0; x < 4; ++x) {
			int xw = cnWidth(x);
			corners[x] = new JLabel();
			corners[x].setPreferredSize(new Dimension(xw, 25));
			if (x == 0) {
				String txt = "mine";
				if (w700) {
					txt = Integer.toBinaryString(3);
					while (txt.length() < 4) txt = "0" + txt;
					txt = txt.replaceAll("1", " \u2191 ");
					txt = txt.replaceAll("0", " \u2193 ");
				}
				corners[x].setText(txt);
			} else if (x == 1) {
				corners[x].setText("mine2");
			}
			add(corners[x]);
		}
	}
}
