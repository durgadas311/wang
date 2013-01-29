// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Paper.java,v 1.7 2013/01/29 19:58:18 drmiller Exp $

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
	final String ident = "$Id: Wang_Paper.java,v 1.7 2013/01/29 19:58:18 drmiller Exp $";

	interface Wang_Plottable extends Printable {
		void clear();
		void addPlot(int x, int y, int xd, int yd);
		void addPlot(String s, int x, int y);
		void addText(String s);
		// from JComponent:
		void setBackground(Color c);
		void setForeground(Color c);
		void setPreferredSize(Dimension d);
		Dimension getSize();
		Font getFont();
		void paint(Graphics g);
		void repaint();
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
	boolean _hasGraphic;
	int _fx, _fy, _fa;
	double _gx, _gy;
	int _ox, _oy;

	private void clear() {
		_eop = 0;
		//_plot = false;
		//_shifted = false;
		//_x = _y = 0;
		_text.clear();
		_hasGraphic = (_fx == 0);
	}

	String _footer;
	JMenuBar _mb;

	public void setScale(double sx, double sy) {
		_gx = sx;
		_gy = sy;
	}

	public void setOrigin(int ox, int oy) {
		_ox = ox;
		_oy = oy;
	}

	// variable length, a.k.a. continuous form, paper
	public Wang_Paper(String model, String descr,
				Font font, int charWidth, int charHeight) {
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

		// setting this messes up horiz scrollbar...
		//_text.setPreferredSize(new Dimension(60 * _fx, 32 * _fy));
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);

		pa.setFont(font);
		FontMetrics fm = pa.getFontMetrics(pa.getFont());
		_fa = fm.getAscent();
		_fx = fm.charWidth('M');
		_fy = fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

		clear();

		_scroll = new JScrollPane(pa);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setPreferredSize(new Dimension(charWidth * _fx, charHeight * _fy));
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

		_frame = new JFrame("Wang " + model + " " + descr);
		_frame.setLayout(new FlowLayout());
		_text = new PlotOnlyArea();
		_text.setPreferredSize(new Dimension(dotWidth, dotHeight));
		_text.setBackground(Color.white);
		_text.setForeground(Color.black);

		_fa = _fx = _fy = 0;
		_gx = 1;	// or scaling?
		_gy = -1;	// or scaling?

		clear();

		_scroll = new JScrollPane((JPanel)_text);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(dotWidth, dotHeight));
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
		if (_plotter || _hasGraphic) {
			Dimension d = _text.getSize();
			java.awt.image.BufferedImage i =
				new java.awt.image.BufferedImage(d.width, d.height,
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			_text.paint(i.getGraphics());
			try {
				javax.imageio.ImageIO.write(i, "png", file);
			} catch (IOException ee) {
				System.err.println("error writing " + _model + " PNG");
			}
		} else {
			PlotTextArea pa = (PlotTextArea)_text;
			FileOutputStream fo;
			try {
				fo = new FileOutputStream(file);
			} catch (FileNotFoundException ee) {
				System.err.println("chosen " + _model + " file not found?");
				return;
			}
			try {
				fo.write(pa.getText().getBytes());
				fo.write('\n');
				fo.close();
			} catch (IOException ee) {
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
			String sfx, dsc;
			if (_plotter || _hasGraphic) {
				sfx = "png";
				dsc = "PNG image files";
			} else {
				sfx = "txt";
				dsc = "Text files";
			}
			SuffFileChooser ch = new SuffFileChooser("Save", sfx, dsc,
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

	class PlotTextArea extends JTextArea
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

		public void clear() {
			setText("");
			setCaretPosition(0);
			_nplots = 0;
			_xplots = 0;
			//_plotArray.dispose();
			_plotArray = null;
			repaint();
		}

		private plot[] _plotArray;
		private int _nplots;
		private int _xplots;

		public void addPlot(String s, int x, int y) {
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
		}

		public void addPlot(int x, int y, int xd, int yd) {
			System.err.format("should not call addPlot(%d, %d, %d, %d)\n",
				x, y, xd, yd);
		}

		public void addText(String s) {
			append(s);
			_eop += s.length();
			setCaretPosition(_eop);
		}

		public void paint(Graphics g) {
			super.paint(g);
			int x;
			for (x = 0; x < _xplots; ++x) {
				double xx, yy;
				xx = (_plotArray[x].x * _gx) + 0.5;
				yy = (_plotArray[x].y * _gy) + 0.5 + _fa;
				g.drawString(_plotArray[x].s, (int)xx, (int)yy);
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

			FontMetrics fm = this.getFontMetrics(_text.getFont());
			// 156 chars platten width of IBM Selectric...
			double nf = this.getFont().getSize() * (w0 / 156.0) /
							fm.charWidth('M');
			g2d.setFont(this.getFont().deriveFont((float)nf));

			int did = 0;
			String s;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			int l = g2d.getFont().getSize();
			double gx = (w0 / 1300.0);
			double gy = (l / (100.0 / 6.0));
			int lpp = 60; // (int)(h0 / l);
			int max = getLineCount();
			int i = 0;
			while (pg <= pageIndex) {
				int ln;
				for (ln = 0; ln < lpp; ++ln) {
					int nn = ln + pg * lpp;
					if (nn >= max) break;
					try {
						int ls = getLineStartOffset(nn);
						int ll = getLineEndOffset(nn) - ls;
						s = getText(ls, ll);
					} catch(javax.swing.text.BadLocationException ee) {
						break;
					}
					if (pg == pageIndex) {
						++did;
						if (s.length() > 0) { // not blank line...
							g2d.drawString(s, 0, ln * l + l);
						}
					}
				}
				if (pg == pageIndex) {
					int ps = (int)(h0 * pg);
					int pe = (int)(ps + h0);
					for (i = 0; i < _xplots; ++i) {
						double xx, yy;
						// convert 1/100ths to points...
						xx = (_plotArray[i].x * gx) + 0.5;
						yy = (_plotArray[i].y * gy) + 0.5;
						if (yy >= ps && yy < pe) {
							++did;
							g2d.drawString(_plotArray[i].s,
								(int)xx, (int)yy - ps + 1);
						}
					}
				}
				++pg;
			}
			if (did > 0) {
				s = new String("Page " + pg +
					" - " + _footer);
				g2d.drawString(s, 0, (lpp + 1) * l + l);
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

		public void addPlot(String s, int x, int y) {
			System.err.format("should not call addPlot(\"%s\", %d, %d)\n",
					s, x, y);
		}

		public void addText(String s) {
			System.err.format("should not call addText(\"%s\")\n", s);
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

		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			super.paint(g2d);
			int x;
			for (x = 0; x < _xplots; ++x) {
				double xx, yy;
				xx = (_plotArray[x].x * _gx) + 0.5;
				yy = (_plotArray[x].y * _gy) + 0.5;
				if (_plotArray[x].xd < 0) {
					g2d.drawOval((int)xx + _ox - 1, (int)yy + _oy - 1,
							1, 1);
				} else {
					double xd = (_plotArray[x].xd * _gx) + 0.5;
					double yd = (_plotArray[x].yd * _gy) + 0.5;
					g2d.drawLine((int)xx + _ox, (int)yy + _oy,
						(int)xd + _ox, (int)yd + _oy);
				}
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

			int did = 0;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			
			double gx = 1;	// TBD
			double gy = 1;	// TBD
			if (h0 < w0) {
				gy = h0 / (1000 * Math.abs(_gy));
				gx = gy * _gx;
			} else {
				gx = w0 / (1000 * _gx);
				gy = gx * Math.abs(_gy);
			}
//System.err.println("print() scaling is " + gx + "x" + gy);
			if (pageIndex == 0) {	// only one page - ever
				int i;
				for (i = 0; i < _xplots; ++i) {
					double xx, yy;
					// convert 1/1000ths to points...
					xx = (_plotArray[i].x * gx) + 0.5;
					if (xx < 0.0) xx = 0.0;
					if (xx >= w0) xx = w0 - 0.1;
					yy = (_plotArray[i].y * gy) + 0.5;
					if (yy < 0.0) yy = 0.0;
					if (yy >= h0) yy = h0 - 0.1;
					++did;
					if (_plotArray[i].xd < 0) {
						g2d.drawOval((int)xx - 1, (int)yy - 1,
								1, 1);
					} else {
						double xd = (_plotArray[i].xd * gx) + 0.5;
						if (xd < 0.0) xd = 0.0;
						if (xd >= w0) xd = w0 - 0.1;
						double yd = (_plotArray[i].yd * gy) + 0.5;
						if (yd < 0.0) yd = 0.0;
						if (yd >= h0) yd = h0 - 0.1;
//System.err.println("("+_plotArray[i].x+","+_plotArray[i].y+")-("+_plotArray[i].xd+","+_plotArray[i].yd+") => ("+xx+","+yy+")-("+xd+","+yd+")");
						g2d.drawLine((int)xx, (int)yy,
							(int)xd, (int)yd);
					}
				}
			}
			if (did > 0) {
				g2d.drawString(_footer, 0, (int)h0 - 20); // TBD
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
