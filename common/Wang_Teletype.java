// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_Teletype.java,v 1.2 2014/01/03 01:21:44 drmiller Exp $

import javax.swing.*;

class Wang_Teletype extends ASR33_Teletype
		implements Wang_InputDevice
{
	final String ident = "$Id: Wang_Teletype.java,v 1.2 2014/01/03 01:21:44 drmiller Exp $";

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

	private int _glrn;
	private int _iob;
	private int _bytes;
	private boolean _cr;	// last character was CR...
	boolean _input;
	private byte _shifted = (byte)0x12;	// default to (start with) Shift Down

	public void reset() {
		_glrn = 0;
		_input = false;
	}

	public void onOff(boolean on) {
	}

	private void xOn() {
		// start the paper tape reader...
	}

	private void xOff() {
		// stop the paper tape reader...
	}

	private InputProxy _inp;

	private class InputProxy implements Runnable {
		private ASR33_Teletype _tty;
		public InputProxy(ASR33_Teletype tty) {
			_tty = tty;
			Thread t = new Thread(this);
			t.start();
		}

		public void run() {	// look for input, only when input enabled...?
			while(_input) {
				int b = _tty.ttyGet();
				if (b < 0) { // do GO now? This is really a disconnect, not user
					break;
				}
				if (b == 0x01) { // ^A == Resume (GO)
					xOff();
					Wang_UI.getCore().replyIO(_iob, GO);
					_input = false;
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
					} else if (b == 0x08) { // ^H == Shift-Up
						sendCode(SHIFTUP);
						continue;
					} else if (b == 0x0c) { // ^L == Shift-Down
						sendCode(SHIFTDN);
						continue;
					}
					if (_cr && b == 0x0a) { // ignore LF imm after CR
						continue;
					}
					// See if character converts...
					byte[] tr = Wang_UI.getCharConv().asciiToTiltrotate((byte)b);
					if (tr != null) {
						if (tr[0] != _shifted) {
							sendCode(tr[0]);
							_shifted = tr[0];
						}
						sendCode(tr[1]); // ignored in TYPE mode?
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
					Wang_UI.getCore().replyIO(_iob, GO);
					_input = false;
					// quit thread now???
					continue;
				}
			}
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
		_inp = new InputProxy(this);
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
			"$Revision: 1.2 $ $Date: 2014/01/03 01:21:44 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

//	private void sendACK() {
//		Wang_UI.getCore().ackIO(5);
//	}

	private void sendCode(int code) {
		if (!_input) return;
		Wang_UI.getCore().replyIO(_iob, code);
		++_bytes;
		_cr = (code == 0x18);	// Selectric RETURN+INDEX code...
	}

	public Wang_Teletype() {
		_input = false;
		_bytes = 0;
		_glrn = 0;
		_iob = 0;
		_cr = false;

		Wang_UI.registerCN36(this);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
