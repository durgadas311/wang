// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_PaperTapeReader.java,v 1.9 2014/01/26 14:52:57 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;

class Wang_PaperTapeReader implements Wang_GroupIODevice
{
	final String ident = "$Id: Wang_PaperTapeReader.java,v 1.9 2014/01/26 14:52:57 drmiller Exp $";

	public static final String Model = "03";
	public static final String Description = "Paper Tape Reader";

	// Group 1 00 00 = skip non-num, tread numeric until non-numeric, GO
	// Group 1 00 07 = skip until CR, GO

	String _prop;
	String _mountLabel;
	String[] _pickLabel;
	String[] _fileType;
	File _file;
	Component _comp;

	int _iob;
	boolean _input;	// send to Wang vs. skip (00-00 vs. 00-07)
	boolean _end;
	int _currByte;	// -1 for none (BOT or EOT)
	private static final String numerics = "0123456789.+-";
	InputStream _fin;

	private boolean isNumeric() {
		int x = numerics.indexOf((char)_currByte);
		return (x >= 0);
	}

	public void reset() {
		_input = false;
		_iob = 0;
	}

	private void tape_close() {
		if (_fin != null) {
			try {
				_fin.close();
			} catch (Exception ee) {}
			_fin = null;
		}
	}

	private void tape_open() {
		_end = false;
		_currByte = -1;
		if (_file == null) {
			return;
		}
		try {
			_fin = new FileInputStream(_file);
		} catch (Exception ee) {
		}
	}

	private void getByte() {
		if (_fin == null) {
			_end = true;
			_currByte = -1;
			return;
		}
		int b = -1;
		try {
			b = _fin.read();
		} catch(Exception ee) {
		}
		if (b < 0) {
			_end = true;
			_currByte = -1;
		} else {
			_currByte = (b & 0x0ff);
		}
	}

	private void skiptoNum() {
		if (_currByte < 0) {
			getByte();
		}
		// what about pass-thru of data to OutputWriter?
		while (_currByte >= 0 && !isNumeric()) {
			getByte();
		}
	}

	private void skiptoCR() {
		if (_end) {
			return;
		}
		if (_currByte < 0) {
			getByte();
		}
		// what about pass-thru of data to OutputWriter?
		while (_currByte >= 0 && _currByte != '\n') {
			getByte();
		}
	}

	public boolean start_cn36(int iob, int c) {
		if (_file == null) {
			//unless we allow mounting a tape later...
			return false;
		}
		// currently, don't care if running program or not...
		if ((iob & 0x05) != 4) return false;
		_iob = iob;
		if (c == 0x00) {
			_input = true;
			// arrange to read number and send GO
			// can this start here, or must we wait until
			// core processes our return?
			skiptoNum();
			do_ack(iob);
			return true;
		}
		if (c == 0x07) {
			_input = true;	// do not send codes to Calculator...
					// but must still send GO/EOT...
			// arrange to skip to CR and send GO
			skiptoCR();
			// no data transmitted... but is GO still needed???
			// current byte is NOT numeric.
			do_ack(iob);
			return true;
		}
		return false;
	}

	public void pickFile(JMenuItem m) {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(_comp);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			m.setText(getName() + " - " + _file.getName());
		} else {
			_file = null;
			m.setText(getName() + " - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().newInstance(),
				_prop,
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		tape_open();
	}

	private void sendNum() {
		int b = 0;
		if (_currByte >= '0' && _currByte <= '9') {
			b = (_currByte & 0x0f) + E0;
		} else if (_currByte == '.') {
			b = DP;
		} else if (_currByte == '-') {
			b = CHG_SIGN;
		}
		if (b > 0) {
			Wang_UI.getCore().replyIO(_iob, b);
			getByte();
		}
	}

	public void do_ack(int iob) {
		// should be ACK for previous code... should enable next...
		// TODO
		// check mode of operation and send next code...
		// What about pass-through of data?
		if (_input) {
			if (isNumeric()) {
				sendNum();
			} else {
				// Not sure if both should be sent, but without
				// the GO a program can't continue after EOT so
				// for the sake of programmability we add it here.
				// (a RETURN from the EOT subroutine goes back to
				// keyboard mode, not running the program)
				if (_end) {
					Wang_UI.getCore().replyIO(_iob, EOT);
				}
				Wang_UI.getCore().replyIO(_iob, GO);
				_input = false;
			}
		}
	}

	public void do_dev(int iob, int b) {
		// right now only ACK happens
		// TODO
	}

	public int getGLRN() { return 0; }

	public JMenuItem getMenu(int key) {
		String status = "not mounted";
		if (_file != null) {
			status = _file.getName();
		}
		return new JMenuItem(getName() + " - " + status, key);
	}

	public Wang_PaperTapeReader(String prop, Component comp) {
		//super(Wang_UI.getSeries() + Model, Description);

		_input = false;
		_mountLabel = "Mount Tape";
		_pickLabel = new String[]{"Wang Data files","Text Files"};
		_fileType = new String[]{"wdf","txt"};
		_prop = prop;
		_comp = comp;
		_file = Wang_UI.getProperties().getFile(_prop, true, Wang_UI.getDir());
		if (_file != null) {
			tape_open();
		}

		Wang_CN36_Bus.registerCN36(this);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
