// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_Teletype.java,v 1.3 2014/01/03 23:48:40 drmiller Exp $

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;

class Wang_Teletype extends ASR33_Teletype
		implements Wang_InputDevice, ActionListener
{
	final String ident = "$Id: Wang_Teletype.java,v 1.3 2014/01/03 23:48:40 drmiller Exp $";

	public static final String Model = "07";
	public static final String Description = "Teletype";

	// Group 1 15 xx = "Run" mode (similar to PaperTapeReader)
	// Group 2 15 xx = "Learn" mode
	// 15 00 = X ON (activate paper tape, as for '03)
	// 15 01 = print "1"
	// 15 02 = print "2"
	// 15 03 = print "3"
	// 15 04 = print "4"
	// all other: normal start
	// Accept all "OutputWriter" text from calculator.

	static private JMenu _mu = null;
	static private JMenuItem _miRdr = null;
	static private JMenuItem _miPunT = null;
	static private JMenuItem _miPunS = null;

	static public JMenu getMenu() {
		if (_mu == null) {
			String status = " (unknown)";
			_mu = new JMenu(getName() + "...");
			_miRdr = new JMenuItem("Reader" + status, KeyEvent.VK_R);
			_mu.add(_miRdr);
			JMenu mu = new JMenu("Punch...");
			_miPunS = new JMenuItem("Save", KeyEvent.VK_S);
			mu.add(_miPunS);
			_miPunT = new JMenuItem("Tear Off", KeyEvent.VK_T);
			mu.add(_miPunT);
			_mu.add(mu);
			_mu.setEnabled(false);
		}
		return _mu;
	}

	String _propBase;
	String _mountLabel;
	String[] _pickLabel;
	String[] _fileType;
	File _file;
	boolean _xOn;

	boolean _end;
	InputStream _fin;

	private int getRdrByte() {
		if (_fin == null) {
			_end = true;
			return -1;
		}
		int b = -1;
		try {
			b = _fin.read();
		} catch(Exception ee) {
		}
		if (b < 0) {
			_end = true;
		} else {
			// TTY keyboard could not generate characters 0x60-0x7f,
			// but the I/O channel, punch, and reader do support all.
			if (b > 0x7f) {
				b = 0x00ff; // RUBOUT
			}
		}
		return b;
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
		if (_file == null) {
			return;
		}
		try {
			_fin = new FileInputStream(_file);
		} catch (Exception ee) {}
	}

	private void setupRdr() {
		String status = " (not mounted)";
		if (_file != null) {
			status = _file.getName();
			tape_open();
		}
		_miRdr.setText("Reader" + status);
	}

	private int _glrn;
	private int _iob;
	private int _bytes;
	private boolean _cr;	// last character was CR...
	boolean _input;

	public void reset() {
		_glrn = 0;
		_input = false;
	}

	public void onOff(boolean on) {
		if (!on) {
			_input = false;
			_inp = null;
		}
		super.onOff(on);
		if (_mu != null) {
			_mu.setEnabled(on);
		}
	}

	private void xOn() {
		// start the paper tape reader...
		_xOn = true;
		// need to wakeup InputProxy... how?
	}

	private void xOff() {
		// stop the paper tape reader...
	}

	private void pickRdrFile() {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(null);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
		} else {
			_file = null;
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().newInstance(),
				_propBase + "rdr_image",
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		setupRdr();
	}

	private void truncPun() {
	}

	private void saveTruncPun() {
		truncPun();
	}

	private InputProxy _inp;

	private class InputProxy implements Runnable {
		private ASR33_Teletype _tty;
		private boolean _running;

		public InputProxy(ASR33_Teletype tty) {
			_tty = tty;
			_running = false;
		}

		public void restart() {
			if (!_running) { // note, possible race
				Thread t = new Thread(this);
				t.start();
			}
		}

		public void run() {	// look for input, only when input enabled...?
			_running = true;
			while(_input) {
				int b;
				if (_xOn) {
					b = getRdrByte();
					if (b < 0) { // EOT == X OFF
						xOff();
						continue;
					}
				} else {
					// It should never be the case that this
					// is blocked when the calculator sends
					// an X ON, as those are mutually exclusive
					// operating modes of the calculator.
					// This thread shold never be running while
					// the calculator is printing to OutputWriter.
					b = _tty.ttyGet();
					if (b < 0) { // i.e. disconnect
						break;
					}
				}
				if (b == 0x00ff) { // RUB ignored
					continue;
				}
				if (b == 0x01) { // ^A == Resume (GO)
					xOff();
					_glrn = 0;
					_input = false;
					Wang_UI.getCore().replyIO(_iob, GO);
					// quit thread now???
					continue;
				}
				if (_glrn != 0) {
					if (b == 0x15) { // ^U == WRITE ALPHA
						sendCode(START);
						continue;
					} else if (b == 0x16) { // ^V == END ALPHA
						sendCode(END);
						continue;
					}
					if (_cr && b == 0x0a) { // ignore LF imm after CR
						continue;
					}
					// See if character converts...
					byte[] tr = Wang_UI.getCharConv().asciiTtyToTiltrotate((byte)b);
					if (tr != null) {
						// no shifting automatically (?)
						sendCode(tr[1]);
						ttyPrint((char)b);
						continue;
					}
				} else {
					if (b >= '0' && b <= '9') {
						sendCode(E0 + (b - '0'));
						ttyPrint((char)b);
						continue;
					} else if (b == '.') {
						sendCode(DP);
						ttyPrint((char)b);
						continue;
					} else if (b == '-') {
						sendCode(CHG_SIGN);
						ttyPrint((char)b);
						continue;
					} else if (b == '^') {
						sendCode(SET_EXP);
						ttyPrint((char)b);
						continue;
					} else if (b == '_') { // back-arrow on old TTYs...
						sendCode(CLR_DSP);
						ttyPrint((char)b);
						continue;
					} else if (b == 0x02) { // ^B == SR 0000
						xOff();
						sendCode(SR0);
						continue;
					} else if (b == 0x03) { // ^C == SR 0001
						xOff();
						sendCode(SR1);
						continue;
					} else if (b == 0x04) { // ^D == SR 0002
						xOff();
						sendCode(SR2);
						continue;
					}
				}
				// if still here, error... decide fate...
				if (_bytes > 0) {
					xOff();
					_input = false;
					_glrn = 0;
					Wang_UI.getCore().replyIO(_iob, GO);
					// quit thread now???
					continue;
				}
			}
			_running = false;
		}
	}

	public boolean start_cn36(int iob, int c) {
		// don't care about run vs. keyboard modes?
		// how would this work from a running program?!
		// especially if GLRN is asserted...
		if ((iob & ~0x3) != 4) return false; // group 1 or 2
		if ((c & ~0x0f) != 0xf0) return false; // 15 xx
		_iob = iob;
		switch(c & 0x0f) {
		case 0:
			xOn();
			break;
		case 1:
		case 2:
		case 3:
		case 4:
			ttyPrint((char)('0' + (c & 0x0f)));
			break;
		default:
			break;
		}
		if ((iob & 1) != 0) {
			_glrn = 1;
		}
		_input = true;
		_bytes = 0;
		_inp.restart();
		return true;
	}

	public void do_ack(int iob) {
		// should be ACK for previous code... should enable next...
		// TODO
	}

	public void do_dev(int iob, int b) {
		// right now only ACK happens
		// TODO
	}

	public int getGLRN() { return _glrn; }

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang607.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.3 $ $Date: 2014/01/03 23:48:40 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	private void sendCode(int code) {
		if (!_input) return;
		Wang_UI.getCore().replyIO(_iob, code);
		++_bytes;
		_cr = (code == 0x18);	// Selectric RETURN+INDEX code...
	}

	public void newConnection(Socket s) {
		_mu.setEnabled(s != null);
	}

	public Wang_Teletype(String propBase) {
		_input = false;
		_bytes = 0;
		_glrn = 0;
		_iob = 0;
		_cr = false;
		_inp = new InputProxy(this);

		_propBase = propBase;
		_mountLabel = "Mount Tape";
		_pickLabel = new String[]{"Wang Data files","Text Files"};
		_fileType = new String[]{"wdf","txt"};
		_file = Wang_UI.getProperties().getFile(_propBase + "rdr_image", true, Wang_UI.getDir());
		getMenu(); // setup now in case not already done...

		setupRdr();
		_miRdr.addActionListener(this);
		_miPunS.addActionListener(this);
		_miPunT.addActionListener(this);

		Wang_UI.registerCN36(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Menu event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_R) {
			pickRdrFile();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_T) {
			truncPun();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			saveTruncPun();
			return;
		}
		// error - unknown menu action
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
