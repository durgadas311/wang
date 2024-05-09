// Copyright (c) 2011,2014 Douglas Miller
// $Id: ASR33_Teletype.java,v 1.9 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

abstract class ASR33_Teletype
	implements Wang_OutputDevice, Runnable
{
	final String ident = "$Id: ASR33_Teletype.java,v 1.9 2014/01/14 21:53:51 drmiller Exp $";

	String _propBase;

	private boolean _on;
	private boolean _shifted;

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
		// The TTY keyboard never generated "lower case" (etc), so fold...
		if (b > 0x7f) {
			b = 0x00ff; // RUBOUT - ignored
		} else if (b >= 0x60) {
			b -= 32;
		}
		return b;
	}

	abstract public void newConnection(Socket s);

	abstract public boolean inputEnabled();

	// character has not been (otherwise) sent to TTY/PUN
	abstract public void ctrlChar(char c);

	private class ConnectionProxy implements Runnable {
		private InputStream _in;
		static private final int IAC = 255;
		static private final int WILL = 251;
		static private final int WONT = 252;
		static private final int DO = 253;
		static private final int DONT = 254;
		static private final int SB = 250;
		//static private final int SE = 240;
		static private final int EOR = 239;

		private boolean _debug = false;
		private int _state = -1; // -1: not telnet (that we know, yet),
					 // 0: outside of telnet protocol (i.e. normal)

		public ConnectionProxy(Socket s) throws Exception {
			_in = s.getInputStream();
			Thread t = new Thread(this);
			t.start();
		}

		private boolean doTelnet(int b) {
			if (b < 0) return false;
//System.err.format("Socket recv %03d\n", b);
			switch (_state) {
			case IAC:
				if (b == IAC) {
					break;
				} else if (b == EOR) {
					if (_debug) System.err.format("EOR\n", b);
					b = 0;
				}
				_state = b;
				break;
			case WILL:
				if (_debug) System.err.format("IAC WILL %d\n", b);
				_state = 0;
				break;
			case WONT:
				if (_debug) System.err.format("IAC WONT %d\n", b);
				_state = 0;
				break;
			case DO:
				if (_debug) System.err.format("IAC DO %d\n", b);
				_state = 0;
				break;
			case DONT:
				if (_debug) System.err.format("IAC DONT %d\n", b);
				_state = 0;
				break;
			case SB:
				if (_debug) System.err.format("IAC DONT %d\n", b);
				_state = 0;
				break;
			case 0: // not in Telnet protocol...
			case -1: // not yet in Telnet protocol...
				if (b != IAC) {
					return false;
				}
				_state = IAC;
				break;
			default:
				System.err.format("IAC %d ???\n", _state);
				_state = 0; // not the right reaction...
				break;
			}
			return true;
		}

		public void run() {
			int b = 0;
			while (b >= 0) {
				try {
					b = _in.read();
//System.err.format("Socket recv %02x\n", b);
				} catch (Exception ee) {
					b = -1;
				}
				if (doTelnet(b)) {
					continue;
				}
				// why does telnet send a NUL after CR???
				if (b == 0) continue;
				if (inputEnabled()) {
					_currByte = b;
					synchronized(this) {
						notifyAll();
					}
				} else {
					ttyPrint('\007'); // BEL
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

	private boolean _crPrint;

	public void ttyPrint(char c) {
		// Need more than just "toupper()" as the tty forces
		// all characters 96-127 into 64-95.
		if (c < ' ' && c != '\n' && c != '\r' && c != '\007') return;
		if (_crPrint && c == '\n') return;
		_crPrint = (c == '\r');
		int b = c;
		if (b > 0x7f) return;
		if (b > 0x5f) {
			b -= 32;
		}

		if (_remote != null) {
			try {
				_out.write(b);
				if (_crPrint) {
					_out.write('\n');
				}
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
		// Need to drop all connections and destroy ServerSocket, and
		// recreate it... only do that if it changes...
		// For now, require user to de-install 707 and re-install.
	}

	public void reset() {
		_shifted = false;
	}

	public void setPaper(double w, double h) { if (w + h == 0.0) {} }

	public void do_bell() {
		ttyPrint('\007');
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
		// has not been printed...
		ctrlChar('\022');
	}

	public void do_clrtab() {
		// PUN off (a.k.a DC4 or ^T)
		// has not been printed...
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

	public int getRBS() { return 1; } // always ready, for now

	public ASR33_Teletype(String propBase, int port) {
		_shifted = false;
		_crPrint = false;
		_remote = null;
		_on = true;
		_th = null;
		_propBase = propBase;
		boolean done = false;
		boolean newDone = false;
		InetAddress ia;
		String host = Wang_UI.getProperties().getProperty(_propBase + "host");
		if (host == null) {
			host = "";
		}
		String gotHost = host;
		while (!done) {
			done = newDone;
			try {
				if (gotHost.length() == 0) {
					ia = InetAddress.getLocalHost();
				} else {
					ia = InetAddress.getByName(gotHost);
				}
				_ss = new ServerSocket(port, 1, ia);
				done = true;
			} catch(Exception e) {
//System.err.println("host=" + gotHost + " port=" + port + " " + e.toString());
				_ss = null;
			}
			if (_ss == null) {
				newDone = true;
				gotHost = "127.0.0.1";
			}
		}
		if (_ss != null) {
			if (!host.equals(gotHost)) {
				Wang_UI.warning("ASR33_Teletype", "Falling back to host " + gotHost);
			}
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
