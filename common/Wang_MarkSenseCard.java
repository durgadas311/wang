// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_MarkSenseCard.java,v 1.3 2013/02/06 16:48:26 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.awt.event.*;

class Wang_MarkSenseCard extends JLabel
		implements MouseListener, KeyListener {
	static final long serialVersionUID = 311614000000L;

	Font font1 = new Font("Sans-serif", Font.PLAIN, 18);
	Font font2 = new Font("Sans-serif", Font.PLAIN, 11);

	byte[] _code;
	int _code_used;
	String _title = new String("my_prog_3");
	int _pgix;
	int _npg;
	java.text.SimpleDateFormat _timestamp =
		new java.text.SimpleDateFormat("MMM" + "\u2003 " + "d" + "\u2003\u2003" + "y");
	String _date;
	Rectangle _top, _bottom;

	final String[] pr_16 = {
		"E", "T", "+", "-", "\u00D7", "\u00F7", "ST", "RE",
		"*", "*", "f", "F", "A", "B", "C", "D", ""
	};
	final String[] pr_17 = {
		"0", "1", "2", "3", "4", "5", "6", "7",
		"8", "9", "10", "11", "12", "13", "14", "15", ""
	};
	final String[] pr_18 = {
		"S", "RE", "W", "GO", "Jo", "J+", "SN", "CS",
		"TN", "RD", "LN", "e\u207F", "x\u00B2", "\u221AX", "LP", "1/x",
		"  ", ""
	};
	final String[] pr_19 = {
		"M", "ST", "\u03B1", "SP", "J\u00F8", "Je", "S\u00B9", "C\u00B9",
		"T\u00B9", "DR", "LG", "10\u207F", "I", "|x|", "EP", "RT",
		"", ""
	};

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);
		g2d.setColor(Color.black);
		g2d.setFont(font2);
		int s;
		for (s = 0; s < 40; ++s) {
			int cx = _pgix * 40 + s;
			if (cx >= _code_used) break;
			byte c = _code[cx];
			double ry = s * 28.8 + 48.0;
			String step = String.format("%03d", cx / 10);
			g2d.drawString(step, 90, (int)Math.round(ry + 8));
			int b;
			for (b = 0; b < 8; ++b) {
				double rx = (b * 38.4) + 168.0;
				boolean m = ((c & 0x80) != 0);
				c <<= 1;
				if (m) {
					g2d.fillRect((int)Math.round(rx), (int)Math.round(ry), 20, 10);
				}
			}
		}
		while (s > 0 && s < 40) {
			double ry = s * 28.8 + 48.0;
			g2d.fillRect((int)Math.round(130.1), (int)Math.round(ry), 20, 10);
			++s;
		}
		g2d.setFont(font1);
		for (s = 0; s < 40; ++s) {
			double ry = s * 28.8 + 62.0;
			int cx = _pgix * 40 + s;
			if (cx >= _code_used) break;
			byte c = _code[cx];
			int h = (c >> 4) & 0x0f;
			int l = (c & 0x0f);
			String t = pr_16[h];
			if (h == 8) {
				t += pr_18[l];
			} else if (h == 9) {
				t += pr_19[l];
			} else {
				t += pr_17[l];
			}
			g2d.drawString(t, 30, (int)Math.round(ry));
		}

		AffineTransform orig = g2d.getTransform();
		g2d.rotate(-Math.PI/2);
		g2d.drawString(_title, -1100, 20);
		g2d.drawString(Integer.toString(_pgix + 1), -550, 20);
		g2d.drawString(Integer.toString(_npg), -460, 20);
		g2d.drawString(_date, -400, 20);
		g2d.setTransform(orig);
	}

	public Wang_MarkSenseCard(String pgm) {
		super();
		setIcon(new ImageIcon(getClass().getResource("icons/Wang_MarkSenseCard.gif")));
		setBackground(new Color(236,226,190));
		setOpaque(true);
		setPreferredSize(new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight()));
		_top = new Rectangle(0, 0, 10, 10);
		_bottom = new Rectangle(0, getIcon().getIconHeight() - 10, 10, 10);

		_code = new byte[2048];
		_code_used = 0;
		_pgix = 0;
		if (pgm == null) {
			_title = "new_program";
		} else {
			_title = pgm;
		}
		_date = _timestamp.format(new java.util.Date());
		File file = new File(Wang_UI.getDir() + "/" + _title);
		if (file != null && file.exists()) {
			_date = _timestamp.format(file.lastModified());
			FileInputStream f;
			try {
				f = new FileInputStream(file);
				_code_used = f.read(_code);
			} catch (Exception ee) {
			}
			if (_code_used >= 2 &&
					(_code[_code_used - 2] & 0x0ff) == 0x9e) {
				if ((_code[_code_used - 1] & 0x0ff) == 0x9e) {
					_code_used -= 1;
				} else {
					_code_used -= 2;
				}
			}
		}
		_npg = (_code_used + 39) / 40;
		addMouseListener(this);
	}

	public void keyTyped(KeyEvent e) { }

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			--_pgix;
			if (_pgix < 0) {
				_pgix = 0;
			}
			repaint();
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			++_pgix;
			if (_pgix >= _npg) {
				_pgix = _npg - 1;
			}
			// allow for adding new page???
			repaint();
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			scrollRectToVisible(_top);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			scrollRectToVisible(_bottom);
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void mouseClicked(MouseEvent e) {
		double x = e.getX();
		double y = e.getY();
		y = (y - 48.0) / 28.8;
		x = (x - 168.0) / 38.4;
		boolean iny = ((y - Math.floor(y)) * 28.8 <= 10);
		boolean inx = ((x - Math.floor(x)) * 38.4 <= 20);
		if (inx && iny && y >= 0 && y <= 39) {
			int cx = _pgix * 40 + (int)y;
			if (x >= 0 && x <= 7) {
				int bit = 0x80 >> (int)x;
				_code[cx] ^= bit;
				if (cx >= _code_used) _code_used = cx + 1;
				repaint();
//System.err.println("step " + Math.floor(y) + " bit " + Math.floor(x));
			} else if (Math.floor(x) == -1.0) {
				if (cx == _code_used - 1) {
					// leaves stale data...
					--_code_used;
					repaint();
				} else if (cx == _code_used) {
					// exposes stale data...
					++_code_used;
					repaint();
				}
			} else {
System.err.println("step " + Math.floor(y) + " bit " + Math.floor(x));
			}
		}
	}
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }

}
