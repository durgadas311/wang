// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_TapeDrive.java,v 1.19 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.swing.border.*;

class Wang_TapeDrive extends JComponent
{
	final String ident = "$Id: Wang_TapeDrive.java,v 1.19 2014/01/14 21:53:51 drmiller Exp $";
	static final long serialVersionUID = 311457692039L;
	java.io.RandomAccessFile _tf;
	boolean _wr;
	boolean _end;
	boolean _ready;
	boolean _tape_on;
	boolean _eot;
	boolean _prot;
	byte _op; // "live" version of _prot (++)
	int _index;
	JLabel _window;
	JLabel _cassette;
	Wang_Keys _eject;
	File _file;
	String _mountLabel;
	String _pickLabel;
	String _fileType;
	String _recordName;
	String _file_prop;
	byte _recordMark;
	int _recordLen;
	int _bytc;
	boolean _initReady;

	public Wang_TapeDrive(Wang_Keys ej, String label,
				Color doorColor, Color windowColorRef,
				String name, String fileKind,
				String fileType, String recordName,
				byte recordMark, // 0 == unused
				int recordLen,   // 0 == unused
				boolean autoReady,
				String file_prop) {
		_eject = ej;
		_file_prop = file_prop;
		Font font;
		_file = null;
		_index = 0;
		_end = false;
		_wr = false;
		_ready = false;
		_tape_on = false;
		_eot = false;
		_op = 0;
		_prot = false;
		_tf = null;
		_initReady = autoReady;

		if (name != null) {
			_mountLabel = "Mount " + name + " Tape";
		} else {
			_mountLabel = "Mount Tape";
		}
		if (fileKind != null) {
			_pickLabel = "Wang " + fileKind + " files";
		} else {
			_pickLabel = "Wang files";
		}
		if (fileType != null) {
			_fileType = fileType;
		} else {
			_fileType = "wng";
		}
		_recordName = recordName;
		_recordMark = recordMark;
		_recordLen = recordLen;

		setLayout(new FlowLayout());

		Border lb;
		JLayeredPane jp = new JLayeredPane();
		jp.setOpaque(true);
		jp.setPreferredSize(new Dimension(300, 200));

		java.net.URL url;
		ImageIcon ic;
		JLabel cs;

		int layer = 1;

		// create a version of color with transparency
		Color windowColor = new Color(windowColorRef.getRed(),
						windowColorRef.getGreen(),
						windowColorRef.getBlue(),
						192);

		JLabel cass = new JLabel(label, SwingConstants.CENTER);
		// this may not be the correct threashold...
		boolean tooDark = (doorColor.getRed() + doorColor.getGreen() +
					doorColor.getBlue() < 10);
		if (tooDark) {
			lb = BorderFactory.createBevelBorder(BevelBorder.RAISED,
						Color.white, Color.gray);
			cass.setForeground(Color.white);
		} else {
			lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
			cass.setForeground(Color.black); // needs different threashold...
		}
		cass.setBorder(lb);
		cass.setVerticalAlignment(SwingConstants.TOP);
		cass.setHorizontalAlignment(SwingConstants.CENTER);
		cass.setBackground(doorColor);
		cass.setOpaque(true);
		font = null;
		font = new Font("Serif", Font.PLAIN, 18);
		cass.setPreferredSize(new Dimension(300, 200));
		cass.setBounds(0, 0, 300, 200);
		cass.setFont(font);
		jp.add(cass, new Integer(layer), layer);
		++layer;

		url = this.getClass().getResource("icons/cassette_none_gry.png");
		ic = new ImageIcon(url);
		cs = new JLabel(ic);
		cass.setOpaque(true);	// image handles transparency...
		cs.setBounds(50, 75, 201, 100);
		jp.add(cs, new Integer(layer), layer);
		++layer;

		url = this.getClass().getResource("icons/cassette_tape_data.png");
		ic = new ImageIcon(url);
		_cassette = new JLabel(ic);
		cass.setOpaque(true);	// image handles transparency...
		_cassette.setBounds(50, 75, 201, 100);
		_cassette.setVisible(false);
		jp.add(_cassette, new Integer(layer), layer);
		++layer;

		_window = new JLabel("Tape Source/Dest");
		lb = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		_window.setBorder(lb);
		_window.setVerticalAlignment(SwingConstants.BOTTOM);
		_window.setHorizontalAlignment(SwingConstants.LEFT);
		_window.setForeground(Color.black);
		_window.setBackground(windowColor);
		_window.setOpaque(true);	// color handles transparency...
		font = null;
		font = new Font("Sans-serif", Font.PLAIN, 10);
		_window.setPreferredSize(new Dimension(200, 100));
		_window.setBounds(50, 75, 200, 100);
		_window.setFont(font);

		_file = Wang_UI.getProperties().getFile(file_prop, true, Wang_UI.getDir());
		if (_file != null) {
			_prot = true;
		}
		tape_open();
		update_tape();
		jp.add(_window, new Integer(layer), layer);
		++layer;

		add(jp);

	}

	private void update_tape() {
		String txt;
		if (_file == null) {
			txt = new String("<HTML><FONT SIZE=+1>(no tape)</FONT></HTML>");
		} else {
			_cassette.setVisible(true);
			String eot;
			String prot;
			if (_eot) {
				eot = new String(" (end)");
			} else {
				eot = new String("");
			}
			if (_prot) {
				prot = new String(" <B>(R/O)</B>");
			} else {
				prot = new String("");
			}
			txt = new String("<HTML><B>Tape Name:</B>" + prot + "<BR>" +
				_file.getName() + "<BR>" +
				"<B>" + _recordName + " #</B> " + _index + eot +
				"</HTML>");
		}
		_window.setText(txt);
		repaint();
	}

	private void pick_file() {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
				_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			_prot = ch.isProtected();
			if (_file.exists() && !_file.canWrite()) {
				_prot = true;
			}
		} else {
			_file = null;
			_prot = false;
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().newInstance(),
				_file_prop,
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		tape_open();
	}

	private void tape_open() {
		if (_file == null) {
			_eot = false;
			_index = 0;
			return;
		}
		String mode = "rw";
		if (_prot) mode = "r";
		try {
			_tf = new RandomAccessFile(_file.getAbsolutePath(), mode);
		} catch (FileNotFoundException ee) {
			// can't happen?
			_file = null;
			_prot = false;
			return;
		}
		// not needed?
		try {
			_tf.seek(0);
		} catch (IOException ee) {
			// can't happen?
		}
		_eot = false;
		_index = 0;
		_ready = _initReady;
	}

	// Not used by Wang1200 (only when _recordMark != 0)
	private int tape_skipone() {
		int nb = 0;
		int n = 1;
		byte[] b1 = new byte[]{0};
		while (n == 1 && (b1[0] & 0x00ff) != (_recordMark & 0x00ff)) {
			try {
				n = _tf.read(b1);
			} catch (IOException ee) {
				// close? _tf = null?
				n = 0;
			}
			if (n == 1) {
				++nb;
			}
		}
		if (n == 1) { // must have seen END PROG...
			// gobble next byte, don't care what it was (for now).
			try {
				n = _tf.read(b1);
			} catch (IOException ee) {
			}
		}
		if (nb > 0) {
			++_index;
			return 1;
		}
		_eot = true;
		return 0;
	}

	private void tape_position(int newidx) {
		if (_file == null) return;
		if (newidx < 0) return;

		if (_recordLen > 0) {
			if (newidx == 0) { // rewind
				try {
					_tf.seek(0);
				} catch (IOException ee) {
					// can't happen?
				}
				_index = 0;
				_eot = false;
				return;
			}
			if (newidx == _index) return;	// should not happen
			try {
				_tf.seek(newidx * _recordLen);
			} catch (IOException ee) {
				// can't happen?
			}
			_index = newidx;

		} else if (_recordMark != 0) {
			if (newidx == _index) return;	// should not happen
			// TBD: change position of file I/O
			if (newidx < _index) { // rewind
				try {
					_tf.seek(0);
				} catch (IOException ee) {
					// can't happen?
				}
				_index = 0;
				_eot = false;
			}
			while (_index < newidx && tape_skipone() == 1);
		}
		// assert: _index == newidx
	}

	public Wang_Keys ejectKey() {
		return _eject;
	}

	public boolean do_button(Wang_Keys btn) {
		// this kills any in-progress operations...
		_tape_on = false;
		if (btn.code == Wang_Keys.TAPE_READY) { // not for Wang1200
			if (_file == null) {
				_ready = false;
				return true;
			}
			_ready = btn.state;
			return false;
		}
		_ready = false;
		if (btn.code == Wang_Keys.TAPE_REW) { // not for Wang1200
			tape_position(_index - 1);
		} else if (btn.code == Wang_Keys.TAPE_FF) { // not for Wang1200
			tape_position(_index + 1);
		} else if (btn.code == Wang_Keys.TAPE_EJECT) {
			_cassette.setVisible(false);
			pick_file();
		}
		update_tape();
		return true;	// reset button OFF - i.e. momentary only
	}

	private void tape_close() {
		if (_tf != null) {
			try {
				_tf.close();
			} catch (IOException ee) {
			}
			_end = false;
			_tf = null;
		}
	}

	private int tape_read() {
		byte[] b1 = new byte[]{0};
		int n = 0;
		if (_tf == null || _end || !_tape_on || !_ready) {
			return -1;
		}
		try {
			n = _tf.read(b1);
		} catch (IOException ee) {
			// close? _tf = null?
			n = 0;
		}
		if (n != 1) {
			_end = true;
			_eot = true;
			return -1;
		} else {
			return b1[0] & 0x00ff;
		}
	}

	private void tape_write(byte b) {
		if (_prot) return;
		try {
			_tf.write(b);
		} catch (IOException ee) {
			// can't happen?
		}
	}

	private byte _last_rec_byte;
	private int _num_rec_bytes;

	public void tape_on(int wr) {
		if (wr == 0) { // tape on - read
			if (_ready) _tape_on = true;
			_end = false;
			_wr = false; // redundant
		} else {
			_last_rec_byte = 0;
			_num_rec_bytes = 0;
			if (_ready) _tape_on = true;
			_wr = true;
			_end = false;
		}
		if (_ready && (!_wr || !_prot)) _op = 1;
		else _op = 0;
	}

	public void tape_off(int wr) {
		// should not happen on Wang1200...
		// but might happen many times without a matching tape_on()
		// (e.g. every time PRIME is pressed)
		if (_recordMark != 0 && _wr && _ready && _num_rec_bytes > 0) {
			_num_rec_bytes = 0;
			// always write at least this... this is either the
			// first or second "_recordMark"
			tape_write(_recordMark);
			// only if last byte before tape-off was END PROG...
			_end = ((_last_rec_byte & 0x00ff) ==
					(_recordMark & 0x00ff)); // END PROG
			if (!_end) {
				// did not just write END PROG, so need
				// to mark end of tape "file".
				// use _recordMark 0xff to mean "invisible" END PROG
				tape_write((byte)0xff);
			}
			++_index; // display updated later..
		}
		_tape_on = false;
		_wr = (wr != 0);
		_wr = false;
		_end = false;
		_op = 0;
		update_tape();
		//if (_ready) _tf.flush(); // not needed anyway?
	}

	private void tape_onOff(boolean on, byte rc, byte tm, byte hi, byte rv, byte hl) {
		if (tm != 0) {} // stupid warnings
		_wr = (rc != 0);
//		if (_wr && !_end && _ready) {
//			++_index;
//		}
		_tape_on = _ready && on;
// not the lock signal?	// visual indicator of what might be the "door lock"
//		if (hl != 0) {
//			ejectBtn().setBackground(_Key.red1);
//		} else {
//			ejectBtn().setBackground(_Key.white1);
//		}
		if (hl != 0) {
			if (_tape_on) {
				// fast-forward or rewind...
				_op = 0;
				// now change file position...
				// TBD: what to do for FORWARD
				tape_position(rv != 0 ? 0 : -1);
			} else {
				if (_ready) _op = 1;
				else _op = 0;
			}
		} else if (hi != 0) {
			// ready for record/play...
			// TBD: test RO file...
			if (_ready && (!_wr || !_prot)) _op = 1;
			else _op = 0;
			// for reverse, just update position...
			if (_tape_on && rv != 0) {
				tape_position(_index - 1);
			}
		}
		if (!_tape_on) _bytc = 0;
		_end = false;
		update_tape();
	}

	public int tape_prot() { return _op; }

	public void tape_on(byte rc, byte tm, byte hi, byte rv, byte hl) {
		tape_onOff(true, rc, tm, hi, rv, hl);
	}

	public void tape_off(byte rc, byte tm, byte hi, byte rv, byte hl) {
		tape_onOff(false, rc, tm, hi, rv, hl);
	}

	public int tape_play() {
		// request for next byte
		int b = tape_read();
		if (b < 0) {
			if (!_end && _bytc > 0) {
				_bytc = 0;
				++_index;
				update_tape();
			}
			_end = true;
		} else if (_recordLen > 0) {
			++_bytc;
			if (_bytc >= _recordLen) {
//System.err.println("Tape Read ++index ("+_index+" @ "+_bytc+")");
				_bytc = 0;
				++_index;
				update_tape();
			}
		} else if (_recordMark != 0) {
			if (b == (_recordMark & 0x00ff)) { // END PROG
				// there is always one more byte..
				// but, if not _recordMark or 0xff then
				// we must pass the two bytes as-is.
				b = tape_read();
				// might be old image... treat EOF same...
				if (b < 0) {    // saw EOF
					b = _recordMark;
				}
				if (b == 0xff) {
					b = -1;
				} else if (b != (_recordMark & 0x00ff)) {
					// need to back-pedal...
					b |= ((_recordMark & 0x00ff) << 8);
					// caller must check high byte...
					return b;
				}
				++_index;
				_end = true;
				update_tape();
			}
		}
		return b;
	}

	public void tape_record(int byt) {
		if (!_ready) return;
		++_num_rec_bytes;
		tape_write((byte)byt);
		if (_recordLen > 0) {
			++_bytc;
			if (_bytc >= _recordLen) {
//System.err.println("Tape Write ++index ("+_index+" @ "+_bytc+")");
				_bytc = 0;
				++_index;
				update_tape();
			}
		} else if (_recordMark != 0) {
			_last_rec_byte = (byte)byt;
		}
	}
}
