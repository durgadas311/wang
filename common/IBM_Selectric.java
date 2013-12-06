// Copyright (c) 2011,2012 Douglas Miller
// $Id: IBM_Selectric.java,v 1.5 2013/12/06 20:54:06 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

class IBM_Selectric extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: IBM_Selectric.java,v 1.5 2013/12/06 20:54:06 drmiller Exp $";

	public void setProperties(Wang_Properties p) { }

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

	JTextArea _jtext;

	public IBM_Selectric(String model, String descr) {
		super(model, descr,
				new Font("Monospaced", Font.PLAIN, 12), false);
		_wfm = super.getFontMetrics();

		// default to portrait 8.5x11
		setPaper(8.5, 11);

		_text.setCaret(new TypeBallCaret());
		_text.enableCursor(true); 
		_jtext = (JTextArea)_text;
	}

	Wang_FontMetrics _wfm;
	private boolean _shifted;

	public void do_bell() {
	}

	public void do_shift_up() {
		_shifted = true;
	}

	public void do_shift_dn() {
		_shifted = false;
	}

	public void do_lock(int on) {
		if (on == 0) {}
	}

	public void do_settab() {
	}

	public void do_clrtab() {
	}

	public void do_tab() {
		// need to honor tabsets
		_text.appendText("\t");
	}

	public void do_crlf() {
		// need to check if at end of text, else
		// must non-destructively move to next line...
		_text.appendText("\n");
	}

	public void do_index() {
		// need to add spaces equiv to current column...
try {
		int pos = _jtext.getCaretPosition();
		int nl = _jtext.getLineCount();
		int cur = _jtext.getLineOfOffset(pos);
		if (cur + 1 == nl) {
			int col = pos - _jtext.getLineStartOffset(cur);
			_text.appendText("\n");
			// must be better way...
			while (col > 0) {
				_text.appendText(" ");
				--col;
			}
		} else {
			// handle later... previous revindex(es) landed us here...
		}
} catch(Exception e) { }
	}

	public void do_revindex() {
		// must move to previous line, possibly padding with spaces
	}

	public void do_space() {
		// need to check if at end of text, else
		// must non-destructively move to next character...
		_text.appendText(" ");
	}

	public void do_backspace() {
		// only back up as far as previous line-end
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
				r.x + (_wfm.width / 2) - (_caret.getWidth(comp) / 2),
						r.y + _wfm.height, comp);
			if (b) b = false;
		}
	}

	public void do_cn24_direct(char c) {
		if (c == ' ') {}
	}

	public void do_cn24(byte b) {
		boolean printable = true;
		if ((b & 0x0f) == 0x08) { // control characters...
			printable = false;
			switch((b & 0x30) >> 4) {
			case 0: // tab
				do_tab();
				break;
			case 1:	// return+index
				do_crlf();
				break;
			case 2: // nothing
			case 3: // nothing
				return;
			}
		} else if ((b & 0x06) == 0x02) {
			// X2, X3, Xa, Xb
			printable = false;
			switch((b & 0x39)) {
			case 0x00:	// space
				do_space();
				break;
			case 0x01:	// bkspace
				do_backspace();
				break;
			case 0x08:	// set tab
				do_settab();
				return;
			case 0x09:	// clr tab
				do_clrtab();
				return;
			case 0x10:
				do_shift_dn();
				return;
			case 0x11:
				do_shift_up();
				return;
			case 0x18:
				do_index();
				break;
			case 0x19:
				do_revindex();
				break;
			default:	// 2x, 3x: nothing
				return;
			}
		}
		if (printable) {
//System.err.println("doing byte "+b);
			String s;
			s = Wang_UI.getCharConv().tiltrotateToAscii(b, _shifted);
			if (s != null) {
				_text.appendText(s);
			}
		}
		_text.repaint();

		// "auto raise"...
		onOff(true);
	}
}
