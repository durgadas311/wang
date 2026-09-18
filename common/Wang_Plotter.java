// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.text.DefaultCaret;
import java.awt.image.*;

// Accrding to schematics:
//
// ChrSpc Xs/Ys causes both to be applied at the end of each char.
// But, if both were non-zero then the next character would be printed
// diagonally. Seems that either one of Xs or Ys should be "0" under
// normal circumstances, or else something non-obvious is going on.
// The signs of Xs and Ys are saved in FFs separate from the magnitudes
// (signs in L37B/L27B on 6249, magnitudes in L25/L27/L43 on 6248).
// Executing the 01-10 command saves the current signs in the spare FFs,
// and performing the LOSP phase of character printing recalls those
// signs (as well as saved dx/dy) into position to affect UX/DX/UY/DY.
//
// X register (R01) issues 02-02, 03-02, 03-10 for positive values. 
// Y register (R00) issues 02-10, 03-02, 03-03 for positive values. 
// 712/612 Brochure (unk date, "preliminary") states that X is R00, 
// but the 600 microcode proves X is R01. The Streitmatter doc contradicts
// itself on the same page.
//
// ChrSpc might normally have Y=0 to print left-right. ChrSpc is
// applied after the STOP word in chargen ROM.
//
// ChrSiz is applied to each dx/dy in chargen ROM (but not ChrSpc
// or draw/move).
//
// Un-plotted (print mode) 01-02, 01-03, 01-08, 01-10, 01-11 do not
// perform any functions, only when in plot mode (preceded by 03-08).
//
// Characters may not be printed in plot mode, only print mode. To control
// starting point of text, use draw/move command (600: 05-02/05-03).
// Characters are always printed "upright", regardless of spacing
// direction.
//
// The Streitmatter doc claims the entire (scaled) plot area is divided up
// into 999 units. However, this does not compute. The delta X/Y registers,
// that hold the values sent by the calculator, are 10 bits eachs and so can
// range from 0 to 1023. But the D/A counters used to position the pen are
// 12 bit and can range from 0 to 4095. Counter overflow holds the D/A values
// at 4095. "Check Scale" with PRST asserted (pressed) sets the D/A to 2000.
// So while the max delta for a give command (draw/move) is 1023, the entire
// plot area seems to max out at 4095. There appears to be a 4x factor between
// delta values and D/A clocking, presumably transforming 0-1023 into 0-4095.
//
// ChrSiz stores a 4-bit value. When used, the value is loaded into a 4-bit
// up-counter using essentially "ChrSiz ^ 0x1110", with the cycle ending when
// this counter overflows. This implies character scaling that does not match
// the Streitmatter doc (i.e. "3" is not larger than "2").
//
// ChrSpc stores 8-bit values for X and Y and is limited to 0-255. It appears
// that processing ChrSpc (LOSP) or regular draw/move injects a constant 0b1110
// into the scale counter. This should result in 2 clocks, but the input to this
// counter is a FF that toggles from the low bit of the delta FFs. Unclear just
// what sort of scaling is indicated without understanding the delta circuitry better.
// But a 4x would explain the 12-bit D/A counters vs. 10-bit delta counters.
//
// The delta circuitry on 6248 seems excessively convoluted, but appears to locate
// the most-significant (non-zero) bit in "dX | dY" and then counts off that
// many clocks. These clocks are passed through a FF and the scale counter,
// multiplying by at least 4x to the D/A counters. This possibly explains why
// the delta counters range 0-1023 while the D/A counters range 0-4095, representing
// a logical space of 1024 units mapped into a physical space of 4096 - both
// representing the full plot range. This would mean that one plot command is
// capable of spaning the entire plot range. It is not known just what sort of
// performance such a plot command would have, both in time it takes the calculator
// to generate the X/Y increments as well as the time it takes for the plotter to
// increment over the resulting dX/dY (at 4x) to change the D/A counters.

class Wang_Plotter extends Wang_Paper
	implements Wang_OutputDevice, ActionListener
{

	public void setProperties(Wang_Properties p) { }

	public static final String Model = "12";
	public static final String Description = "Plotter";

	private static final Color _black = Color.black;
	private static final Color _blue = new Color(0, 0, 190);
	private static final Color _green = new Color(0, 190, 0);
	private static final Color _red = new Color(190, 0, 0);

	private static JMenuItem pmi = null;
	private static Wang_Plotter thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Description;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_Plotter s_getInstance() {
		if (thus != null) return thus;
		thus = new Wang_Plotter();
		return thus;
	}

	static JMenuItem dev_mi = null;
	private boolean plugged = false;

	public String getModel() { return s_getModel(); }
	public String getName() { return s_getName(); }
	public void plugIn(JMenu mu) {
		if (plugged) return;
		plugged = true;
		if (pmi != null) {
			pmi.setText(s_getName() + " (installed)");
		}
		if (mu != null) {
			mu.add(getMenu());
		}
		Wang_CN24_dev.connect(this);
		onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		if (Wang_CN24_dev.get() == this) {
			Wang_CN24_dev.connect(null);
		}
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		onOff(false);
	}
	public boolean isPlugged() { return plugged; }
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName(), KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}

	boolean _plot;	// mode, plot or print...

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang612.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.30 $ $Date: 2014/01/14 21:53:51 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JRadioButton) {
			JRadioButton m = (JRadioButton)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_0) { 
				_text.setPen(_black);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_1) { 
				_text.setPen(_blue);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_2) { 
				_text.setPen(_green);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_3) { 
				_text.setPen(_red);
				return;
			}
		} else if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_D) { 
				onOff(true);
				return;
			}
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

			if (m.getMnemonic() == KeyEvent.VK_A) { 
				doSetPlotArea();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_B) { 
				showAbout();
				return;
			}
		}
		super.actionPerformed(e);
	}

	JTextArea _org_x_tx, _org_y_tx, _siz_x_tx, _siz_y_tx;
	JPanel _org_x_pn, _org_y_pn, _siz_x_pn, _siz_y_pn;
	JPanel _dia_pn;
	JOptionPane _plot_area;
	static final int OPTION_APPLY = 0;
	static final int OPTION_CANCEL = 1;
	static final int OPTION_NONE = 2;
	private Object[] _btns;

	double _orgX, _orgY, _sizeX, _sizeY;

	private void doSetPlotArea() {
		_org_x_tx.setText(Double.toString(_orgX));
		_org_y_tx.setText(Double.toString(_orgY));
		_siz_x_tx.setText(Double.toString(_sizeX));
		_siz_y_tx.setText(Double.toString(_sizeY));
		Dialog dlg = _plot_area.createDialog(null, "Set Plot Area");
		dlg.setVisible(true);
		Object res = _plot_area.getValue();
		if (_btns[OPTION_APPLY].equals(res)) {
			try {
				double xs = Double.parseDouble(_org_x_tx.getText());
				double ys = Double.parseDouble(_org_y_tx.getText());
				double xw = Double.parseDouble(_siz_x_tx.getText());
				double yw = Double.parseDouble(_siz_y_tx.getText());
				setPlotArea(xs, ys, xw, yw);
			} catch(Exception e) { }
		}
	}

	private void makePlotAreaDialog() {
		// Create dialog for Plot Area
		_org_x_tx = new JTextArea();
		_org_x_tx.setPreferredSize(new Dimension(50, 20));
		_org_x_pn = new JPanel();
		_org_x_pn.add(new JLabel("X Org:"));
		_org_x_pn.add(_org_x_tx);

		_org_y_tx = new JTextArea();
		_org_y_tx.setPreferredSize(new Dimension(50, 20));
		_org_y_pn = new JPanel();
		_org_y_pn.add(new JLabel("Y Org:"));
		_org_y_pn.add(_org_y_tx);

		_siz_x_tx = new JTextArea();
		_siz_x_tx.setPreferredSize(new Dimension(50, 20));
		_siz_x_pn = new JPanel();
		_siz_x_pn.add(new JLabel("X Size:"));
		_siz_x_pn.add(_siz_x_tx);

		_siz_y_tx = new JTextArea();
		_siz_y_tx.setPreferredSize(new Dimension(50, 20));
		_siz_y_pn = new JPanel();
		_siz_y_pn.add(new JLabel("Y Size:"));
		_siz_y_pn.add(_siz_y_tx);

		_dia_pn = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		_dia_pn.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 1;
		s.gridy = 1;
		s.weightx = 1;
		s.weighty = 1;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(_org_x_pn, s);
		_dia_pn.add(_org_x_pn);
		s.gridy += 1;
		gridbag.setConstraints(_org_y_pn, s);
		_dia_pn.add(_org_y_pn);
		s.gridy += 1;
		gridbag.setConstraints(_siz_x_pn, s);
		_dia_pn.add(_siz_x_pn);
		s.gridy += 1;
		gridbag.setConstraints(_siz_y_pn, s);
		_dia_pn.add(_siz_y_pn);

		Icon icon = null;
		_btns = new Object[2];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_CANCEL] = "Cancel";
		_plot_area = new JOptionPane(_dia_pn, JOptionPane.QUESTION_MESSAGE,
			JOptionPane.OK_CANCEL_OPTION, icon, _btns);

	}

	public void reset() {
		// anything?
	}

	private void setup() {
		System.err.println("Plotter Setup menu");
	}

	private class Plotter_CharGen {
		public boolean stop;
		public boolean pen;
		public byte dx;
		public byte dy;
	}

	Plotter_CharGen[][] cn24_chrgen;

	private void setup_chrgen() {
		InputStream inp = this.getClass().getResourceAsStream("plotter_chrgen.rom");
		cn24_chrgen = new Plotter_CharGen[64][];
		// there MUST be an easier way...
		try {
			int b, mag;
			boolean sgn;	// true = positive
			int x, y;
			for (x = 0; x < 64; ++x) {
				cn24_chrgen[x] = new Plotter_CharGen[16];
				for (y = 0; y < 16; ++y) {
					cn24_chrgen[x][y] = new Plotter_CharGen();
					// assumes little-endian, and never EOF
					b = inp.read();
					b |= inp.read() << 8;
					cn24_chrgen[x][y].stop = (b & 0b1000000000) != 0;
					cn24_chrgen[x][y].pen = (b & 1) != 0;
					sgn = (b & 0b0000100000) != 0;
					mag = (b & 0b0111000000) >> 6;
					cn24_chrgen[x][y].dy = (byte)(sgn ? mag : -mag);
					sgn = (b & 0b0000000010) != 0;
					mag = (b & 0b0000011100) >> 2;
					cn24_chrgen[x][y].dx = (byte)(sgn ? mag : -mag);
				}
			}
			inp.close();
		} catch(Exception e) {
			System.err.println("Failed to read character generator");
		}
	}

	double _pageWidth;	// inches for physical paper
	double _pageHeight;	// inches for physical paper
	double _scaleX;		// ratio for PlotArea (1/1024ths to paper)
	double _scaleY;		// ratio for PlotArea (1/1024ths to paper)

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
		_orgX = xs;
		_orgY = ys;
		_sizeX = xw;
		_sizeY = yh;
		int ox = (int)Math.floor(xs * 72.0 * 2.0);
		int oy = (int)Math.floor(ys * 72.0 * 2.0);
		int sx = (int)Math.round(xw * 72.0 * 2.0);
		int sy = (int)Math.round(yh * 72.0 * 2.0);
		super.setUseableArea(ox, oy, sx, sy);

//		double dpi;
		// This device always plots within a 1024x1024 virtual area
//		if (xw < yh) {
//			dpi = 1024.0 / xw;
//		} else {
//			dpi = 1024.0 / yh;
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
		// need to translate 1/1024ths into points...
		_scaleX = gx * (sx / 1024.0);
		_scaleY = gy * (sy / 1024.0);
		home();
		_text.setCaret(new PlotBarCaret());
		setCursor(_x, _y);
		_text.repaint();
	}

	private class PlotBarCaret extends DefaultCaret
			implements ImageObserver {

		int _plot_pen1 = 20;
		int _plot_pen2 = 10;
		Color _bar = new Color(128,128,128,128);
		Color _bar_lt = new Color(168,168,168,128);
		Color _bar_dk = new Color(108,108,108,128);

		private Image _pen_holder;
		boolean _draw_bar;

		public PlotBarCaret() {
			java.net.URL url = getClass().getResource("icons/penholder.png");
			_pen_holder = Toolkit.getDefaultToolkit().getImage(url);
			_draw_bar = true;
		}

		public boolean imageUpdate(Image img,
                           int infoflags,
                           int x,
                           int y,
                           int width,
                           int height) {
			repaint();
// can't figure out why sometimes it does not draw...
//System.err.println("PlotBarCaret.imageUpdate() " + infoflags);
			// we get about 32 calls before all bits are available...
			return ((infoflags & ImageObserver.ALLBITS) == 0);
		}

		public void paint(Graphics g) {
			//JComponent comp = getComponent();
			Graphics2D g2d = (Graphics2D)g;
			Dimension d = _text.getSize();
			Point p = getMagicCaretPosition();

if (_draw_bar) {
			int ytd = p.y - _plot_pen2 - 5;
			int yb = p.y + _plot_pen2 + 5;
			int ybd = d.height - yb;

			// plotter bar highlight:
			g2d.setColor(_bar_lt);
			g2d.fillRect(p.x + _plot_pen2, 0, 3, ytd);
			g2d.fillRect(p.x + _plot_pen2, yb, 3, ybd);
			// plotter bar shadow:
			g2d.setColor(_bar_dk);
			g2d.fillRect(p.x + _plot_pen2 + _plot_pen1 - 3, 0, 3, ytd);
			g2d.fillRect(p.x + _plot_pen2 + _plot_pen1 - 3, yb, 3, ybd);
			// main plotter bar:
			g2d.setColor(_bar);
			g2d.fillRect(p.x + _plot_pen2 + 3, 0, _plot_pen1 - 6, ytd);
			g2d.fillRect(p.x + _plot_pen2 + 3, yb, _plot_pen1 - 6, ybd);
} else {
			g2d.setColor(_bar);
}
			// cross-hairs:
			g2d.drawLine(p.x, p.y - _plot_pen2 + 2, p.x, p.y + _plot_pen2 - 2);	
			g2d.drawLine(p.x - _plot_pen2 + 2, p.y, p.x + _plot_pen2 - 2, p.y);

			boolean b = g.drawImage(_pen_holder,
					p.x - _plot_pen2 - 5, p.y - _plot_pen2 - 5, this);
			if (!b) {
				b = false;
//System.err.println("failed drawImage?");
			}
		}
	}

	public void setPaper(double w, double h) {
		_pageWidth = w;
		_pageHeight = h;
		double dpi = 72.0 * 2.0; // set desired resultion
		int pw = (int)Math.floor(w * dpi);
		int ph = (int)Math.floor(h * dpi);
		super.setPage(pw, ph);
	}

	private class MnemonicAction extends AbstractAction {
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

		mi = new JMenuItem("Plot Area...", KeyEvent.VK_A);
		mi.addActionListener(this);
		mu.add(mi);

		makePlotAreaDialog();

		//

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

		mu = new JMenu("Help");
		mi = new JMenuItem("About", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);

		setup_chrgen();
		home();
		_dx = 0;
		_dy = 0;
		// hw defaults (sort of)
		_cx = 1;
		_cy = 1;
		_sx = 0;
		_sy = 0;
		setCursor(_x, _y);
		_text.enableCursor(true);
		_text.setPen(Color.black);
	}

	private int _x, _y;
	private boolean _gx, _gy;	// signs for dx/dy (true=pos)
	private int _dx, _dy;	// 10 bits in hw
	private int _cx, _cy;	// 4 bits in hw, scaling factor for char gen steps
	private int _sx, _sy;	// 8 bits in hw, num steps advanced after char
	private boolean _sgx, _sgy;	// signs for _sx/_sy

	// According to the schematics, the character generator uses 1024x10 ROM
	// organized as 64 characters (A4-A9) of 16-word "steps" (A0-A3) with each
	// word formatted as:
	//
	//	9  8  7  6  5  4  3  2  1  0
	//	S  ---Y---  y  ---X---  x  P
	//
	//	S = STOP (end of character, after current word)
	//	Y = delta Y (0-7)
	//	y = Y direction (1=up/+)
	//	X = delta X (0-7)
	//	x = X direction (1=up/+)
	//	P = Pen up/down (1=down/draw)
	//
	// Assumed starting position is character cell lower-left,
	// each character must return to that position when finished.
	//
	// All of TKA=0 are printable and have valid characters in the ROM,
	// TKA!=0 do not print for -02,-03,-08,-10,-11 characters and
	// presumably the ROM has no contents for those. The character
	// 01-07 probably prints, but cannot find documentation to show
	// what character that is.
	//
	// Without actual ROM dumps, cannot confirm this.

	private boolean _plotChar(byte p) {
		boolean res = false, r;
		Plotter_CharGen[] cg;
		cg = cn24_chrgen[p];
//System.err.println("cg = " + cg);
		if (cg == null) return res;
		if (cg[0].stop && cg[0].dx == 0 && cg[0].dy == 0) return res;
		int i;
		for (i = 0; i < 16; ++i) { // hw did not stop without STOP
//System.err.println("cg[" + i + "] = " + cg[i]);
			r = _plot(cg[i].pen, cg[i].dx * _cx, cg[i].dy * _cy);
			res = (res || r);
			if (cg[i].stop) break;
		}
		return res;
	}

	private boolean plotChar(byte p) {
//System.err.format("Character %02x\n", p);
		if (p >= 64) return false;
		//boolean res =
		_plotChar(p);
		// Technically, both _sx and _sy are applied.
		// TODO: is this scaled by _cx/_cy? seems not.
		if (_sgx) _x += _sx;
		else _x -= _sx;
		if (_sgy) _y += _sy;
		else _y -= _sy;
		_dx = 0;
		_dy = 0;
		return true;
	}

	private void setCursor(int x, int y) {
		int px = (int)Math.round(x * _scaleX);
		int py = (int)Math.round((1023 - y) * _scaleY);
		_text.setCursor(px, py);
	}

	private boolean _plot(boolean draw, int dx, int dy) {
		// hw seems to clock D/A at 4x, so 1023 becomes 4095.
		// Just use 1x values here.
		int xd = _x + dx;
		if (xd < 0) xd = 0;
		if (xd >= 1024) xd = 1023;
		int yd = _y + dy;
		if (yd < 0) yd = 0;
		if (yd >= 1024) yd = 1023;
//System.err.println("Plot " + _x + "," + _y + " -> " + xd + "," + yd);
		// Plotter origin is different than our drawables... flip "y".
		if (draw) {
			int px = (int)Math.round(_x * _scaleX);
			int py = (int)Math.round((1023 - _y) * _scaleY);
			if (dx == 0 && dy == 0) {
				// plot a "dot"...
				_text.addPlot(px, py, -1, -1);
			} else {
				int pdx = (int)Math.round(xd * _scaleX);
				int pdy = (int)Math.round((1023 - yd) * _scaleY);
				_text.addPlot(px, py, pdx, pdy);
			}
		}
		_x = xd;
		_y = yd;
		return true;
	}

	private boolean _plot(boolean draw) {
		int dx = _dx & 0x3ff;
		if (!_gx) dx = -dx;
		int dy = _dy & 0x3ff;
		if (!_gy) dy = -dy;
		return _plot(draw, dx, dy);
	}

	private boolean plot() {
		return _plot(true);
	}

	private boolean move() {
		//System.err.println("move(" + _dx + "," + _dy + ")");
		return _plot(false);
	}

	private boolean chrSize() {
		//System.err.println("chrSize(" + _dx + "," + _dy + ")");
		// Accrding to schematic, this should be:
		_cx = _cy = (_dx & 0x00f);
		if (_cx == 0) _cx = _cy = 1; // not exactly hw...
		return false;
	}

	private boolean chrSpace() {
		//System.err.format("chrSpace(%s %d, %s %d)\n", _gx, _dx, _gy, _dy);
		_sx = (_dx & 0x0ff);
		_sy = (_dy & 0x0ff);
		_sgx = _gx;
		_sgy = _gy;
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

	public void do_bell() {}
	public void do_shift_up() {}
	public void do_shift_dn() {}
	public void do_lock(int on) { if (on == 0) {} }
	public void do_settab() {}
	public void do_clrtab() {}
	public void do_tab() {}
	public void do_crlf() {}
	public void do_index() {}
	public void do_revindex() {}
	public void do_space() { if (plotChar((byte)0x02)) { setCursor(_x, _y); _text.repaint(); } }
	public void do_backspace() {}

	public void do_cn24_direct(char c) {
		if (c == ' ') {}
	}

	public void do_cn24(byte c) {
		boolean drew = false;
		if ((c & 0x26) == 0x22) {
			// simple movement - generated by calculator
			// TODO: requires plot mode?
			// all must return here or else _dx/_dy get cleared
			switch(c) {
			case 0x22:	// X+
				_gx = true;
				++_dx;
				break;
			case 0x23:	// X-
				_gx = false;
				++_dx;
				break;
			case 0x2a:	// Y+
				_gy = true;
				++_dy;
				break;
			case 0x2b:	// Y-
				_gy = false;
				++_dy;
				break;
			case 0x32:	// X+/Y+
				_gx = true;
				_gy = true;
				++_dx;
				++_dy;
				break;
			case 0x33:	// X-/Y+
				_gx = false;
				_gy = true;
				++_dx;
				++_dy;
				break;
			case 0x3a:	// X+/Y-
				_gx = true;
				_gy = false;
				++_dx;
				++_dy;
				break;
			case 0x3b:	// X-/Y-
				_gx = false;
				_gy = false;
				++_dx;
				++_dy;
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
			// return; // OK to clear _dx/_dy?
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
				// Hardware does not appear to print in PLOT mode
				// drew = plotChar(c);
				break;
			}
			// ignore anything else
		} else {
			// TODO: None of these are implemented... only char print.
			switch(c) {
			case 0x12:
			case 0x13:
				// pen up/down only in PLOT mode...
				break;
			case 0x18:
				break;
			case 0x1a:
				break;
			case 0x1b:
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

	public int getRBS() { return 1; } // always ready, for now
}
