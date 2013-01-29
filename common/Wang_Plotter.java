// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Plotter.java,v 1.11 2013/01/29 21:09:45 drmiller Exp $

import java.awt.event.*;
import javax.swing.*;
import java.io.*;

class Wang_Plotter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_Plotter.java,v 1.11 2013/01/29 21:09:45 drmiller Exp $";
	public static final String Model = "12";
	public static final String Description = "Plotter";

	boolean _plot;	// mode, plot or print...
	private byte[] cn24_xlate;

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_U) { 
				setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_H) { 
				home();
				return;
			}
		}
		super.actionPerformed(e);
	}

	public void reset() {
		// anything?
	}

	private void setup() {
		System.err.println("Plotter Setup menu");
	}

	private class Plotter_CharGen {
		public byte pen;
		public byte dx;
		public byte dy;
	}

	Plotter_CharGen[][] cn24_chrgen;

	private void setup_chrgen() {
		InputStream inp = this.getClass().getResourceAsStream("plotter_chrgen.dat");
		cn24_chrgen = new Plotter_CharGen[64][];
		// there MUST be an easier way...
		try {
			int x, y;
			for (x = 0; x < 64; ++x) {
				cn24_chrgen[x] = new Plotter_CharGen[16];
				for (y = 0; y < 16; ++y) {
					cn24_chrgen[x][y] = new Plotter_CharGen();
//System.err.format("cn24_chrgen[%d][%d] = b[%d]\n", x, y, z);
					cn24_chrgen[x][y].pen = (byte)inp.read(); //b[z];
					cn24_chrgen[x][y].dx = (byte)inp.read(); //b[z];
					cn24_chrgen[x][y].dy = (byte)inp.read(); //b[z];
				}
			}
			inp.close();
		} catch(Exception e) {
			System.err.println("Failed to read character generator");
		}
	}

	// This is no longer needed, really. Decode control chars explicitely?
	private void setup_xlate() {
		cn24_xlate = new byte[64];
		cn24_xlate[0x00] = '-';
		cn24_xlate[0x01] = 'Y';
		cn24_xlate[0x02] = ' ';
		cn24_xlate[0x03] = '/';
		cn24_xlate[0x04] = 'Q';
		cn24_xlate[0x05] = 'P';
		cn24_xlate[0x06] = '+';
		cn24_xlate[0x07] = 'J';
		cn24_xlate[0x08] = '}';
		cn24_xlate[0x09] = '?';
		cn24_xlate[0x0a] = '=';
		cn24_xlate[0x0b] = '{';
		cn24_xlate[0x0c] = ',';
		cn24_xlate[0x0d] = ':';
		cn24_xlate[0x0e] = 'F';
		cn24_xlate[0x0f] = 'G';

		cn24_xlate[0x10] = 'W';
		cn24_xlate[0x11] = 'S';
		cn24_xlate[0x12] = '\013';	// plot
		cn24_xlate[0x13] = '\014';	// move
		cn24_xlate[0x14] = 'I';
		cn24_xlate[0x15] = '\'';
		cn24_xlate[0x16] = '.';
		cn24_xlate[0x17] = '\020';	// EXTENSION: pen color/stroke
		cn24_xlate[0x18] = '\015';	// char size
		cn24_xlate[0x19] = 'O';
		cn24_xlate[0x1a] = '\016';	// char spacing
		cn24_xlate[0x1b] = '\017';	// home
		cn24_xlate[0x1c] = 'A';
		cn24_xlate[0x1d] = 'R';
		cn24_xlate[0x1e] = 'V';
		cn24_xlate[0x1f] = 'M';

		cn24_xlate[0x20] = 'B';
		cn24_xlate[0x21] = 'H';
		cn24_xlate[0x22] = '\001';	// step x+
		cn24_xlate[0x23] = '\002';	// step x-
		cn24_xlate[0x24] = 'K';
		cn24_xlate[0x25] = 'E';
		cn24_xlate[0x26] = 'N';
		cn24_xlate[0x27] = 'T';
		cn24_xlate[0x28] = '\003';	// print mode (ignored)
		cn24_xlate[0x29] = '1';
		cn24_xlate[0x2a] = '\004';	// step y+
		cn24_xlate[0x2b] = '\005';	// step y-
		cn24_xlate[0x2c] = 'C';
		cn24_xlate[0x2d] = 'D';
		cn24_xlate[0x2e] = 'U';
		cn24_xlate[0x2f] = 'X';

		cn24_xlate[0x30] = '9';
		cn24_xlate[0x31] = '0';
		cn24_xlate[0x32] = '\006';	// step x+y+
		cn24_xlate[0x33] = '\007';	// step x-y+
		cn24_xlate[0x34] = '6';
		cn24_xlate[0x35] = '5';
		cn24_xlate[0x36] = '2';
		cn24_xlate[0x37] = 'Z';
		cn24_xlate[0x38] = '\010';	// plot mode (ignored)
		cn24_xlate[0x39] = '4';
		cn24_xlate[0x3a] = '\011';	// step x+y-
		cn24_xlate[0x3b] = '\012';	// step x-y-
		cn24_xlate[0x3c] = '8';
		cn24_xlate[0x3d] = '7';
		cn24_xlate[0x3e] = '3';
		cn24_xlate[0x3f] = 'L';

		// No characters over 0x3f are ever received...
		// Those codes cause the Wang to emit move commands followed
		// by the associated code 0x00-0x3f.
	}

	public void setPaper(double w, double h) {
		// width and height, in inches, of "printable" area...
		double dpi;
		if (w < h) {
			dpi = 1000 / w;
		} else {
			dpi = 1000 / h;
		}
		int x = (int)(w * dpi + 0.5);
		int y = (int)(h * dpi + 0.5);
		super.setPage(x, y);
		double sx = 1.0;
		double sy = 1.0;
		if (w < h) {
			sy = h / w;
		} else if (h < w) {
			sx = w / h;
		}
		super.setScale(sx, sy);
	}

	public Wang_Plotter() {
		super(Wang_UI.getSeries() + Model, Description, 1000, 1000);
		setPaper(11.0 - 1.0, 8.5 - 1.0);
		JMenu mu;
		mu = new JMenu("Plotter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Home", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);
		setup_xlate();
		setup_chrgen();
		home();
		_dx = 0;
		_dy = 0;
		_cx = 1;
		_cy = 1;
		_sx = 6;
		_sy = 9; // not used?
	}

	private int _x, _y;
	private int _dx, _dy;
	private int _cx, _cy;
	private int _sx, _sy;

	private boolean _plotChar(byte p) {
		boolean res = false, r;
		Plotter_CharGen[] cg;
		cg = cn24_chrgen[p];
//System.err.println("cg = " + cg);
		if (cg == null) return res;
		int sx = _x;
		int sy = _y;
		int i;
		for (i = 0; i < 16; ++i) {
//System.err.println("cg[" + i + "] = " + cg[i]);
			if (cg[i].pen == 0 && cg[i].dx == 0 && cg[i].dy == 0) {
				break;
			}
			if ((cg[i].pen & 0x80) != 0) {
				byte q = (byte)(cg[i].pen & 0x3f);
				r = _plotChar(q);
				_x = sx;
				_y = sy;
			} else {
				r = _plot(cg[i].pen != 0, cg[i].dx * _cx, cg[i].dy * _cy);
			}
			res = (res || r);
		}
		_x = sx;
		_y = sy;
		return res;
	}
	private boolean plotChar(byte p) {
//System.err.format("Character %02x\n", p);
		if (p >= 64) return false;
		boolean res = _plotChar(p);
		_x += _sx;
		_dx = 0;
		_dy = 0;
		return res;
	}

	private boolean _plot(boolean draw, int dx, int dy) {
		int xd = _x + dx;
		if (xd < 0) xd = 0;
		if (xd >= 1000) xd = 999;
		int yd = _y + dy;
		if (yd < 0) yd = 0;
		if (yd >= 1000) yd = 999;
//System.err.println("Plot " + _x + "," + _y + " -> " + xd + "," + yd);
		// Plotter origin is different than our drawables... flip "y".
		if (draw) {
			if (dx == 0 && dy == 0) {
				// plot a "dot"...
				_text.addPlot(_x, 999 - _y, -1, -1);
//System.err.format("plot dot %d<=%d %d<=%d\n", _x, xd, _y, yd);
			} else {
				_text.addPlot(_x, 999 - _y, xd, 999 - yd);
			}
		}
		_x = xd;
		_y = yd;
		return draw;
	}

	private boolean plot() {
		return _plot(true, _dx, _dy);
	}

	private boolean move() {
		//System.err.println("move(" + _dx + "," + _dy + ")");
		return _plot(false, _dx, _dy);
	}

	private boolean index() {
		_y -= _sy;
		if (_y < 0) _y = 0;
		return false;
	}

	private boolean return_carr() {
		_x = 0;
		return false;
	}

	private boolean rev_index() {
		_y += _sy;
		if (_y >= 1000) _y = 999;
		return false;
	}

	private boolean return_index() {
		return_carr();
		return index();
	}

	private boolean chrSize() {
		//System.err.println("chrSize(" + _dx + "," + _dy + ")");
		if (_dx > 0 && _dx < 16) {
			_cx = _cy = _dx;
		}
		return false;
	}

	private boolean chrSpace() {
		//System.err.println("chrSpace(" + _dx + "," + _dy + ")");
		_sx = _dx;
		_sy = _dy;
		return false;
	}

	private boolean plotMode() {
		//System.err.println("plotMode");
		_plot = true;
		return false;
	}

	private boolean printMode() {
		//System.err.println("printMode");
		_plot = false;
		return false;
	}

	private boolean home() {
		// Plotter origin is different than our drawables... flip "y".
		_x = 0;
		_y = 0;
		return false;
	}

	private boolean setPen() {
		System.err.println("setPen(" + _dx + "," + _dy + ")");
		return false;
	}

	public void do_cn24(byte[] b) {
		boolean drew = false;
		byte c = b[0];
		if ((c & 0x26) == 0x22) {
			// simple movement - generated by calculator
			// TBD: requires plot mode?
			// all must return here or else _dx/_dy get cleared
			switch(c) {
			case 0x22:
				_dx += 1;
				break;
			case 0x23:
				_dx -= 1;
				break;
			case 0x2a:
				_dy += 1;
				break;
			case 0x2b:
				_dy -= 1;
				break;
			case 0x32:
				_dx += 1;
				_dy += 1;
				break;
			case 0x33:
				_dx -= 1;
				_dy += 1;
				break;
			case 0x3a:
				_dx += 1;
				_dy -= 1;
				break;
			case 0x3b:
				_dx -= 1;
				_dy -= 1;
				break;
			}
			return;
		} else if ((c & 0x2f) == 0x28) {
			// mode change - generated by calculator
			switch(c) {
			case 0x28:
				drew = printMode();
				break;
			case 0x38:
				drew = plotMode();
				break;
			}
		} else if (_plot) {
			// special
			switch(c) {
			case 0x12:
				drew = plot();	// a.k.a pen down (dx == dy == 0)
				break;
			case 0x13:
				drew = move();	// a.k.a pen up (dx == dy == 0)
				break;
			case 0x18:
				drew = chrSize();	// set character size (0-15) (Y)
				break;
			case 0x1a:
				drew = chrSpace();	// set character spacing (X,Y)
				break;
			case 0x1b:
				drew = home();	// move to lower-left (0,0)
				break;
			// extensions - keep?
			case 0x17:
				drew = setPen();
				break;
			default:
				drew = plotChar(c);
				break;
			}
			// ignore anything else
		} else {
			// some characters have no encoding?
			switch(c) {
			case 0x18:
				drew = return_index();
				break;
			case 0x1a:
				drew = index();
				break;
			case 0x1b:
				drew = rev_index();
				break;
			default:
				drew = plotChar(c);
				break;
			}
		}
		_dx = _dy = 0;
		if (drew) {
			_text.repaint();
		}
		// "auto raise"...
		onOff(true);
	}

	static public String getName() {
		return Wang_UI.getSeries() + Model + " " + Description;
	}
}
