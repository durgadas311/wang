// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_FunctionLabelBar.java,v 1.3 2014/01/09 23:01:19 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

// file format:
//
// [fn set: 0,1,2,3]
//     [f0 label]
//     ...
//     [f15 label]
// [fn set, labels...]
//
// Blanks lines before "fn set", or after "f15 label", are ignored.
//
// Label text may contain HTML.
//
// For 500/600, fn set translates:
//   val  func            settings
//    0 = f(x)            f(x)
//    1 = F(x)     a.k.a. SHIFT f(x)
//    2 = ROM f(x) a.k.a. [8]+[4]
//    3 = ROM F(x) a.k.a. SHIFT [8]+[4]
//
// (For 700, 0-3 are toggle switch settings)
//
// Todo: 1) support arbitrary (0-15) "fn set" (for use as SEARCH labels).
//       2) dynamically alter labels when switch settings change.
//       3) associate label files with program images and load automatically.
//       4) generate label file from symtab of linked program (wpcc).

class Wang_FunctionLabelBar extends JPanel
		implements MouseListener {

	static final long serialVersionUID = 311999692037L;
	boolean _w700;
	File currLabel = null;

	static final Dimension dim = new Dimension(50, 25);

	// currently, only two rows may be loaded at a time.
	private JLabel[] f = new JLabel[16];
	private JLabel[] F = new JLabel[16];

	private JLabel[] corners = new JLabel[4];
	private int leftNudge = 0;

	private int fnPosition(int x) {
		return x * 50 + 87 + leftNudge;
	}

	private int cnWidth(int x) {
		int xw = 62;
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
	// 25-87            = corner[0] (width = 62 + Nudge)
	// 87 + Nudge + 0   = f(0)  (width = 50)
	// 87 + Nudge + 50  = f(1)  (width = 50)
	// ...
	// 87 + Nudge + 750 = f(15) (width = 50)
	// 87 + Nudge + 800 = corner[1)] (width = 62 - Nudge)
	// 951-974 metal tab (950-975 reserved for hold-down)

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);

		g2d.setColor(Color.black);
		for (int x = 0; x < 17; ++x) {
			int xp = fnPosition(x);
			g2d.drawLine(xp, 0, xp, 50);
			if (x < 16) {
				f[x].setLocation(xp, 0);
				F[x].setLocation(xp, 25);
			}
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

	private void loadLabels(File file) {
		if (file == null) {
			for (int x = 0; x < 16; ++x) {
				f[x].setText("");
				F[x].setText("");
			}
			for (int x = 0; x < 4; ++x) {
				corners[x].setText("");
			}
			return;
		}
		int lno = 0;
		try {
			BufferedReader in = new BufferedReader(new
				InputStreamReader(new FileInputStream(file)));
			int x = -1; // am between label sections
			int z = 0;
			while (z <= 4) {
				String s = in.readLine();
				if (s == null) break;
				++lno;
				if (s.length() > 0 && s.charAt(0) == '#') continue;
				if (x < 0) {
					if (s.length() == 0) continue;
					corners[z].setText(s);
					z += 2;
					x = 0;
					continue;
				} else if (x >= 16) {
					corners[z - 1].setText(s);
					x = -1;
					continue;
				}
				if (z == 2) {
					f[x].setText(s);
				} else {
					F[x].setText(s);
				}
				++x;
			}
			currLabel = file;
		} catch (Exception ee) {
			// Warning?
System.err.println("Line " + lno + ": " + ee.toString());
			return;
		}
	}

	private void pickLabelFile() {
		SuffFileChooser ch = new SuffFileChooser("Load Key Labels",
			"wfl", "Function Label Files", Wang_UI.getDir());
		if (currLabel != null) {
			ch.setSelectedFile(currLabel);
		}
		File file = null;
		int rv = ch.showDialog(null);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
		} else {
			file = null;
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().newInstance(),
				"wang_function_labels",
				file == null ? "" : file.getName());
		} catch(Exception ee) {}
		loadLabels(file);
	}

	public Wang_FunctionLabelBar() {
		_w700 = (Wang_UI.getSeries().equals("7"));
		if (_w700) {
			leftNudge += 23;
		}
		setPreferredSize(new Dimension(975, 50));
		setBackground(Wang_Colors.ivory);
		addMouseListener(this);
		for (int x = 0; x < 16; ++x) {
			f[x] = new JLabel("", SwingConstants.CENTER);
			f[x].setPreferredSize(dim);
			add(f[x]);
			F[x] = new JLabel("", SwingConstants.CENTER);
			F[x].setPreferredSize(dim);
			add(F[x]);
		}
		// +-------+   +------+
		// |   0   |   |  1   |
		// +-------+...+------+
		// |   2   |   |  3   |
		// +-------+   +------+
		for (int x = 0; x < 4; ++x) {
			int xw = cnWidth(x);
			corners[x] = new JLabel("", SwingConstants.CENTER);
			corners[x].setPreferredSize(new Dimension(xw, 25));
			add(corners[x]);
		}
		loadLabels(Wang_UI.getProperties().getFile("wang_function_labels",
							true, Wang_UI.getDir()));
	}

	public void mouseClicked(MouseEvent e) {
		// don't care where, for now.
		if (e.getButton() == MouseEvent.BUTTON3) {
			pickLabelFile();
		}
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
}
