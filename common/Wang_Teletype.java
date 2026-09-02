// Copyright (c) 2011,2026 Douglas Miller

import java.io.*;
import javax.swing.*;
import java.awt.event.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingDeque;

// This device has no visible object (no GUI).
// It provides an telnet (socket) listening point to
// which an ASR33-like client atteches. Normally, this
// client would be ASR33telnet.jar. Anything else may
// not supply paper tape or similar features.

class Wang_Teletype extends ASR33_Teletype
		implements Wang_GroupIODevice, ActionListener
{
	public static final String Model = "07";
	public static final String Description = "Teletype";

	private static JMenuItem pmi = null;
	private static Wang_Teletype thus = null;
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
	public static Wang_Teletype s_getInstance() {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s07_",
			Wang_UI.getSeries(), Wang_UI.getSeries());
		thus = new Wang_Teletype(p);
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
		Wang_CN24_dev.connect(this);
		Wang_CN36_Bus.registerCN36(this);
		onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		if (Wang_CN24_dev.get() == this) {
			Wang_CN24_dev.connect(null);
		}
		Wang_CN36_Bus.deregisterCN36(this);
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		onOff(false);
	}
	public boolean isPlugged() { return plugged; }
	public JMenuItem getMenu() {
		// TODO: this should represent a setup menu
		// for host/port.
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName() + " (no GUI)", KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}


	// Group 1 00 xx = "Run" mode (similar to PaperTapeReader)
	// Group 2 00 xx = "Learn" mode
	// 00 00 = X ON (activate paper tape, as for '03)
	// 00 01 = print "1"
	// 00 02 = print "2"
	// 00 03 = print "3"
	// 00 04 = print "4"
	// all other: normal start
	// Accept all "OutputWriter" text from calculator.

	boolean gkbd;
	private int _glrn;
	private int _iob;
	private int _bytes;
	private boolean _cr;	// last character was CR...
	boolean _input;

	public boolean inputEnabled() { return _input; }

	public void reset() {
		if (_outp != null) _outp.reset();
		_glrn = 0;
		_input = false;
	}

	public void onOff(boolean on) {
		if (!on) {
			_input = false;
			_inp = null;
		}
		super.onOff(on);
	}

	private void xOn() {
		// start the paper tape reader...
		ttyPrint('\021');	// ^Q
	}

	private void xOff() {
		// stop the paper tape reader...
		ttyPrint('\023');	// ^S
	}

	private InputProxy _inp;
	private OutputProxy _outp;

	private class OutputProxy implements Runnable {
		LinkedBlockingDeque<Integer> giChr;

		public OutputProxy() {
			giChr = new LinkedBlockingDeque<Integer>();
			Thread t = new Thread(this);
			t.start();
		}

		public void reset() {
			giChr.clear();
		}

		public void putChr(int c) {
			giChr.add(c);
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
					_glrn = 0;
					_input = false;
				}
				Wang_UI.getCore().replyIO(_iob, c);
			}
		}
	}

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
			while (_input) {
				int b;
				// It should never be the case that this
				// is blocked when the calculator sends
				// an X ON, as those are mutually exclusive
				// operating modes of the calculator.
				// This thread should never be running while
				// the calculator is printing to OutputWriter.
				b = _tty.ttyGet();
				if (b < 0) { // i.e. disconnect
					break;
				}
				// echo all characters? echoed chars might also
				// be punched, so echo everything and let the
				// ttyPrint() methods strip out anything.

				// echo character. User should assume no echo means
				// the calculator is not in an input mode.
				// If ttyGet (ConnectionProxy) can detect that
				// it could echo BEL.
				ttyPrint((char)b);

				if (b == 0x00ff) { // RUB ignored
					continue;
				}
				// X ON, X OFF, PUN ON, PUN OFF from keyboard or reader
				// should also be supported.

				if (b == 0x01) { // ^A == Resume (GO)
					do_crlf();
					xOff();
					_outp.putChr(GO);
					// quit thread now???
					// _input (_glrn) does not officially end
					// until GO is actually sent to
					// calculator.
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
						continue;
					}
				} else {
					if (b >= '0' && b <= '9') {
						sendCode(E0 + (b - '0'));
						continue;
					} else if (b == '.') {
						sendCode(DP);
						continue;
					} else if (b == '-') {
						sendCode(CHG_SIGN);
						continue;
					} else if (b == '^') {
						sendCode(SET_EXP);
						continue;
					} else if (b == '_') { // back-arrow on old TTYs...
						sendCode(CLR_DSP);
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
				// invalid char - if still here, error... decide fate...
				if (_bytes > 0) {
					do_crlf();
					xOff();
					_outp.putChr(GO);
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
		_input = ((c & ~0x0f) == 0x00); // 00 xx
		if (!_input) return _input;
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
		_bytes = 0;
		_inp.restart();
		return _input;
	}

	public void do_ack(int iob) { } // not used

	public int getGLRN() { return _glrn; }
	public void setGKBD(boolean state) { gkbd = !state; }
	public boolean isBlockIO() { return false; }
	public boolean isDevEnabled() { return _input; }

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang607.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.9 $ $Date: 2014/01/26 14:52:57 $<BR>"+
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
		_outp.putChr(code);
		++_bytes;
		_cr = (code == 0x18);	// Selectric RETURN+INDEX code...
	}

	public void newConnection(Socket s) {
		boolean start = (s != null);
		if (start && _input) {
			_inp.restart();
		}
	}

	public Wang_Teletype(String propBase) {
		super(propBase, Integer.valueOf("10" + s_getModel()));

		_input = false;
		_bytes = 0;
		_glrn = 0;
		_iob = 0;
		_cr = false;
		_outp = new OutputProxy();
		_inp = new InputProxy(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Menu event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_D) {
			// TODO: setup host/port dialog
		}
		// error - unknown menu action
	}
}
