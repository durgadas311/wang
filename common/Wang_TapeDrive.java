// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_TapeDrive.java,v 1.9 2013/02/11 08:46:35 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.io.*;
import javax.swing.border.*;

class Wang_TapeDrive extends JComponent
{
	final String ident = "$Id: Wang_TapeDrive.java,v 1.9 2013/02/11 08:46:35 drmiller Exp $";
	static final long serialVersionUID = 311457692039L;
	java.io.RandomAccessFile _tf;
	java.io.OutputStream _fout;
	byte[] bb = new byte[2];
	byte[] b1 = new byte[1];
	boolean _wr;
	boolean _end;
	boolean _ready;
	boolean _tape_on;
	boolean _eot;
	boolean _prot;
	int _index;
	JLabel _window;
	JLabel _cassette;
	File _file;
	String _mountLabel;
	String _pickLabel;
	String _fileType;
	String _recordName;
	String _file_prop;
	byte _recordMark;

	public Wang_TapeDrive(OutputStream fout, String label,
				Color doorColor, Color windowColorRef,
				String name, String fileKind,
				String fileType, String recordName,
				byte recordMark, String file_prop) {
		_fout = fout;
		_file_prop = file_prop;
		Font font;
		_file = null;
		_index = 0;
		_end = false;
		_wr = false;
		_ready = false;
		_tape_on = false;
		_eot = false;
		_prot = false;
		_tf = null;

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
			//_fe.warning(_file.getAbsolutePath(), ee.getMessage());
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
		//_ready = true;
	}

	// Not used by Wang1200
	private int tape_skipone() {
		int nb = 0;
		int n = 1;
		b1[0] = 0;
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
		if ((_recordMark & 0x00ff) != (byte)0x00) {
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
		} else {
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
				_tf.seek(newidx * 108);
			} catch (IOException ee) {
				// can't happen?
			}
			_index = newidx;
		}
		// assert: _index == newidx
	}

	public boolean do_button(Wang_Keys btn) {
		// this kills any in-progress operations...
		_tape_on = false;
		if (btn.code == Wang_Keys.TAPE_READY) {
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

	private void send_word() {
		try {
			_fout.write(bb);
			_fout.flush();
		} catch (IOException ee) {
		}
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

	private void tape_read() {
		int n = 0;
		if (_tf == null || _end || !_tape_on || !_ready) {
			bb[0] = 0;
			bb[1] = 0x0e;
			return;
		}
		try {
			n = _tf.read(b1);
		} catch (IOException ee) {
			// close? _tf = null?
			n = 0;
		}
		if (n != 1) {
			bb[0] = 0;
			bb[1] = 0x0e;
			_end = true;
			_eot = true;
		} else {
			bb[0] = b1[0];
			bb[1] = 0x0c;
		}
	}

	private void tape_write(byte[] b) {
		if (_prot) return;
		try {
			_tf.write(b[0]);
		} catch (IOException ee) {
			// can't happen?
		}
	}

	public void do_tape(byte[] b) {
		if (b[1] == 0x0d) {		// tape on - read
			if (b[0] == 0) { // tape-on
				if (_ready) _tape_on = true;
				_end = false;
				_wr = false; // redundant
			} else { // request for next byte
				tape_read();
				if ((bb[0] & 0x00ff) == (_recordMark & 0x00ff)) { // END PROG
					// there is always one more byte..
					tape_read();
					// might be old image... treat EOF same...
					if ((bb[1] & 0x00ff) == 0x0e) {	// saw EOF
						bb[0] = _recordMark;
						bb[1] = 0x0c;
					}
					if ((bb[0] & 0x00ff) != (_recordMark & 0x00ff)) {
						bb[0] = 0;
						bb[1] = 0x0e;
					}
					++_index; // display updated later...
					_end = true;
				}
				send_word();
			}
			return;
		} else if (b[1] == 0x0f) {	// tape on - write
			// should not happen on Wang1200...
			if (_ready) _tape_on = true;
			_wr = true;
			_end = false;
		} else if (b[1] == 0x0e) {	// tape off
			// should not happen on Wang1200...
			if (_wr && !_end && _ready) {
				// did not just write END PROG, so need
				// to mark end of tape "file".
				// use _recordMark 0xff to mean "invisible" END PROG
				b[1] = 0x0c;
				b[0] = _recordMark;
				tape_write(b);
				b[0] = (byte)0xff;
				tape_write(b);
				++_index;
			}
			_tape_on = false;
			_wr = false;
			_end = false;
			update_tape();
			//if (_ready) _tf.flush(); // not needed anyway?
		} else if (b[1] == 0x0c) {	// tape write
			if (!_ready) return;
			tape_write(b);
			if ((_recordMark & 0x00ff) != (byte)0x00) {
				// only if last byte before tape-off is END PROG...
				_end = ((b[0] & 0x00ff) == (_recordMark & 0x00ff)); // END PROG
				if (_end) {
					tape_write(b); // write _recordMark _recordMark - true END PROG
					++_index; // display updated later..
				}
			}
		} else {
			System.err.format("invalid tape command (%04x)\n", (b[1] << 8) | b[0]);
		}
	}
}
