// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_PlottingOutputWriter.java,v 1.1 2013/02/08 09:55:43 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class Wang_PlottingOutputWriter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_PlottingOutputWriter.java,v 1.1 2013/02/08 09:55:43 drmiller Exp $";

	public static final String Model = "02";
	public static final String Description = "Plotting Output Writer";

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
				_dx = _dy = 0;
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
		// now need to be able to convert 1/100ths onto pw x ph page...
		double sx = (12.0 * _wfm.width) / 100.0;
		double sy = (6.0 * _wfm.height) / 100.0;
		super.setScale(sx, sy);
	}

	public Wang_PlottingOutputWriter() {
		super(Wang_UI.getSeries() + Model, Description,
				new Font("Monospaced", Font.PLAIN, 12));
		_wfm = super.getFontMetrics();

		// default to portrait 8.5x11 with margins
		setPaper(8.5 - 1.0, 11 - 1.0);

		JMenu mu;
		mu = new JMenu("Typewriter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Home", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);

		_adjacent = false;
		_text.setCursor(_x, _y);
		_text.enableCursor(true); 
	}

	Wang_FontMetrics _wfm;
	private boolean _shifted;
	private boolean _plot;
	private boolean _adjacent;
	private int _x, _y;
	private int _dx, _dy;

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

	public void do_cn24(byte[] b) {
		boolean printable = true;
		if ((b[0] & 0x0f) == 0x08) { // control characters...
			switch((b[0] & 0x30) >> 4) {
			case 0: // nothing
				break;
			case 1:	// return+index handled below...
				_adjacent = false;
				_x = 0;
				if (_plot) return;
				index();
				printable = false;
				break;
			case 2:	// print mode
				_plot = false;	// cleanup?
				return;
			case 3:	// plot mode
				_plot = true;
				_dx = _dy = 0;
				return;
			}
		} else if ((b[0] & 0x06) == 0x02) {
			switch((b[0] & 0x30) >> 4) {
			case 0: // space/bspace or nothing
				_adjacent = false;
				if (_plot) return;
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
				_adjacent = false;
				if (_plot) return;
				if ((b[0] & 1) == 0) {
					index();
				} else {
					revindex();
				}
				printable = false;
				break;
			case 2:	// stepping
			case 3:	// stepping
				if (!_plot) return;
				switch(b[0] & 0x19) {
				case 0x00:
					_dx += 1;
					break;
				case 0x01:
					_dx -= 1;
					break;
				case 0x08:
					_dy += 1;
					break;
				case 0x09:
					_dy -= 1;
					break;
				case 0x10:
					_dx += 1;
					_dy += 1;
					break;
				case 0x11:
					_dx -= 1;
					_dy += 1;
					break;
				case 0x18:
					_dx += 1;
					_dy -= 1;
					break;
				case 0x19:
					_dx -= 1;
					_dy -= 1;
					break;
				}
				return;
			}
		}
		String s = null;
		if (printable) {
			s = Wang_UI.getCharConv().tiltrotateToAscii(b[0], _shifted);
		}
		if (_plot) {
			_x += _dx;
			if (_x < 0) _x = 0;
			if (_x >= 1300) _x = 1299; // 13 in. platten
			_y += _dy;
			if (_y < 0) _y = 0;
			_adjacent = false;
		}
		if (printable && s != null) {
			int m;
			if (_adjacent) {
				m = _text.appendLastPlot(s, _x, _y);
			} else {
				m = _text.addPlot(s, _x, _y);
			}
			if (!_plot) _x += m;
			_adjacent = true;
		}
		_text.setCursor(_x, _y);
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
