// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Plotter.java,v 1.18 2013/02/13 23:09:57 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

class Wang_Plotter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_Plotter.java,v 1.18 2013/02/13 23:09:57 drmiller Exp $";
	public static final String Model = "12";
	public static final String Description = "Plotter";

	boolean _plot;	// mode, plot or print...

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JRadioButton) {
			JRadioButton m = (JRadioButton)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_0) { 
				_text.setPen(Color.black);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_1) { 
				_text.setPen(Color.blue);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_2) { 
				_text.setPen(Color.green);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_3) { 
				_text.setPen(Color.red);
				return;
			}

			if (m.getMnemonic() == KeyEvent.VK_A) { 
				setPlotArea(0.5, 0.5, 7.5, 7.5);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_B) { 
				setPlotArea(3.0, 0.5, 7.5, 7.5);
				return;
			}

			if (m.getMnemonic() == KeyEvent.VK_C) { 
				_text.setZoom(1.0);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_D) { 
				_text.setZoom(2.0);
				return;
			}
		} else if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_U) { 
				setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_H) { 
				home();
				setCursor(_x, _y);
				_text.repaint();
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

	double _pageWidth;	// inches for physical paper
	double _pageHeight;	// inches for physical paper
	double _scaleX;		// ratio for PlotArea (1/1000ths to paper)
	double _scaleY;		// ratio for PlotArea (1/1000ths to paper)

	// Plottable region, in inches
	public void setPlotArea(double xs, double ys,
				double xw, double yh) {
		// width and height, in inches, of "printable" area...
		if (xs >= _pageWidth) {
			// reject completely... need error...
			return;
		}
		if (ys >= _pageHeight) {
			// reject completely... need error...
			return;
		}
		// not possible?
		if (xs < 0.0) { // clip
			xw += xs; // reduce xw
			xs = 0.0;
		}
		if (ys < 0.0) { // clip
			yh += ys; // reduce xw
			ys = 0.0;
		}
		int ox = (int)(xs * 72.0);
		int oy = (int)(ys * 72.0);
		int sx = (int)(xw * 72.0 + 0.5);
		int sy = (int)(yh * 72.0 + 0.5);
		super.setUseableArea(ox, oy, sx, sy);

//		double dpi;
		// This device always plots within a 1000x1000 virtual area
//		if (xw < yh) {
//			dpi = 1000.0 / xw;
//		} else {
//			dpi = 1000.0 / yh;
//		}
//		int x = (int)(xw * dpi + 0.5);
//		int y = (int)(yh * dpi + 0.5);
		double gx = 1.0;
		double gy = 1.0;
		if (xw < yh) {
			gy = yh / xw;
		} else if (yh < xw) {
			gx = xw / yh;
		}
		// We scale all coords before passing to 'super'...
		// need to translate 1/1000ths into points...
		_scaleX = gx * (sx / 1000.0);
		_scaleY = gy * (sy / 1000.0);
		home();
		setCursor(_x, _y);
		_text.repaint();
	}

	public void setPaper(double w, double h) {
		_pageWidth = w;
		_pageHeight = h;
		super.setPage((int)(w * 72.0), (int)(h * 72.0));
	}

	private class MnemonicAction extends AbstractAction {

		static final long serialVersionUID = 311602000004L;

		public MnemonicAction(int key) {
			putValue(Action.MNEMONIC_KEY, key);
		}
		public void actionPerformed(ActionEvent e) { }
	}

	public Wang_Plotter() {
		super(Wang_UI.getSeries() + Model, Description);
		setPaper(11.0, 8.5);
		setPlotArea(0.5, 0.5, 7.5, 7.5);
		JMenu mu;
		mu = new JMenu("Plotter");
		JMenuItem mi;
		JMenu smu;
		ButtonGroup grp;
		JRadioButton op;

		smu = new JMenu("Zoom...");
		grp = new ButtonGroup();
		op = new JRadioButton("1x", true);
		op.setAction(new MnemonicAction(KeyEvent.VK_C));
		op.addActionListener(this);
		op.setText("1x"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		op = new JRadioButton("2x");
		op.setAction(new MnemonicAction(KeyEvent.VK_D));
		op.addActionListener(this);
		op.setText("2x"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		mu.add(smu);

		smu = new JMenu("Plot Area...");
		grp = new ButtonGroup();
		op = new JRadioButton("0,0-7.5x7.5", true);
		op.setAction(new MnemonicAction(KeyEvent.VK_A));
		op.addActionListener(this);
		op.setText("0,0-7.5x7.5"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		op = new JRadioButton("3,0-7.5x7.5");
		op.setAction(new MnemonicAction(KeyEvent.VK_B));
		op.addActionListener(this);
		op.setText("3,0-7.5x7.5"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		mu.add(smu);

		smu = new JMenu("Pen...");
		grp = new ButtonGroup();
		op = new JRadioButton("Black", true);
		op.setAction(new MnemonicAction(KeyEvent.VK_0));
		op.addActionListener(this);
		op.setText("Black"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		op = new JRadioButton("Blue");
		op.setAction(new MnemonicAction(KeyEvent.VK_1));
		op.addActionListener(this);
		op.setText("Blue"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		op = new JRadioButton("Green");
		op.setAction(new MnemonicAction(KeyEvent.VK_2));
		op.addActionListener(this);
		op.setText("Green"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		op = new JRadioButton("Red");
		op.setAction(new MnemonicAction(KeyEvent.VK_3));
		op.addActionListener(this);
		op.setText("Red"); // didn't we already do this?
		grp.add(op);
		smu.add(op);
		mu.add(smu);

		mi = new JMenuItem("Home", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);

		super.addMenu(mu);

		setup_chrgen();
		home();
		_dx = 0;
		_dy = 0;
		_cx = 1;
		_cy = 1;
		_sx = 6;
		_sy = 9;
		setCursor(_x, _y);
		_text.enableCursor(true);
		_text.setPen(Color.black);
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
		//boolean res =
		_plotChar(p);
		_x += _sx;
		_dx = 0;
		_dy = 0;
		return true;
	}

	private void setCursor(int x, int y) {
		int px = (int)Math.round(x * _scaleX);
		int py = (int)Math.round((999 - y) * _scaleY);
		_text.setCursor(px, py);
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
			int px = (int)Math.round(_x * _scaleX);
			int py = (int)Math.round((999 - _y) * _scaleY);
			if (dx == 0 && dy == 0) {
				// plot a "dot"...
				_text.addPlot(px, py, -1, -1);
//System.err.format("plot dot %d<=%d %d<=%d\n", _x, xd, _y, yd);
			} else {
				int pdx = (int)Math.round(xd * _scaleX);
				int pdy = (int)Math.round((999 - yd) * _scaleY);
				_text.addPlot(px, py, pdx, pdy);
			}
		}
		_x = xd;
		_y = yd;
		return true;
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
		return true;
	}

	private boolean return_carr() {
		_x = 0;
		return true;
	}

	private boolean rev_index() {
		_y += _sy;
		if (_y >= 1000) _y = 999;
		return true;
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
		if (_dx > 0 && _dy > 0 && _dx < 1000 && _dy < 1000) {
			_sx = _dx;
			_sy = _dy;
		}
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
		return true;
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
			setCursor(_x, _y);
			_text.repaint();
		}
		// "auto raise"...
		onOff(true);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
