// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_Teletype.java,v 1.1 2014/01/02 20:16:52 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_Teletype extends ASR33_Teletype
		implements Wang_InputDevice
{
	final String ident = "$Id: Wang_Teletype.java,v 1.1 2014/01/02 20:16:52 drmiller Exp $";

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

	public void reset() {
		_glrn = 0;
		_input = false;
	}

	public void onOff(boolean on) {
	}

	public boolean start_cn36(int iob, int c) {
		// don't care about run vs. keyboard modes?
		// how would this work from a running program?!
		// especially if GLRN is asserted...
		if ((iob & ~0x3) != 4) return false; // group 1 or 2
		if ((c & ~0x0f) != 0xf0) return false; // 15 xx
		switch(c & 0x0f) {
		case 0:
			xOn();
			break;
		case 1:
		case 2:
		case 3:
		case 4:
			do_cn24_direct('0' + (c & 0x0f));
			break;
		default:
			break;
		}
		if ((iob & 1) != 0) {
			_glrn = 1;
		}
		_input = true;
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
			"$Revision: 1.1 $ $Date: 2014/01/02 20:16:52 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	boolean _input;

//	private void sendACK() {
//		Wang_UI.getCore().ackIO(5);
//	}

	private void sendCode(byte b) {
		if (!_input) return;
		Wang_UI.getCore().replyIO(5, (b & 0x0ff));
	}

	private byte _shifted = (byte)0x12;	// default to (start with) Shift Down

	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		e.consume(); // prevent JTextArea from seeing it
		byte b = (byte)c;
		byte[] tr = Wang_UI.getCharConv().asciiToTiltrotate(b);
		if (tr[0] != _shifted) {
			sendCode(tr[0]);
			_shifted = tr[0];
			do_cn24(tr[0]);
		}
		sendCode(tr[1]); // ignored in TYPE mode?
		do_cn24(tr[1]);
	}

	public Wang_Teletype() {
		_input = false;

		Wang_UI.registerCN36(this);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
