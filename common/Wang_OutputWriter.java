// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_OutputWriter.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $

import java.awt.*;

class Wang_OutputWriter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_OutputWriter.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $";
	private byte[] cn24_xlate;
	private String[] cn24_spcl;

	public void reset() {
		// anything?
	}

	private void setup_xlate() {
		cn24_xlate = new byte[256];
		cn24_xlate[0x00] = '-';
		cn24_xlate[0x01] = 'y';
		cn24_xlate[0x02] = ' ';
		cn24_xlate[0x03] = '\b';
		cn24_xlate[0x04] = 'q';
		cn24_xlate[0x05] = 'p';
		cn24_xlate[0x06] = '=';
		cn24_xlate[0x07] = 'j';
		// cn24_xlate[0x08] = ' ';	// no op
		cn24_xlate[0x09] = '/';
		//cn24_xlate[0x0a] = ' ';	// no op
		//cn24_xlate[0x0b] = ' ';	// no op
		cn24_xlate[0x0c] = ',';
		cn24_xlate[0x0d] = ';';
		cn24_xlate[0x0e] = 'f';
		cn24_xlate[0x0f] = 'g';

		cn24_xlate[0x10] = 'w';
		cn24_xlate[0x11] = 's';
		//cn24_xlate[0x12] = '';	// shift dn
		//cn24_xlate[0x13] = '';	// shift up
		cn24_xlate[0x14] = 'i';
		cn24_xlate[0x15] = '\'';
		cn24_xlate[0x16] = '.';
		cn24_xlate[0x17] = '\001';	// 1/2...
		cn24_xlate[0x18] = '\n';
		cn24_xlate[0x19] = 'o';
		cn24_xlate[0x1a] = '\n';
		//cn24_xlate[0x1b] = '\n';	// rev index
		cn24_xlate[0x1c] = 'a';
		cn24_xlate[0x1d] = 'r';
		cn24_xlate[0x1e] = 'v';
		cn24_xlate[0x1f] = 'm';

		cn24_xlate[0x20] = 'b';
		cn24_xlate[0x21] = 'h';
		//cn24_xlate[0x22] = '+';	// step x+
		//cn24_xlate[0x23] = '+';	// step x-
		cn24_xlate[0x24] = 'k';
		cn24_xlate[0x25] = 'e';
		cn24_xlate[0x26] = 'n';
		cn24_xlate[0x27] = 't';
		//cn24_xlate[0x28] = '';	// print mode
		cn24_xlate[0x29] = 'l';
		//cn24_xlate[0x2a] = '+';	// step y+
		//cn24_xlate[0x2b] = '+';	// step y-
		cn24_xlate[0x2c] = 'c';
		cn24_xlate[0x2d] = 'd';
		cn24_xlate[0x2e] = 'u';
		cn24_xlate[0x2f] = 'x';

		cn24_xlate[0x30] = '9';
		cn24_xlate[0x31] = '0';
		//cn24_xlate[0x32] = '';	// step x+y+
		//cn24_xlate[0x33] = '';	// step x-y+
		cn24_xlate[0x34] = '6';
		cn24_xlate[0x35] = '5';
		cn24_xlate[0x36] = '2';
		cn24_xlate[0x37] = 'z';
		//cn24_xlate[0x38] = '';	// plot mode
		cn24_xlate[0x39] = '4';
		//cn24_xlate[0x3a] = '';	// step x+y-
		//cn24_xlate[0x3b] = '';	// step x-y-
		cn24_xlate[0x3c] = '8';
		cn24_xlate[0x3d] = '7';
		cn24_xlate[0x3e] = '3';
		cn24_xlate[0x3f] = '1';

		// shifted versions...
		cn24_xlate[0x40] = '_';
		cn24_xlate[0x41] = 'Y';
		cn24_xlate[0x42] = ' ';
		cn24_xlate[0x43] = '\b';
		cn24_xlate[0x44] = 'Q';
		cn24_xlate[0x45] = 'P';
		cn24_xlate[0x46] = '+';
		cn24_xlate[0x47] = 'J';
		cn24_xlate[0x49] = '?';
		cn24_xlate[0x4c] = ',';
		cn24_xlate[0x4d] = ':';
		cn24_xlate[0x4e] = 'F';
		cn24_xlate[0x4f] = 'G';

		cn24_xlate[0x50] = 'W';
		cn24_xlate[0x51] = 'S';
		cn24_xlate[0x54] = 'I';
		cn24_xlate[0x55] = '"';
		cn24_xlate[0x56] = '.';
		cn24_xlate[0x57] = '\002';	// 1/4
		cn24_xlate[0x58] = '\n';
		cn24_xlate[0x59] = 'O';
		cn24_xlate[0x5a] = '\n';
		//cn24_xlate[0x5b] = '\n';	// rev index
		cn24_xlate[0x5c] = 'A';
		cn24_xlate[0x5d] = 'R';
		cn24_xlate[0x5e] = 'V';
		cn24_xlate[0x5f] = 'M';

		cn24_xlate[0x60] = 'B';
		cn24_xlate[0x61] = 'H';
		cn24_xlate[0x64] = 'K';
		cn24_xlate[0x65] = 'E';
		cn24_xlate[0x66] = 'N';
		cn24_xlate[0x67] = 'T';
		cn24_xlate[0x69] = 'L';
		cn24_xlate[0x6c] = 'C';
		cn24_xlate[0x6d] = 'D';
		cn24_xlate[0x6e] = 'U';
		cn24_xlate[0x6f] = 'X';

		cn24_xlate[0x70] = '(';
		cn24_xlate[0x71] = ')';
		cn24_xlate[0x74] = '\003';	// cent
		cn24_xlate[0x75] = '%';
		cn24_xlate[0x76] = '@';
		cn24_xlate[0x77] = 'Z';
		cn24_xlate[0x79] = '$';
		cn24_xlate[0x7c] = '*';
		cn24_xlate[0x7d] = '&';
		cn24_xlate[0x7e] = '#';
		cn24_xlate[0x7f] = '!';

		cn24_spcl = new String[32];
		cn24_spcl[0x01] = "\u00BD";
		cn24_spcl[0x02] = "\u00BC";
		cn24_spcl[0x03] = "\u00A2";
	}

	public Wang_OutputWriter() {
		super("611 Output Writer",
				new Font("Monospaced", Font.PLAIN, 10),
				96, 32, 0, 0);
		setup_xlate();
	}

	private boolean _shifted;
	private boolean _plot;
	private int _x, _y;
	private int _dx, _dy;

	private void index() {
		_y += 14;
	}

	private void revindex() {
		_y -= 14;
		if (_y < 0) _y = 0;
	}

	private void space() {
		_x += 10;
		if (_x >= 1300) _x = 1299;
	}

	private void bkspace() {
		_x -= 10;
		if (_x < 0) _x = 0;
	}

	public void do_cn24(byte[] b) {
		if ((b[0] & 0x0f) == 0x08) { // control characters...
			switch((b[0] & 0x30) >> 4) {
			case 0: // nothing
				break;
			case 1:	// return+index handled below...
				_x = 0;
				if (_plot) return;
				index();
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
				if (_plot) return;
				if ((b[0] & 1) == 0) {
					space();
				} else {
					bkspace();
				}
				break;
			case 1:	// index/rev or shift...
				if ((b[0] & 0x0e) == 0x02) {
					_shifted = ((b[0] & 1) != 0);
					return;
				}
				if (_plot) return;
				if ((b[0] & 1) == 0) {
					index();
				} else {
					revindex();
				}
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
		byte p;
		if (_shifted) {
			p = cn24_xlate[b[0] + 0x40];
		} else {
			p = cn24_xlate[b[0]];
		}
		byte[] bb;
		String s;
		if (p == 0) {
			s = new String("<"+b[0]+">");
		} else if (p < 0x07) {
			s = cn24_spcl[p];
		} else {
			bb = new byte[1];
			bb[0] = p;
			s = new String(bb);
		}
		if (_plot) {
			_x += _dx;
			if (_x < 0) _x = 0;
			if (_x >= 1300) _x = 1299; // 13 in. platten
			_y += _dy;
			if (_y < 0) _y = 0;
			_hasGraphic = true;
			_text.addPlot(s, _x, _y);
			_text.repaint();
			//_text.setCaretPosition(_eop); // to what?
			// todo: need to get JScrollPane to update...
		} else {
			_text.append(s);
			_eop += s.length();
			_text.setCaretPosition(_eop);
		}
		// "auto raise"...
		onOff(true);
	}
}
