// Copyright (c) 2011,2012 Douglas Miller
// $Id: ASR33_Teletype.java,v 1.4 2014/01/04 22:26:32 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

abstract class ASR33_Teletype
	implements Wang_OutputDevice, Runnable
{
	final String ident = "$Id: ASR33_Teletype.java,v 1.4 2014/01/04 22:26:32 drmiller Exp $";

	private boolean _on;
	private boolean _shifted;
	private boolean _punch;

	Thread _th;
	private ServerSocket _ss;
	private Socket _remote;
	private OutputStream _out;

	public Component getComponent() { return null; }
	public JFrame getFrame() { return null; }
	public void onOff(boolean on) {
		_on = on;
		if (on) {
			// "_on" allows new connections, anything else?
			// but this case is not used.
		} else {
			// this object is about to be destroyed
			// let thread tear-down...
			if (_th != null) {
				// _th.interrupt(); does not seem to do anything,
				// but forcing the ServerSocket closed does terminate
				// the accept().
				try { _ss.close(); } catch (Exception ee) {}
				_th = null;
			}
		}
	}
	public boolean onOff() {
		// return (_remote != null) ?
		return _on;
	}

	ConnectionProxy _cp;
	int _currByte;

	public int ttyGet() {
		// might need to support multiple TTYs, but
		// still only one character at time.
		int b = -1;
		synchronized(_cp) {
			try {
				_cp.wait();
				b = _currByte;
			} catch (Exception ee) {
				// assume all is dead?
				b = -1;
			}
		}
		// The TTY never generated "lower case" (etc), so fold...
		if (b > 0x7f) {
			b = 0x00ff; // RUBOUT - ignored
		} else if (b >= 0x60) {
			b -= 32;
		}
		return b;
	}

	abstract public void newConnection(Socket s);
	abstract public void ctrlChar(char c);

	private class ConnectionProxy implements Runnable {
		private InputStream _in;
		public ConnectionProxy(Socket s) throws Exception {
			_in = s.getInputStream();
			Thread t = new Thread(this);
			t.start();
		}

		public void run() {
			int b = 0;
			while (b >= 0) {
				try {
					b = _in.read();
				} catch (Exception ee) {
					b = -1;
				}
				_currByte = b;
				synchronized(this) {
					notifyAll();
				}
			}
			// already did notifyAll()...
			_currByte = -1;
			tearDown();
		}
	}

	private void subscribe(Socket s) {
		// Right now only one connection allowed
		if (!_on || _remote != null) {
			try { s.close(); } catch(IOException e) { }
			return;
		}
		try {
			_cp = new ConnectionProxy(s);
			_out = s.getOutputStream();
		} catch (Exception ee) {
			try { s.close(); } catch(IOException e) { }
			return;
		}
		_remote = s;
		newConnection(s);
	}

	private synchronized void tearDown() {
		if (_remote != null) {
			try {
				_out.close();
				_remote.close();
			} catch(Exception ee) {}
			_out = null;
			_remote = null;
			newConnection(null);
		}
	}

	public void ttyPrint(char c) {
		// Need more than just "toupper()" as the tty forces
		// all characters 96-127 into 64-95.
		if (c < ' ' && c != '\n' && c != '\r') return;
		int b = c;
		if (b > 0x7f) return;
		if (b > 0x5f) {
			b -= 32;
		}

		// technically, the TTYs each have their own punch/reader,
		// but using telnet as the "tty" thwarts that idea.
		// right now we only support one TTY anyway.
		if (_punch) {
			// write to file...
		}
		if (_remote != null) {
			try {
				_out.write(b);
			} catch(IOException e) {
				tearDown();
			}
		}
	}

	private void ttyPrint(String s) {
		int x;
		for (x = 0; x < s.length(); ++x) {
			ttyPrint(s.charAt(x));
		}
	}

	public void setProperties(Wang_Properties p) {
		// might be paper tape image file - one for reader, one for punch
		// also might change listening host/port...
	}

	public void reset() {
		_shifted = false;
		_punch = false;
	}

	public void setPaper(double w, double h) {
	}

	public void do_bell() {
	}

	public void do_shift_up() {
		_shifted = true;
	}

	public void do_shift_dn() {
		_shifted = false;
	}

	public void do_lock(int on) {
		if (on == 0) {}
	}

	public void do_settab() {
		// PUN on (a.k.a DC2 or ^R)
		ctrlChar('\022');
	}

	public void do_clrtab() {
		// PUN off (a.k.a DC4 or ^T)
		ctrlChar('\024');
	}

	public void do_tab() {
		// not supported on TTYs
	}

	public void do_crlf() {
		ttyPrint('\r');
		ttyPrint('\n');
	}

	public void do_index() {
		ttyPrint('\n');
	}

	public void do_revindex() {
		// not supported on TTYs
	}

	public void do_space() {
		ttyPrint(' ');
	}

	public void do_backspace() {
		// not supported on TTYs
	}

	public void do_cn24_direct(char c) {
		ttyPrint(c);
	}

	public void do_cn24(byte b) {
		boolean printable = true;
		if ((b & 0x0f) == 0x08) { // control characters...
			printable = false;
			switch((b & 0x30) >> 4) {
			case 0: // tab
				do_tab();
				break;
			case 1:	// return+index
				do_crlf();
				break;
			case 2: // nothing
			case 3: // nothing
				return;
			}
		} else if ((b & 0x06) == 0x02) {
			// X2, X3, Xa, Xb
			printable = false;
			switch((b & 0x39)) {
			case 0x00:	// space
				do_space();
				break;
			case 0x01:	// bkspace
				do_backspace();
				break;
			case 0x08:	// set tab
				do_settab();
				return;
			case 0x09:	// clr tab
				do_clrtab();
				return;
			case 0x10:
				do_shift_dn();
				return;
			case 0x11:
				do_shift_up();
				return;
			case 0x18:
				do_index();
				break;
			case 0x19:
				do_revindex();
				break;
			default:	// 2x, 3x: nothing
				return;
			}
		}
		if (printable) {
			String s;
			s = Wang_UI.getCharConv().tiltrotateToAsciiTty(b, _shifted);
			if (s != null) {
				ttyPrint(s);
			}
		}
	}

	public ASR33_Teletype() {
		_shifted = false;
		_punch = false;
		_remote = null;
		_on = true;
		_th = null;
		try {
			int p = 10707;	// use Series... 10607 or 10707...
			InetAddress ia;
			//ia = InetAddress.getLocalHost();
			ia = InetAddress.getByName("127.0.0.1");
			_ss = new ServerSocket(p, 1, ia);
		} catch(Exception e) {
			Wang_UI.warning("ASR33_Teletype", e.toString());
			_ss = null;
		}
		if (_ss != null) {
			_th = new Thread(this);
			_th.start();
		}
	}

	public void run() {
		Socket s;
		while (_on) {
			try {
				s = _ss.accept();
			} catch(IOException e) {
				// e.g. java.net.SocketException: Socket closed
				break;
			}
			subscribe(s);
		}
		tearDown();
		try {
			_ss.close();
		} catch(IOException e) { }
		_ss = null;
		if (_on) {
			Wang_UI.warning("ASR33_Teletype", "Exiting in error");
		}
	}
}
