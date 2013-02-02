// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Paper.java,v 1.14 2013/02/02 01:39:04 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

class Wang_Paper
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: Wang_Paper.java,v 1.14 2013/02/02 01:39:04 drmiller Exp $";

	interface Wang_Plottable extends Printable {
		void clear();
		void addPlot(int x, int y, int xd, int yd);
		int addPlot(String s, int x, int y);
		int appendLastPlot(String s, int x, int y);
		void setCursor(int x, int y);
		boolean enableCursor(boolean on);
		void saveAsText(FileOutputStream fo) throws Exception;

		// from JComponent:
		void setBackground(Color c);
		void setForeground(Color c);
		void setPreferredSize(Dimension d);
		Dimension getSize();
		Font getFont();
		void paint(Graphics g);
		void repaint();
		void revalidate();
		void scrollRectToVisible(Rectangle r);
	}

	String _model;
	String _descr;

	private JFrame _frame;
	Wang_Plottable _text;
	private JScrollPane _scroll;
	private boolean _plotter;	// i.e. not printer w/continuous forms

	private int _xoff, _yoff;
	int _eop;
	private boolean _onoff;
	int _fx, _fy, _fa;
	double _gx, _gy;
	int _ox, _oy;
	int _base_x, _base_y;

	private void clear() {
		_eop = 0;
		//_plot = false;
		//_shifted = false;
		//_x = _y = 0;
		_text.clear();
		_text.setPreferredSize(new Dimension(_base_x, _base_y));
		_text.revalidate();
		_text.repaint();
	}

	FontMetrics _fm;
	String _footer;
	JMenuBar _mb;

	public void setPage(int x, int y) {
		_base_x = x;
		_base_y = y;
		_text.setPreferredSize(new Dimension(_base_x, _base_y));
		_text.revalidate();
		_text.repaint();
	}

	public void setScale(double sx, double sy) {
		_gx = sx;
		_gy = sy;
	}

	public void setOrigin(int ox, int oy) {
		_ox = ox;
		_oy = oy;
	}

	public class Wang_FontMetrics {
		int ascent;
		int width;
		int height;
	}
	public Wang_FontMetrics getFontMetrics() {
		Wang_FontMetrics wfm = new Wang_FontMetrics();
		wfm.ascent = _fa;
		wfm.width = _fx;
		wfm.height = _fy;
		return wfm;
	}

	// variable length, a.k.a. continuous form, paper
	public Wang_Paper(String model, String descr, Font font) {
		_plotter = false;
		_model = model;
		_descr = descr;
		_onoff = false;
		_ox = 0;
		_oy = 0;

		_frame = new JFrame("Wang " + model + " " + descr);
		_frame.setLayout(new FlowLayout());
		PlotTextArea pa = new PlotTextArea();
		_text = pa;

		_base_x = 60 * _fx;
		_base_y = 32 * _fy;
		_text.setPreferredSize(new Dimension(_base_x, _base_y));
		_text.setBackground(Color.white);
		_text.setForeground(Color.black);
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);

		pa.setFont(font);
		_fm = pa.getFontMetrics(pa.getFont());
		_fa = _fm.getAscent();
		_fx = _fm.charWidth('M');
		_fy = _fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

		clear();

		_scroll = new JScrollPane(pa);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setPreferredSize(new Dimension(1024, 768));
		_frame.add(_scroll);

		_mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("File");
		_mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Tear Off", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(_mb);
		_frame.pack();	// set size according to content...

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
		
		_frame.addComponentListener(this);
	}

	// fixed size paper - e.g. flatbed plotter
	public Wang_Paper(String model, String descr,
				int dotWidth, int dotHeight) {
		_plotter = true;
		_model = model;
		_descr = descr;
		_onoff = false;
		_ox = 0;
		_oy = 0;

		_base_x = dotWidth;
		_base_y = dotHeight;

		_frame = new JFrame("Wang " + model + " " + descr);
		_frame.setLayout(new FlowLayout());
		_text = new PlotOnlyArea();
		_text.setPreferredSize(new Dimension(_base_x, _base_y));
		_text.setBackground(Color.white);
		_text.setForeground(Color.black);

		_fa = _fx = _fy = 0;
		_gx = 1;	// or scaling?
		_gy = -1;	// or scaling?

		clear();

		_scroll = new JScrollPane((JPanel)_text);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(1024, 768));
		_frame.add(_scroll);

		_mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("File");
		_mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("New Paper", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(_mb);
		_frame.pack();	// set size according to content...

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;

		_frame.addComponentListener(this);
	}

	private void save(File file) {
		String ext = SuffFileFilter.getExtension(file);
		if (_plotter && !ext.equals("png")) {
			Wang_UI.warning("Save", "Can't save Plotter output as text");
			return;
		}
		if (!ext.equals("png") && !ext.equals("txt")) {
			Wang_UI.warning("Save", "Can only save output as .txt or .png, not " + ext);
			return;
		}
		if (ext.equals("png")) {
			Dimension d = _text.getSize();
			java.awt.image.BufferedImage i =
				new java.awt.image.BufferedImage(d.width, d.height,
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			boolean saved = _text.enableCursor(false);
			_text.paint(i.getGraphics());
			_text.enableCursor(saved);
			try {
				javax.imageio.ImageIO.write(i, "png", file);
			} catch (IOException ee) {
				System.err.println("error writing " + _model + " PNG");
			}
		} else {
			FileOutputStream fo;
			try {
				fo = new FileOutputStream(file);
			} catch (FileNotFoundException ee) {
				System.err.println("chosen " + _model + " file not found?");
				return;
			}
			try {
				_text.saveAsText(fo);
				fo.write('\n');
				fo.close();
			} catch (Exception ee) {
				System.err.println("error writing " + _model + " TXT");
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown " + _model + " event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_T) {
			clear();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			SuffFileChooser ch = new SuffFileChooser("Save",
					new String[] {"png", "txt"},
					new String[] {"PNG image files", "Text files"},
					Wang_UI.getDir());
			int rv = ch.showDialog(_frame);
			if (rv == JFileChooser.APPROVE_OPTION) {
				save(ch.getSelectedFile());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(OrientationRequested.LANDSCAPE);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.75, (float)0.5, (float)7.0, (float)10.0, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(_text);
			boolean print = pj.printDialog(aset);
			if (print) {
				java.util.Date dt = new java.util.Date();
				_footer = new String("Wang " + _model + " " + _descr + " - " +
					Wang_UI.getTimestamp().format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println(_model + " menu " + e.getActionCommand() +
						" not implemented yet");
	}

	public JFrame getFrame() { return _frame; }

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
		if (e.getComponent() == _frame) {
			Dimension fdim = _frame.getSize(); 
			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
			_scroll.setPreferredSize(_scroll.getSize());
			_frame.setSize(fdim.width, fdim.height); // redundant?
			_frame.setPreferredSize(_frame.getSize());
		}
	}

	class PlotTextArea extends JPanel
			implements Printable, Wang_Plottable {
		static final long serialVersionUID = 311457692040L;
		class plot {
			plot(String s_, int x_, int y_) {
				s = s_;
				x = x_;
				y = y_;
			}
			public String s;
			public int x;
			public int y;
		}

// Need to sort plot array before printing...
//		class TextPlotComparator implements Comparator {
//			public int compare(plot ob1, plot ob2) {
//				if (ob1.y < ob2.y) return -1;
//				else if (ob1.y > ob2.y) return 1;
//				else if (ob1.x < ob2.x) return -1;
//				else if (ob1.x > ob2.x) return 1;
//				else return 0;
//			}
//			public boolean equals(plot ob1) {
//				return (ob1.y == y && ob1.x == x);
//			}
//		}

		public void clear() {
			_nplots = 0;
			_xplots = 0;
			//_plotArray.dispose();
			_plotArray = null;
			repaint();
		}

		private plot[] _plotArray;
		private int _nplots;
		private int _xplots;
		private int _last = -1;
		private int _cx, _cy;
		private boolean _enableCursor;

		public boolean enableCursor(boolean on) {
			boolean ret = _enableCursor;
			_enableCursor = on;
			return ret;
		}

		public int addPlot(String s, int x, int y) {
			int n = _xplots;
			if (_xplots + 1 > _nplots) {
				int o = _nplots;
				_nplots += 256;
				plot[] p = new plot[_nplots];
				if (o > 0) {
					System.arraycopy(_plotArray, 0, p, 0, o);
				}
				_plotArray = p;
			}
			_plotArray[n] = new plot(s, x, y);
			++_xplots;
			_last = n;
			if (y >= getHeight()) {
				// should bump by "page size"...
				Dimension d = new Dimension(getWidth(), getHeight() + 100);
				setPreferredSize(d);
				revalidate();
				repaint();
			}
			return _fm.stringWidth(s);
		}

		public int appendLastPlot(String s, int x, int y) {
			if (_last < 0) {
				return addPlot(s, x, y);
			} else {
				_plotArray[_last].s += s;
				return _fm.stringWidth(s);
			}
		}

		public void addPlot(int x, int y, int xd, int yd) {
			System.err.format("should not call addPlot(%d, %d, %d, %d)\n",
				x, y, xd, yd);
		}

		public void setCursor(int x, int y) {
			_cx = x;
			_cy = y;
			scrollRectToVisible(new Rectangle(x - 10, y - 10, x + 10, y + 10));
		}

		public void saveAsText(FileOutputStream fo) throws Exception {
			int x = 0, y = 0;
			int dx, dy;
			int i;
			for (i = 0; i < _xplots; ++i) {
				dy = _plotArray[i].y - y;
				if (dy > 0) {
					// if the two strings differ at all in Y,
					// make sure at least one newline is saved.
					while (dy > 0) {
						fo.write('\n');
						dy -= _fy;
					}
					x = 0;
				}
				dx = _plotArray[i].x - x;
				if (dx > 0) {
					// only put spaces if X differ by at least 1 sp
					while (dx > _fx) {
						fo.write(' ');
						dx -= _fx;
					}
				}
				fo.write(_plotArray[i].s.getBytes());
				y = _plotArray[i].y;
				x = _plotArray[i].x + _fm.stringWidth(_plotArray[i].s);
			}
		}

		private void paintString(Graphics2D g2d, plot ps, int yoff) {
			// Can't seem to get drawString to line up with
			// _fx spacing...
			//g2d.drawString(ps.s, ps.x, ps.y + _fa);
			char[] ca = ps.s.toCharArray();
			int x = ps.x;
			int y = ps.y - yoff + _fa;
			int i = 0;
			for (i = 0; i < ca.length; ++i) {
				g2d.drawChars(ca, i, 1, x, y);
				x += _fx;
			}
		}

		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.addRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
			super.paint(g2d);
			g2d.scale(_gx, _gy);
			int x;
			for (x = 0; x < _xplots; ++x) {
				paintString(g2d, _plotArray[x], 0);
			}
			if (_enableCursor) {
				// don't want this for "save" option...
				g2d.setColor(Color.red);
				//g2d.drawLine(_cx, _cy, _cx, _cy + _fy);
				g2d.drawRect(_cx, _cy, _fx, _fy);
			}
		}

		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();
			double y0 = pf.getImageableY();
			double w0 = pf.getImageableWidth();
			double h0 = pf.getImageableHeight();
			int pg = 0;
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);
			int l = g2d.getFont().getSize();

			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);

			pg = pageIndex + 1;
			String s = new String("Page " + pg +
					" - " + _footer);
			g2d.drawString(s, 0, (int)h0 - (l + 5) / 6);
			h0 -= l; // make space for footer line

			Dimension d = getSize();
			double gx = w0 / d.width;
			double gy = gx;
			// divide up pane (virtically only) by h0...
			g2d.scale(gx, gy);

//			FontMetrics fm = this.getFontMetrics(_text.getFont());
//			// 156 chars platten width of IBM Selectric...
//			double nf = this.getFont().getSize() * (w0 / 156.0) /
//							fm.charWidth('M');
//			g2d.setFont(this.getFont().deriveFont((float)nf));
			g2d.setFont(this.getFont());

			int did = 0;
			int i = 0;
			// sorted list, find first one that is beyond
			// "current page" and reset next page from there.
			pg = 0;
			int ps = 0;
			int pe = ps + (int)h0;
			for (i = 0; i < _xplots; ++i) {
				double yy, zz;
				// convert to same units as 'h0'
				yy = (_plotArray[i].y * gy);
				zz = yy + (_fy * gy);
				if (yy >= pe) {	// on next page, so move there...
					ps += (int)h0;
					pe = ps + (int)h0;
					++pg;
				} else if (yy >= ps) {
					// on this page, or beyond it...
					if (zz <= pe) {
						// well contained, no problems
					} else {
						// must be half-off... set next page
						++pg;
						ps = (int)yy;
						pe = ps + (int)h0;
					}
				} else {
					// on previous page? how?
				}
				// Note, once pg > pageIndex we can just stop...
				if (pg == pageIndex) {
					++did;
					paintString(g2d, _plotArray[i], (int)Math.round(ps / gy));
				}
			}
			if (did > 0) {
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	class PlotOnlyArea extends JPanel
			implements Printable, Wang_Plottable {
		static final long serialVersionUID = 311457692040L;
		class plot {
			plot(int x_, int y_, int xd_, int yd_) {
				x = x_;
				y = y_;
				xd = xd_;
				yd = yd_;
			}
			public int x;
			public int y;
			public int xd;
			public int yd;
		}

		public void clear() {
			_nplots = 0;
			_xplots = 0;
			//_plotArray.dispose();
			_plotArray = null;
			repaint();
		}

		private plot[] _plotArray;
		private int _nplots;
		private int _xplots;
		private int _cx, _cy;
		private boolean _enableCursor;

		public boolean enableCursor(boolean on) {
			boolean ret = _enableCursor;
			_enableCursor = on;
			return ret;
		}

		public int addPlot(String s, int x, int y) {
			System.err.format("should not call addPlot(\"%s\", %d, %d)\n",
					s, x, y);
			return 0;
		}

		public int appendLastPlot(String s, int x, int y) {
			System.err.format("should not call appendLastPlot(\"%s\", %d, %d)\n",
					s, x, y);
			return 0;
		}

		public void addPlot(int x, int y, int xd, int yd) {
			int n = _xplots;
			if (_xplots + 1 > _nplots) {
				int o = _nplots;
				_nplots += 256;
				plot[] p = new plot[_nplots];
				if (o > 0) {
					System.arraycopy(_plotArray, 0, p, 0, o);
				}
				_plotArray = p;
			}
			_plotArray[n] = new plot(x, y, xd, yd);
			++_xplots;
		}

		public void setCursor(int x, int y) {
			_cx = x;
			_cy = y;
			scrollRectToVisible(new Rectangle(x - 10, y - 10, x + 10, y + 10));
		}

		public void saveAsText(FileOutputStream fo) throws Exception {
			System.err.format("should not call saveAsText()\n");
			// throw exception?
		}

		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.addRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
			super.paint(g2d);
			g2d.scale(_gx, _gy);
			int x;
			for (x = 0; x < _xplots; ++x) {
				if (_plotArray[x].xd < 0) {
					g2d.drawOval(_plotArray[x].x,
						_plotArray[x].y,
						1, 1);
				} else {
					g2d.drawLine(_plotArray[x].x,
						_plotArray[x].y,
						_plotArray[x].xd,
						_plotArray[x].yd);
				}
			}
			if (_enableCursor) {
				// don't want this for "save" option...
				g2d.setColor(Color.red);
				g2d.drawLine(_cx, _cy - 10, _cx, _cy + 10);
				g2d.drawLine(_cx - 10, _cy, _cx + 10, _cy);
			}
		}

		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();		// in points
			double y0 = pf.getImageableY();		// in points
			double w0 = pf.getImageableWidth();	// in points
			double h0 = pf.getImageableHeight();	// in points
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);
//System.err.println("print() " + pageIndex + ": " + x0 + "," + y0 + " " + w0 + "x" + h0);
			int l = g2d.getFont().getSize();

			int did = 0;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);

			g2d.drawString(_footer, 0, (int)h0 - (l + 5) / 6);
			h0 -= l; // make space for footer line

			double gs = 1.0;
			Dimension d = getSize();
			double ow0 = d.width;
			double oh0 = d.height;
			g2d.scale(_gx, _gy);
			if (w0 / h0 > ow0 / oh0) {
				gs = h0 / oh0;
			} else {
				gs = w0 / ow0;
			}
			g2d.scale(gs, gs);
			// virtual paper size is 1000x1000
			g2d.drawRect(0, 0, 1000, 1000);
			if (pageIndex == 0) {	// only one page - ever
				int i;
				for (i = 0; i < _xplots; ++i) {
					++did;
					if (_plotArray[i].xd < 0) {
						g2d.drawOval(_plotArray[i].x,
							_plotArray[i].y,
							1, 1);
					} else {
						g2d.drawLine(_plotArray[i].x,
							_plotArray[i].y,
							_plotArray[i].xd,
							_plotArray[i].yd);
					}
				}
			}
			if (did > 0) {
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	public boolean onOff() {
		return _onoff;
	}

	public void onOff(boolean on) {
		if (_onoff == on) return;
		_onoff = on;
		_frame.setVisible(_onoff);
	}

	public void addMenu(JMenu mu) {
		_mb.add(mu);
	}
}
