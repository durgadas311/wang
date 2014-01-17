// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_FunctionLabelBar.java,v 1.7 2014/01/17 23:02:55 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

// file format:
//
// [fnleft label]
// [f0 label]
// ...
// [f15 label]
// [fnright label]
// ...repeat once...
//
// Blanks lines before/after set are ignored. Supported character escapes:
//
//	\^	Up arrow
//	\v	Down arrow
//	\s	Hard space (e.g. &nbsp;)
//
// Escapes withing an HTML label are translated to HTML equivalents. If label
// is not HTML (does not begin with '<') then the translations are unicode.
//
// Todo: 1) [n/a] support arbitrary (0-15) "fn set" (for use as SEARCH labels).
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
		int xw = 62;	// 62.5, so adjust odd members
		if ((x & 1) == 1) {
			xw -= leftNudge - 1;
		} else {
			xw += leftNudge;
		}
		return xw;
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
		BufferedReader in = null;
		int x = -1; // am between label sections
		int z = 0;
		try {
			in = new BufferedReader(new
				InputStreamReader(new FileInputStream(file)));
			while (z < 4) {
				String s = in.readLine();
				if (s == null) break;
				++lno;
				s = s.replaceFirst("^#.*", "");
				// need to avoid stripping out HTML &# sequences...
				// how? for now, require blank before hash.
				s = s.replaceFirst("\\s\\s*#.*", "");
				if (s.length() > 0 && s.charAt(0) == '<') {
					s = s.replaceAll("\\\\\\^", "&#8593;");
					s = s.replaceAll("\\\\v", "&#8595;");
					s = s.replaceAll("\\\\s", "&nbsp;");
				} else {
					s = s.replaceAll("\\\\\\^", "\u2191");
					s = s.replaceAll("\\\\v", "\u2193");
					s = s.replaceAll("\\\\s", " ");
				}
//System.err.format("using %d %d %s\n", z, x, s);
				if (x < 0) {
					if (s.length() == 0) continue;
					corners[z].setText(s);
					x = 0;
					continue;
				} else if (x >= 16) {
					corners[z + 1].setText(s);
					z += 2;
					x = -1;
					continue;
				}
				if (z == 0) {
					f[x].setText(s);
				} else {
					F[x].setText(s);
				}
				++x;
			}
			currLabel = file;
		} catch (Exception ee) {
			// pop-up warning?
//System.err.println("Line " + lno + ": " + ee.toString());
			Wang_UI.warning("Load Key Labels",
				"Line " + lno + ": " + ee.toString());
		}
		try { // ug, "finally" makes this easier?
			if (in != null) {
				in.close();
			}
		} catch (Exception ee) {}
		while (z < 4) {
			if (x < 0) {
				corners[z].setText("");
				x = 0;
			}
			while (x < 16) {
				if (z == 0) {
					f[x].setText("");
				} else {
					F[x].setText("");
				}
				++x;
			}
			corners[z + 1].setText("");
			x = -1;
			z += 2;
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

		for (int x = 0; x < 16; ++x) {
			f[x] = new JLabel("", SwingConstants.CENTER);
			f[x].setPreferredSize(dim);
			F[x] = new JLabel("", SwingConstants.CENTER);
			F[x].setPreferredSize(dim);
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
		}

		GridBagLayout gb = new GridBagLayout();
		setLayout(gb);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.anchor = GridBagConstraints.NORTH;

		JPanel pan;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(25, 50));
		pan.setOpaque(false);
		s.gridheight = 2;
		gb.setConstraints(pan, s);
		add(pan);
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(25, 50));
		pan.setOpaque(false);
		s.gridx += 18 + 1;
		gb.setConstraints(pan, s);
		add(pan);
		s.gridheight = 1;

		s.gridx = 1;
		gb.setConstraints(corners[0], s);
		add(corners[0]);
		s.gridx += 1;
		for (int x = 0; x < 16; ++x) {
			gb.setConstraints(f[x], s);
			add(f[x]);
			s.gridx += 1;
		}
		gb.setConstraints(corners[1], s);
		add(corners[1]);

		s.gridy += 1;
		s.gridx = 1;
		gb.setConstraints(corners[2], s);
		add(corners[2]);
		s.gridx += 1;
		for (int x = 0; x < 16; ++x) {
			gb.setConstraints(F[x], s);
			add(F[x]);
			s.gridx += 1;
		}
		gb.setConstraints(corners[3], s);
		add(corners[3]);

		loadLabels(Wang_UI.getProperties().getFile("wang_function_labels",
							true, Wang_UI.getDir()));
		addMouseListener(this);
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
