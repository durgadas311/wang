// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_OutputWriter.java,v 1.11 2013/02/17 04:44:45 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

class Wang_OutputWriter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_OutputWriter.java,v 1.11 2013/02/17 04:44:45 drmiller Exp $";

	public static final String Model = "01";
	public static final String Description = "Output Writer";

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_U) {
				//setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_H) {
				//home();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_T) {
				_x = _y = 0;
				_text.setCursor(_x, _y);
				//return; fall through and perform base class too...
			}
		}
		super.actionPerformed(e);
	}

	public void reset() {
		// anything?
	}

	public void setPaper(double w, double h) {
		// width and height, in inches, of "printable" area...
		// assuming 12cpi and 6lpi, and 100dpi,
		// compute size of page in pixels.
		double pw = (12.0 * _wfm.width) * w; // page width in points
		double ph = (6.0 * _wfm.height) * h; // page height in points
		super.setPage((int)pw, (int)ph);
		//super.setScale(1.0, 1.0); // another way
	}

	public Wang_OutputWriter() {
		super(Wang_UI.getSeries() + Model, Description,
				new Font("Monospaced", Font.PLAIN, 12), false);
		_wfm = super.getFontMetrics();

		// default to portrait 8.5x11 with margins
		setPaper(8.5 - 1.0, 11 - 1.0);

		JMenu mu;
		mu = new JMenu("Typewriter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);

		_text.setCaret(new TypeBallCaret());
		_text.setCursor(_x, _y);
		_text.enableCursor(true); 
	}

	Wang_FontMetrics _wfm;
	private boolean _shifted;
	private int _x, _y;

	private void index() {
		_y += _wfm.height;
	}

	private void revindex() {
		_y -= _wfm.height;
		if (_y < 0) _y = 0;
	}

	private void space() {
		_x += _wfm.width;
		if (_x >= 1300) _x = 1299;
	}

	private void bkspace() {
		_x -= _wfm.width;
		if (_x < 0) _x = 0;
	}

	private class TypeBallCaret extends DefaultCaret {
		static final long serialVersionUID = 311601000040L;

		private Image _caret;

		public TypeBallCaret() {
			java.net.URL url = getClass().getResource("icons/selectric.png");
			_caret = Toolkit.getDefaultToolkit().getImage(url).getScaledInstance(25, -1, Image.SCALE_DEFAULT);
		}
		public void paint(Graphics g) {
			JTextComponent comp = getComponent();

			Rectangle r = null;
			try {
				r = comp.modelToView(getDot());
			} catch(Exception e) { }
			if (r == null) return;
			// 'r' defines location of caret...
//System.err.println("TypeBallCaret.paint() "+r.x+","+r.y);
			boolean b = g.drawImage(_caret,
				r.x + (r.width / 2) - (_caret.getWidth(comp) / 2),
						r.y + r.height, comp);
			if (b) b = false;
		}
	}

	public void do_cn24(byte[] b) {
		boolean printable = true;
		if ((b[0] & 0x0f) == 0x08) { // control characters...
			switch((b[0] & 0x30) >> 4) {
			case 0: // nothing
				break;
			case 1:	// return+index handled below...
				index();
				printable = false;
				break;
			}
		} else if ((b[0] & 0x06) == 0x02) {
			switch((b[0] & 0x30) >> 4) {
			case 0: // space/bspace or nothing
				if ((b[0] & 1) == 0) {
					space();
				} else {
					bkspace();
				}
				printable = false;
				break;
			case 1:	// index/rev or shift...
				if ((b[0] & 0x0e) == 0x02) {
					_shifted = ((b[0] & 1) != 0);
					return;
				}
				if ((b[0] & 1) == 0) {
					index();
				} else {
					revindex();
				}
				printable = false;
				break;
			}
		}
		if (printable) {
System.err.println("doing byte "+b[0]);
			String s;
			s = Wang_UI.getCharConv().tiltrotateToAscii(b[0], _shifted);
			if (s != null) {
				_text.appendText(s);
			}
		}
		_text.repaint();

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
