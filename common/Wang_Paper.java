// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Paper.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $

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
	final String ident = "$Id: Wang_Paper.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $";

	private JFrame _frame;
	PlotTextArea _text;
	private JScrollPane _scroll;

	private int _xoff, _yoff;
	int _eop;
	private boolean _onoff;
	boolean _hasGraphic;
	int _fx, _fy, _fa;
	double _gx, _gy;

	private void clear() {
		_text.setText("");
		_eop = 0;
		_text.setCaretPosition(_eop);
		//_plot = false;
		//_shifted = false;
		//_x = _y = 0;
		_text.clear();
		_hasGraphic = false;
	}

	String _footer;

	public Wang_Paper(String device, Font font, int charWidth, int charHeight,
						int dotWidth, int dotHeight) {
		_onoff = false;

		_frame = new JFrame("Wang " + device);
		_frame.setLayout(new FlowLayout());
		_text = new PlotTextArea();
		_text.setFont(font);

		// setting this messes up horiz scrollbar...
		//_text.setPreferredSize(new Dimension(60 * _fx, 32 * _fy));
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);

		clear();

		FontMetrics fm = _text.getFontMetrics(_text.getFont());
		_fa = fm.getAscent();
		_fx = fm.charWidth('M');
		_fy = fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		if (charWidth != 0 && charHeight != 0) {
			_scroll.setPreferredSize(new Dimension(charWidth * _fx, charHeight * _fy));
		} else {
			_scroll.setPreferredSize(new Dimension(dotWidth, dotHeight));
		}
		_frame.add(_scroll);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("File");
		mb.add(mu);
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

		_frame.setJMenuBar(mb);
		_frame.pack();	// set size according to content...

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
		
		_frame.addComponentListener(this);
	}

	private void save611(File file) {
		if (_hasGraphic) {
			java.awt.image.BufferedImage i =
				new java.awt.image.BufferedImage(_text.getWidth(),
								_text.getHeight(),
					java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
			_text.paint(i.getGraphics());
			try {
				javax.imageio.ImageIO.write(i, "png", file);
			} catch (IOException ee) {
				System.err.println("error writing 611 PNG");
			}
		} else {
			FileOutputStream fo;
			try {
				fo = new FileOutputStream(file);
			} catch (FileNotFoundException ee) {
				System.err.println("chosen 611 file not found?");
				return;
			}
			try {
				fo.write(_text.getText().getBytes());
				fo.write('\n');
				fo.close();
			} catch (IOException ee) {
				System.err.println("error writing 611 TXT");
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown 611 event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_T) {
			clear();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			String sfx, dsc;
			if (_hasGraphic) {
				sfx = "png";
				dsc = "PNG image files";
			} else {
				sfx = "txt";
				dsc = "Text files";
			}
			SuffFileChooser ch = new SuffFileChooser("Save", sfx, dsc, new File(".")); // w600_fe._dir
			int rv = ch.showDialog(_frame);
			if (rv == JFileChooser.APPROVE_OPTION) {
				save611(ch.getSelectedFile());
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
				//java.util.Date dt = new java.util.Date();
				_footer = new String("Wang 601/602/611 OutputWriter - " +
					"xxxx"); //w600_fe._timestamp.format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println("611 menu " + e.getActionCommand() +
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
			implements Printable {
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
			_nplots = 0;
			_xplots = 0;
			//_plotArray.dispose();
			_plotArray = null;
			//_x = _y = 0;
		}

		private plot[] _plotArray;
		private int _nplots;
		private int _xplots;

		public void addPlot(String s, int x, int y) {
			int n = _xplots++;
			if (_xplots > _nplots) {
				int o = _nplots;
				_nplots += 256;
				plot[] p = new plot[_nplots];
				if (o > 0) {
					System.arraycopy(_plotArray, 0, p, 0, o);
				}
				_plotArray = p;
			}
			_plotArray[n] = new plot(s, x, y);
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

			FontMetrics fm = _text.getFontMetrics(_text.getFont());
			// 156 chars platten width of IBM Selectric...
			double nf = _text.getFont().getSize() * (w0 / 156.0) /
							fm.charWidth('M');
			g2d.setFont(_text.getFont().deriveFont((float)nf));

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
								(int)xx, (int)yy - ps + l);
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

	public boolean onOff() {
		return _onoff;
	}

	public void onOff(boolean on) {
		if (_onoff == on) return;
		_onoff = on;
		_frame.setVisible(_onoff);
	}
}
