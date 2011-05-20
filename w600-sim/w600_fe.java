// Copyright (c) 2011 Douglas Miller
// $Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
//import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

class _Key {
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";

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

	static final int SHIFT = -1;
	static final int FEED = -2;
	static final int TAPE_EJECT = -3;
	static final int TAPE_REW = -4;
	static final int TAPE_FF = -5;
	static final int TAPE_READY = -6;

	static final int PROG_CODE(int a, int b) {
		// shift is += 01 00...
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
	public boolean isSHIFT() {
		return (code == SHIFT);
	}
	public boolean isFEED() {
		return (code == FEED);
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

public class w600_fe {
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";

	public static File _dir = new File(System.getProperty("user.home") + "/Wang600Files");

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
		java.io.BufferedReader ferr = null;
		GridBagLayout gridbag = new GridBagLayout();

		if (args.length == 0 || args[0].compareTo("-t") != 0) {
			try {
				Process _be = null;
				_be = Runtime.getRuntime().exec("./w600-sim -b");
				fout = _be.getOutputStream();
				fin = _be.getInputStream();
				ferr = new BufferedReader(new InputStreamReader(_be.getErrorStream()));
				Runtime.getRuntime().addShutdownHook(new FEexit(_be));
				new Wang600_SimError(ferr);
			} catch (IOException ee) {
				System.err.println("Unable to exec back-end!");
				System.exit(1);
			}
		}
		_dir.mkdir();
		JFrame front_end = new JFrame("Wang 600 Advanced Programmable Calculator");
		java.net.URL url = w600_fe.class.getResource("icons/wang600-48x48.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		front_end.setIconImage(img);

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

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(80, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridheight = 2;
		gridbag.setConstraints(pan, s);
		s.gridheight = 1;
		front_end.add(pan);

		Wang600_Display dsp = new Wang600_Display(fin);
		s.gridx = 1;
		s.gridy = 0;
		gridbag.setConstraints(dsp, s);
		front_end.add(dsp);

		url = w600_fe.class.getResource("icons/logo-sm.gif");
		ImageIcon ic = new ImageIcon(url);
		JLabel lab = new JLabel(ic);
		s.gridx = 1;
		s.gridy = 1;
		s.insets.left = 10;
		s.insets.bottom = 25;
		s.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(lab, s);
		s.anchor = GridBagConstraints.NORTH;
		s.insets.left = 0;
		s.insets.bottom = 0;
		s.gridy = 0;
		front_end.add(lab);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(110, 25));
		pan.setOpaque(false);
		s.gridx = 2;
		s.gridheight = 2;
		gridbag.setConstraints(pan, s);
		s.gridheight = 1;
		front_end.add(pan);

		Wang600_Tape tape = new Wang600_Tape(fout);
		s.gridx = 3;
		s.gridheight = 2;
		gridbag.setConstraints(tape, s);
		s.gridheight = 1;
		front_end.add(tape);

		Wang600_Printer prt = new Wang600_Printer();

		Wang600_Keyboard kbd = new Wang600_Keyboard(fout, dsp.pe, dsp.me, prt, tape);
		s.gridx = 0;
		s.gridy = 2;
		s.gridwidth = 4;
		gridbag.setConstraints(kbd, s);
		s.gridwidth = 1;
		front_end.add(kbd);
		front_end.addKeyListener(kbd);

		Wang600_SimInput inp = new Wang600_SimInput(fin, dsp, prt, tape);

		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,640);
		front_end.setVisible(true);
	}
}

class Wang600_ProgErr extends JComponent {
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

	GridBagLayout gridbag = new GridBagLayout();
	JPanel pan;

	public Wang600_ProgErr(String label) {
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

class Wang600_SimError
		implements Runnable
{
	BufferedReader _fin;

	public Wang600_SimError(BufferedReader f) {
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

class Wang600_SimInput
		implements Runnable
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	Wang600_Display _dsp;
	Wang600_Printer _prt;
	Wang600_Tape _tape;
	Wang600_CN24 _cn24;

	InputStream _fin;

	public Wang600_SimInput(InputStream f, Wang600_Display dsp,
			Wang600_Printer prt, Wang600_Tape tape) {
		_dsp = dsp;
		_prt = prt;
		_tape = tape;
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
			if ((b[1]  & ~7) == 0x00) {
				_dsp.do_display(b);
			} else if ((b[1]  & ~1) == 0x08) {
				_prt.do_printer(b);
			} else if ((b[1]  & ~3) == 0x0c) {
				_tape.do_tape(b);
			} else if (b[1] == 0x10) {
				_cn24.do_cn24(b);
			} else {
				System.err.println("Unexpected traffic ("+n+")"+b[1]+" "+b[0]);
			}
		}
	}
}

class Wang600_Printer
	implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	final int PR_NUM_COL = 20;
	final int PR_XCOL_WID = 3;
	final int PR_XCOL_STRT = 15;
	final int PR_BUF_LEN = PR_XCOL_STRT + PR_XCOL_WID * (PR_NUM_COL - PR_XCOL_STRT);

	// these will be replaced with some translation to a special font...
	final String[] pr_16 = {
		" E", " T", " +", " -", " \u00D7", " \u00F7", "ST", "RE",
		" *", " *", " f", " F", " A", " B", " C", " D", "  "
	};
	final String[] pr_17 = {
		"0 ", "1 ", "2 ", "3 ", "4 ", "5 ", "6 ", "7 ",
		"8 ", "9 ", "10", "11", "12", "13", "14", "15", "  "
	};
	final String[] pr_18 = {
		"S   ", "RE  ", "W   ", "Go  ", "Jo  ", "J+  ", "SN  ", "CS  ",
		"TN  ", "RD  ", "LN  ", "e\u207F  ", "x\u00B2  ", "\u221AX  ", "LP  ", "1/x ",
		"  ", ""
	};
	final String[] pr_19 = {
		"  M ", "  ST", "  \u03B1 ", "  Sp", "  J\u00F8", "  Je", "  S\u00B9", "  C\u00B9",
		"  T\u00B9", "  DR", "  LG", " 10\u207F", "  I ", " |x|", "  EP", "  RT",
		"  ", ""
	};
	final String[] pr_20 = {
		"X", "Y", "Z", "A", "B", "C", "D", "E",
		"F", "G", "H", "I", "J", "K", "L", "M", " "
	};
	final String[][] pr_16_20 = {
		pr_16, pr_17, pr_18, pr_19, pr_20
	};
	final String[] pr_0_15 = {
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
		".", "\u25EF", null, "+", "-", " "
	};
	final String[] pr_ovr = {
		".",".",".",".","O","V","E","R","F","L","O","W",".",".",".","."
		};

	byte[] _pr_buf;
	int _eop;

	JFrame _frame;
	JTextArea _text;
	JScrollPane _scroll;

	private void clear_buf() {
		int x;
		for (x = 0; x < PR_NUM_COL; ++x) {
			_pr_buf[x] = 0x10;
		}
	}

	public Wang600_Printer() {
		_pr_buf = new byte[PR_NUM_COL];
		clear_buf();
		_eop = 0;
		_frame = new JFrame("Wang 600 Printer");
		// TBD icon or not
		_frame.setLayout(new FlowLayout());
		_text = new JTextArea(32, 26); // can user resize?
		_text.setLineWrap(false);
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);
		_text.setFont(new Font("Monospaced", Font.PLAIN, 12));
		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//_scroll.getViewport().setBackground(_Key.empty);
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
		//_frame.setSize(260,500);
		_frame.pack();	// set size according to content...
		//_frame.setVisible(true);	// make visible based on "printer on"
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "Tear Off") {
			_text.setText("");
			_eop = 0;
			_text.setCaretPosition(_eop);
			return;
		}
		if (e.getActionCommand() == "Save") {
			FileOutputStream fo;
			JFileChooser ch = new JFileChooser(w600_fe._dir);
			int rv = ch.showOpenDialog(_frame);
			if (rv == JFileChooser.APPROVE_OPTION) {
				try {
					fo = new FileOutputStream(ch.getSelectedFile());
				} catch (FileNotFoundException ee) {
					return;
				}
				try {
					fo.write(_text.getText().getBytes());
					fo.write('\n');
					fo.close();
				} catch (IOException ee) {
				}
			}
			return;
		}
		if (e.getActionCommand() == "Print") {
//			PrinterJob job = PrinterJob.getPrinterJob();
//			job.setPrintable(_text.getPrintable());
//			boolean doit = job.printDialog();
//			if (doit) {
//				job.print();
//			}
////			_text.print(_text.getGraphics());
//			return;
		}
		System.err.println("printer menu " + e.getActionCommand() +
						" not implemented yet");
	}

	public void onOff(boolean on) {
		_frame.setVisible(on);
	}

	public void feed() {
		// apparently, this changes getCaretPosition() here,
		// but not in do_printer() below!
		_text.append("\n");
		++_eop;
		_text.setCaretPosition(_eop);
	}

	// col = { 0..19 };
	public void do_printer(byte[] b) {
		int x, y, z;
		int col = (((b[1] & 1) << 4) | ((b[0] & 0xf0) >> 4)) & 0x1f;
		if (col == 0x1f) {
			String s;
			if (_eop > 0) {
				_text.append("\n");
				++_eop;
			}
			if (_pr_buf[PR_XCOL_STRT + 2] == 0x10 &&
			    _pr_buf[PR_XCOL_STRT + 3] != 0x10) {
				_pr_buf[PR_XCOL_STRT + 2] = 0x11;
			} else if (_pr_buf[PR_XCOL_STRT + 3] == 0x10 &&
			    _pr_buf[PR_XCOL_STRT + 2] != 0x10) {
				_pr_buf[PR_XCOL_STRT + 3] = 0x11;
			}
			for (x = 0; x < PR_NUM_COL; ++x) {
				y = _pr_buf[x];
				if (x < PR_XCOL_STRT) {
					if (y >= 0x10) {
						s = pr_0_15[15]; // blank
					} else {
						s = pr_0_15[y];
						if (s == null) {
							// "....OVERFLOW...."
							s = pr_ovr[x];
						}
					}
				} else {
					if (x == PR_XCOL_STRT) {
						// 16th column on print drum,
						// but not accessible by code...
						s = pr_0_15[15]; // blank
						_text.append(s);
						_eop += s.length();
					}
					z = x - PR_XCOL_STRT;
					String[] ww = pr_16_20[z];
					s = ww[y];
				}
				_text.append(s);
				_eop += s.length();
			}
			_text.setCaretPosition(_eop);
			clear_buf();
			return;
		}
		int drm = (b[0] & 0x0f);
		_pr_buf[col] = (byte)drm;
	}
}

class Wang600_Tape extends JComponent
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692039L;
	java.io.RandomAccessFile _tf;
	java.io.OutputStream _fout;
	byte[] bb = new byte[2];
	byte[] b1 = new byte[1];
	boolean _end;
	boolean _ready;
	boolean _tape_on;
	boolean _eot;
	int _index;
	JLabel _window;
	File _file;

	public Wang600_Tape(OutputStream fout) {
		_fout = fout;
		Font font;
		_file = null;
		_index = 0;
		_end = false;
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

		JLabel cass = new JLabel("<HTML><BR><FONT SIZE=+2><B>WANG</B></FONT>" +
					" 600 Series</HTML>",
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
		//
		// TBD: select new file...
		JFileChooser ch = new JFileChooser(w600_fe._dir);
		// setup filter....
		// ExampleFileFilter filter = new ExampleFileFilter();
		// filter.addExtension("wng");
		// filter.setDescription("WANG program Images");
		// ch.setFileFilter(filter);
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
			} else { // request for next byte
				tape_read();
				send_word();
				if ((bb[0] & 0x00ff) == 0x9e) { // End Prog
					++_index; // display updated later..
					// TBD: temporary EOF, until tape off/on
					_end = true;
				}
			}
			return;
		} else if (b[1] == 0x0f) {	// tape on - write
			if (_ready) _tape_on = true;
			_end = false;
		} else if (b[1] == 0x0e) {	// tape off
			_tape_on = false;
			_end = false;
			update_tape();
			//if (_ready) _tf.flush(); // not needed anyway?
		} else if (b[1] == 0x0c) {	// tape write
			if (!_ready) return;
			tape_write(b);
			if ((b[0] & 0x00ff) == 0x9e) { // End Prog
				++_index; // display updated later..
			}
		} else {
			System.err.println("invalid tape command");
		}
	}
}

class Wang600_CN24
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	public void do_cn24(byte[] b) {
		if (b[1] == 0) return;
	}
}

class Wang600_Display extends JComponent
		implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','>','u','<','t',' '};

	byte[] disp_a;
	JLabel disp;
	InputStream _fin;

	Wang600_ProgErr pe;
	Wang600_ProgErr me;
	boolean flashing;
	boolean state;
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

	public Wang600_Display(InputStream f) {
		String blank = "--- ++++++++ ---";
		disp_a = new byte[16];
		disp_a = blank.getBytes();
		flashing = false;
		state = false;
		timer = new Timer(100, this);

		_fin = f;

		setLayout(new FlowLayout());
		disp = new JLabel(blank, SwingConstants.CENTER);
		disp.setForeground(_Key.neon);
		disp.setBackground(_Key.empty);
		disp.setOpaque(true);
		Font font = null;
		java.io.InputStream ttf = null;
		ttf = Wang600_Display.class.getResourceAsStream(
							"fonts/Wang600Display.ttf");
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
			} catch (IOException ee) {
			}
			font = font.deriveFont(28f);
		}
		if (font == null) {
			System.err.println("Missing font \"Wang600Display.ttf\", using default");
			font = new Font("Monospaced", Font.PLAIN, 40);
		}
		disp.setPreferredSize(new Dimension(475, 75));
		disp.setFont(font);

		add(disp);

		pe = new Wang600_ProgErr("Prog<BR>Error");
		pe.setOn(false);
		me = new Wang600_ProgErr("Mach<BR>Error");
		me.setOn(false);

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

		if ((b[1] & 2) != 0) {
			me.setOn(true);
			setFlashing(true);
		} else {
			me.setOn(false);
		}
		if ((b[1] & 1) != 0) {
			pe.setOn(true);
			setFlashing(true);
		} else {
			pe.setOn(false);
		}
		if ((b[1] & 3) == 0) {
			setFlashing(false);
		}
		String s;
		if ((b[1] & 4) != 0) {
			// blank-out display while Wang is not refreshing...
			s = new String("                ");
		} else {
			ds = (b[0] >> 4) & 0x0f;
			dc = b[0] & 0x0f;
			if (ds == 0 || ds == 13) {
				c = sign_chr[dc];
			} else {
				c = disp_chr[dc];
			}
			disp_a[ds] = c;
			s = new String(disp_a);
		}
		disp.setText(s);
		repaint();
	}
}

//class FocusHog extends InputVerifier
//	implements ActionListener
//{
//	public boolean verify(JComponent input) {
//System.err.println("hog");
//		return false;
//	}
//
//	public boolean shouldYieldFocus(JComponent input) {
//System.err.println("HOG");
//		return false;
//	}
//
//	public void actionPerformed(ActionEvent e) {
//System.err.println("hog?");
//		Wang600_Keyboard kb = (Wang600_Keyboard)e.getSource();
//		shouldYieldFocus(kb);
//	}
//}

class Wang600_Keyboard extends JComponent
	implements ActionListener, KeyListener
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang600_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _shift;
	int _shift_kbd;
	int _shift_btn;
	int _meta;
	int _mode0;
	int _mode1;
	OutputStream _fout;
	Wang600_Tape _tape;
	Wang600_Printer _prt;

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
			int tg = _kbds[y]._keys[z].getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			if (_kbds[y]._keys[z].state) {
				setToggle(false, _kbds[y]._keys[z], _kbds[y]._buttons[z]);
			}
		}
		setToggle(!_kbds[y]._keys[x].state, _kbds[y]._keys[x], _kbds[y]._buttons[x]);
	}

	private void do_keycode(int code) {
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

	private void do_button(ActionEvent e, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
			// we really want to trap the "key down" event and set _shift then...
			_shift = true;
		}
		if (_kbds[y]._keys[x].isSHIFT()) {
			setShift(!_shift);
			return;
		}
		if (_kbds[y]._keys[x].isFEED()) {
			_prt.feed();
			return;
		}
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
		if (type == _Key.METAP) {
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
		}
		if (type == _Key.MODE1) {
			boolean on = ((_mode1 & 2) != 0);
			_prt.onOff(on);
			code = _Key.MODE1 | _mode1;
		}
		if (type == _Key.META) {
			code &= 0x00f;
			code |= (_meta << 4);
			type = 0;
		}
		if (_shift) {
			if (type == _Key.SPCL) {
				code += 4;
			} else if (type == 0) {
				code |= 0x010;
			}
			setShift(false);
		}

		do_keycode(code);
	}

	JFrame _frame;
	JEditorPane _text;
	JScrollPane _scroll;
	JButton _help;
	boolean _help_on;

	public Wang600_Keyboard(OutputStream fo, Wang600_ProgErr pe, Wang600_ProgErr me,
				Wang600_Printer prt, Wang600_Tape tape) {
		int x;
		_tape = tape;
		_prt = prt;
		_kbds = new Wang600_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_shift = false;
		_meta = 0;
		_fout = fo;
		_help_on = false;

		java.net.URL url = Wang600_Keyboard.class.getResource("docs/wang600.html");

		_frame = new JFrame("Wang 600 Help");
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
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//_scroll.getViewport().setBackground(_Key.empty);
		_frame.add(_scroll);
		_frame.pack();
		//_frame.setVisible(true);

		Dimension dim = new Dimension(500, 25);
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;
		Wang600_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

		kbd = new Wang600_Keyboard_stick();
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
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

		kbd = new Wang600_Keyboard_meta(pe, me);
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
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

		_help = new JButton("Help");
		_help.setBackground(_Key.empty);
		_help.setForeground(Color.black);
		_help.setOpaque(true);
		_help.setBorderPainted(false);
		_help.setFocusPainted(false);
		_help.setPreferredSize(new Dimension(75,25));
		_help.addActionListener(this);

		kbd = new Wang600_Keyboard_main(_help);
		for (x = 0; x < kbd._nkeys; ++x) {
			if (kbd._keys[x].code == _Key.SHIFT) {
				_shift_kbd = _nkbds;
				_shift_btn = x;
			}
			kbd._buttons[x].addActionListener(this);
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

		// can't seem to stop losing focus...
		//FocusHog fh = new FocusHog();
		//this.setInputVerifier(fh);
		//addActionListener(fh);
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
		if (e.getSource() == _help) {
			_help_on = !_help_on;
			if (_help_on) {
				// this still isn't right...
				_frame.pack();
			}
			_frame.setVisible(_help_on);
			return;
		}
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(e, y, x);
					return;
				}
			}
		}
	}
}

class Wang600_Keyboards extends JComponent
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang600_Keyboards() { }

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
		java.net.URL url = Wang600_Keyboards.class.getResource(icon);
		ImageIcon ic = new ImageIcon(url);
		butt = new JButton(ic);
		butt.setBackground(key.color);
		butt.setBorder(lb);
		butt.setOpaque(true);

		dim.width = 50 * lx;
		dim.height = 50 * ly;
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
		c.anchor = GridBagConstraints.CENTER;

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
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		if (botlab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+botlab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 2;
			gridbag.setConstraints(lab, c);
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

class Wang600_Keyboard_main extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 54;

	public Wang600_Keyboard_main(JButton help) {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		Dimension dim = new Dimension(25, 200);
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

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(75, 200));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c, 1, 1, 0, 0,"icons/prime.gif",
			new _Key(_Key.orange1, _Key.SPCL_KEY(0)));
		addButton(c,1, 1, 0, 1, "icons/rad_deg.gif",
			new _Key(_Key.green1,_Key.PROG_CODE(8,9)));
		addButton(c,1, 2, 0, 2, "icons/shift.gif",
			new _Key(_Key.white1, _Key.SHIFT));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/sin.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,6)));
		addButton(c,1, 1, 0, 1, "icons/tan.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,8)));
		addButton(c,1, 1, 0, 2, "icons/logex.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,10)));
		addButton(c,1, 1, 0, 3, "icons/x2.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,12)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/cos.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,7)));
		addButton(c,1, 1, 0, 1, "icons/inv.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,15)));
		addButton(c,1, 1, 0, 2, "icons/ex.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,11)));
		addButton(c,1, 1, 0, 3, "icons/sqrt.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,13)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(1,15)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(5,15)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,15)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,15)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(3,15)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(2,15)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(7,15)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/chg_sign.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,12)));
		addButton(c,1, 1, 0, 1, "icons/clear_disp.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,15)));
		addButton(c,1, 1, 0, 2, "icons/set_exp.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,11)));
		addButton(c,2, 1, 0, 3, "icons/zero.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/seven.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,7)));
		addButton(c,1, 1, 0, 1, "icons/four.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,4)));
		addButton(c,1, 1, 0, 2, "icons/one.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,1)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/eight.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,8)));
		addButton(c,1, 1, 0, 1, "icons/five.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,5)));
		addButton(c,1, 1, 0, 2, "icons/two.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,2)));
		addButton(c,2, 1, 0, 3, "icons/dp.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,10)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/nine.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,9)));
		addButton(c,1, 1, 0, 1, "icons/six.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,6)));
		addButton(c,1, 1, 0, 2, "icons/three.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,3)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(3,14)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(2,14)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(7,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(1,14)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(5,14)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,14)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,14)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/ld_prog.gif",
			new _Key(_Key.orange1, _Key.PROG_CODE(8,14)));
		addButton(c,1, 1, 0, 1, "icons/search.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,0)));
		addButton(c,1, 2, 0, 2, "icons/go.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(8,3)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/jif0.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,4)));
		addButton(c,1, 1, 0, 1, "icons/jifplus.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,5)));
		addButton(c,1, 1, 0, 2, "icons/recall_xx.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,1)));
		addButton(c,1, 1, 0, 3, "icons/print.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,2)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/i_o.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,2)));
		addButton(c,1, 1, 0, 1, "icons/group1.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,13)));
		addButton(c,1, 1, 0, 2, "icons/group2.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,14)));
		addButton(c,1, 1, 0, 3, "icons/indir.gif",
			new _Key(_Key.orange1, _Key.PROG_CODE(15,11)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/set_pc.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(2)));
		addButton(c,1, 1, 0, 1, "icons/verif_prog.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(1)));
		addButton(c,1, 1, 0, 2, "icons/rec_prog.gif",
			new _Key(_Key.orange1, _Key.SPCL_KEY(3)));
		addButton(c,1, 1, 0, 3, "icons/step.gif",
			new _Key(_Key.green1, _Key.MODE0_CHG(8,8)));
		++_col;

		s.gridx = _col;
		s.gridy = 3;
		s.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(help, s);
		add(help);
		++_col;

		_col = 0;
		_row += 4;
	}
}

class Wang600_Keyboard_meta extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 16;

	public Wang600_Keyboard_meta(Wang600_ProgErr pe, Wang600_ProgErr me) {
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

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(70, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		setLayout(gridbag);

		addButton(c,1, 1, 0, 0, "icons/k00.gif",
			new _Key(_Key.white1, _Key.META_KEY(0)));
		addButton(c,1, 1, 1, 0, "icons/k01.gif",
			new _Key(_Key.white1, _Key.META_KEY(1)));
		addButton(c,1, 1, 2, 0, "icons/k02.gif",
			new _Key(_Key.white1, _Key.META_KEY(2)));
		addButton(c,1, 1, 3, 0, "icons/k03.gif",
			new _Key(_Key.white1, _Key.META_KEY(3)));
		addButton(c,1, 1, 4, 0, "icons/k04.gif",
			new _Key(_Key.white1, _Key.META_KEY(4)));
		addButton(c,1, 1, 5, 0, "icons/k05.gif",
			new _Key(_Key.white1, _Key.META_KEY(5)));
		addButton(c,1, 1, 6, 0, "icons/k06.gif",
			new _Key(_Key.white1, _Key.META_KEY(6)));
		addButton(c,1, 1, 7, 0, "icons/k07.gif",
			new _Key(_Key.white1, _Key.META_KEY(7)));
		addButton(c,1, 1, 8, 0, "icons/k08.gif",
			new _Key(_Key.white1, _Key.META_KEY(8)));
		addButton(c,1, 1, 9, 0, "icons/k09.gif",
			new _Key(_Key.white1, _Key.META_KEY(9)));
		addButton(c,1, 1, 10, 0, "icons/k10.gif",
			new _Key(_Key.white1, _Key.META_KEY(10)));
		addButton(c,1, 1, 11, 0, "icons/k11.gif",
			new _Key(_Key.white1, _Key.META_KEY(11)));
		addButton(c,1, 1, 12, 0, "icons/k12.gif",
			new _Key(_Key.white1, _Key.META_KEY(12)));
		addButton(c,1, 1, 13, 0, "icons/k13.gif",
			new _Key(_Key.white1, _Key.META_KEY(13)));
		addButton(c,1, 1, 14, 0, "icons/k14.gif",
			new _Key(_Key.white1, _Key.META_KEY(14)));
		addButton(c,1, 1, 15, 0, "icons/k15.gif",
			new _Key(_Key.white1, _Key.META_KEY(15)));
		_col += 16;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 50));
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
		_row += 1;

	}
}

class Wang600_Keyboard_stick extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.50 2011/05/20 23:46:34 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 22;

	public Wang600_Keyboard_stick() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
		Dimension dim = new Dimension(20, 30);
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
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 30, 1, 0, 0,"Clear","",null, false,
			new _Key(_Key.red1, _Key.PROG_CODE(0,14)));

		addPushButton(c, 5, 1, 1, 0,"T","1",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,1))));
		addPushButton(c, 5, 1, 2, 0,"+","2",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,2))));
		addPushButton(c, 5, 1, 3, 0,"-","3",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,3))));
		addPushButton(c, 5, 1, 4, 0,"X","4",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,4))));
		addPushButton(c, 5, 1, 5, 0,"&divide;","5",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,5))));
		addPushButton(c, 5, 1, 6, 0,"St","6",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,6))));
		addPushButton(c, 5, 1, 7, 0,"Re","7",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,7))));
		addPushButton(c, 5, 1, 8, 0,"f(x)","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(15,10))));
		addPushButton(c, 5, 1, 9, 0,"Sp<BR>\u2193<BR>On", "8",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.META_PRE(8,8))));
		addPushButton(c, 5, 1, 10, 0,"Fl<BR>\u2195<BR>Sc","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(4,_Key.MODE0_CHG(1,1))));
		addPushButton(c, 5, 1, 11, 0,"Deg<BR>\u2195<BR>Rad","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(5,_Key.MODE1_CHG(1,1))));
		addPushButton(c, 5, 1, 12, 0,"Printer<BR>\u2193<BR>On","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(6,_Key.MODE1_CHG(2,2))));
		addPushButton(c, 5, 1, 13, 0,"Paper<BR>Feed","",null, false,
			new _Key(_Key.white1, _Key.FEED));
		_col += 14;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(30, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addTapeButton(c, 5, 1, 0, 0, "Release", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(7,_Key.TAPE_EJECT)));

		addTapeButton(c, 5, 1, 1, 0, "Forward", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(7,_Key.TAPE_FF)));

		addTapeButton(c, 5, 1, 2, 0, "Tape Ready", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(7,_Key.TAPE_READY)));

		addTapeButton(c, 5, 1, 3, 0, "Rewind", _Key.white2,
			new _Key(_Key.ivory, _Key.GROUP(7,_Key.TAPE_REW)));

		_col = 0;
		_row += 1;

	}
}
