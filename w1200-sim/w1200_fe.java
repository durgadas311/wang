// Copyright (c) 2011,2012 Douglas Miller
// $Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.net.Socket;
import java.lang.Math;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.Desktop;

class _Key {
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";

	static final Color orange1 = new Color(255, 210, 180);
	static final Color orange2 = new Color(255, 255, 100);	// illuminated
	static final Color blue1 = new Color(190, 230, 255);
	static final Color blue2 = new Color(255, 255, 100);	// illuminated
	static final Color green1 = new Color(230, 240, 220);
	static final Color pink1 = new Color(255, 220, 220);
	static final Color pink2 = new Color(255, 255, 100);	// illuminated
	static final Color white1 = new Color(250, 250, 250);
	static final Color white2 = new Color(150, 150, 150);
	static final Color white3 = new Color(200, 200, 200);
	static final Color illum1 = new Color(255, 255, 100);
	static final Color red1 = new Color(255, 128, 128);
	static final Color red2 = new Color(180, 150, 150);
	static final Color neon = new Color(244,157,33);
	static final Color neon2 = new Color(214,127,13);
	static final Color empty = new Color(50,50,50);
	static final Color gray = new Color(100,100,100);
	static final Color slate = new Color(65,65,65);
	static final Color ivory = new Color(236,226,190);
	static final Color beige = new Color(230,220,210);
	static final Color aqua = new Color(143,219,195);

	static final int SPCL  = 0x0100;
	static final int MODE1 = 0x0200;
	static final int MODE2 = 0x0300;
	static final int ALT   = 0x0400;
	static final int MODE3 = 0x0500;

	public _Key(Color sl, int c) {
		this.color = sl;
		this.altcolor = sl;
		this.code = c;
		this.state = false;
	}

	public _Key(Color sl, Color xl, int c) {
		this.color = sl;
		this.altcolor = xl;
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
	static final int MODE1_CHG(int a, int b) {
		return (MODE1 | (a << 4) | b);
	}
	static final int MODE2_CHG(int a, int b) {
		return (MODE2 | (a << 4) | b);
	}
	static final int MODE3_CHG(int a, int b) {
		return (MODE3 | (a << 4) | b);
	}
	static final int SPCL_KEY(int b) {
		return (SPCL | b);
	}
	static final int ALT_KEY(int b) {
		return (ALT | b);
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
	public void setOn(boolean on) {
		state = on;
		if (on) {
			button.setBackground(altcolor);
		} else {
			button.setBackground(color);
		}
	}

	Color color;
	Color altcolor;
	int code;
	boolean state;
	JButton button;
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
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";

	public static File _dir;
	public static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
		java.io.BufferedReader ferr = null;
		GridBagLayout gridbag = new GridBagLayout();
		String dir;

		dir = System.getenv("WANG1200HOME");
		if (dir == null) {
			dir = System.getProperty("user.home") + "/Wang1200Files";
		}
		_dir = new File(dir);

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
		_dir.mkdir();
		JFrame front_end = new JFrame("Wang 1200 Word Processing System");
		java.net.URL url = w1200_fe.class.getResource("icons/wang1200-96x96.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		front_end.setIconImage(img);
		front_end.setFocusTraversalKeysEnabled(false);	// allows TAB key to work...
		int row = 0, col = 0;

		front_end.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = col;
		s.gridy = row;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.anchor = GridBagConstraints.NORTH;
		JPanel pan;

		url = w1200_fe.class.getResource("icons/logo-sm.gif");
		ImageIcon ic = new ImageIcon(url);
		JLabel lab = new JLabel(ic);
		lab.setPreferredSize(new Dimension(150, 100));
		lab.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);
		++col;
		s.anchor = GridBagConstraints.NORTH;

		Wang1200_TapeEject ej = new Wang1200_TapeEject();
		Wang1200_Tape tapel = new Wang1200_Tape(fout, ej, "Left");
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 2;
		gridbag.setConstraints(tapel, s);
		s.gridheight = 1;
		front_end.add(tapel);
		col += 2;
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.insets.top = 0;
		s.insets.bottom = 50;
		s.insets.left = 5;
		s.insets.right = 5;
		s.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(ej, s);
		front_end.add(ej);
		++col;
		s.anchor = GridBagConstraints.NORTH;
		s.insets.top = 0;
		s.insets.bottom = 0;
		s.insets.left = 0;
		s.insets.right = 0;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);
		++col;

		ej = new Wang1200_TapeEject();
		Wang1200_Tape taper = new Wang1200_Tape(fout, ej, "Right");
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 2;
		gridbag.setConstraints(taper, s);
		front_end.add(taper);
		col += 2;
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.insets.top = 0;
		s.insets.bottom = 50;
		s.insets.left = 5;
		s.insets.right = 5;
		s.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(ej, s);
		front_end.add(ej);
		++col;
		s.anchor = GridBagConstraints.NORTH;
		s.insets.top = 0;
		s.insets.bottom = 0;
		s.insets.left = 0;
		s.insets.right = 0;
		col = 0;
		++row;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 5));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 8;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);
		++row;

		Wang1200_Indicator tml = new Wang1200_Indicator("TAPE<BR>MOVING");
		Wang1200_Indicator er = new Wang1200_Indicator("RECORD");
		Wang1200_Indicator tmr = new Wang1200_Indicator("TAPE<BR>MOVING");
		Wang1200_Indicator na = new Wang1200_Indicator("NO<BR>ADJUST");
		Wang1200_Indicator el = new Wang1200_Indicator("END OF<BR>DOCUMENT");

		// This now creates a widget for insertion in main frame...
		Wang1200_Model611 m611f = new Wang1200_Model611();

		Wang1200_Keyboard kbd = new Wang1200_Keyboard(fout,
				tml, er, tmr, na, el,
				tapel, taper, m611f);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 8;
		gridbag.setConstraints(kbd, s);
		front_end.add(kbd);
		++row;
		front_end.addKeyListener(kbd);

		_Key skl = kbd.locateKey(_Key.ALT_KEY(1));
		_Key shl = kbd.locateKey(_Key.ALT_KEY(2));
		_Key csl = kbd.locateKey(_Key.MODE2_CHG(7, 7));

		Wang1200_Help help = new Wang1200_Help(front_end);
		Wang1200_SimInput inp = new Wang1200_SimInput(fin,
				tml, er, tmr, na, el,
				skl, shl, csl,
				help,
				tapel, taper, m611f);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Paper");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Print Setup", KeyEvent.VK_U);
		mi.addActionListener(m611f);
		mu.add(mi);
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(m611f);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(m611f);
		mu.add(mi);
		mi = new JMenuItem("Tear Off", KeyEvent.VK_T);
		mi.addActionListener(m611f);
		mu.add(mi);

		mu = new JMenu("Help");
		mb.add(mu);
		mi = help.getMenuItemHelp();
		mi.addActionListener(inp);
		mu.add(mi);
		mi = help.getMenuItemAbout();
		mi.addActionListener(inp);
		mu.add(mi);

		front_end.setJMenuBar(mb);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 5));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(_Key.beige);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,640);

		front_end.pack();	// set size according to content...

		front_end.setVisible(true);
	}
}

class Wang1200_Indicator extends JLabel {
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
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
		setFont(new Font("Sans-serif", Font.PLAIN, 6));
		setPreferredSize(new Dimension(40, 20));
		setHorizontalAlignment(SwingConstants.CENTER);
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
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	Wang1200_Tape _tapel;
	Wang1200_Tape _taper;
	Wang1200_Model611 _m611;
	Wang1200_Indicator _tml;
	Wang1200_Indicator _er;
	Wang1200_Indicator _tmr;
	Wang1200_Indicator _na;
	Wang1200_Indicator _el;
	_Key _skl;
	_Key _shl;
	_Key _csl;

	private Wang1200_Help _help;

	InputStream _fin;

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Menu event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_H) {
			_help.toggle();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_A) {
			_help.showAbout();
			return;
		}
	}

	public Wang1200_SimInput(InputStream f,
			Wang1200_Indicator tml,
			Wang1200_Indicator er,
			Wang1200_Indicator tmr,
			Wang1200_Indicator na,
			Wang1200_Indicator el,
			_Key skl,
			_Key shl,
			_Key csl,
			Wang1200_Help help,
			Wang1200_Tape tapel,
			Wang1200_Tape taper,
			Wang1200_Model611 cn24) {
		_tml = tml;
		_er = er;
		_tmr = tmr;
		_na = na;
		_el = el;
		_skl = skl;
		_shl = shl;
		_csl = csl;
		_tapel = tapel;
		_taper = taper;
		_m611 = cn24;
		_fin = f;
		_help = help;
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
			} else if ((b[1]  & 0xfc) == 0x04) {
				// indicator lamps...
				_er.setOn((b[0] & 0x01) != 0);	// RECORD
				_tmr.setOn((b[0] & 0x02) != 0);	// TAPE MOVE, R
				_tml.setOn((b[0] & 0x04) != 0);	// TAPE MOVE, L
				_el.setOn((b[0] & 0x08) != 0);	// END DOC
				_na.setOn((b[0] & 0x10) != 0);	// NO ADJ
				_csl.setOn((b[0] & 0x20) != 0);	// CHAR/STOP
				_shl.setOn((b[0] & 0x40) != 0);	// SEARCH
				_skl.setOn((b[0] & 0x80) != 0);	// SKIP
			} else if ((b[1]  & 0xf8) == 0x18) {
				// carriage control commands
				int c = b[0] & 0x0f;
				if (c == 0) {
					_m611.do_space();
				} else if (c == 1) {
					_m611.do_backspace();
				} else if (c == 2) {
					_m611.do_tab();		// tab
				} else if (c == 3) {
					_m611.do_crlf();
				} else if (c == 4) {
					_m611.do_shift_up();
				} else if (c == 5) {
					_m611.do_shift_dn();
				} else if (c == 8) {
					_m611.do_index();
				} else if (c == 9) {
					_m611.do_settab();	// set tab
				} else if (c == 10) {
					_m611.do_clrtab();	// clear tab
				} else if (c == 13) {
					_m611.do_lock(0);	// unlock keyboard
				} else if (c == 14) {
					_m611.do_lock(1);	// lock keyboard
				} else if (c == 15) {
					_m611.do_bell();	// ring bell
				}
			} else if ((b[1]  & ~1) == 0x0c) {
				if ((b[1] & 1) == 0) {
					_tapel.do_tape(b);
				} else {
					_taper.do_tape(b);
				}
			} else if ((b[1]  & ~1) == 0x0e) {
				if ((b[0] & 1) == 0) {
					_tapel.do_tape(b);
				} else {
					_taper.do_tape(b);
				}
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_m611.reset();
				//etc...
			} else if (b[1] == 0x10) {
				_m611.do_cn24(b);
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
//		if (e.getWindow() == _m611.getFrame()) {
//			_m611.onOff(false);
//			return;
//		}
	}
}

class Wang1200_TapeEject extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	static final long serialVersionUID = 311057692031L;
	static final int num_keys = 1;

	public Wang1200_TapeEject() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		setLayout(gridbag);

		addTapeButton(c, 1, 1, 0, 0,_Key.white2,
			new _Key(_Key.white1, _Key.GROUP(7,_Key.TAPE_EJECT)));
		setPreferredSize(new Dimension(24,24));
		setBackground(Color.black);
	}

	public _Key getKey() {
		return _keys[0];
	}

	public JButton getBtn() {
		return _buttons[0];
	}
}

class Wang1200_Tape extends JComponent
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	static final long serialVersionUID = 311457692039L;
	java.io.RandomAccessFile _tf;
	java.io.OutputStream _fout;
	Wang1200_TapeEject _btn;
	String _name;
	byte[] bb = new byte[2];
	byte[] b1 = new byte[1];
	boolean _wr;
	boolean _end;
	boolean _ready;
	boolean _tape_on;
	boolean _eot;
	boolean _prot;
	byte _op;
	int _index;
	int _bytc;
	JLabel _window;
	File _file;

	public _Key ejectKey() {
		return _btn.getKey();
	}

	public JButton ejectBtn() {
		return _btn.getBtn();
	}

	public Wang1200_Keyboards getKbd() {
		return _btn;
	}

	public Wang1200_Tape(OutputStream fout, Wang1200_TapeEject btn, String name) {
		_fout = fout;
		_btn = btn;
		_name = name;
		Font font;
		_file = null;
		_index = 0;
		_bytc = 0;
		_end = false;
		_wr = false;
		_ready = false;
		_tape_on = false;
		_eot = false;
		_op = 0;
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
		lb = BorderFactory.createBevelBorder(BevelBorder.RAISED,
				Color.white, Color.gray);
		cass.setBorder(lb);
		cass.setVerticalAlignment(SwingConstants.TOP);
		cass.setHorizontalAlignment(SwingConstants.CENTER);
		cass.setForeground(Color.white);
		cass.setBackground(Color.black);
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
		String txt;
		if (_file == null) {
			txt = new String("<HTML><FONT SIZE=+2>(no tape)</FONT></HTML>");
		} else {
			String eot;
			String prot;
			if (_eot) {
				eot = new String(" (end)");
			} else {
				eot = new String("");
			}
			if (_prot) {
				prot = new String(" <B>(R/O)</B>");
			} else {
				prot = new String("");
			}
			txt = new String("<HTML><B>Tape Name:</B>" + prot + "<BR>" +
				_file.getName() + "<BR>" +
				"<B>Block #</B> " + _index + eot +
				"</HTML>");
		}
		_window.setText(txt);
		repaint();
	}

	private void pick_file() {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser("Mount " + _name + " Tape",
						"wng", "Wang word processor files");
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			_prot = ch.isProtected();
		} else {
			_file = null;
			_prot = false;
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
		_ready = true;
	}

	private void tape_position(int newidx) {
//System.err.println("Tape Position: "+_index+" -> "+newidx);
		if (_file == null) return;
		if (newidx < 0) return;
		if (newidx == 0) {	// rewind
			try {
				_tf.seek(0);
			} catch (IOException ee) {
				// can't happen?
			}
			_index = 0;
			_eot = false;
			return;
		}
		if (newidx == _index) return;	// should not happen?
		try {
//System.err.println("Seeking "+ (newidx * 108));
			// each block is 108 bytes (currently)
			_tf.seek(newidx * 108);
		} catch (IOException ee) {
			// can't happen?
		}
		_index = newidx;
		// assert: _index == newidx
	}

	public boolean do_button(_Key btn) {
		// this kills any in-progress operations...
		_tape_on = false;
		_ready = false;
		if (btn.code == _Key.TAPE_EJECT) {
			pick_file();
			tape_position(0);
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
			_ready = false;
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
			++_bytc;
			if (_bytc >= 108) {
//System.err.println("Tape Read ++index ("+_index+" @ "+_bytc+")");
				_bytc = 0;
				++_index;
				update_tape();
			}
		}
	}

	private void tape_write(byte[] b) {
		try {
			_tf.write(b[0]);
		} catch (IOException ee) {
			// can't happen?
		}
		++_bytc;
		if (_bytc >= 108) {
//System.err.println("Tape Write ++index ("+_index+" @ "+_bytc+")");
			_bytc = 0;
			++_index;
			update_tape();
		}
	}

	public void do_tape(byte[] b) {
		if (b[1] == 0x0e) {	// tape on/off/req
			if ((b[0] & 0x0c0) == 0x40) { // request data
				tape_read();
//				if (false) {
//					++_index; // display updated later...
//				}
				send_word();
			} else { // tape on/off - read/write, etc...
//				if (_wr && !_end && _ready) {
//					++_index;
//				}
				_tape_on = _ready && ((b[0] & 0x0c0) == 0);
				_wr = ((b[0] & 0x20) != 0);
				boolean rv = ((b[0] & 0x10) != 0);
				boolean hi = ((b[0] & 0x08) != 0);
				boolean hl = ((b[0] & 0x04) != 0);
// not the lock signal?		// visual indicator of what might be the "door lock"
//				if (hl) {
//					ejectBtn().setBackground(_Key.red1);
//				} else {
//					ejectBtn().setBackground(_Key.white1);
//				}
				if (hl) {
					if (_tape_on) {
						// fast-forward or rewind...
						_op = 0;
						// now change file position...
						// TBD: what to do for FORWARD
						tape_position(rv ? 0 : -1);
					} else {
						if (_ready) _op = 1;
						else _op = 0;
					}
				} else if (hi) {
					// ready for record/play...
					// TBD: test RO file...
					if (_ready && (!_wr || !_prot)) _op = 1;
					else _op = 0;
					// for reverse, just update position...
					if (_tape_on && rv) {
						tape_position(_index - 1);
					}
				}
				if (!_tape_on) _bytc = 0;
				_end = false;
				update_tape();
				bb[1] = 0x0f;
				bb[0] = b[0];	// how stupid is java?!?!
				bb[0] &= 1;	//
				bb[0] |= (_op << 1);//
				send_word();
			}
			return;
		} else if ((b[1] & ~1) == 0x0c) {	// tape write
			if (!_ready) return;
			tape_write(b);
			// only if last byte before tape-off is END PROG...
			//_end = ((b[0] & 0x00ff) == 0x9e); // END PROG
			//if (_end) {
			//	++_index; // display updated later..
			//}
		} else {
			System.err.format("invalid tape command (%04x)\n", (b[1] << 8) | b[0]);
		}
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
	private String _btn;
	private class TapeProt extends JComponent {
		static final long serialVersionUID = 31170769203L;
		public Checkbox btn;
		public TapeProt(String b) {
			btn = new Checkbox(b);
			setLayout(new FlowLayout());
			add(btn);
		}
	}
	private TapeProt _prot;
	public SuffFileChooser(String btn, String sfx, String dsc) {
		super(w1200_fe._dir);
		SuffFileFilter f = new SuffFileFilter(sfx, dsc);
		setFileFilter(f);
		_btn = btn;
		setApproveButtonText(btn);
		setApproveButtonToolTipText(btn);
		setDialogTitle(btn);
		setDialogType(JFileChooser.SAVE_DIALOG);
		_sfx = "." + sfx;

		_prot = new TapeProt("Protect");
		setAccessory(_prot);
	}
	public int showDialog(Component frame) {
		int rv = super.showDialog(frame, _btn);
		if (rv == JFileChooser.APPROVE_OPTION) {
			if (getSelectedFile().getName().endsWith(_sfx)) {
				return rv;
			}
			File f = new File(getSelectedFile().getAbsolutePath().concat(_sfx));
			setSelectedFile(f);
		}
		return rv;
	}
	public boolean isProtected() {
		return _prot.btn.getState();
	}
}

class Wang1200_Model611
	extends JComponent
	implements ActionListener, ComponentListener
{
	static final long serialVersionUID = 31140769203L;
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	private byte[] cn24_xlate;
	private byte[] cn24_revxlate;
	private char[] cn24_spcl;
	private byte[] cn24_tabstops;
	private int _max;	// last active tab stop, or zero if none

	public void reset() {
		// anything?
	}

	public byte ascii2roti(char c) {
		byte cc;
		cc = cn24_revxlate[c];
		// cleanup needed? ctl codes...
		return cc;
	}
	public byte roti2ascii(byte c) {
		byte cc = cn24_xlate[c];
		// cleanup needed? ctl codes...
		return cc;
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
		cn24_xlate[0x29] = 'l';
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
		cn24_xlate[0x3f] = '1';

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
		cn24_xlate[0x69] = 'L';
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
		cn24_xlate[0x7f] = '!';

		cn24_spcl = new char[32];
		cn24_spcl[0x01] = '\u00BD';
		cn24_spcl[0x02] = '\u00BC';
		cn24_spcl[0x03] = '\u00A2';

		cn24_revxlate = new byte[256];
		cn24_revxlate['-'] = 0x00;
		cn24_revxlate['y'] = 0x01;
		cn24_revxlate[' '] = 0x02;
		cn24_revxlate['\b'] = 0x13;
		cn24_revxlate['q'] = 0x04;
		cn24_revxlate['p'] = 0x05;
		cn24_revxlate['='] = 0x06;
		cn24_revxlate['j'] = 0x07;
		cn24_revxlate[' '] = 0x03;
		cn24_revxlate['/'] = 0x09;
		cn24_revxlate[','] = 0x0c;
		cn24_revxlate[';'] = 0x0d;
		cn24_revxlate['f'] = 0x0e;
		cn24_revxlate['g'] = 0x0f;

		cn24_revxlate['w'] = 0x10;
		cn24_revxlate['s'] = 0x11;
		cn24_revxlate['i'] = 0x14;
		cn24_revxlate['\''] = 0x15;
		cn24_revxlate['.'] = 0x16;
		cn24_revxlate['['] = 0x17;	// 1/2...
		cn24_revxlate['\n'] = 0x18;
		cn24_revxlate['o'] = 0x19;
		cn24_revxlate['\n'] = 0x33;
		cn24_revxlate['\t'] = 0x23;
		cn24_revxlate['a'] = 0x1c;
		cn24_revxlate['r'] = 0x1d;
		cn24_revxlate['v'] = 0x1e;
		cn24_revxlate['m'] = 0x1f;

		cn24_revxlate['b'] = 0x20;
		cn24_revxlate['h'] = 0x21;
		cn24_revxlate['k'] = 0x24;
		cn24_revxlate['e'] = 0x25;
		cn24_revxlate['n'] = 0x26;
		cn24_revxlate['t'] = 0x27;
		cn24_revxlate['l'] = 0x29;
		cn24_revxlate['c'] = 0x2c;
		cn24_revxlate['d'] = 0x2d;
		cn24_revxlate['u'] = 0x2e;
		cn24_revxlate['x'] = 0x2f;

		cn24_revxlate['9'] = 0x30;
		cn24_revxlate['0'] = 0x31;
		cn24_revxlate['6'] = 0x34;
		cn24_revxlate['5'] = 0x35;
		cn24_revxlate['2'] = 0x36;
		cn24_revxlate['z'] = 0x37;
		cn24_revxlate['4'] = 0x39;
		cn24_revxlate['8'] = 0x3c;
		cn24_revxlate['7'] = 0x3d;
		cn24_revxlate['3'] = 0x3e;
		cn24_revxlate['1'] = 0x3f;

		// shifted versions...
		cn24_revxlate['_'] = (byte)0x80;
		cn24_revxlate['Y'] = (byte)0x81;
		cn24_revxlate['Q'] = (byte)0x84;
		cn24_revxlate['P'] = (byte)0x85;
		cn24_revxlate['+'] = (byte)0x86;
		cn24_revxlate['J'] = (byte)0x87;
		cn24_revxlate['?'] = (byte)0x89;
		cn24_revxlate[','] = (byte)0x8c;
		cn24_revxlate[':'] = (byte)0x8d;
		cn24_revxlate['F'] = (byte)0x8e;
		cn24_revxlate['G'] = (byte)0x8f;

		cn24_revxlate['W'] = (byte)0x90;
		cn24_revxlate['S'] = (byte)0x91;
		cn24_revxlate['|'] = (byte)0x92;	// Set Tab
		cn24_revxlate['I'] = (byte)0x94;
		cn24_revxlate['"'] = (byte)0x95;
		cn24_revxlate['.'] = (byte)0x96;
		cn24_revxlate['{'] = (byte)0x97;	// 1/4
		cn24_revxlate['O'] = (byte)0x99;
		cn24_revxlate['A'] = (byte)0x9c;
		cn24_revxlate['R'] = (byte)0x9d;
		cn24_revxlate['V'] = (byte)0x9e;
		cn24_revxlate['M'] = (byte)0x9f;

		cn24_revxlate['B'] = (byte)0xa0;
		cn24_revxlate['H'] = (byte)0xa1;
		cn24_revxlate['K'] = (byte)0xa4;
		cn24_revxlate['E'] = (byte)0xa5;
		cn24_revxlate['N'] = (byte)0xa6;
		cn24_revxlate['T'] = (byte)0xa7;
		cn24_revxlate['L'] = (byte)0xa9;
		cn24_revxlate['C'] = (byte)0xac;
		cn24_revxlate['D'] = (byte)0xad;
		cn24_revxlate['U'] = (byte)0xae;
		cn24_revxlate['X'] = (byte)0xaf;

		cn24_revxlate['('] = (byte)0xb0;
		cn24_revxlate[')'] = (byte)0xb1;
		cn24_revxlate['^'] = (byte)0xb4;	// cent
		cn24_revxlate['%'] = (byte)0xb5;
		cn24_revxlate['@'] = (byte)0xb6;
		cn24_revxlate['Z'] = (byte)0xb7;
		cn24_revxlate['$'] = (byte)0xb9;
		cn24_revxlate['*'] = (byte)0xbc;
		cn24_revxlate['&'] = (byte)0xbd;
		cn24_revxlate['#'] = (byte)0xbe;
		cn24_revxlate['!'] = (byte)0xbf;
	}
	private PlotTextArea _text;
	private JScrollPane _scroll;
	private JLabel _carriage;
	private JLabel _bell;
	private JLabel _lock;
	private javax.swing.Timer timer;

//	private int _xoff, _yoff;
	private int _eop;	// position of user character
	private int _eoc;	// position of internal cursor
	private int _eol;	// position of internal newline
	//
	// Typewriter emulation of carriage and non-destructive space/backspace:
	//            +--- "_eop"
	//            |               +-- "_eol"
	//            v               v
	// user typed characters here.\n
	// -----------@
	//            ^
	//            +--- "_eoc"
	//
	// Where '-' is blank spaces, '@' is the cursor character (\u25b2),
	// and '\n' is the internally-maintain newline - not part of user typed text.
	//
	// Actions:
	//
	// SP:   ++_eop;
	//       _text.insert(" ",_eoc); ++_eoc;
	//
	// BS:   --_eop;
	//       --_eoc; _text.replaceRange(null, _eoc, _eoc + 1);
	//
	// CR:   _text.replaceRange(null, _eop, _eoc); _text.insert("\n", _eop); _eol = _eop + 1; _eoc = _eol + 1;
	//
	// char: if (_eop < _eol) { _text.replaceRange(char, _eop, _eop + 1); }
	//       else { _text.insert(char, _eol); ++_eol; }
	//       ++_eop;
	//       _text.insert(" ",_eoc); ++_eoc;
	//
	// CLEAR: _text.setText(""); _eop = _eol = _eoc = 0; call(CR);
	//
	//
	double _fx;
	int _fy, _fa;
	double _gx, _gy;

	private void clear() {
		_text.setText("\n\u25b2");
		_eop = _eol = 0;
		_eoc = 1;
		_shifted = false;
		_text.clear();
		_text.setCaretPosition(_eoc + 1);
	}

	String _footer;

	public boolean cursor_left() {
		if (_eoc - 1 <= _eol) {
			return false;
		}
		--_eoc;
		_text.replaceRange(null, _eoc, _eoc + 1);
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
		return true;
	}

	public boolean cursor_right() {
		_text.insert(" ", _eoc);
		++_eoc;
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
		return true;
	}

	public void do_crlf() {
		_text.replaceRange(null, _eol + 1, _eoc);	// this leaves cursor in buffer...
		_text.replaceRange(null, _eop, _eol);		// this leaves \n in buffer...
		_text.insert("\n", _eop);
		++_eop;
		_eol = _eop;
		_eoc = _eol + 1;
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
	}

	public void do_index() {
		do_crlf(); // should it be different?
	}

	public void do_space() {
		if (_eop >= _eol) {
			_text.insert(" ", _eol);
			++_eol;
			++_eoc;
		}
		++_eop;
		cursor_right();
	}

	public void do_backspace() {
//System.err.println("BS");
		if (cursor_left()) {
			--_eop;
		}
	}

	public void do_char(char c) {
		String s = Character.toString(c);
		if (_eop < _eol) {
			if (c == '_') {
				_text.addUnderline(_eop, _eop + 1);
				_text.repaint();
			} else {
				_text.replaceRange(s, _eop, _eop + 1);
				_text.rmUnderline(_eop, _eop + 1); // might repaint()
			}
		} else {
			_text.insert(s, _eol);
			++_eol;
			++_eoc;
		}
		++_eop;
		cursor_right();
	}

	public void do_tab() {
		int col;
		if (_eoc - _eol < _max) {
			do {
				do_space();
				col = _eoc - _eol;
			} while (col <= _max && cn24_tabstops[col - 1] == 0);
		}
	}

	public void do_settab() {
		int col = _eoc - _eol;
		cn24_tabstops[col - 1] = 1;
		if (col > _max) _max = col;
//System.err.println("set tab @ "+col);
	}

	public void do_clrtab() {
		int col = _eoc - _eol;
		cn24_tabstops[col - 1] = 0;
//System.err.println("clear tab @ "+col);
		if (_max == col) {
			while (col > 0 && cn24_tabstops[col - 1] == 0) --col;
			_max = col;
		}
	}

	public void do_lock(int lk) {
		if (lk != 0) {
			_lock.setBackground(_Key.neon);
		} else {
			_lock.setBackground(_Key.aqua);
		}
	}

	public void do_bell() {
		_bell.setBackground(_Key.neon);
		timer.start();
		Toolkit.getDefaultToolkit().beep();
	}

	public void do_shift_dn() {
		_shifted = false;
	}

	public void do_shift_up() {
		_shifted = true;
	}

	private JTextField _cpi_t;
	private JPanel _cpi_f;
	private double _cpi;
	private JTextField _cpl_t;
	private JPanel _cpl_f;
	private double _cpl;
	private JTextField _lpi_t;
	private JPanel _lpi_f;
	private double _lpi;
	private JTextField _lpp_t;
	private JPanel _lpp_f;
	private double _lpp;
	private Checkbox _fute;
	private boolean _fte;
	private JTextField _fut;
	private String _ftt;
	private OrientationRequested _ort;
	private double _pfz;

//	private int[] _cursor_x;
//	private int[] _cursor_y;
//	private int _cursor_n;

	public Wang1200_Model611() {
		setup_xlate();
		cn24_tabstops = new byte[256];
		int x;
		for (x = 0; x < 256; ++x) cn24_tabstops[x] = 0;
		_max = 0;

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);

		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		_carriage = new JLabel();
		_carriage.setVerticalAlignment(SwingConstants.CENTER);
		_carriage.setHorizontalAlignment(SwingConstants.CENTER);
		_carriage.setPreferredSize(new Dimension(30, 15));
		_carriage.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_carriage.setOpaque(true);
		_carriage.setForeground(Color.black);
		_carriage.setBackground(_Key.aqua);
		_carriage.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.weightx = 1;
		s.weighty = 0;
		s.anchor = GridBagConstraints.EAST;
		s.insets = new Insets(0, 0, 0, 3);
		gridbag.setConstraints(_carriage, s);
		add(_carriage);

		_bell = new JLabel("bell");
		_bell.setVerticalAlignment(SwingConstants.CENTER);
		_bell.setHorizontalAlignment(SwingConstants.CENTER);
		_bell.setPreferredSize(new Dimension(30, 15));
		_bell.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_bell.setOpaque(true);
		_bell.setForeground(Color.black);
		_bell.setBackground(_Key.aqua);
		_bell.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.gridx = 1;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.anchor = GridBagConstraints.CENTER;
		s.insets = new Insets(0, 3, 0, 3);
		gridbag.setConstraints(_bell, s);
		add(_bell);
		timer = new Timer(500, this);

		_lock = new JLabel("lock");
		_lock.setVerticalAlignment(SwingConstants.CENTER);
		_lock.setHorizontalAlignment(SwingConstants.CENTER);
		_lock.setPreferredSize(new Dimension(30, 15));
		_lock.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_lock.setOpaque(true);
		_lock.setForeground(Color.black);
		_lock.setBackground(_Key.aqua);
		_lock.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.gridx = 2;
		s.gridy = 0;
		s.weightx = 1;
		s.weighty = 0;
		s.anchor = GridBagConstraints.WEST;
		s.insets = new Insets(0, 3, 0, 0);
		gridbag.setConstraints(_lock, s);
		add(_lock);

		_text = new PlotTextArea();
		_text.setFont(new Font("Monospaced", Font.PLAIN, 10));

		// setting this messes up horiz scrollbar...
		//_text.setPreferredSize(new Dimension(60 * _fx, 32 * _fy));
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);
		_text.setFocusable(false);

		clear();

		FontMetrics fm = _text.getFontMetrics(_text.getFont());
		_fa = fm.getAscent();
		char[] cc = { 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M' };
		int wi = fm.charsWidth(cc, 0, 10);
		_fx = wi / 10.0;
		_fy = fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

//		_cursor_x = new int[8];
//		_cursor_y = new int[8];
//		_cursor_n = 0;
//
//		x = (int)Math.round(_fx / 2.0);
//		y = 0;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		x *= 2;
//		y += (int)Math.round(_fy / 2.0);
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		y *= 2;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		x = 0;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		y /= 2;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension((int)Math.round(96 * _fx), 32 * _fy + 24));
		s.gridx = 0;
		s.gridy = 1;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 3;
		s.insets = new Insets(0, 0, 0, 0);
		s.anchor = GridBagConstraints.CENTER;
		gridbag.setConstraints(_scroll, s);
		add(_scroll);

		//parent.pack();	// set size according to content...

//		Dimension fdim = getSize();
//		Dimension sdim = _scroll.getSize();
//		_xoff = fdim.width - sdim.width;
//		_yoff = fdim.height - sdim.height;

		Dimension dim = new Dimension(50, 20);
		_cpi = 10.0;
		_cpi_t = new JTextField();
		_cpi_t.setPreferredSize(dim);
		_cpi_f = new JPanel();
		_cpi_f.add(new JLabel("Chars/Inch:"));
		_cpi_f.add(_cpi_t);

		_cpl = 0.0; // 75.0;
		_cpl_t = new JTextField();
		_cpl_t.setPreferredSize(dim);
		_cpl_f = new JPanel();
		_cpl_f.add(new JLabel("Chars/Line:"));
		_cpl_f.add(_cpl_t);

		_lpi = 6.0;
		_lpi_t = new JTextField();
		_lpi_t.setPreferredSize(dim);
		_lpi_f = new JPanel();
		_lpi_f.add(new JLabel("Lines/Inch:"));
		_lpi_f.add(_lpi_t);

		_lpp = 0.0; // 66.0;
		_lpp_t = new JTextField();
		_lpp_t.setPreferredSize(dim);
		_lpp_f = new JPanel();
		_lpp_f.add(new JLabel("Lines/Page:"));
		_lpp_f.add(_lpp_t);

		_fte = false;
		_fute = new Checkbox("Enable footers");

		_ftt = "Wang 1200 Output";
		_fut = new JTextField();

		// Portrait/Landscape? Left/Right margin? Top/Bottom?
		_ort = OrientationRequested.PORTRAIT;

		addComponentListener(this);
	}

	private void save611(File file) {
		FileOutputStream fo;
		try {
			fo = new FileOutputStream(file);
		} catch (FileNotFoundException ee) {
			System.err.println("chosen 611 file not found?");
			return;
		}
		_text.save(fo);
		try {
			fo.close();
		} catch (IOException ee) {
			System.err.println("error writing 611 TXT");
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			timer.stop();
			_bell.setBackground(_Key.aqua);
			return;
		}
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
			sfx = "txt";
			dsc = "Text files";
			SuffFileChooser ch = new SuffFileChooser("Save", sfx, dsc);
			int rv = ch.showDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				save611(ch.getSelectedFile());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_U) {
			_cpi_t.setText(Double.toString(_cpi));
			_cpl_t.setText(Double.toString(_cpl));
			_lpi_t.setText(Double.toString(_lpi));
			_lpp_t.setText(Double.toString(_lpp));
			_fute.setState(_fte);
			_fut.setText(_ftt);
			Object[] dia = { _cpi_f,
					_cpl_f,
					_lpi_f,
					_lpp_f,
					_fute,
					"Footer Text:", _fut
				};
			int ret = JOptionPane.showConfirmDialog(this, dia, "Set Page Geometry",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if (ret != JOptionPane.OK_OPTION) {
				return;
			}
			_cpi = Double.valueOf(_cpi_t.getText());
			_cpl = Double.valueOf(_cpl_t.getText());
			_lpi = Double.valueOf(_lpi_t.getText());
			_lpp = Double.valueOf(_lpp_t.getText());
			_fte = _fute.getState();
			_ftt = _fut.getText();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(_ort);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.5, (float)0.5, (float)7.5, (float)10.0, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(_text);
			boolean print = pj.printDialog(aset);
			if (print) {
				// Would like to setup page here, only once, but for some
				// idiotic reason PrinterJob won't tell me the PageFormat.

				java.util.Date dt = new java.util.Date();
				_footer = new String(_ftt + " - " +
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

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
//		if (e.getComponent() == this) {
//			Dimension fdim = getSize(); 
//			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff - 24);
//System.err.println("_scroll.setSize " + _scroll.getSize());
//			_scroll.setPreferredSize(_scroll.getSize());
//			setSize(fdim.width, fdim.height); // redundant?
//System.err.println("this.setSize " + getSize());
//			setPreferredSize(getSize());
//		}
	}

	private boolean _shifted;

	class PlotTextArea extends JTextArea
			implements Printable {
		static final long serialVersionUID = 311457692040L;

		class underlines {
			underlines(int s, int e) {
				start = s;
				end = e;
			}
			public boolean extend(int s, int e) {
				if (e == start) {
					start = s;
					return true;
				}
				if (end == s) {
					end = e;
					return true;
				}
				return false;
			}
			public int trim(int s, int e) {
				// assert (s < e)
				if (e <= start || s >= end) return 0;
				if (s <= start) {
					start = e;
					return 1;
				}
				if (e >= end) {
					end = s;
					return 1;
				}
				return 2;
			}
			public int start;
			public int end;
		}

		public void clear() {
			_nund = 0;
			_und = null;
		}

//		public void drawCursor(Graphics g) {
//			Polygon p = new Polygon(_cursor_x, _cursor_y, _cursor_n);
//			int x = (_eoc - _eol) * (int)Math.round(_fx);
//			try {
//				int ln = _text.getLineOfOffset(_eoc);
//				int y = ln * _fy;
//				p.translate(x, y);
//				g.fillPolygon(p);
//			} catch(javax.swing.text.BadLocationException ee) {
//			}
//		}

		private underlines[] _und;
		private int _nund;

		private void do_1underline(Graphics g, int ls, int le, int x, int pos, double w) {
			int ls_ = ls;
			int le_ = le;
			if (ls_ < _und[x].start) ls_ = _und[x].start;
			if (le_ > _und[x].end) le_ = _und[x].end;
			int x1 = (int)Math.round((ls_ - ls) * w);
			int x2 = (int)Math.round((le_ - ls) * w);
			int y = pos + 1;
			g.drawLine(x1, y, x2, y);
		}

		private void do_underline(Graphics g, int ls, int ll, int pos, double w) {
			int le = ls + ll;
			int x;
			for (x = 0; x < _nund; ++x) {
				if (_und[x].start < le && _und[x].end >= ls) {
					do_1underline(g, ls, le, x, pos, w);
				}
			}
		}

		public void addUnderline(int s, int e) {
			int x;
			for (x = 0; x < _nund; ++x) {
				if (_und[x].extend(s, e)) return;
			}
			++_nund;
			underlines[] u = new underlines[_nund];
			if (x > 0) {
				System.arraycopy(_und, 0, u, 0, x);
			}
			u[x] = new underlines(s, e);
			_und = u;
		}

		public void rmUnderline(int s, int e) {
			int x, r = 0;
			for (x = 0; x < _nund; ++x) {
				r = _und[x].trim(s, e);
				if (r != 0) break;
			}
			if (r == 0) return;
			// either case, display needs to repaint...
			if (r == 2) {
				// must split _und[x]...
				// _und[x].start ... s ... e ... _und[x].end
				int t = _und[x].end;
				_und[x].end = s;
				s = e;
				e = t;
				x = _nund;
				++_nund;
				underlines[] u = new underlines[_nund];
				if (x > 0) {
					System.arraycopy(_und, 0, u, 0, x);
				}
				u[x] = new underlines(s, e);
				_und = u;
			}
			repaint();
		}

		private String strUnderline(String s) {
			int i;
			int n = s.length();
			String r = new String("");
			i = 0;
			while (i < n) {
				r += s.substring(i, i + 1) + "\b_";
				++i;
			}
			return r;
		}

		public void save(FileOutputStream fo) {
			int a, b, l;
			int x;

		try {
			a = 0;
			x = 0;
			while (a < _eop) {
				if (x >= _nund) {
					b = _eop;
				} else {
					b = _und[x].start;
				}
				l = b - a;
				if (l > 0) {
					fo.write(getText(a, l).getBytes());
				}
				if (x < _nund) {
					// can this ever be zero length?
					a = _und[x].end;
					String s;
					s = getText(b, a - b);
					// too bad this doesn't work...
					//s = s.replaceAll("(.)", "\\1\b_");
					s = strUnderline(s);
					fo.write(s.getBytes());
					++x;
				} else {
					a = b;
				}
			}
			//fo.write('\n');
		} catch (javax.swing.text.BadLocationException ee) {
			System.err.println("error extracting 611 TXT");
		} catch (IOException ee) {
			System.err.println("error writing 611 TXT");
		}

		}

		public void paint(Graphics g) {
			super.paint(g);
			int x;
			for (x = 0; x < _nund; ++x) {
				try {
					int ln = _text.getLineOfOffset(_und[x].start);
					int ls = _text.getLineStartOffset(ln);
					int le = _text.getLineEndOffset(ln);
					do_1underline(g, ls, le, x, ln * _fy + _fa, _fx);
				} catch(javax.swing.text.BadLocationException ee) {
				}
			}
			// drawCursor(g);
		}

		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();
			double y0 = pf.getImageableY();
			double w0 = pf.getImageableWidth();
			double h0 = pf.getImageableHeight();

			int fl = 0;
			if (_fte) {
				fl = 2;
			}

			// This should be done after the printDialog, only
			// once per job.  But for some stupid reason the
			// PrinterJob object doesn't allow getting PageFormat.
			double nf, d;
			nf = 0.0;
			if (_lpp > 0) {
				nf = Math.floor(h0 / (_lpp + fl));// start with LPP sized font...
			}
			if (_lpi > 0.0) {
				d = 72.0 / _lpi;		// see if LPI requires smaller font...
				if (nf == 0.0 || d < nf) nf = d;
			}
			FontMetrics fm;
			if (_cpi > 0.0) {
				d = _fy *		// see if CPI requires smaller font...
					((72.0 / _cpi) / _fx);
				if (nf == 0.0 || d < nf) nf = d;
			}
			if (_cpl > 0.0) {
				d = Math.floor(w0 / _cpl);	// see if CPL requires smaller font...
				if (nf == 0.0 || d < nf) nf = d;
			}
			_pfz = nf;

			int pg = 0;
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);

			if (_pfz > 0.0) {
				g2d.setFont(_text.getFont().deriveFont((float)_pfz));
			} else {
				g2d.setFont(_text.getFont());
			}
			fm = g2d.getFontMetrics(g2d.getFont());
			char[] cc = { 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M' };
			int wi = fm.charsWidth(cc, 0, 10);
			double w = wi / 10.0;
//System.err.println("Got printer font "+g2d.getFont().getFontName()+" size "+g2d.getFont().getSize()+" width(M) "+w);

			int lpp = (int)_lpp;
			if (lpp == 0.0) {
				lpp = (int)Math.floor(h0 / g2d.getFont().getSize());
			}
//System.err.println("Using font size "+g2d.getFont().getSize()+"pt, L/P="+lpp);

			int did = 0;
			String s;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			g2d.setStroke(new BasicStroke((float)0.5));
			int l = g2d.getFont().getSize();
			int max = getLineCount();
			while (pg <= pageIndex) {
				int ln;
				for (ln = 0; ln < lpp; ++ln) {
					int nn = ln + pg * lpp;
					if (nn >= max) break;
					int ls, ll;
					try {
						ls = getLineStartOffset(nn);
						ll = getLineEndOffset(nn) - ls;
						if (ls >= _eop) break;
						s = getText(ls, ll);
					} catch(javax.swing.text.BadLocationException ee) {
						break;
					}
					if (pg == pageIndex) {
						++did;
						if (s.length() > 0) { // not blank line...
							g2d.drawString(s, 0, ln * l + l);
							do_underline(g2d, ls, ll, ln * l + l, w);
						}
					}
				}
				if (pg == pageIndex) {
				}
				++pg;
			}
			if (did > 0) {
				if (fl > 0) {
					s = new String("Page " + pg +
						" - " + _footer);
					g2d.drawString(s, 0, (lpp + 1) * l + l); // consumes 2 lines...
				}
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	public void do_cn24(byte[] b) {
		byte p;
		if (_shifted) {
			p = cn24_xlate[b[0] + 0x40];
		} else {
			p = cn24_xlate[b[0]];
		}
		char c;
		if (p == 0) {
			c = '\u2588';
		} else if (p < 0x07) {
			c = cn24_spcl[p];
		} else {
			c = (char)p;
		}
//if (c == '_') System.err.println("UL");
//System.err.println("ch="+c);
		do_char(c);
	}
	public void do_cn24_direct(char c) {
		// ugh... beats going through cn24_xlate/cn24_revxlate...
		if (c == '^') c = '\u00A2';		// cent
		else if (c == '[') c = '\u00BD';	// 1/2
		else if (c == '{') c = '\u00BC';	// 1/4
		else if (c == '|') {
			do_settab();
			return;
		// clear tab, too?
		}
		if (c == '\b') {
			do_backspace();
		} else if (c == '\n') {
			do_crlf();
		} else if (c == '\t') {
			do_tab();
		} else if (c == ' ') {
			do_space();
		} else if (c != '\0') {
			do_char(c);
		}
	}
}

class Wang1200_Help extends JComponent
	implements ActionListener, WindowListener, ComponentListener, HyperlinkListener
{
	static final long serialVersionUID = 311857692031L;
	private JFrame _frame;
	private JEditorPane _text;
	private JScrollPane _scroll;
	private int _xoff, _yoff;
	private JMenuItem _help;
	private JMenuItem _about;
	private boolean _help_on;
	private JFrame _main;

	public JMenuItem getMenuItemHelp() {
		return _help;
	}

	public JMenuItem getMenuItemAbout() {
		return _about;
	}

	public Wang1200_Help(JFrame frame) {
		_main = frame;
		_help = new JMenuItem("Show Help", KeyEvent.VK_H);;
		_about = new JMenuItem("About", KeyEvent.VK_A);
		_help_on = false;

		java.net.URL url = Wang1200_Keyboard.class.getResource("docs/wang1200.html");
		_frame = new JFrame("Wang 1200 Help");
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (IOException ee) {
			System.err.println("can't create Help JEditorPane "+url);
		}
		_text.setEditable(false);
		_text.setFont(new Font("Sans-serif", Font.PLAIN, 12));
		int z = _text.getFont().getSize();
		// for some reason, this randomly messes up scroll size...
		//_text.setPreferredSize(new Dimension(60 * z, 32 * z));
		_text.addHyperlinkListener(this);

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(60 * z, 32 * z));

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Topic");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Basic Operation", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("About the Simulator", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Resources and Links", KeyEvent.VK_L);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(mb);
		_frame.add(_scroll);
		_frame.pack();

		_frame.addWindowListener(this);
		_frame.addComponentListener(this);

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
	}

	public void showAbout() {
		JOptionPane.showMessageDialog(_main,
				"Wang 1200 Word Processor System\n"+
				"Simulator\n"+
				"$Revision: 1.52 $ $Date: 2012/01/28 15:44:49 $\n\n"+
				"Developed by Douglas Miller\n"+
				"http://www.durgadas.com/wang1200.html\n\n"+
				"With Jim Battle\n"+
				"http://wang1200.org\n"
			);
	}

	public void toggle() {
		setOn(!_help_on);
	}

	private void setOn(boolean on) {
		_help_on = on;
		if (on) {
			_frame.pack();
			_help.setText("Hide Help");
		} else {
			_help.setText("Show Help");
		}
		_frame.setVisible(on);
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (e.getWindow() == _frame) {
			setOn(false);
			return;
		}
	}

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
			return;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			java.net.URL url = null;
			// should use a table to lookup url?
			if (m.getMnemonic() == KeyEvent.VK_B) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_L) {
				url = Wang1200_Keyboard.class.getResource("docs/wang1200links.html");
			} else {
				System.err.println("help menu " + e.getActionCommand() +
						" not implemented yet");
				return;
			}
			try {
				_text.setPage(url);
			} catch (IOException ee) {
			}
			return;
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent r) {
		if (r.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			if (r.getURL().getProtocol().compareTo("file") == 0 ||
			    r.getURL().getProtocol().compareTo("jar") == 0) {
				String doc = r.getURL().getFile();
				if (r.getURL().getProtocol().compareTo("jar") == 0) {
					// ugh! must be a better way...
					doc = doc.replaceFirst("/wang1200\\.jar!/","/");
					doc = doc.replaceFirst("file:","");
				}
				try {
					Desktop.getDesktop().open(new File(doc));
				} catch (IOException e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				} catch(Exception e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				}
			} else {
				try {
					Desktop.getDesktop().browse(r.getURL().toURI());
				} catch (IOException e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				} catch(Exception e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				}
			}
		}
	}
}

class Wang1200_Keyboard extends JComponent
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 4;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang1200_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _code;
	int _code_kbd;
	int _code_btn;
	int _mode1;
	int _mode2;
	int _mode3;
	byte[] code_xlate;
	OutputStream _fout;
	Wang1200_Tape _tapel;
	Wang1200_Tape _taper;
	Wang1200_Model611 _m611;

	public _Key locateKey(int code) {
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (_kbds[y]._keys[x].code == code) {
					return _kbds[y]._keys[x];
				}
			}
		}
		// fatal error?
		return null;
	}

	private void setCode(boolean _new) {
		_code = _new;
		if (_code) {
			_kbds[_code_kbd]._buttons[_code_btn].setBackground(_Key.illum1);
		} else {
			_kbds[_code_kbd]._buttons[_code_btn].setBackground(_kbds[_code_kbd]._keys[_code_btn].color);
		}
	}

	private void setToggle(boolean on, _Key key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == _Key.MODE1) {
			_mode1 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE2) {
			_mode2 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE3) {
			_mode3 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == _Key.MODE1) {
				_mode1 |= key.getMode();
			} else if (key.getType() == _Key.MODE2) {
				_mode2 |= key.getMode();
			} else if (key.getType() == _Key.MODE3) {
				_mode3 |= key.getMode();
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

	private void set_group_mode1(int g, int y, int x, boolean alt) {
		int z;
		_Key key = _kbds[y]._keys[x];
		int mode = key.getMode();
		int numon = 0;
		boolean couldbe = (alt && (mode == 0 || (mode & 4) != 0));
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			if (_kbds[y]._keys[z] == null) continue;
			_Key key2 = _kbds[y]._keys[z];
			int tg = _kbds[y]._keys[z].getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			int mode2 = key2.getMode();
			boolean dbldown = (couldbe && mode2 != mode &&
				(mode2 == 0 || (mode2 & 4) != 0));
			if (key2.state) {
				if (dbldown) {
					// leave button down...
					_mode1 = key2.getMask(); // all on...
					++numon;
				} else {
					key2.state = false;
					_mode1 &= ~key2.getMask();
					_kbds[y]._buttons[z].setBackground(key2.color);
				}
			}
		}
		// never toggle?
		key.state = !key.state || (numon == 0);
		if (key.state) {
			_mode1 |= key.getMode();
			_kbds[y]._buttons[x].setBackground(key.altcolor);
		} else {
			_mode1 &= ~key.getMask();
			_kbds[y]._buttons[x].setBackground(key.color);
		}
	}

	public void do_keycode(boolean coded, int code) {
		if ((code & ~0x0ff) == 0 && _code) {
			int ix = code & 0x00f;
			code &= ~0x00f;
			code |= code_xlate[ix];
		}
		if (!coded) setCode(false);
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

	private void do_button(boolean alt, boolean coded, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if (_kbds[y]._keys[x].isSHIFT()) {
			if (!coded) setCode(!_code);
			return;
		}
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x] == _tapel.ejectKey()) {
			boolean st = _tapel.do_button(_kbds[y]._keys[x]);
			if (st) {
				setToggle(false,
					_kbds[y]._keys[x], _kbds[y]._buttons[x]);
			}
			return;
		} else if (_kbds[y]._keys[x] == _taper.ejectKey()) {
			boolean st = _taper.do_button(_kbds[y]._keys[x]);
			if (st) {
				setToggle(false,
					_kbds[y]._keys[x], _kbds[y]._buttons[x]);
			}
			return;
		}
		if (g != 0) {
			if (type == _Key.MODE1) {
				set_group_mode1(g, y, x, alt);
			} else {
				set_group(g, y, x);
			}
		}
		// _mode1, _mode2, _mode3 were already updated above...
		if (type == _Key.MODE1) {
			code = _Key.MODE1 | _mode1;
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				code |= _kbds[y]._keys[x].getMode();
			}
		}
		if (type == _Key.MODE2) {
			// was not handled above!
			// these bits should not really be static...
			_mode2 &= ~_kbds[y]._keys[x].getMask();
			_mode2 |= _kbds[y]._keys[x].getMode();
			code = _Key.MODE2 | _mode2;
		}
		if (type == _Key.MODE3) {
			code = _Key.MODE3 | _mode3;
		}
		if (type == _Key.SPCL) {
			code |= _Key.SPCL;
		}
		if (type == _Key.ALT) {
			code |= _Key.ALT;
		}

		do_keycode(coded, code);
	}

	public Wang1200_Keyboard(OutputStream fo,
				Wang1200_Indicator tml,
				Wang1200_Indicator er,
				Wang1200_Indicator tmr,
				Wang1200_Indicator na,
				Wang1200_Indicator el,
				Wang1200_Tape tapel,
				Wang1200_Tape taper,
				Wang1200_Model611 m611f) {
		int x;
		_tapel = tapel;
		_taper = taper;
		_m611 = m611f;
		_kbds = new Wang1200_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_code = false;
		_fout = fo;
		_mode3 = 1;	// initial value... we just know it...

		code_xlate = new byte[16];
		code_xlate[0] = 8;
		code_xlate[1] = 1;
		code_xlate[2] = 2;
		code_xlate[3] = 2;
		code_xlate[4] = 4;
		code_xlate[5] = 5;
		code_xlate[6] = 6;
		code_xlate[7] = 7;
		code_xlate[8] = 8;
		code_xlate[9] = 10;
		code_xlate[10] = 10;
		code_xlate[11] = 11;
		code_xlate[12] = 12;
		code_xlate[13] = 13;
		code_xlate[14] = 14;
		code_xlate[15] = 11;

		GridBagConstraints s = new GridBagConstraints();
//		JPanel pan;
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
				_code_kbd = _nkbds;
				_code_btn = x;
			}
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		s.gridx = _col;
		s.gridy = _row;
//		pan = new JPanel();
//		pan.setPreferredSize(new Dimension(400, 25));
//		pan.setOpaque(false);
//		gridbag.setConstraints(pan, s);
//		add(pan);
		gridbag.setConstraints(_m611, s);
		add(_m611);
		++_col;

		kbd = new Wang1200_Keyboard_right(na, el);
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		//setFocusTraversalKeysEnabled(false);
		setFocusCycleRoot(true);
		setRequestFocusEnabled(true);
		// setTransferHandler(TransferHandler newHandler) 

		tapel.ejectBtn().addActionListener(this);
		tapel.ejectBtn().setFocusable(false);
		taper.ejectBtn().addActionListener(this);
		taper.ejectBtn().setFocusable(false);
		_kbds[_nkbds] = tapel.getKbd();
		++_nkbds;
		_kbds[_nkbds] = taper.getKbd();
		++_nkbds;
	}

	public void keyTyped(KeyEvent e) {
//System.err.println("key pressed "+e.getKeyCode()+" "+e.getKeyChar());
if (e.isActionKey()) {
System.err.println("action");
}
		char c = e.getKeyChar();
		// every key gets printed...
		if (c == ']') c = '1';	// feable attempt to handle type elements confusion
		if (c == '}') c = '!';	// feable attempt to handle type elements confusion
		_m611.do_cn24_direct(c);
		int i = _m611.ascii2roti(c) & 0x0ff;
		boolean coded = ((e.getModifiers() & InputEvent.ALT_MASK) != 0);
		// for some reason, OSX java (at least) destroys the keycode
		// when ALT is down. Works on Linux...
		do_keycode(coded, i);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			setCode(true);
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			setCode(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
		boolean alt = ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
		boolean coded = ((e.getModifiers() & InputEvent.ALT_MASK) != 0);
		// must be a button, find out which
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(alt, coded, y, x);
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

class Wang1200_Keyboards extends JPanel
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang1200_Keyboards() { }

	int _nkeys;
	_Key[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
						int lx, int ly, int px, int py,
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
		gb.setConstraints(butt, c);

		ct.add(butt);
		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addPushButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
				int lx, int ly, int px, int py,
				String botlab, Color alt, boolean init, _Key key) {
		final Dimension dim = new Dimension(20, 30); // button
		final Dimension dim2 = new Dimension(30, 10); // label
		final Font font = new Font("Sans-serif", Font.PLAIN, 7);
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

		lx = py; // stupid warnings
		py = lx; // stupid warnings

		c.gridx = _col + px;
		c.gridy = _row;
		c.gridwidth = ly;
		c.gridheight = py - 1;

		gb.setConstraints(butt, c);
		ct.add(butt);
		c.gridy += c.gridheight;

		if (botlab.length() > 0) {
			JLabel lab;
			lab = new JLabel(botlab);
			lab.setFont(font);
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			lab.setPreferredSize(dim2);
			lab.setHorizontalAlignment(SwingConstants.CENTER);
			c.gridheight = 1;
			gb.setConstraints(lab, c);
			ct.add(lab);
		}

		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addTapeButton(GridBagConstraints c, int lx, int ly, int px, int py,
				Color alt, _Key key) {
		final Dimension dim = new Dimension(20, 20);
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

		c.gridx = _col + px;
		c.gridy = _row + 0;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}
}

class Wang1200_Keyboard_left extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
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

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		// do upper panel separately... stupid gridbag
		JPanel upper = new JPanel();
		GridBagLayout ugb = new GridBagLayout();
		upper.setLayout(ugb);
		upper.setOpaque(false);

		_row = 0;
		_col = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 40));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		++_col;

		addPushButton(c,ugb,upper,
			5, 3, 0, 4,"LEFT",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE1_CHG(1,0))));
		addPushButton(c,ugb,upper,
			5, 3, 3, 4,"RIGHT",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE1_CHG(1,1))));
		addPushButton(c,ugb,upper,
			5, 3, 6, 4,"TRANS.",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(2,_Key.MODE1_CHG(12,8))));
		addPushButton(c,ugb,upper,
			5, 3, 9, 4,"PLAY",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.MODE1_CHG(12,0))));
		addPushButton(c,ugb,upper,
			5, 3, 12, 4,"RECORD",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(2,_Key.MODE1_CHG(12,4))));
		_col += 15;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 40));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		++_col;

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(upper, c);
		add(upper);

		JPanel middle = new JPanel();
		GridBagLayout mgb = new GridBagLayout();
		middle.setLayout(mgb);
		middle.setOpaque(false);

		// don't bother with SINGLE/DOUBLE ?

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(tml, c);
		middle.add(tml);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(er, c);
		middle.add(er);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(tmr, c);
		middle.add(tmr);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(middle, c);
		add(middle);

		_col = 0;
		_row += 2;

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		_col += 16;

		JPanel lower = new JPanel();
		GridBagLayout lgb = new GridBagLayout();
		lower.setLayout(lgb);
		lower.setOpaque(false);

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5,"icons/rewind.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(1)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/forward.gif",
			new _Key(_Key.orange1,_Key.SPCL_KEY(2)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/reset.gif",
			new _Key(_Key.pink1, _Key.SPCL_KEY(0)));
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/back_line.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(7,3)));
		addButton(c,lgb,lower,
			1, 2, 0, 5, 5, 5, "icons/code.gif",
			new _Key(_Key.white1, _Key.illum1, _Key.SHIFT));
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(lower, c);
		add(lower);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
				Color.white, Color.gray));
		//setPreferredSize(new Dimension(160, 260));
		setBackground(_Key.slate);
		setOpaque(true);
	}
}

class Wang1200_Keyboard_right extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.52 2012/01/28 15:44:49 drmiller Exp $";
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

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		// do upper panel separately... stupid gridbag
		JPanel upper = new JPanel();
		GridBagLayout ugb = new GridBagLayout();
		upper.setLayout(ugb);
		upper.setOpaque(false);

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		ugb.setConstraints(na, c);
		upper.add(na);
		c.gridy = _row + 2;
		c.gridwidth = 4;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(40, 1));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		c.gridy = _row + 3;
		c.gridheight = 2;
		ugb.setConstraints(el, c);
		upper.add(el);
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		c.insets = new Insets(0,0,0,0);
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		addPushButton(c,ugb,upper,
			5, 3, 0, 5,"SAME",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.MODE3_CHG(3,1))));
		addPushButton(c,ugb,upper,
			5, 3, 3, 5,"ADJUST",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.MODE3_CHG(3,2))));
		addPushButton(c,ugb,upper,
			5, 3, 6, 5,"JUSTIFY",_Key.red2, false,
			new _Key(_Key.red1, _Key.GROUP(3,_Key.MODE3_CHG(3,3))));
		_col += 9;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(upper, c);
		add(upper);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		JPanel lower = new JPanel();
		GridBagLayout lgb = new GridBagLayout();
		lower.setLayout(lgb);
		lower.setOpaque(false);

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15,200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/para.gif",
			new _Key(_Key.white1, _Key.MODE2_CHG(7,4)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/line.gif",
			new _Key(_Key.white1, _Key.MODE2_CHG(7,5)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/word.gif",
			new _Key(_Key.white1, _Key.MODE2_CHG(7,6)));
		addButton(c,lgb,lower,
			1, 1, 0, 15, 5, 5, "icons/char-stop.gif",
			new _Key(_Key.pink1, _Key.pink2, _Key.MODE2_CHG(7,7)));
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20, 200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/auto_start.gif",
			new _Key(_Key.green1, _Key.MODE2_CHG(7,1)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/memo_out.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(6,3)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/search.gif",
			new _Key(_Key.blue1, _Key.blue2, _Key.ALT_KEY(2)));
		addButton(c,lgb,lower,
			1, 1, 0, 15, 5, 5, "icons/skip.gif",
			new _Key(_Key.orange1, _Key.orange2, _Key.ALT_KEY(1)));
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(lower, c);
		add(lower);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
				Color.white, Color.gray));
		//setPreferredSize(new Dimension(160, 260));
		setBackground(_Key.slate);
		setOpaque(true);
	}
}
