// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.concurrent.LinkedBlockingDeque;

class Wang_PaperTapeReader implements Wang_GroupIODevice, ActionListener, Runnable
{
	public static final String Model = "03";
	public static final String Description = "Paper Tape Reader";

	private static JMenuItem pmi = null;
	private static Wang_PaperTapeReader thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Description;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_PaperTapeReader s_getInstance(Component comp) {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s03_image",
			Wang_UI.getSeries(), Wang_UI.getSeries());
		thus = new Wang_PaperTapeReader(p, comp);
		return thus;
	}

	static JMenuItem dev_mi = null;
	private boolean plugged = false;

	public String getModel() { return s_getModel(); }
	public String getName() { return s_getName(); }
	public void plugIn(JMenu mu) {
		if (plugged) return;
		plugged = true;
		if (pmi != null) {
			pmi.setText(s_getName() + " (installed)");
		}
		if (mu != null) {
			mu.add(getMenu());
		}
		Wang_CN36_Bus.registerCN36(this);
		// onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		Wang_CN36_Bus.deregisterCN36(this);
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		// onOff(false);
	}
	public boolean isPlugged() { return plugged; }
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		String status = "not mounted";
		if (_file != null) {
			status = _file.getName();
		}
		dev_mi = new JMenuItem(s_getName() + " - " + status, KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}

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
	LinkedBlockingDeque<Integer> giChr;
	boolean gkbd; // actually, !GKBD

	private boolean isNumeric() {
		int x = numerics.indexOf((char)_currByte);
		return (x >= 0);
	}

	public void reset() {
		giChr.clear(); // still could be one in the chamber...
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
		_input = false;
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
			sendNum(); // queue entire number, and GO
		} else if (c == 0x07) {
			_input = true;	// do not send codes to Calculator...
					// but must still send GO/EOT...
			// arrange to skip to CR and send GO
			skiptoCR();
			// no data transmitted... but is GO still needed.
			giChr.add(GO);
		}
		return _input;
	}

	// queue entire number, including GO.
	// _input == true is implied.
	private void sendNum() {
		while (isNumeric()) {
			int b = 0;
			if (_currByte >= '0' && _currByte <= '9') {
				b = (_currByte & 0x0f) + E0;
			} else if (_currByte == '.') {
				b = DP;
			} else if (_currByte == '-') {
				b = CHG_SIGN;
			}
			if (b > 0) {
				giChr.add(b);
			}
			getByte();
		}
		// Not sure if both should be sent, but without
		// the GO a program can't continue after EOT so
		// for the sake of programmability we add it here.
		// (a RETURN from the EOT subroutine goes back to
		// keyboard mode, not running the program)
		if (_end) {
			giChr.add(EOT);
		}
		giChr.add(GO);
	}

	public void do_ack(int iob) {} // not used
	public void do_dev(int iob, int b) {} // not a block i/o device

	public int getGLRN() { return 0; }
	public void setGKBD(boolean state) { gkbd = !state; }
	public boolean isBlockIO() { return false; }
	public boolean isDevEnabled() { return _input; }
	public void setProperties(Wang_Properties p) {}
	public boolean onOff() { return false; }
	public void onOff(boolean vis) {}
	public JFrame getFrame() { return null; }
	public Component getComponent() { return null; }

	public Wang_PaperTapeReader(String prop, Component comp) {
		//super(Wang_UI.getSeries() + Model, Description);
		giChr = new LinkedBlockingDeque<Integer>();

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
		Thread t = new Thread(this);
		t.start();
	}

	public void actionPerformed(ActionEvent e) {
		// There is only one, but decode it anyway...
		Object src = e.getSource();
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		if (mi.getMnemonic() != KeyEvent.VK_D) return;
		// assert mi == dev_mi
		// There is no window, so only pop-up file dialog
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(_comp);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			mi.setText(getName() + " - " + _file.getName());
		} else {
			_file = null;
			mi.setText(getName() + " - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().
					getDeclaredConstructor().newInstance(),
				_prop,
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		tape_open();
	}

	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giChr.take();
				while (!gkbd) {
					Thread.sleep(10);
				}
			} catch (Exception ee) {}
			if (c < 0) continue; // or break?
			if (!_input) continue; // PRIME, etc.
			if (c == GO) {
				_input = false;
			}
			Wang_UI.getCore().replyIO(_iob, c);
		}
	}
}
