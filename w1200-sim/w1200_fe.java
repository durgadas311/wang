// Copyright (c) 2011 Douglas Miller
// $Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;
import java.net.Socket;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

class _Key {
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color white2 = new Color(150, 150, 150, 255);
	static final Color white3 = new Color(200, 200, 200, 255);
	static final Color illum1 = new Color(255, 255, 100, 255);
	static final Color red1 = new Color(255, 128, 128, 255);
	static final Color red2 = new Color(190, 128, 128, 255);
	static final Color neon = new Color(244,157,33);
	static final Color neon2 = new Color(214,127,13);
	static final Color empty = new Color(50,50,50);
	static final Color gray = new Color(100,100,100);
	static final Color ivory = new Color(236,226,190);
	static final Color beige = new Color(230,230,230);
	static final Color aqua = new Color(143,219,195);

	static final int MODE0 = 0x0200;
	static final int MODE1 = 0x0300;
	static final int MODE2 = 0x0400;

	public _Key(Color sl, int c) {
		this.color = sl;
		this.altcolor = sl;
		this.code = c;
		this.state = false;
	}

	static final int SHIFT = -1;
	static final int TAPE_EJECT = -3;
	static final int TAPE_REW = -4;
	static final int TAPE_FF = -5;
	static final int TAPE_READY = -6;

	static final int PROG_CODE(int a, int b) {
		// shift is += 01 00...
		return ((a << 4) | b);
	}
	// 'a' is mask of bits that change
	static final int MODE0_CHG(int a, int b) {
		return (MODE0 | (a << 4) | b);
	}
	static final int MODE1_CHG(int a, int b) {
		return (MODE1 | (a << 4) | b);
	}
	static final int MODE2_CHG(int a, int b) {
		return (MODE2 | (a << 4) | b);
	}
	// group is never sent.
	// group=-1 is toggle (no group)
	// group=0 is momentary switch (no group)
	// group=N is ganged bank N (radio buttons)
	static final int GROUP(int a, int b) {
		return ((a << 12) | b);
	}

	public int getCode() {
		return code & 0x0ff;
	}
	public int getMode() {
		return code & 0x0f;
	}
	public int getMask() {
		return (code >> 4) & 0x0f;
	}
	public int getType() {
		return code & (0x0f << 8);
	}
	public int getGroup() {
		return (code >> 12);
	}
	public boolean isSHIFT() {
		return (code == SHIFT);
	}
	public boolean isTAPE() {
		return (code <= TAPE_EJECT);
	}

	Color color;
	Color altcolor;
	int code;
	boolean state;
}

// (red) CLEAR button is 00 14...
// f(x) is 10 xx
// F(x) is 11 xx
// XCHG is 14 xx
// I/O, etc is 15 xx

class FEexit extends Thread {
	private Process _be = null;

	public FEexit(Process be) {
		_be = be;
	}
	public void run() {
		if (_be != null) {
			_be.destroy();
			try {
				_be.waitFor();
			} catch (InterruptedException ee) {
			}
		}
	}
}

public class w1200_fe
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";

	public static File _dir;
	public static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
		java.io.BufferedReader ferr = null;
		GridBagLayout gridbag = new GridBagLayout();
		String dir;

		dir = System.getenv("WANGHOME");
		if (dir == null) {
			dir = System.getProperty("user.home") + "/Wang1200Files";
		}

		boolean test = (args.length > 0 && args[0].compareTo("-t") == 0);
		boolean back = (args.length > 0 && args[0].compareTo("-b") == 0);
		boolean web = (args.length > 0 && args[0].compareTo("-w") == 0);
		if (back) {
			fout = System.out;
			fin = System.in;
		} else if (web) {
			String host = System.getenv("WANG1200_HOST");
			String port = System.getenv("WANG1200_PORT");
			if (args.length >= 3) {
				port = args[2];
				host = args[1];
			}
			if (host == null || port == null) {
				System.err.println("Usage: w1200_fe -w <host> <port>");
				System.exit(1);
			}
			try {
				Socket sock = new Socket(host, Integer.parseInt(port));
				fout = sock.getOutputStream();
				fin = sock.getInputStream();
			} catch (IOException ee) {
				System.err.println("Unable to open socket to back-end!");
				System.exit(1);
			}
		} else if (!test) {
			try {
				Process _be = null;
				_be = Runtime.getRuntime().exec("./w1200-sim -b");
				fout = _be.getOutputStream();
				fin = _be.getInputStream();
				ferr = new BufferedReader(new InputStreamReader(_be.getErrorStream()));
				Runtime.getRuntime().addShutdownHook(new FEexit(_be));
				new Wang1200_SimError(ferr);
			} catch (IOException ee) {
				System.err.println("Unable to exec back-end!");
				System.exit(1);
			}
		}
		//_dir.mkdir();
		JFrame front_end = new JFrame("Wang 1200 Word Processing System");
		//java.net.URL url = w1200_fe.class.getResource("icons/wang1200-48x48.png");
		//Image img = Toolkit.getDefaultToolkit().getImage(url);
		//front_end.setIconImage(img);

		front_end.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.anchor = GridBagConstraints.NORTH;
		JPanel pan;

		Wang1200_Tape tapel = new Wang1200_Tape(fout);
		s.gridx = 0;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(tapel, s);
//System.err.println("tapel "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		s.gridheight = 1;
		front_end.add(tapel);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 25));
		pan.setOpaque(false);
		s.gridx = 1;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
//System.err.println("pan "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		front_end.add(pan);

		Wang1200_Tape taper = new Wang1200_Tape(fout);
		s.gridx = 2;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(taper, s);
//System.err.println("taper "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		front_end.add(taper);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 1;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(pan, s);
//System.err.println("pan "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		front_end.add(pan);
		s.gridwidth = 1;

		Wang1200_Indicator tml = new Wang1200_Indicator("TAPE<BR>MOVING");
		Wang1200_Indicator er = new Wang1200_Indicator("RECORD");
		Wang1200_Indicator tmr = new Wang1200_Indicator("TAPE<BR>MOVING");
		Wang1200_Indicator na = new Wang1200_Indicator("NO<BR>ADJUST");
		Wang1200_Indicator el = new Wang1200_Indicator("END OF<BR>DOCUMENT");

		Wang1200_Keyboard kbd = new Wang1200_Keyboard(fout,
				tml, er, tmr, na, el,
				tapel, taper);
		s.gridx = 0;
		s.gridy = 2;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(kbd, s);
//System.err.println("kbd "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		front_end.add(kbd);
		front_end.addKeyListener(kbd);

		Wang1200_Model611 m611f = new Wang1200_Model611();
		Wang1200_Model630 m630f = new Wang1200_Model630(kbd);

		Wang1200_SimInput inp = new Wang1200_SimInput(fin,
				tml, er, tmr, na, el,
				tapel, taper, m611f, m630f);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("601/602/611 OutputWriter", KeyEvent.VK_O);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = new JMenuItem("630 Disk - not mounted", KeyEvent.VK_D);
		mi.addActionListener(inp);
		mu.add(mi);

		front_end.setJMenuBar(mb);
		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,640);
		front_end.setVisible(true);
	}
}

class Wang1200_Indicator extends JLabel {
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

//	GridBagLayout gridbag = new GridBagLayout();
	JLabel lab;

	public Wang1200_Indicator(String label) {
//		GridBagConstraints s = new GridBagConstraints();
//
//		setLayout(gridbag);
//
//		s.fill = GridBagConstraints.NONE;
//		s.gridx = 0;
//		s.gridy = 0;
//		s.weightx = 0;
//		s.weighty = 0;
//		s.gridwidth = 1;
//		s.gridheight = 1;
//		s.insets.left = 0;
//		s.insets.right = 0;
//		s.anchor = GridBagConstraints.CENTER;

		setText("<HTML><CENTER>"+label+"</CENTER></HTML>");
		setFont(new Font("Sans-serif", Font.PLAIN, 10));
		setPreferredSize(new Dimension(50, 25));
		setForeground(Color.black);
		setBackground(_Key.gray);
		setOpaque(true);
//		s.gridx = 0;
//		s.gridy = 0;
//		gridbag.setConstraints(lab, s);
////		add(lab);
	}

	public void setOn(boolean on) {
		if (on) {
			setBackground(_Key.neon);
		} else {
			setBackground(_Key.gray);
		}
	}
}

class Wang1200_SimError
		implements Runnable
{
	BufferedReader _fin;

	public Wang1200_SimError(BufferedReader f) {
		_fin = f;
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		String s;
		while (true) {
			try {
				s = _fin.readLine();
			} catch (IOException ee) {
				// System.err.println("Broken pipe for SimError!");
				return;
			}
			if (s == null) {
				return;
			}
			System.err.println(s);
			System.err.flush();
		}
	}
}

class Wang1200_SimInput
		implements Runnable, WindowListener, ActionListener
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	Wang1200_Tape _tapel;
	Wang1200_Tape _taper;
	Wang1200_Model611 _m611;
	Wang1200_Model630 _m630;
	Wang1200_Indicator _tml;
	Wang1200_Indicator _er;
	Wang1200_Indicator _tmr;
	Wang1200_Indicator _na;
	Wang1200_Indicator _el;

	InputStream _fin;

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Devices event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_O) {
			_m611.onOff(!_m611.onOff());
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_D) {
			_m630.pickFile(m);
			return;
		}
	}

	public Wang1200_SimInput(InputStream f,
			Wang1200_Indicator tml,
			Wang1200_Indicator er,
			Wang1200_Indicator tmr,
			Wang1200_Indicator na,
			Wang1200_Indicator el,
			Wang1200_Tape tapel,
			Wang1200_Tape taper,
			Wang1200_Model611 cn24,
			Wang1200_Model630 m630) {
		_tml = tml;
		_er = er;
		_tmr = tmr;
		_na = na;
		_el = el;
		_tapel = tapel;
		_taper = taper;
		_m611 = cn24;
		_m611.getFrame().addWindowListener(this);
		_m630 = m630;
		_fin = f;
		if (f != null) {
			Thread t = new Thread(this);
			t.start();
		}
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	public void run() {
		int n = 0;
		byte[] b = new byte[2];

		while (true) {
			try {
				n = _fin.read(b);
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
					_fin.read(m);
					String err = new String(m);
					System.err.println(err);
				} catch (IOException ee) {
					System.err.println("ugh!");
				}
				System.exit(1);
			} else if ((b[1]  & ~3) == 0x0c) {
				_tapel.do_tape(b);
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_m611.reset();
				_m630.reset();
				//etc...
			} else if (b[1] == 0x10) {
				_m611.do_cn24(b);
			} else if ((b[1] & ~0x1f) == 0x20) {
				_m630.do_dev(b);
			} else {
				System.err.format("Unexpected traffic (%d) %02x %02x\n", n, b[1], b[0]);
			}
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (e.getWindow() == _m611.getFrame()) {
			_m611.onOff(false);
			return;
		}
	}
}

class Wang1200_Tape extends JComponent
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
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
	int _index;
	JLabel _window;
	File _file;

	public Wang1200_Tape(OutputStream fout) {
		_fout = fout;
		Font font;
		_file = null;
		_index = 0;
		_end = false;
		_wr = false;
		_ready = false;
		_tape_on = false;
		_eot = false;
		_tf = null;
		tape_open();

		setLayout(new FlowLayout());

		Border lb;
		JLayeredPane jp = new JLayeredPane();
		jp.setOpaque(true);
		jp.setPreferredSize(new Dimension(300, 200));

		_window = new JLabel("Tape Source/Dest");
		lb = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		_window.setBorder(lb);
		_window.setVerticalAlignment(SwingConstants.BOTTOM);
		_window.setHorizontalAlignment(SwingConstants.LEFT);
		_window.setForeground(Color.black);
		_window.setBackground(_Key.aqua);
		_window.setOpaque(true);
		font = null;
		font = new Font("Sans-serif", Font.PLAIN, 12);
		_window.setPreferredSize(new Dimension(200, 100));
		_window.setBounds(50, 75, 200, 100);
		_window.setFont(font);
		update_tape();
		jp.add(_window, new Integer(1), 500);

		JLabel cass = new JLabel();
		lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		cass.setBorder(lb);
		cass.setVerticalAlignment(SwingConstants.TOP);
		cass.setHorizontalAlignment(SwingConstants.CENTER);
		cass.setForeground(Color.black);
		cass.setBackground(_Key.ivory);
		cass.setOpaque(true);
		font = null;
		font = new Font("Serif", Font.PLAIN, 18);
		cass.setPreferredSize(new Dimension(300, 200));
		cass.setBounds(0, 0, 300, 200);
		cass.setFont(font);
		jp.add(cass, new Integer(0), 400);

		add(jp);
	}

	private void update_tape() {
		String fn;
		String eot;
		if (_eot) {
			eot = new String(" (end)");
		} else {
			eot = new String("");
		}
		if (_file == null) {
			fn = new String("(none)");
		} else {
			fn = _file.getName();
		}
		String txt = new String("<HTML><B>Tape Source/Dest</B><BR>" +
				"<B>Name:</B><BR>" +
				fn + "<BR>" +
				"<B>File #</B> " + _index + eot +
				"</HTML>");
		_window.setText(txt);
		repaint();
	}

	private void pick_file() {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser("Mount",
						"wng", "Wang program files");
		int rv = ch.showOpenDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
		} else {
			_file = null;
		}
		tape_open();
	}

	private void tape_open() {
		if (_file == null) {
			_eot = false;
			_index = 0;
			return;
		}
		try {
			_tf = new RandomAccessFile(_file.getAbsolutePath(), "rw");
		} catch (FileNotFoundException ee) {
			// can't happen?
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
	}

	private int tape_skipone() {
		int nb = 0;
		int n = 1;
		b1[0] = 0;
		while (n == 1 && (b1[0] & 0x00ff) != 0x9e) {
			try {
				n = _tf.read(b1);
//System.err.println(_index + ": at " + _tf.getFilePointer() + " got " + b1[0]);
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
		if (newidx == _index) return;	// should not happen
		// TBD: change position of file I/O
		if (newidx < _index) {
			try {
				_tf.seek(0);
			} catch (IOException ee) {
				// can't happen?
			}
			_index = 0;
			_eot = false;
		}
		while (_index < newidx && tape_skipone() == 1);
		// assert: _index == newidx
	}

	public boolean do_button(_Key btn) {
		// this kills any in-progress operations...
		_tape_on = false;
		if (btn.code == _Key.TAPE_READY) {
			if (_file == null) {
				_ready = false;
				return true;
			}
			_ready = btn.state;
			return false;
		}
		_ready = false;
		if (btn.code == _Key.TAPE_REW) {
			tape_position(_index - 1);
		} else if (btn.code == _Key.TAPE_FF) {
			tape_position(_index + 1);
		} else if (btn.code == _Key.TAPE_EJECT) {
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
				if ((bb[0] & 0x00ff) == 0x9e) { // END PROG
					// there is always one more byte..
					tape_read();
					// might be old image... treat EOF same...
					if ((bb[1] & 0x00ff) == 0x0e) {	// saw EOF
						bb[0] = (byte)0x9e;
						bb[1] = 0x0c;
					}
					if ((bb[0] & 0x00ff) != 0x9e) {
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
			if (_ready) _tape_on = true;
			_wr = true;
			_end = false;
		} else if (b[1] == 0x0e) {	// tape off
			if (_wr && !_end && _ready) {
				// did not just write END PROG, so need
				// to mark end of tape "file".
				// use 0x9e 0xff to mean "invisible" END PROG
				b[1] = 0x0c;
				b[0] = (byte)0x9e;
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
			// only if last byte before tape-off is END PROG...
			_end = ((b[0] & 0x00ff) == 0x9e); // END PROG
			if (_end) {
				tape_write(b); // write 0x9e 0x9e - true END PROG
				++_index; // display updated later..
			}
		} else {
			System.err.println("invalid tape command");
		}
	}
}

class Wang1200_Model630 {
	private int _cmd;
	private int _adr;
	private boolean _wr;
	private int _len;
	private int _idx;
	private Wang1200_Keyboard _kbd;
	java.io.RandomAccessFile _f;
	File _file;
	byte[] _buf;

	public Wang1200_Model630(Wang1200_Keyboard kbd) {
		reset();
		_kbd = kbd;
		_buf = new byte[256]; // largest transfer
	}

	private void disk_close() {
		if (_f != null) {
			try {
				_f.close();
			} catch (IOException ee) {
			}
			_f = null;
		}
	}

	private void disk_open() {
		if (_file == null) {
			return;
		}
		try {
			_f = new RandomAccessFile(_file.getAbsolutePath(), "rw");
		} catch (FileNotFoundException ee) {
			// can't happen?
			return;
		}
	}

	private int disk_read(int len) {
		int n = 0;
		try {
			_f.read(_buf, 0, len);
		} catch (IOException ee) {
			n = 1;
		}
		return n;
	}

	private int disk_write(int len) {
		int n = 0;
		try {
			_f.write(_buf, 0, len);
		} catch (IOException ee) {
			n = 1;
		}
		return n;
	}

	public void pickFile(JMenuItem m) {
		disk_close();

		SuffFileChooser ch = new SuffFileChooser("Mount",
						"wng", "Wang program files");
		int rv = ch.showOpenDialog(_kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			m.setText("630 Disk - " + _file.getName());
		} else {
			_file = null;
			m.setText("630 Disk - not mounted");
		}

		disk_open();
	}

	public void do_dev(byte[] b) {
		int res;
		if (_cmd == 0 && (b[1] & 0x0f) == 1) { // ACK when no command
			return;
		}
		++_cmd;
//System.err.println("dev 2 ["+_cmd+"] "+b[0]);
		boolean dat = ((b[1] & 0x10) != 0);
		if (_cmd <= 4 && dat || _cmd > 4 && !dat) {
System.err.println("sync error");
			return;
		}
		int bb;
//try{
// Thread.currentThread().sleep(50);
//}
//catch(InterruptedException ie){
//}
		// unless we know otherwise, just ACK with a "0"...
		bb = ((b[1] & 0xf0) << 8) | (_cmd & 0x0ff); // temp! debug
		if (_cmd < 4) {
			_adr <<= 8;
			_adr |= (b[0] & 0x00ff);
			bb |= 0x0100;
		} else if (_cmd == 4) {
			_wr = ((b[0] & 0x80) != 0);
			_len = (b[0] & 0x7f);
			bb |= 0x0100;
			if (_len == 0) {
				_len = 64;
			} else if (_len > 1) {
				_len <<= 2;
			}
//System.err.println("command "+_adr+" "+_wr+" "+_len);
			try {
				_f.seek(_adr);
			} catch (IOException ee) {
//System.err.println("seek "+_adr+" failed");
			}
			_idx = 0;
			if (!_wr) {
				res = disk_read(_len);
				bb = (bb & 0xff00) | (res & 0x00ff); // result code
//System.err.println("rd result "+res+" ("+_len+")");
			}
		} else {
			if (_idx < _len) {
				if (_wr) {
					bb |= 0x0100;
					_buf[_idx] = b[0];
				} else {
					bb = (bb & 0xff00) | (_buf[_idx] & 0x00ff);
				}
			} else {
				if (_wr) {
					bb |= 0x0100;
					res = disk_write(_len);
				} else {
					res = 0; // something else?
				}
				bb = (bb & 0xff00) | (res & 0x00ff); // result code
				_cmd = 0;
//System.err.println("result "+res+" ("+_idx+")");
			}
			++_idx;
		}
//System.err.printf("got %02x%02x put %04x\n", b[1], b[0], bb);
		_kbd.do_keycode(bb);
	}

	public void reset() {
//System.err.println("clear ("+_len+")");
		_cmd = 0;
		_adr = 0;
		_len = 0;
		_wr = false;
		// cancel anything...
	}
}

class SuffFileFilter extends javax.swing.filechooser.FileFilter {
	private String _sfx;
	private String _dsc;

	private String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}

	public SuffFileFilter(String suffix, String desc) {
		_sfx = suffix;
		_dsc = desc;
	}

	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		String extension = getExtension(f);
		if (extension != null &&
				extension.equals(_sfx)) {
			return true;
		}
		return false;
	}

	public String getDescription() {
		return _dsc;
	}
}

class SuffFileChooser extends JFileChooser {
	static final long serialVersionUID = 311457692041L;
	private String _sfx;
	public SuffFileChooser(String btn, String sfx, String dsc) {
		super(w1200_fe._dir);
		SuffFileFilter f = new SuffFileFilter(sfx, dsc);
		setFileFilter(f);
		setApproveButtonText(btn);
		_sfx = "." + sfx;
	}
	public int showOpenDialog(Component frame) {
		int rv = super.showOpenDialog(frame);
		if (rv == JFileChooser.APPROVE_OPTION) {
			if (getSelectedFile().getName().endsWith(_sfx)) {
				return rv;
			}
			File f = new File(getSelectedFile().getAbsolutePath().concat(_sfx));
			setSelectedFile(f);
		}
		return rv;
	}
}

class Wang1200_Model611
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	private byte[] cn24_xlate;
	private String[] cn24_spcl;

	public void reset() {
		// anything?
	}

	private void setup_xlate() {
		cn24_xlate = new byte[256];
		cn24_xlate[0x00] = '-';
		cn24_xlate[0x01] = 'y';
		cn24_xlate[0x02] = ' ';
		cn24_xlate[0x03] = '\b';
		cn24_xlate[0x04] = 'q';
		cn24_xlate[0x05] = 'p';
		cn24_xlate[0x06] = '=';
		cn24_xlate[0x07] = 'j';
		// cn24_xlate[0x08] = ' ';	// no op
		cn24_xlate[0x09] = '/';
		//cn24_xlate[0x0a] = ' ';	// no op
		//cn24_xlate[0x0b] = ' ';	// no op
		cn24_xlate[0x0c] = ',';
		cn24_xlate[0x0d] = ';';
		cn24_xlate[0x0e] = 'f';
		cn24_xlate[0x0f] = 'g';

		cn24_xlate[0x10] = 'w';
		cn24_xlate[0x11] = 's';
		//cn24_xlate[0x12] = '';	// shift dn
		//cn24_xlate[0x13] = '';	// shift up
		cn24_xlate[0x14] = 'i';
		cn24_xlate[0x15] = '\'';
		cn24_xlate[0x16] = '.';
		cn24_xlate[0x17] = '\001';	// 1/2...
		cn24_xlate[0x18] = '\n';
		cn24_xlate[0x19] = 'o';
		cn24_xlate[0x1a] = '\n';
		//cn24_xlate[0x1b] = '\n';	// rev index
		cn24_xlate[0x1c] = 'a';
		cn24_xlate[0x1d] = 'r';
		cn24_xlate[0x1e] = 'v';
		cn24_xlate[0x1f] = 'm';

		cn24_xlate[0x20] = 'b';
		cn24_xlate[0x21] = 'h';
		//cn24_xlate[0x22] = '+';	// step x+
		//cn24_xlate[0x23] = '+';	// step x-
		cn24_xlate[0x24] = 'k';
		cn24_xlate[0x25] = 'e';
		cn24_xlate[0x26] = 'n';
		cn24_xlate[0x27] = 't';
		//cn24_xlate[0x28] = '';	// print mode
		cn24_xlate[0x29] = '1';
		//cn24_xlate[0x2a] = '+';	// step y+
		//cn24_xlate[0x2b] = '+';	// step y-
		cn24_xlate[0x2c] = 'c';
		cn24_xlate[0x2d] = 'd';
		cn24_xlate[0x2e] = 'u';
		cn24_xlate[0x2f] = 'x';

		cn24_xlate[0x30] = '9';
		cn24_xlate[0x31] = '0';
		//cn24_xlate[0x32] = '';	// step x+y+
		//cn24_xlate[0x33] = '';	// step x-y+
		cn24_xlate[0x34] = '6';
		cn24_xlate[0x35] = '5';
		cn24_xlate[0x36] = '2';
		cn24_xlate[0x37] = 'z';
		//cn24_xlate[0x38] = '';	// plot mode
		cn24_xlate[0x39] = '4';
		//cn24_xlate[0x3a] = '';	// step x+y-
		//cn24_xlate[0x3b] = '';	// step x-y-
		cn24_xlate[0x3c] = '8';
		cn24_xlate[0x3d] = '7';
		cn24_xlate[0x3e] = '3';
		cn24_xlate[0x3f] = 'l';

		// shifted versions...
		cn24_xlate[0x40] = '_';
		cn24_xlate[0x41] = 'Y';
		cn24_xlate[0x42] = ' ';
		cn24_xlate[0x43] = '\b';
		cn24_xlate[0x44] = 'Q';
		cn24_xlate[0x45] = 'P';
		cn24_xlate[0x46] = '+';
		cn24_xlate[0x47] = 'J';
		cn24_xlate[0x49] = '?';
		cn24_xlate[0x4c] = ',';
		cn24_xlate[0x4d] = ':';
		cn24_xlate[0x4e] = 'F';
		cn24_xlate[0x4f] = 'G';

		cn24_xlate[0x50] = 'W';
		cn24_xlate[0x51] = 'S';
		cn24_xlate[0x54] = 'I';
		cn24_xlate[0x55] = '"';
		cn24_xlate[0x56] = '.';
		cn24_xlate[0x57] = '\002';	// 1/4
		cn24_xlate[0x58] = '\n';
		cn24_xlate[0x59] = 'O';
		cn24_xlate[0x5a] = '\n';
		//cn24_xlate[0x5b] = '\n';	// rev index
		cn24_xlate[0x5c] = 'A';
		cn24_xlate[0x5d] = 'R';
		cn24_xlate[0x5e] = 'V';
		cn24_xlate[0x5f] = 'M';

		cn24_xlate[0x60] = 'B';
		cn24_xlate[0x61] = 'H';
		cn24_xlate[0x64] = 'K';
		cn24_xlate[0x65] = 'E';
		cn24_xlate[0x66] = 'N';
		cn24_xlate[0x67] = 'T';
		cn24_xlate[0x69] = '!';
		cn24_xlate[0x6c] = 'C';
		cn24_xlate[0x6d] = 'D';
		cn24_xlate[0x6e] = 'U';
		cn24_xlate[0x6f] = 'X';

		cn24_xlate[0x70] = '(';
		cn24_xlate[0x71] = ')';
		cn24_xlate[0x74] = '\003';	// cent
		cn24_xlate[0x75] = '%';
		cn24_xlate[0x76] = '@';
		cn24_xlate[0x77] = 'Z';
		cn24_xlate[0x79] = '$';
		cn24_xlate[0x7c] = '*';
		cn24_xlate[0x7d] = '&';
		cn24_xlate[0x7e] = '#';
		cn24_xlate[0x7f] = 'L';

		cn24_spcl = new String[32];
		cn24_spcl[0x01] = "\u00BD";
		cn24_spcl[0x02] = "\u00BC";
		cn24_spcl[0x03] = "\u00A2";
	}
	private JFrame _frame;
	private PlotTextArea _text;
	private JScrollPane _scroll;

	private int _xoff, _yoff, _eop;
	private boolean _onoff;
	boolean _hasGraphic;
	int _fx, _fy, _fa;
	double _gx, _gy;

	private void clear() {
		_text.setText("");
		_eop = 0;
		_text.setCaretPosition(_eop);
		_plot = false;
		_shifted = false;
		_x = _y = 0;
		_text.clear();
		_hasGraphic = false;
	}

	String _footer;

	public Wang1200_Model611() {
		setup_xlate();

		_onoff = false;

		_frame = new JFrame("Wang 611 Output Writer");
		_frame.setLayout(new FlowLayout());
		_text = new PlotTextArea();
		_text.setFont(new Font("Monospaced", Font.PLAIN, 10));

		// setting this messes up horiz scrollbar...
		//_text.setPreferredSize(new Dimension(60 * _fx, 32 * _fy));
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);

		clear();

		FontMetrics fm = _text.getFontMetrics(_text.getFont());
		_fa = fm.getAscent();
		_fx = fm.charWidth('M');
		_fy = fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(96 * _fx, 32 * _fy));
		_frame.add(_scroll);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("File");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Tear Off", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(mb);
		_frame.pack();	// set size according to content...

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
		
		_frame.addComponentListener(this);
	}

	private void save611(File file) {
		if (_hasGraphic) {
			java.awt.image.BufferedImage i =
				new java.awt.image.BufferedImage(_text.getWidth(),
								_text.getHeight(),
					java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
			_text.paint(i.getGraphics());
			try {
				javax.imageio.ImageIO.write(i, "png", file);
			} catch (IOException ee) {
				System.err.println("error writing 611 PNG");
			}
		} else {
			FileOutputStream fo;
			try {
				fo = new FileOutputStream(file);
			} catch (FileNotFoundException ee) {
				System.err.println("chosen 611 file not found?");
				return;
			}
			try {
				fo.write(_text.getText().getBytes());
				fo.write('\n');
				fo.close();
			} catch (IOException ee) {
				System.err.println("error writing 611 TXT");
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown 611 event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_T) {
			clear();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			String sfx, dsc;
			if (_hasGraphic) {
				sfx = "png";
				dsc = "PNG image files";
			} else {
				sfx = "txt";
				dsc = "Text files";
			}
			SuffFileChooser ch = new SuffFileChooser("Save", sfx, dsc);
			int rv = ch.showOpenDialog(_frame);
			if (rv == JFileChooser.APPROVE_OPTION) {
				save611(ch.getSelectedFile());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(OrientationRequested.LANDSCAPE);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.75, (float)0.5, (float)7.0, (float)10.0, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(_text);
			boolean print = pj.printDialog(aset);
			if (print) {
				java.util.Date dt = new java.util.Date();
				_footer = new String("Wang 601/602/611 OutputWriter - " +
					w1200_fe._timestamp.format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println("611 menu " + e.getActionCommand() +
						" not implemented yet");
	}

	public JFrame getFrame() { return _frame; }

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
		if (e.getComponent() == _frame) {
			Dimension fdim = _frame.getSize(); 
			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
			_scroll.setPreferredSize(_scroll.getSize());
			_frame.setSize(fdim.width, fdim.height); // redundant?
			_frame.setPreferredSize(_frame.getSize());
		}
	}

	private boolean _shifted;
	private boolean _plot;
	private int _x, _y;
	private int _dx, _dy;

	class PlotTextArea extends JTextArea
			implements Printable {
		static final long serialVersionUID = 311457692040L;
		class plot {
			plot(String s_, int x_, int y_) {
				s = s_;
				x = x_;
				y = y_;
			}
			public String s;
			public int x;
			public int y;
		}

		public void clear() {
			_nplots = 0;
			_xplots = 0;
			//_plotArray.dispose();
			_plotArray = null;
			_x = _y = 0;
		}

		private plot[] _plotArray;
		private int _nplots;
		private int _xplots;

		private void addPlot(String s, int x, int y) {
			int n = _xplots++;
			if (_xplots > _nplots) {
				int o = _nplots;
				_nplots += 256;
				plot[] p = new plot[_nplots];
				if (o > 0) {
					System.arraycopy(_plotArray, 0, p, 0, o);
				}
				_plotArray = p;
			}
			_plotArray[n] = new plot(s, x, y);
		}

		public void paint(Graphics g) {
			super.paint(g);
			int x;
			for (x = 0; x < _xplots; ++x) {
				double xx, yy;
				xx = (_plotArray[x].x * _gx) + 0.5;
				yy = (_plotArray[x].y * _gy) + 0.5 + _fa;
				g.drawString(_plotArray[x].s, (int)xx, (int)yy);
			}
		}

		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();
			double y0 = pf.getImageableY();
			double w0 = pf.getImageableWidth();
			double h0 = pf.getImageableHeight();
			int pg = 0;
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);

			FontMetrics fm = _text.getFontMetrics(_text.getFont());
			// 156 chars platten width of IBM Selectric...
			double nf = _text.getFont().getSize() * (w0 / 156.0) /
							fm.charWidth('M');
			g2d.setFont(_text.getFont().deriveFont((float)nf));

			int did = 0;
			String s;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			int l = g2d.getFont().getSize();
			double gx = (w0 / 1300.0);
			double gy = (l / (100.0 / 6.0));
			int lpp = 60; // (int)(h0 / l);
			int max = getLineCount();
			int i = 0;
			while (pg <= pageIndex) {
				int ln;
				for (ln = 0; ln < lpp; ++ln) {
					int nn = ln + pg * lpp;
					if (nn >= max) break;
					try {
						int ls = getLineStartOffset(nn);
						int ll = getLineEndOffset(nn) - ls;
						s = getText(ls, ll);
					} catch(javax.swing.text.BadLocationException ee) {
						break;
					}
					if (pg == pageIndex) {
						++did;
						if (s.length() > 0) { // not blank line...
							g2d.drawString(s, 0, ln * l + l);
						}
					}
				}
				if (pg == pageIndex) {
					int ps = (int)(h0 * pg);
					int pe = (int)(ps + h0);
					for (i = 0; i < _xplots; ++i) {
						double xx, yy;
						// convert 1/100ths to points...
						xx = (_plotArray[i].x * gx) + 0.5;
						yy = (_plotArray[i].y * gy) + 0.5;
						if (yy >= ps && yy < pe) {
							++did;
							g2d.drawString(_plotArray[i].s,
								(int)xx, (int)yy - ps + l);
						}
					}
				}
				++pg;
			}
			if (did > 0) {
				s = new String("Page " + pg +
					" - " + _footer);
				g2d.drawString(s, 0, (lpp + 1) * l + l);
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	private void index() {
		_y += 14;
	}

	private void revindex() {
		_y -= 14;
		if (_y < 0) _y = 0;
	}

	private void space() {
		_x += 10;
		if (_x >= 1300) _x = 1299;
	}

	private void bkspace() {
		_x -= 10;
		if (_x < 0) _x = 0;
	}

	public void do_cn24(byte[] b) {
		if ((b[0] & 0x0f) == 0x08) { // control characters...
			switch((b[0] & 0x30) >> 4) {
			case 0: // nothing
				break;
			case 1:	// return+index handled below...
				_x = 0;
				if (_plot) return;
				index();
				break;
			case 2:	// print mode
				_plot = false;	// cleanup?
				return;
			case 3:	// plot mode
				_plot = true;
				_dx = _dy = 0;
				return;
			}
		} else if ((b[0] & 0x06) == 0x02) {
			switch((b[0] & 0x30) >> 4) {
			case 0: // space/bspace or nothing
				if (_plot) return;
				if ((b[0] & 1) == 0) {
					space();
				} else {
					bkspace();
				}
				break;
			case 1:	// index/rev or shift...
				if ((b[0] & 0x0e) == 0x02) {
					_shifted = ((b[0] & 1) != 0);
					return;
				}
				if (_plot) return;
				if ((b[0] & 1) == 0) {
					index();
				} else {
					revindex();
				}
				break;
			case 2:	// stepping
			case 3:	// stepping
				if (!_plot) return;
				switch(b[0] & 0x19) {
				case 0x00:
					_dx += 1;
					break;
				case 0x01:
					_dx -= 1;
					break;
				case 0x08:
					_dy += 1;
					break;
				case 0x09:
					_dy -= 1;
					break;
				case 0x10:
					_dx += 1;
					_dy += 1;
					break;
				case 0x11:
					_dx -= 1;
					_dy += 1;
					break;
				case 0x18:
					_dx += 1;
					_dy -= 1;
					break;
				case 0x19:
					_dx -= 1;
					_dy -= 1;
					break;
				}
				return;
			}
		}
		byte p;
		if (_shifted) {
			p = cn24_xlate[b[0] + 0x40];
		} else {
			p = cn24_xlate[b[0]];
		}
		byte[] bb;
		String s;
		if (p == 0) {
			s = new String("<"+b[0]+">");
		} else if (p < 0x07) {
			s = cn24_spcl[p];
		} else {
			bb = new byte[1];
			bb[0] = p;
			s = new String(bb);
		}
		if (_plot) {
			_x += _dx;
			if (_x < 0) _x = 0;
			if (_x >= 1300) _x = 1299; // 13 in. platten
			_y += _dy;
			if (_y < 0) _y = 0;
			_hasGraphic = true;
			_text.addPlot(s, _x, _y);
			_text.repaint();
			//_text.setCaretPosition(_eop); // to what?
			// todo: need to get JScrollPane to update...
		} else {
			_text.append(s);
			_eop += s.length();
			_text.setCaretPosition(_eop);
		}
		// "auto raise"...
		onOff(true);
	}

	public boolean onOff() {
		return _onoff;
	}

	public void onOff(boolean on) {
		if (_onoff == on) return;
		_onoff = on;
		_frame.setVisible(_onoff);
	}
}

class Wang1200_Keyboard extends JComponent
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang1200_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _shift;
	int _shift_kbd;
	int _shift_btn;
	int _mode0;
	int _mode1;
	int _mode2;
	OutputStream _fout;
	Wang1200_Tape _tapel;
	Wang1200_Tape _taper;

	private void setShift(boolean _new) {
		_shift = _new;
		if (_shift) {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_Key.illum1);
		} else {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_kbds[_shift_kbd]._keys[_shift_btn].color);
		}
	}

	private void setToggle(boolean on, _Key key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == _Key.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE1) {
			_mode1 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE2) {
			_mode2 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == _Key.MODE0) {
				_mode0 |= key.getMode();
			} else if (key.getType() == _Key.MODE1) {
				_mode1 |= key.getMode();
			} else if (key.getType() == _Key.MODE2) {
				_mode2 |= key.getMode();
			}
		} else {
			btn.setBackground(key.color);
		}
		key.state = on;
	}

	private void set_group(int g, int y, int x) {
		int z;
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			int tg = _kbds[y]._keys[z].getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			if (_kbds[y]._keys[z].state) {
				setToggle(false, _kbds[y]._keys[z], _kbds[y]._buttons[z]);
			}
		}
		setToggle(!_kbds[y]._keys[x].state, _kbds[y]._keys[x], _kbds[y]._buttons[x]);
	}

	public void do_keycode(int code) {
		if (_fout == null) {
			int t = code >> 8;
			int h = (code >> 4) & 0x0f;
			int l = code & 0x0f;
			System.err.format("%d %02d %02d (%04x)\n", t, h, l, code);
		} else {
			byte[] b = new byte[2];
			b[0] = (byte)(code & 0x0ff);
			b[1] = (byte)(code >> 8);
			try {
				_fout.write(b);
				_fout.flush();	// why?
			} catch (IOException ee) {
				System.err.println("Broken pipe for keyboard!");
				_fout = null;
			}
		}
	}

	private void do_button(boolean shifted, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if (_kbds[y]._keys[x].isSHIFT()) {
			if (!shifted) setShift(!_shift);
			return;
		}
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x].isTAPE()) {
			set_group(g, y, x);
			boolean st = _tapel.do_button(_kbds[y]._keys[x]);
			if (st) {
				setToggle(!_kbds[y]._keys[x].state,
					_kbds[y]._keys[x], _kbds[y]._buttons[x]);
			}
			return;
		}
		if (g != 0) {
			set_group(g, y, x);
		}
		// _mode0, _mode1, _meta were already updated above...
		if (type == _Key.MODE0) {
			code = _Key.MODE0 | _mode0;
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				code |= _kbds[y]._keys[x].getMode();
			}
		}
		if (type == _Key.MODE1) {
			code = _Key.MODE1 | _mode1;
		}
		if (type == _Key.MODE2) {
			code = _Key.MODE2 | _mode2;
		}
		if (type == 0 && _shift && (code & 0x0f0) == 0x080) {
			code |= 0x010;
		}
		if (!shifted) setShift(false);

		do_keycode(code);
	}

	public Wang1200_Keyboard(OutputStream fo,
				Wang1200_Indicator tml,
				Wang1200_Indicator er,
				Wang1200_Indicator tmr,
				Wang1200_Indicator na,
				Wang1200_Indicator el,
				Wang1200_Tape tapel,
				Wang1200_Tape taper) {
		int x;
		_tapel = tapel;
		_taper = taper;
		_kbds = new Wang1200_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_shift = false;
		_fout = fo;

		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;
		Wang1200_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

		kbd = new Wang1200_Keyboard_left(tml, er, tmr);
		for (x = 0; x < kbd._nkeys; ++x) {
			if (kbd._keys[x].code == _Key.SHIFT) {
				_shift_kbd = _nkbds;
				_shift_btn = x;
			}
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
//System.err.println("  kbd.left "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		s.gridx = _col;
		s.gridy = _row;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(400, 25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
//System.err.println("  kbd.pan "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		add(pan);
		++_col;

		kbd = new Wang1200_Keyboard_right(na, el);
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
//System.err.println("  kbd.right "+s.gridx+","+s.gridy+" "+s.gridwidth+"x"+s.gridheight);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		//setFocusTraversalKeysEnabled(false);
		setFocusCycleRoot(true);
		setRequestFocusEnabled(true);
		// setTransferHandler(TransferHandler newHandler) 
	}

	public void keyTyped(KeyEvent e) {
//System.err.println("key pressed "+e.getKeyCode()+" "+e.getKeyChar());
if (e.isActionKey()) {
System.err.println("action");
}
		char c = e.getKeyChar();
		do_keycode(c);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(true);
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			java.net.URL url = null;
			// should use a table to lookup url?
			if (m.getMnemonic() == KeyEvent.VK_B) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200.html");
			} else if (m.getMnemonic() == KeyEvent.VK_U) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200calc.html");
			} else if (m.getMnemonic() == KeyEvent.VK_D) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200tape.html");
			} else if (m.getMnemonic() == KeyEvent.VK_A) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200samp.html");
			} else if (m.getMnemonic() == KeyEvent.VK_P) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200prog.html");
			} else if (m.getMnemonic() == KeyEvent.VK_F) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200func.html");
			} else if (m.getMnemonic() == KeyEvent.VK_T) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200tech.html");
			} else if (m.getMnemonic() == KeyEvent.VK_C) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200codes.html");
			} else if (m.getMnemonic() == KeyEvent.VK_K) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200bycode.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_G) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200bugs.html");
			} else {
				System.err.println("help menu " + e.getActionCommand() +
						" not implemented yet");
				return;
			}
//			try {
//				//_text.setPage(url);
//			} catch (IOException ee) {
//			}
if (false) System.err.println("stupid warnings "+url);
			return;
		}
//		if (e.getSource() == _help) {
//			_help_on = !_help_on;
//			if (_help_on) {
//				// this still isn't right...
//				_frame.pack();
//				_help.setBackground(_Key.neon2);
//			} else {
//				_help.setBackground(_Key.empty);
//			}
//			_frame.setVisible(_help_on);
//			return;
//		}
		// must be a button, find out which
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					boolean shifted = ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
					do_button(shifted, y, x);
					return;
				}
			}
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
//		if (e.getWindow() == _frame) {
//			_help_on = false;
//			_help.setBackground(_Key.empty);
//			_frame.setVisible(_help_on);
//			return;
//		}
//		if (e.getWindow() == _prt.getFrame()) {
//			do_button(false, _print_kbd, _print_btn);
//			return;
//		}
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
//		if (e.getComponent() == _frame) {
//			Dimension fdim = _frame.getSize(); 
//			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
//			_scroll.setPreferredSize(_scroll.getSize());
//			_frame.setSize(fdim.width, fdim.height); // redundant?
//			_frame.setPreferredSize(_frame.getSize());
//		}
	}
}

class Wang1200_Keyboards extends JComponent
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang1200_Keyboards() { }

	int _nkeys;
	_Key[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py,
						int gx, int gy,
						String icon, _Key key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		java.net.URL url = Wang1200_Keyboards.class.getResource(icon);
if (url != null) {
		ImageIcon ic = new ImageIcon(url);
		butt = new JButton(ic);
} else {
		butt = new JButton("<HTML><CENTER>"+icon+"</CENTER></HTML>");
}
		butt.setBackground(key.color);
		butt.setBorder(lb);
		butt.setOpaque(true);
		// butt.setHorizontalAlignment(SwingConstants.CENTER); // didn't help...

		dim.width = 50 * lx;
		dim.height = 50 * ly;
		butt.setPreferredSize(dim);
		butt.setMargin(inset);

		c.gridwidth = lx * gx;
		c.gridheight = ly * gy;
		c.gridx = _col + px;
		c.gridy = _row + py;
		gridbag.setConstraints(butt, c);
//System.err.println("butt."+icon+" "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);

		add(butt);
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addPushButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String botlab, Color alt, boolean init, _Key key) {
		final Dimension dim = new Dimension(15, 30);
		JButton butt;
		if (alt != null) {
			key.altcolor = alt;
		}

		butt = new JButton();

		butt.setPreferredSize(dim);
		if (init) {
			butt.setBackground(key.altcolor);
		} else {
			butt.setBackground(key.color);
		}
		key.state = init;
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		butt.setBorder(lb);
		butt.setOpaque(true);

		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = ly;
		c.anchor = GridBagConstraints.CENTER;

		JLabel lab ;

		c.gridx = _col + px;
		c.gridy = _row;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
//System.err.println("pb."+botlab+" "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(butt);

		if (botlab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+botlab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 1;
			gridbag.setConstraints(lab, c);
//System.err.println("pb."+botlab+".lab "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
			add(lab);
		}

		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addTapeButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String toplab, Color alt, _Key key) {
		final Dimension dim = new Dimension(60, 30);
		JButton butt;
		if (alt != null) {
			key.altcolor = alt;
		}

		butt = new JButton();

		butt.setPreferredSize(dim);
		butt.setBackground(key.color);
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		butt.setBorder(lb);
		butt.setOpaque(true);

		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = ly; // stupid warnings
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.CENTER;

		JLabel lab ;
		if (toplab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+toplab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 12));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 0;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}
}

class Wang1200_Keyboard_left extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 10;

	public Wang1200_Keyboard_left(Wang1200_Indicator tml,
				Wang1200_Indicator er,
				Wang1200_Indicator tmr) {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		setLayout(gridbag);

		addPushButton(c, 5, 3, 0, 0,"LEFT",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(1,0))));
		addPushButton(c, 5, 3, 3, 0,"RIGHT",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(1,4))));
		addPushButton(c, 5, 3, 6, 0,"TRANS.",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(2,_Key.MODE1_CHG(2,4))));
		addPushButton(c, 5, 3, 9, 0,"PLAY",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.MODE1_CHG(2,0))));
		addPushButton(c, 5, 3, 12, 0,"RECORD",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(2,_Key.MODE1_CHG(2,2))));
		_col += 15;

		_col = 0;
		_row += 2;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 5;
		c.gridheight = 1;
		gridbag.setConstraints(tml, c);
//System.err.println("left.tml "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(tml);
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 5;
		c.gridheight = 1;
		gridbag.setConstraints(er, c);
//System.err.println("left.er "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(er);
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 5;
		c.gridheight = 1;
		gridbag.setConstraints(tmr, c);
//System.err.println("left.tmr "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(tmr);
		_col += 5;

		_col = 0;
		_row += 1;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 15;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("left.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		c.gridwidth = 1;
		++_col;

		_col = 0;
		_row += 1;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("left.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 6, 1,"REWIND",
			new _Key(_Key.green1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 1, 6, 1, "FORWARD",
			new _Key(_Key.orange1,_Key.PROG_CODE(8,9)));
		addButton(c,1, 1, 0, 2, 6, 1, "RESET",
			new _Key(_Key.pink1, _Key.PROG_CODE(8,9)));
		_col += 6;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("left.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 6, 1, "BACK<BR>LINE",
			new _Key(_Key.blue1, _Key.PROG_CODE(8,6)));
		addButton(c,1, 2, 0, 1, 6, 1, "CODE",
			new _Key(_Key.white1, _Key.SHIFT));
		_col += 6;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("left.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		_row += 3;
//System.err.println("left finished "+_col+"x"+_row);
		_col = 0;
	}
}

class Wang1200_Keyboard_right extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.1 2011/11/12 00:27:28 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 11;

	public Wang1200_Keyboard_right(Wang1200_Indicator na,
				Wang1200_Indicator el) {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		setLayout(gridbag);

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 5;
		gridbag.setConstraints(na, c);
//System.err.println("right.na "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(na);
		c.gridx = _col;
		c.gridy = _row + 1;
		gridbag.setConstraints(el, c);
//System.err.println("right.el "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(el);
		_col += 5;
		c.gridwidth = 1;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("right.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		c.gridheight = 1;
		++_col;

		addPushButton(c, 5, 3, 0, 0,"SAME",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.MODE2_CHG(3,0))));
		addPushButton(c, 5, 3, 3, 0,"ADJUST",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.MODE2_CHG(3,4))));
		addPushButton(c, 5, 3, 6, 0,"JUSTIFY",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(3,_Key.MODE2_CHG(3,6))));
		_col += 9;

		_col = 0;
		_row += 2;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10,10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("right.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 6, 1, "PARA",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 1, 6, 1, "LINE",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 2, 6, 1, "WORD",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 3, 6, 1, "CHAR/<BR>STOP",
			new _Key(_Key.pink1, _Key.PROG_CODE(0,0)));
		_col += 6;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("right.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 6, 1, "AUTO<BR>START",
			new _Key(_Key.green1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 1, 6, 1, "MEMO<BR>(OUT)",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 2, 6, 1, "SEARCH",
			new _Key(_Key.blue1, _Key.PROG_CODE(0,0)));
		addButton(c,1, 1, 0, 3, 6, 1, "SKIP",
			new _Key(_Key.orange1, _Key.PROG_CODE(0,0)));
		_col += 6;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
//System.err.println("right.pan "+c.gridx+","+c.gridy+" "+c.gridwidth+"x"+c.gridheight);
		add(pan);
		++_col;

		_row += 4;
//System.err.println("right finished "+_col+"x"+_row);
		_col = 0;

	}
}
