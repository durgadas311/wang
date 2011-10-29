// Copyright (c) 2011 Douglas Miller
// $Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

class _Key {
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color white2 = new Color(150, 150, 150, 255);
	static final Color illum1 = new Color(255, 255, 100, 255);
	static final Color red1 = new Color(255, 128, 128, 255);
	static final Color neon = new Color(244,157,33);
	static final Color neon2 = new Color(214,127,13);
	static final Color empty = new Color(50,50,50);
	static final Color ivory = new Color(236,226,190);
	static final Color beige = new Color(230,230,230);
	static final Color aqua = new Color(143,219,195);

	static final int SPCL = 0x0100;
	static final int MODE0 = 0x0200;
	static final int MODE1 = 0x0300;
	static final int META = 0x0400;		// never sent
	static final int METAP = 0x0500;	// never sent

	public _Key(Color sl, int c) {
		this.color = sl;
		this.altcolor = sl;
		this.code = c;
		this.state = false;
	}

	static final int TAPE_EJECT = -3;
	static final int TAPE_REW = -4;
	static final int TAPE_FF = -5;
	static final int TAPE_READY = -6;

	static final int PROG_CODE(int a, int b) {
		return ((a << 4) | b);
	}
	static final int SPCL_KEY(int b) {
		// shift is += 4...
		return (SPCL | b);
	}
	// 'a' is mask of bits that change
	static final int MODE0_CHG(int a, int b) {
		return (MODE0 | (a << 4) | b);
	}
	static final int MODE1_CHG(int a, int b) {
		return (MODE1 | (a << 4) | b);
	}
	static final int META_KEY(int b) {
		return (META | b);
	}
	// a = mask
	static final int META_PRE(int a, int b) {
		return (METAP | (a << 4) | b);
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
	public boolean isTAPE() {
		return (code <= TAPE_EJECT);
	}
	public boolean isMETA() {
		return (getType() == METAP);
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

public class w700_fe
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";

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
			dir = System.getProperty("user.home") + "/Wang700Files";
		}
		_dir = new File(dir);

		boolean test = (args.length > 0 && args[0].compareTo("-t") == 0);
		boolean back = (args.length > 0 && args[0].compareTo("-b") == 0);
		if (back) {
			fout = System.out;
			fin = System.in;
		} else if (!test) {
			try {
				Process _be = null;
				_be = Runtime.getRuntime().exec("./w700-sim -b");
				fout = _be.getOutputStream();
				fin = _be.getInputStream();
				ferr = new BufferedReader(new InputStreamReader(_be.getErrorStream()));
				Runtime.getRuntime().addShutdownHook(new FEexit(_be));
				new Wang700_SimError(ferr);
			} catch (IOException ee) {
				System.err.println("Unable to exec back-end!");
				System.exit(1);
			}
		}
		_dir.mkdir();
		JFrame front_end = new JFrame("Wang 700 Advanced Programmable Calculator");
//		java.net.URL url = w700_fe.class.getResource("icons/wang700-48x48.png");
//		Image img = Toolkit.getDefaultToolkit().getImage(url);
//		front_end.setIconImage(img);

		front_end.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.insets.top = 0;
		s.insets.bottom = 0;
		s.anchor = GridBagConstraints.NORTH;
		JPanel pan;
		JLabel lab;

		lab = new JLabel("Y");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 24));
		lab.setForeground(Color.white);
		lab.setPreferredSize(new Dimension(20, 75));
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);

		Wang700_Display dspy = new Wang700_Display(false);
		s.gridx = 1;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(dspy, s);
		front_end.add(dspy);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 1;
		s.gridheight = 1;
		s.gridwidth = 2;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		lab = new JLabel("X");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 24));
		lab.setForeground(Color.white);
		lab.setPreferredSize(new Dimension(20, 75));
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 2;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);

		Wang700_Display dspx = new Wang700_Display(true);
		s.gridx = 1;
		s.gridy = 2;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(dspx, s);
		front_end.add(dspx);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = 2;
		s.gridy = 0;
		s.gridheight = 3;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		Wang700_Tape tape = new Wang700_Tape(fout);
		s.gridx = 3;
		s.gridy = 0;
		s.gridheight = 4;
		s.gridwidth = 1;
		gridbag.setConstraints(tape, s);
		front_end.add(tape);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 3;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		Wang700_Keyboard kbd = new Wang700_Keyboard(fout, dspx.pe, dspx.me, tape);
		s.gridx = 0;
		s.gridy = 4;
		s.gridheight = 1;
		s.gridwidth = 4;
		gridbag.setConstraints(kbd, s);
		front_end.add(kbd);
		front_end.addKeyListener(kbd);

		Wang700_Model711 m711f = new Wang700_Model711(kbd);
		Wang700_Model730 m730f = new Wang700_Model730(kbd);

		Wang700_SimInput inp = new Wang700_SimInput(fin, dspx, dspy, tape, m711f, m730f);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("701/702/711 OutputWriter", KeyEvent.VK_O);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = new JMenuItem("730 Disk - not mounted", KeyEvent.VK_D);
		mi.addActionListener(inp);
		mu.add(mi);

		front_end.setJMenuBar(mb);
		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,725);
		front_end.setVisible(true);
	}
}

class Wang700_ProgErr extends JComponent {
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

	GridBagLayout gridbag = new GridBagLayout();
	JPanel pan;

	public Wang700_ProgErr(String label) {
		GridBagConstraints s = new GridBagConstraints();

		setLayout(gridbag);

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.anchor = GridBagConstraints.CENTER;

		JLabel lab = new JLabel("<HTML><CENTER>"+label+"</CENTER></HTML>");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
		lab.setPreferredSize(new Dimension(30, 25));
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 0;
		gridbag.setConstraints(lab, s);
		add(lab);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(true);
		pan.setBackground(_Key.empty);
		s.gridy = 1;
		gridbag.setConstraints(pan, s);
		add(pan);
	}

	public void setOn(boolean on) {
		if (on) {
			pan.setBackground(_Key.neon);
		} else {
			pan.setBackground(_Key.empty);
		}
	}
}

class Wang700_SimError
		implements Runnable
{
	BufferedReader _fin;

	public Wang700_SimError(BufferedReader f) {
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

class Wang700_SimInput
		implements Runnable, WindowListener, ActionListener
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	Wang700_Display _dspx;
	Wang700_Display _dspy;
	Wang700_Tape _tape;
	Wang700_Model711 _m711;
	Wang700_Model730 _m730;

	InputStream _fin;

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Devices event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_O) {
			_m711.onOff(!_m711.onOff());
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_D) {
			_m730.pickFile(m);
			return;
		}
	}

	public Wang700_SimInput(InputStream f, Wang700_Display dspx, Wang700_Display dspy,
			Wang700_Tape tape,
			Wang700_Model711 cn24,
			Wang700_Model730 m730) {
		_dspx = dspx;
		_dspy = dspy;
		_tape = tape;
		_m711 = cn24;
		_m711.getFrame().addWindowListener(this);
		_m730 = m730;
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
			if ((b[1] & ~0x80) != 0) {
				if ((b[1] & 0x20) != 0) {
					_dspy.do_display(b);
				} else {
					_dspx.do_display(b);
				}
			} else if ((b[1] & ~1) == 0x08) {
				System.err.println("invalid printer outout");
			} else if ((b[1]  & ~3) == 0x0c) {
				_tape.do_tape(b);
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_m711.reset();
				_m730.reset();
				//etc...
			} else if (b[1] == 0x30) {
				_m711.do_cn24(b);
//			} else if ((b[1] & ~0x1f) == 0x20) {
//				_m730.do_dev(b);
//			} else if ((b[1] & 0x80) != 0) {
//				System.err.println("invalid ROM outout");
			} else {
				System.err.println("Unexpected traffic ("+n+")"+b[1]+" "+b[0]);
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
		if (e.getWindow() == _m711.getFrame()) {
			_m711.onOff(false);
			return;
		}
	}
}

class Wang700_Tape extends JComponent
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
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

	public Wang700_Tape(OutputStream fout) {
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

		JLabel cass = new JLabel("<HTML><BR>" +
			"<FONT SIZE=+2><B>WANG</B></FONT> 700 Series<BR>" +
			"<FONT SIZE=-1>Advanced Programmable Calculator</FONT></HTML>",
			SwingConstants.CENTER);
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

class Wang700_Model730 {
	private int _cmd;
	private int _adr;
	private boolean _wr;
	private int _len;
	private int _idx;
	private Wang700_Keyboard _kbd;
	java.io.RandomAccessFile _f;
	File _file;
	byte[] _buf;

	public Wang700_Model730(Wang700_Keyboard kbd) {
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
			m.setText("730 Disk - " + _file.getName());
		} else {
			_file = null;
			m.setText("730 Disk - not mounted");
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
		super(w700_fe._dir);
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

class Wang700_Model711
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
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

	Wang700_Keyboard _kbd;

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

	public Wang700_Model711(Wang700_Keyboard kbd) {
		setup_xlate();

		_kbd = kbd;
		_onoff = false;

		_frame = new JFrame("Wang 711 Output Writer");
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

	private void save711(File file) {
		if (_hasGraphic) {
			java.awt.image.BufferedImage i =
				new java.awt.image.BufferedImage(_text.getWidth(),
								_text.getHeight(),
					java.awt.image.BufferedImage.TYPE_BYTE_BINARY);
			_text.paint(i.getGraphics());
			try {
				javax.imageio.ImageIO.write(i, "png", file);
			} catch (IOException ee) {
				System.err.println("error writing 711 PNG");
			}
		} else {
			FileOutputStream fo;
			try {
				fo = new FileOutputStream(file);
			} catch (FileNotFoundException ee) {
				System.err.println("chosen 711 file not found?");
				return;
			}
			try {
				fo.write(_text.getText().getBytes());
				fo.write('\n');
				fo.close();
			} catch (IOException ee) {
				System.err.println("error writing 711 TXT");
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown 711 event source type");
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
				save711(ch.getSelectedFile());
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
				_footer = new String("Wang 701/702/711 OutputWriter - " +
					w700_fe._timestamp.format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println("711 menu " + e.getActionCommand() +
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
		_kbd.do_keycode(0);
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

class Wang700_Display extends JComponent
		implements ActionListener
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','>','u','<','t',' '};

	byte[] disp_a;
	JLabel disp;
	int _dp;

	Wang700_ProgErr pe;
	Wang700_ProgErr me;
	boolean flashing;
	boolean state;
	boolean ismain;
	javax.swing.Timer timer;

	private void flasher() {
		if (!flashing) {
			state = false;
			disp.setForeground(_Key.neon);
			return;
		}
		state = !state;
		if (state) {
			disp.setForeground(_Key.neon2);
		} else {
			disp.setForeground(_Key.neon);
		}
	}

	public void actionPerformed(ActionEvent e) {
		// verify the action is for the timer?
		if (e.getSource() == timer) {
			flasher();
//		} else if (e.getSource() == timer2) {
//			blanker();
		} else {
			// what was it? e.getSource().stop()???
		}
	}

	public Wang700_Display(boolean m) {
		String blank = "+0.00000000000 +00";
		disp_a = new byte[18];
		disp_a = blank.getBytes();
		flashing = false;
		state = false;
		ismain = m;
		timer = new Timer(100, this);

		setLayout(new FlowLayout());
		disp = new JLabel(blank, SwingConstants.CENTER);
		disp.setForeground(_Key.neon);
		disp.setBackground(_Key.empty);
		disp.setOpaque(true);
		Font font = null;
		java.io.InputStream ttf = null;
		ttf = Wang700_Display.class.getResourceAsStream(
							"fonts/Wang700Display.ttf");
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
			} catch (IOException ee) {
			}
			font = font.deriveFont(28f);
		}
		if (font == null) {
			System.err.println("Missing font \"Wang700Display.ttf\", using default");
			font = new Font("Sans-serif", Font.PLAIN, 40);
		}
		disp.setPreferredSize(new Dimension(475, 75));
		disp.setFont(font);

		add(disp);

		if (ismain) {
			pe = new Wang700_ProgErr("Prog<BR>Error");
			pe.setOn(false);
			me = new Wang700_ProgErr("Mach<BR>Error");
			me.setOn(false);
		}

	}

	private void setFlashing(boolean on) {
		if (on) {
			if (flashing) return;
			flashing = true;
			timer.start();
		} else {
			if (!flashing) return;
			flashing = false;
			timer.stop();
			flasher();
		}
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	public void do_display(byte[] b) {
		int ds;
		int dc;
		byte c;

//System.err.format("disp %02x %02x\n", b[1], b[0]);
		if ((b[1] & 0x2) != 0) {
			if (ismain) me.setOn(true);
			setFlashing(true);
		} else {
			if (ismain) me.setOn(false);
		}
		if ((b[1] & 0x1) != 0) {
			if (ismain) pe.setOn(true);
			setFlashing(true);
		} else {
			if (ismain) pe.setOn(false);
		}
		if ((b[1] & 0x3) == 0) {
			setFlashing(false);
		}
		String s;
		if ((b[1] & 0x4) != 0) {
			// blank-out display while Wang is not refreshing...
			s = new String("                  ");
//System.err.println("blank "+((b[1] >> 4) & 0x0f)+" "+((b[0] >> 4) & 0x0f)+" "+(b[0] & 0x0f));
		} else {
			ds = (b[0] >> 4) & 0x0f;
			dc = b[0] & 0x0f;
			if (ds == 15) {
				disp_a[16] = ' ';
				disp_a[17] = ' ';
				if ((b[1] & 0x10) == 0) {
					_dp = 0;
					disp_a[_dp + 1] = '.';
				} else {
					dc = 15;
					if (dc >= 13) {
						_dp = 18;
					} else {
						_dp = dc;
						disp_a[_dp + 1] = '.';
					}
				}
				c = disp_chr[dc];
			} else if (ds == 0 || ds == 13) {
				c = sign_chr[dc];
			} else {
				c = disp_chr[dc];
			}
			if (ds > _dp) ++ds;
			disp_a[ds] = c;
			s = new String(disp_a);
//System.err.println("disp \""+s+"\" "+((b[1] >> 4) & 0x0f)+" "+((b[0] >> 4) & 0x0f)+" "+(b[0] & 0x0f));
		}
		disp.setText(s);
		repaint();
	}
}

class Wang700_Keyboard extends JComponent
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang700_Keyboards[] _kbds;
	int _row;
	int _col;
	int _meta;
	int _mode0;
	int _mode1;
	OutputStream _fout;
	Wang700_Tape _tape;

	private void setToggle(boolean on, _Key key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == _Key.METAP) {
			_meta &= ~key.getMask();
		} else if (key.getType() == _Key.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE1) {
			_mode1 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == _Key.METAP) {
				_meta |= key.getMode();
			} else if (key.getType() == _Key.MODE0) {
				_mode0 |= key.getMode();
			} else if (key.getType() == _Key.MODE1) {
				_mode1 |= key.getMode();
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
			if (_kbds[y]._keys[z] == null) continue;
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

	private void do_button(int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x].isTAPE()) {
			set_group(g, y, x);
			boolean st = _tape.do_button(_kbds[y]._keys[x]);
			if (st) {
				setToggle(!_kbds[y]._keys[x].state,
					_kbds[y]._keys[x], _kbds[y]._buttons[x]);
			}
			return;
		}
		if (g != 0) {
			set_group(g, y, x);
		}
		if (_kbds[y]._keys[x].isMETA()) {
			return;
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
		if (type == _Key.SPCL) {
			code |= _Key.SPCL;
			if (false) { // how to shift special keys...
				code += 4;
			}
		}
		if (type == _Key.MODE1) {
			// boolean on = ((_mode1 & 2) != 0);
			// _prt.onOff(on);
			code = _Key.MODE1 | _mode1;
		}
		if (type == _Key.META) {
			code &= 0x00f;
			code |= (_meta << 4);
		}
		do_keycode(code);
	}

	JFrame _frame;
	JEditorPane _text;
	JScrollPane _scroll;
	int _xoff, _yoff;

	public Wang700_Keyboard(OutputStream fo, Wang700_ProgErr pe, Wang700_ProgErr me,
				Wang700_Tape tape) {
		int x;
		_tape = tape;
		_kbds = new Wang700_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_meta = 0;
		_fout = fo;

		java.net.URL url = Wang700_Keyboard.class.getResource("docs/wang700.html");

		_frame = new JFrame("Wang 700 Help");
		// TBD icon or not
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (IOException ee) {
		}
		//_text.setLineWrap(true);
		_text.setEditable(false);
		_text.setFont(new Font("Sans-serif", Font.PLAIN, 12));
		int z = _text.getFont().getSize();
		_text.setPreferredSize(new Dimension(60 * z, 32 * z));

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//_scroll.getViewport().setBackground(_Key.empty);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Topic");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Basic Operation", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Using the Calculator", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Sample Programs", KeyEvent.VK_A);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Using the Tape Drive", KeyEvent.VK_D);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("How to Program", KeyEvent.VK_P);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Programming Techniques", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Programming Functions", KeyEvent.VK_F);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Program Codes", KeyEvent.VK_C);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Functions by Code", KeyEvent.VK_K);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("About the Simulator", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Known Bugs or Problems", KeyEvent.VK_G);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(mb);
		_frame.add(_scroll);
		_frame.pack();
		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
		
		//_frame.setVisible(true);
		_frame.addWindowListener(this);
		_frame.addComponentListener(this);

		Dimension dim = new Dimension(500, 25);
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;
		Wang700_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

		kbd = new Wang700_Keyboard_stick();
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
		++_row;

		s.gridx = _col;
		s.gridy = _row;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		_col = 0;
		_row += 1;

		kbd = new Wang700_Keyboard_meta(pe, me);
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
		++_row;

		s.gridx = _col;
		s.gridy = _row;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		_col = 0;
		_row += 1;

		kbd = new Wang700_Keyboard_main();
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

		//setFocusTraversalKeysEnabled(false);
		setFocusCycleRoot(true);
		setRequestFocusEnabled(true);
		// setTransferHandler(TransferHandler newHandler) 
	}

	public void keyTyped(KeyEvent e) {
//System.err.println("key pressed "+e.getKeyCode()+" "+e.getKeyChar());
		char c = e.getKeyChar();
		if (c >= '0' && c <= '9') {
			do_keycode(c - '0');
		}
		if (c == 'e' || c == 'E') {
			do_keycode(11);
		}
		if (c == '.') {
			do_keycode(10);
		}
		if (c == '+' || c == '-') {
			do_keycode(12);
		}
		if (c == '\b') {
			do_keycode(15);
		}
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			java.net.URL url = null;
			// should use a table to lookup url?
			if (m.getMnemonic() == KeyEvent.VK_B) {
				url = Wang700_Keyboard.class.getResource("docs/wang700.html");
			} else if (m.getMnemonic() == KeyEvent.VK_U) {
				url = Wang700_Keyboard.class.getResource("docs/wang700calc.html");
			} else if (m.getMnemonic() == KeyEvent.VK_D) {
				url = Wang700_Keyboard.class.getResource("docs/wang700tape.html");
			} else if (m.getMnemonic() == KeyEvent.VK_A) {
				url = Wang700_Keyboard.class.getResource("docs/wang700samp.html");
			} else if (m.getMnemonic() == KeyEvent.VK_P) {
				url = Wang700_Keyboard.class.getResource("docs/wang700prog.html");
			} else if (m.getMnemonic() == KeyEvent.VK_F) {
				url = Wang700_Keyboard.class.getResource("docs/wang700func.html");
			} else if (m.getMnemonic() == KeyEvent.VK_T) {
				url = Wang700_Keyboard.class.getResource("docs/wang700tech.html");
			} else if (m.getMnemonic() == KeyEvent.VK_C) {
				url = Wang700_Keyboard.class.getResource("docs/wang700codes.html");
			} else if (m.getMnemonic() == KeyEvent.VK_K) {
				url = Wang700_Keyboard.class.getResource("docs/wang700bycode.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = Wang700_Keyboard.class.getResource("docs/wang700sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_G) {
				url = Wang700_Keyboard.class.getResource("docs/wang700bugs.html");
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
		// must be a button, find out which
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(y, x);
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

	public void windowClosing(WindowEvent e) { }

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
}

class Wang700_Keyboards extends JComponent
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang700_Keyboards() { }

	int _nkeys;
	_Key[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py,
						String icon, _Key key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		java.net.URL url = Wang700_Keyboards.class.getResource(icon);
		ImageIcon ic = new ImageIcon(url);
		butt = new JButton(ic);
		butt.setBackground(key.color);
		butt.setBorder(lb);
		butt.setOpaque(true);
		// butt.setHorizontalAlignment(SwingConstants.CENTER); // didn't help...

		if (ly < 0) {
			ly = -ly;
			dim.height = 50;
		} else {
			dim.height = 50 * ly;
		}
		dim.width = 50 * lx;
		butt.setPreferredSize(dim);
		butt.setMargin(inset);

		c.gridwidth = lx;
		c.gridheight = ly;
		c.gridx = _col + px;
		c.gridy = _row + py;
		gridbag.setConstraints(butt, c);

		add(butt);
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addPushButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String toplab, String botlab, Color alt, boolean init, _Key key) {
		final Dimension dim = new Dimension(15, 30);
		//final Insets inset = new Insets(2,2,2,2);
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
		c.insets.left = ly; // stupid warnings
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = 1;

		JLabel lab ;
		if (toplab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+toplab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 0;
			c.anchor = GridBagConstraints.SOUTH;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		if (botlab.length() > 0) {
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

class Wang700_Keyboard_main extends Wang700_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 67;

	public Wang700_Keyboard_main() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		Dimension dim = new Dimension(50, 200);
		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 4;

		setLayout(gridbag);

//		s.gridx = _col;
//		pan = new JPanel();
//		pan.setPreferredSize(new Dimension(50, 200));
//		pan.setOpaque(false);
//		gridbag.setConstraints(pan, s);
//		add(pan);
//		++_col;

		addButton(c,1, 1, 0, 0, "icons/wr_a.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,12)));
		addButton(c,1, 1, 0, 1, "icons/wr.gif",
			new _Key(_Key.blue1,_Key.PROG_CODE(4,11)));
		addButton(c,1, 1, 0, 2, "icons/int_x.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,8)));
		addButton(c,1, 1, 0, 3, "icons/e10x.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,13)));
		addButton(c,1, 1, 0, 4, "icons/ex.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/end_a.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,13)));
		addButton(c,1, 1, 0, 1, "icons/inv.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,15)));
		addButton(c,1, 1, 0, 2, "icons/abs_x.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,7)));
		addButton(c,1, 1, 0, 3, "icons/log10x.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,10)));
		addButton(c,1, 1, 0, 4, "icons/logex.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,11)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/re_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,5)));
		addButton(c,1, 1, 0, 1, "icons/st_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,4)));
		addButton(c,1, 1, 0, 2, "icons/re_res.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,14)));
		addButton(c,1, 1, 0, 3, "icons/pi.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(6,9)));
		addButton(c,1, 1, 0, 4, "icons/xchg_xy.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(6,6)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/xchg_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,6)));
		addButton(c,1, 1, 0, 1, "icons/div_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,3)));
		addButton(c,1, 1, 0, 2, "icons/mult_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,2)));
		addButton(c,1, 1, 0, 3, "icons/minus_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,1)));
		addButton(c,1, 1, 0, 4, "icons/plus_ind.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/xchg_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,6)));
		addButton(c,1, 1, 0, 1, "icons/div_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,3)));
		addButton(c,1, 1, 0, 2, "icons/mult_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,2)));
		addButton(c,1, 1, 0, 3, "icons/minus_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,1)));
		addButton(c,1, 1, 0, 4, "icons/plus_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/re_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,5)));
		addButton(c,1, 1, 0, 1, "icons/st_dir.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(4,4)));
		addButton(c,1, 1, 0, 2, "icons/y_to_x.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(6,5)));
		addButton(c,1, 2, 0, 3, "icons/x_to_y.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(6,4)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/chg_sign.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,11)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(6,3)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(6,2)));
		addButton(c,1, 1, 0, 3, "icons/minus.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(6,1)));
		addButton(c,1, 1, 0, 4, "icons/plus.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(6,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/sqrt.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(6,12)));
		addButton(c,1, 1, 0, 1, "icons/seven.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,7)));
		addButton(c,1, 1, 0, 2, "icons/four.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,4)));
		addButton(c,1, 1, 0, 3, "icons/one.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,1)));
		addButton(c,1, 1, 0, 4, "icons/zero.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/x2.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(7,13)));
		addButton(c,1, 1, 0, 1, "icons/eight.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,8)));
		addButton(c,1, 1, 0, 2, "icons/five.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,5)));
		addButton(c,1, 1, 0, 3, "icons/two.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,2)));
		addButton(c,1, 1, 0, 4, "icons/dp.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,12)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/clear_x.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,15)));
		addButton(c,1, 1, 0, 1, "icons/nine.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,9)));
		addButton(c,1, 1, 0, 2, "icons/six.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,6)));
		addButton(c,1, 1, 0, 3, "icons/three.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,3)));
		addButton(c,1, 1, 0, 4, "icons/set_exp.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(7,10)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/ld_prog.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,13)));
		addButton(c,1, 1, 0, 1, "icons/end_prog.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,12)));
		addButton(c,1, 1, 0, 2, "icons/stop.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,15)));
		addButton(c,1, 2, 0, 3, "icons/go.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(5,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/skip_err.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,10)));
		addButton(c,1, 1, 0, 1, "icons/skip_ge.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,7)));
		addButton(c,1, 1, 0, 2, "icons/skip_eq.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,9)));
		addButton(c,1, 1, 0, 3, "icons/skip_lt.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,8)));
		addButton(c,2, 1, 0, 4, "icons/search.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(4,7)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/mark.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(4,8)));
		addButton(c,1, 1, 0, 1, "icons/return.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(5,11)));
		addButton(c,1, 1, 0, 2, "icons/group1.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(4,9)));
		addButton(c,1, 1, 0, 3, "icons/group2.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(4,10)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/prime.gif",
			new _Key(_Key.white1, _Key.SPCL_KEY(0)));
		addButton(c,1, 1, 0, 1, "icons/set_pc.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(2)));
		addButton(c,1, 1, 0, 2, "icons/verif_prog.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(1)));
		addButton(c,1, 1, 0, 3, "icons/rec_prog.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(3)));
		addButton(c,1, 1, 0, 4, "icons/step.gif",
			new _Key(_Key.green1, _Key.MODE0_CHG(8,8)));
		++_col;

		_col = 0;
		_row += 5;
	}
}

class Wang700_Keyboard_meta extends Wang700_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 20;

	public Wang700_Keyboard_meta(Wang700_ProgErr pe, Wang700_ProgErr me) {
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
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 2, 1, 0, 1,"80","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,8))));
		addPushButton(c, 2, 1, 1, 1,"40","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.META_PRE(7,4))));
		addPushButton(c, 2, 1, 2, 1,"20","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(4,_Key.META_PRE(7,2))));
		addPushButton(c, 2, 1, 3, 1,"10","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(5,_Key.META_PRE(7,1))));
		_col += 4;

		c.anchor = GridBagConstraints.SOUTH;
		addButton(c,1, -2, 0, 0, "icons/k00.gif",
			new _Key(_Key.white1, _Key.META_KEY(0)));
		addButton(c,1, -2, 1, 0, "icons/k01.gif",
			new _Key(_Key.white1, _Key.META_KEY(1)));
		addButton(c,1, -2, 2, 0, "icons/k02.gif",
			new _Key(_Key.white1, _Key.META_KEY(2)));
		addButton(c,1, -2, 3, 0, "icons/k03.gif",
			new _Key(_Key.white1, _Key.META_KEY(3)));
		addButton(c,1, -2, 4, 0, "icons/k04.gif",
			new _Key(_Key.white1, _Key.META_KEY(4)));
		addButton(c,1, -2, 5, 0, "icons/k05.gif",
			new _Key(_Key.white1, _Key.META_KEY(5)));
		addButton(c,1, -2, 6, 0, "icons/k06.gif",
			new _Key(_Key.white1, _Key.META_KEY(6)));
		addButton(c,1, -2, 7, 0, "icons/k07.gif",
			new _Key(_Key.white1, _Key.META_KEY(7)));
		addButton(c,1, -2, 8, 0, "icons/k08.gif",
			new _Key(_Key.white1, _Key.META_KEY(8)));
		addButton(c,1, -2, 9, 0, "icons/k09.gif",
			new _Key(_Key.white1, _Key.META_KEY(9)));
		addButton(c,1, -2, 10, 0, "icons/k10.gif",
			new _Key(_Key.white1, _Key.META_KEY(10)));
		addButton(c,1, -2, 11, 0, "icons/k11.gif",
			new _Key(_Key.white1, _Key.META_KEY(11)));
		addButton(c,1, -2, 12, 0, "icons/k12.gif",
			new _Key(_Key.white1, _Key.META_KEY(12)));
		addButton(c,1, -2, 13, 0, "icons/k13.gif",
			new _Key(_Key.white1, _Key.META_KEY(13)));
		addButton(c,1, -2, 14, 0, "icons/k14.gif",
			new _Key(_Key.white1, _Key.META_KEY(14)));
		addButton(c,1, -2, 15, 0, "icons/k15.gif",
			new _Key(_Key.white1, _Key.META_KEY(15)));
		_col += 16;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		c.gridx = _col;
		gridbag.setConstraints(pe, c);
		add(pe);
		++_col;
		c.gridx = _col;
		gridbag.setConstraints(me, c);
		add(me);

		++_col;
		_col = 0;
		_row += 2;

	}
}

class Wang700_Keyboard_stick extends Wang700_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.7 2011/10/29 03:16:09 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 22;

	public Wang700_Keyboard_stick() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
//		Dimension dim = new Dimension(20, 30);
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

		java.net.URL url;
		url = w700_fe.class.getResource("icons/logo-2.gif");
		ImageIcon ic = new ImageIcon(url);
		JLabel lab = new JLabel(ic);
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(lab, c);
		c.anchor = GridBagConstraints.NORTH;
		add(lab);
		_col += 1;

		c.gridx = _col;
		c.gridy = 0;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(25,25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 15, 1, 0, 0,"Run","",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,0))));
		addPushButton(c, 15, 1, 1, 0,"Learn","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,4))));
		addPushButton(c, 15, 1, 2, 0,"Learn<BR>and<BR>Print","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,6))));
		addPushButton(c, 15, 1, 3, 0,"List<BR>Program","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,2))));
		_col += 4;

		c.gridx = _col;
		c.gridy = 0;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(250, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addTapeButton(c, 5, 1, 0, 0, "Release", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(6,_Key.TAPE_EJECT)));

		addTapeButton(c, 5, 1, 1, 0, "Forward", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(6,_Key.TAPE_FF)));

		addTapeButton(c, 5, 1, 2, 0, "Tape Ready", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(6,_Key.TAPE_READY)));

		addTapeButton(c, 5, 1, 3, 0, "Rewind", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(6,_Key.TAPE_REW)));
		_col += 4;

		c.gridx = _col;
		c.gridy = 0;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(35,25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		_col = 0;
		_row += 1;

	}
}
