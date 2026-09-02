// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.text.DefaultCaret;
import java.awt.image.*;

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
		_text.setCaret(new PlotBarCaret());
		setCursor(_x, _y);
		_text.repaint();
	}

	private class PlotBarCaret extends DefaultCaret
			implements ImageObserver {
		static final long serialVersionUID = 311601000040L;

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

	public void do_bell() {}
	public void do_shift_up() {}
	public void do_shift_dn() {}
	public void do_lock(int on) { if (on == 0) {} }
	public void do_settab() {}
	public void do_clrtab() {}
	public void do_tab() {}
	public void do_crlf() { if (return_index()) { setCursor(_x, _y); _text.repaint(); } }
	public void do_index() { if (index()) { setCursor(_x, _y); _text.repaint(); } }
	public void do_revindex() { if (rev_index()) { setCursor(_x, _y); _text.repaint(); } }
	public void do_space() { if (plotChar((byte)0x02)) { setCursor(_x, _y); _text.repaint(); } }
	public void do_backspace() {}

	public void do_cn24_direct(char c) {
		if (c == ' ') {}
	}

	public void do_cn24(byte c) {
		boolean drew = false;
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

	public int getRBS() { return 1; } // always ready, for now
}
