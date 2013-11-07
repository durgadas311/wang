// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang600_SimulatorPipe.java,v 1.3 2013/11/07 21:19:38 drmiller Exp $

import java.awt.event.*;
import java.io.*;
import javax.swing.*;

// Receives single byte-stream input from BackEnd simulator and directs
// messages to components.
class Wang600_SimulatorPipe
	implements ActionListener, Wang600_Core
{
	final String ident = "$Id: Wang600_SimulatorPipe.java,v 1.3 2013/11/07 21:19:38 drmiller Exp $";

	// CN-36 "Input" devices (Group 1/2 I/O Protocol)
	private Wang_InputDevice _cn36;	// current active device

	private void do_keycode(int code) {
		if (Wang_UI.getFout() == null) {
			int t = code >> 8;
			int h = (code >> 4) & 0x0f;
			int l = code & 0x0f;
			System.err.format("%d %02d %02d (%04x)\n", t, h, l, code);
		} else {
			byte[] b = new byte[2];
			b[0] = (byte)(code & 0x0ff);
			b[1] = (byte)(code >> 8);
			try {
				Wang_UI.getFout().write(b);
				Wang_UI.getFout().flush();	// why?
			} catch (IOException ee) {
				System.err.println("Broken pipe for keyboard!");
			}
		}
	}

	public void chgMode0() {
		int code = Wang_Keys.MODE0 | Wang600.Kbd.getMode0();
		do_keycode(code);
	}

	public void chgMode1() {
		int code = Wang_Keys.MODE1 | Wang600.Kbd.getMode1();
		do_keycode(code);
	}

	public void pressCmd(int cmd) {
		int code = Wang_Keys.SPCL | cmd;
		do_keycode(code);
	}

	public void sendCN36(int rep) {
		// TODO: accept generic data and apply meta...
		do_keycode(rep);
	}

	public void chgXROM() {
		do_keycode(0x8100);
	}

	java.util.LinkedList<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
		if (!timer.isRunning()) {
			timer.start();
		}
	}

	private javax.swing.Timer timer; // must regulate flow...

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			if (keyCodes.size() > 0) {
				int k = keyCodes.remove();
				do_keycode(k);
			}
			if (keyCodes.size() == 0) {
				timer.stop();
			}
			return;
		}
	}

	public Wang600_SimulatorPipe() {
		timer = new Timer(10, this);
		keyCodes = new java.util.LinkedList<Integer>();

		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		int n = 0;
		byte[] b = new byte[2];

		while (true) {
			try {
				n = Wang_UI.getFin().read(b);
			} catch (IOException ee) {
				// System.err.println("Broken pipe for SimInput!");
				return;
			}
			if (n == 0) {
				continue;
			}
			if (n < 0) {
				//System.err.println("simulator shutdown");
				System.exit(1);
			}
			if ((b[1] & 0x00ff) == 0xf0) {
				// fatal error, message follows...
				byte[] m = new byte[1024];
				try {
					Wang_UI.getFin().read(m);
					String err = new String(m);
					System.err.println(err);
				} catch (IOException ee) {
					System.err.println("ugh!");
				}
				System.exit(1);
			} else if ((b[1] & 0xfc) == 0x00) {
				// there will be 16 total sent...
				// and they are in order: 0-15...
				byte[] m = new byte[32];
				try {
					n = Wang_UI.getFin().read(m);
				} catch (IOException ee) {
				}
if (n != 32) System.err.println("too little? "+n);
				Wang600.Disp.do_display(m);
			} else if ((b[1] & 0xfe) == 0x04) {
				Wang600.Disp.setOv(b[0] & 1);
				Wang600.Disp.setErr(b[0] & 2);
			} else if ((b[1] & 0xfe) == 0x06) {
				Wang600.Disp.do_blanking();
			} else if ((b[1] & ~1) == 0x08) {
				Wang600.Prt.do_printer(b);
			} else if ((b[1]  & ~3) == 0x0c) {
				Wang600.Tape.do_tape(b);
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_cn36 = null;
				if (Wang600.CN24 != null) {
					Wang600.CN24.reset();
				}
				Wang600.M630.reset();
				Wang_UI.resetCN36();
			} else if (b[1] == 0x10) {
				if (Wang600.CN24 != null) {
					Wang600.CN24.do_cn24(b);
				}
			} else if ((b[1] & ~0x1f) == 0x20) { // IOB = 2,3
				// Random-access devices on CN-36
				// might need to support daisy-chained devices?
				Wang600.M630.do_dev(b);
			} else if ((b[1] & ~0x1f) == 0x40) { // IOB = 4,5
				// Group 1 / Group 2 devices on CN-36
				// need to find which device "wants" this...
				// ACK, gets directed to the device "currently
				// in charge", as determined by (prior) start command.
				if (_cn36 != null) {
					// this should only happen for ACKs... (?)
					_cn36.do_cn36(b);
				} else {
					// find a device that "wants" this code
					_cn36 = Wang_UI.startCN36(b);
				}
			} else if ((b[1] & 0x80) != 0) {
				int x;

				// now, the back-end is waiting for us...
				// dump the whole ROM image...
				for (x = 0; x < 2048; ++x) {
					byte bb = Wang600.XROM.getByte(x);
					do_keycode(0x8000 | (bb & 0x0ff));
				}
				do_keycode(0xff00);
			} else {
				System.err.format("Unexpected traffic (%d) %02x %02x\n", n, b[1], b[0]);
			}
		}
	}
}
