// Copyright (c) 2011,2012 Douglas Miller
// $Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $

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
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color white2 = new Color(150, 150, 150, 255);
	static final Color white3 = new Color(200, 200, 200, 255);
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
	static final int METAS = 0x0600;	// never sent

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
	static final int META_SPL(int a, int b) {
		return (METAS | (a << 4) | b);
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
	public boolean isMETA() {
		return (getType() == METAP || getType() == METAS);
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

public class w600_fe
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";

	public static File _dir;
	public static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
		java.io.BufferedReader ferr = null;
		GridBagLayout gridbag = new GridBagLayout();
		String dir;

		dir = System.getenv("WANG600HOME");
		if (dir == null) {
			dir = System.getProperty("user.home") + "/Wang600Files";
		}
		_dir = new File(dir);
		String dispfont;
		dispfont = System.getenv("WANG600_FONT");
		if (dispfont == null) {
			dispfont = "Panaplex9seg.ttf"; // get from env? commandline?
		}

		boolean test = (args.length > 0 && args[0].compareTo("-t") == 0);
		boolean back = (args.length > 0 && args[0].compareTo("-b") == 0);
		boolean web = (args.length > 0 && args[0].compareTo("-w") == 0);
		if (back) {
			fout = System.out;
			fin = System.in;
		} else if (web) {
			String host = System.getenv("WANG600_HOST");
			String port = System.getenv("WANG600_PORT");
			if (args.length >= 3) {
				port = args[2];
				host = args[1];
			}
			if (host == null || port == null) {
				System.err.println("Usage: w600_fe -w <host> <port>");
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

		Wang600_Display dsp = new Wang600_Display(fin, dispfont);
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

		Wang600_Model611 m611f = new Wang600_Model611();
		Wang600_Model630 m630f = new Wang600_Model630(kbd);
		Wang600_XROM xROMf = new Wang600_XROM(kbd);

		Wang600_SimInput inp = new Wang600_SimInput(fin, dsp, prt, tape, m611f, m630f, xROMf);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Expansion ROM - none installed", KeyEvent.VK_R);
		mi.addActionListener(inp);
		mu.add(mi);
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

class Wang600_ProgErr extends JComponent {
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
		implements Runnable, WindowListener, ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
	Wang600_Display _dsp;
	Wang600_Printer _prt;
	Wang600_Tape _tape;
	Wang600_Model611 _m611;
	Wang600_Model630 _m630;
	Wang600_XROM _xROM;

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
		if (m.getMnemonic() == KeyEvent.VK_R) {
			_xROM.pickFile(m);
			return;
		}
	}

	public Wang600_SimInput(InputStream f, Wang600_Display dsp,
			Wang600_Printer prt, Wang600_Tape tape,
			Wang600_Model611 cn24,
			Wang600_Model630 m630,
			Wang600_XROM xROM) {
		_dsp = dsp;
		_prt = prt;
		_tape = tape;
		_m611 = cn24;
		_m611.getFrame().addWindowListener(this);
		_m630 = m630;
		_xROM = xROM;
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
			} else if ((b[1] & 0xfc) == 0x00) {
				// there will be 16 total sent...
				// and they are in order: 0-15...
				byte[] m = new byte[32];
				try {
					n = _fin.read(m);
				} catch (IOException ee) {
				}
if (n != 32) System.err.println("too little? "+n);
				_dsp.do_display(m);
			} else if ((b[1] & 0xfe) == 0x04) {
				_dsp.do_indicators(b);
			} else if ((b[1] & 0xfe) == 0x06) {
				_dsp.do_blanking();
			} else if ((b[1] & ~1) == 0x08) {
				_prt.do_printer(b);
			} else if ((b[1]  & ~3) == 0x0c) {
				_tape.do_tape(b);
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_m611.reset();
				_m630.reset();
				//etc...
			} else if (b[1] == 0x10) {
				_m611.do_cn24(b);
			} else if ((b[1] & ~0x1f) == 0x20) {
				_m630.do_dev(b);
			} else if ((b[1] & 0x80) != 0) {
				_xROM.do_dev(b);
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

class Wang600_Printer
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
		"S   ", "RE  ", "W   ", "GO  ", "Jo  ", "J+  ", "SN  ", "CS  ",
		"TN  ", "RD  ", "LN  ", "e\u207F  ", "x\u00B2  ", "\u221AX  ", "LP  ", "1/x ",
		"  ", ""
	};
	final String[] pr_19 = {
		"  M ", "  ST", "  \u03B1 ", "  SP", "  J\u00F8", "  Je", "  S\u00B9", "  C\u00B9",
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
	PrintTextArea _text;
	JScrollPane _scroll;
	int _yoff;
	String _footer;

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
		_text = new PrintTextArea(32, 26); // can user resize?
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
		Dimension dim = _scroll.getSize();
		int sy = dim.height;
		dim = _frame.getPreferredSize();
		int fy = dim.height;
		_yoff = fy - sy;

		//_frame.setVisible(true);	// make visible based on "printer on"
		_frame.addComponentListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Printer event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_T) {
			_text.setText("");
			_eop = 0;
			_text.setCaretPosition(_eop);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			FileOutputStream fo;
			SuffFileChooser ch = new SuffFileChooser("Save",
							"lst", "Wang list files");
			int rv = ch.showDialog(_frame);
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
		if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(OrientationRequested.LANDSCAPE);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.25, (float)0.25, (float)8.0, (float)10.5, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(_text);
			boolean print = pj.printDialog(aset);
			if (print) {
				java.util.Date dt = new java.util.Date();
				_footer = new String("Wang 600 Printer - " +
					w600_fe._timestamp.format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println("printer menu " + e.getActionCommand() +
						" not implemented yet");
	}

	class PrintTextArea extends JTextArea
			implements Printable {
		static final long serialVersionUID = 311457692042L;
		public PrintTextArea(int a, int b) {
			super(a, b);
		}
		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();
			double y0 = pf.getImageableY();
			double w0 = pf.getImageableWidth();
			double h0 = pf.getImageableHeight();
			int pg = 0;
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);
			g2d.setFont(_text.getFont());

			int did = 0;
			int y = 0;
			int x = 0;
			int off = 0;
			String s;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			int l = g2d.getFont().getSize();
			while (pg <= pageIndex) {
				if (off != 0) {
					off += 1; // skip nl, we hope...
				}
				try {
					s = getText(off, 25);
				} catch(javax.swing.text.BadLocationException ee) {
					break;
				}
				if (!s.startsWith("\n")) { // not blank line...
					if (pg == pageIndex) {
						++did;
						g2d.drawString(s, y * 188, x * l + (int)y0 + 36);
					}
					off += 25;
				}
				++x;
				if (x >= 40) {
					x = 0;
					++y;
					if (y >= 4) {
						y = 0;
						++pg;
					}
				}
			}
			if (did > 0) {
				pg = pageIndex + 1; // 1-based
				s = new String("Page " + pg +
					" - " + _footer);
				g2d.drawString(s, 0, (41 + 1) * l + (int)y0 + 36);
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
		// only one component?
		if (e.getComponent() == _frame) {
			Dimension dim = _frame.getSize(); 
			int fy = dim.height;
			dim = _scroll.getSize();
			int sx = dim.width;
			dim = _frame.getPreferredSize();
			int fx = dim.width;

			int y = (fy - _yoff) / _text.getFont().getSize();
			_text.setSize(y, 26);
			_scroll.setSize(sx, fy - _yoff);
			_scroll.setPreferredSize(_scroll.getSize());
			_frame.setSize(fx, fy);
			_frame.setPreferredSize(_frame.getSize());
			// the above causes the scroll position to get reset...
			//_text.setCaretPosition(_eop); // doesn't do anything?
//JScrollBar sb = _scroll.getVerticalScrollBar(); // also no effect...
//sb.setValue(sb.getMaximum());
			//_frame.pack(); // causes extra events...
		}
	}

	public void onOff(boolean on) {
		_frame.setVisible(on);
	}

	public JFrame getFrame() { return _frame; }

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
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
	boolean _prot;
	int _index;
	JLabel _window;
	File _file;

	public Wang600_Tape(OutputStream fout) {
		_fout = fout;
		Font font;
		_file = null;
		_index = 0;
		_end = false;
		_wr = false;
		_ready = false;
		_tape_on = false;
		_eot = false;
		_prot = false;
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
				"<B>File #</B> " + _index + eot +
				"</HTML>");
		}
		_window.setText(txt);
		repaint();
	}

	private void pick_file() {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser("Mount Tape",
						"wng", "Wang program files");
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
		if (_prot) return;
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

class Wang600_Model630 {
	private int _cmd;
	private int _adr;
	private boolean _wr;
	private int _len;
	private int _idx;
	private Wang600_Keyboard _kbd;
	java.io.RandomAccessFile _f;
	File _file;
	byte[] _buf;

	public Wang600_Model630(Wang600_Keyboard kbd) {
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
		int rv = ch.showDialog(_kbd);
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

class Wang600_XROM {
	private Wang600_Keyboard _kbd;
	File _file;

	public Wang600_XROM(Wang600_Keyboard kbd) {
		_kbd = kbd;
	}

	public void do_dev(byte[] b) {
		// now, the back-end is waiting for us...
		// dump the whole file...
		FileInputStream f;
		if ((b[1] & 0x00ff) != 0x81) {
			// should not happen, but don't keep them waiting...
			_kbd.do_keycode(0xff00);
			return;
		}

		if (_file == null) {
			// should not happen, but don't keep them waiting...
			_kbd.do_keycode(0xff00);
			return;
		}
		try {
			f = new FileInputStream(_file);
		} catch (FileNotFoundException ee) {
			// should not happen, but don't keep them waiting...
			_kbd.do_keycode(0xff00);
			return;
		}
		byte[] buf = new byte[256];
		int x;
		int n;

		do {
			try {
				n = f.read(buf);
			} catch (IOException ee) {
				n = -1;
			}
			for (x = 0; x < n; ++x) {
				_kbd.do_keycode(0x8000 | (buf[x] & 0x0ff));
			}
		} while (n > 0);

		try {
			f.close();
		} catch (IOException ee) {
		}
		_kbd.do_keycode(0xff00);
	}

	public void pickFile(JMenuItem m) {
		SuffFileChooser ch = new SuffFileChooser("Install",
							"wng", "Wang program files");
		int rv = ch.showDialog(_kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			_kbd.do_keycode(0x8100);
			// are we being too optimistic? maybe wait until
			// download succeeds?
			m.setText("Expansion ROM - " + _file.getName());
		} else {
			_file = null;
			m.setText("Expansion ROM - none installed");
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
		super(w600_fe._dir);
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

class Wang600_Model611
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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

	public Wang600_Model611() {
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
			int rv = ch.showDialog(_frame);
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
					w600_fe._timestamp.format(dt));
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

class Wang600_Display extends JComponent
		implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','B','C','D','E',' '};

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

	public Wang600_Display(InputStream f, String fontname) {
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
		ttf = Wang600_Display.class.getResourceAsStream(fontname);
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
			} catch (IOException ee) {
			}
			font = font.deriveFont(40f);
			// special decimal point, optimal placement...
			if (font.canDisplay('\006')) disp_chr[10] = '\006';
			// special one digit, optimal placement...
			if (font.canDisplay('\005')) disp_chr[1] = '\005';
		}
		if (font == null) {
			System.err.println("Missing font \"" +
					fontname + "\", using default");
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

	public void do_indicators(byte[] b) {
		if ((b[0] & 2) != 0) {
			me.setOn(true);
			setFlashing(true);
		} else {
			me.setOn(false);
		}
		if ((b[0] & 1) != 0) {
			pe.setOn(true);
			setFlashing(true);
		} else {
			pe.setOn(false);
		}
		if ((b[0] & 3) == 0) {
			setFlashing(false);
		}
	}

	public void do_blanking() {
		// blank-out display while Wang is not refreshing...
		String s = new String("                ");
		disp.setText(s);
		repaint();
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	public void do_display(byte[] b) {
		int ds;
		ds = 0;
		disp_a[ds] = sign_chr[b[ds * 2 + 0] & 0x0f]; // mant sign
		++ds;
		do {
			disp_a[ds] = disp_chr[b[ds * 2 + 0] & 0x0f];
			++ds;
		} while (ds < 13);
		disp_a[ds] = sign_chr[b[ds * 2 + 0] & 0x0f]; // exp sign
		++ds;
		disp_a[ds] = disp_chr[b[ds * 2 + 0] & 0x0f];
		++ds;
		disp_a[ds] = disp_chr[b[ds * 2 + 0] & 0x0f];
		++ds;

		String s = new String(disp_a);
		disp.setText(s);
		repaint();
	}
}

class Wang600_Keyboard extends JComponent
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
	int _meta_kbd;
	int _print_kbd;
	int _print_btn;
	int _meta;
	int _metas;
	int _mode0;
	int _mode1;
	int _defreg;
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

	private void setDefReg(int _new) {
		_kbds[_meta_kbd]._buttons[_defreg].setBackground(_Key.white1);
		_defreg = _new & 0x0f;
		_kbds[_meta_kbd]._buttons[_defreg].setBackground(_Key.white3);
	}

	private void setToggle(boolean on, _Key key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == _Key.METAP) {
			_meta &= ~key.getMask();
		} else if (key.getType() == _Key.METAS) {
			_metas &= ~key.getMask();
		} else if (key.getType() == _Key.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE1) {
			_mode1 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == _Key.METAP) {
				_meta |= key.getMode();
			} else if (key.getType() == _Key.METAS) {
				_metas |= key.getMode();
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
			if (_shift) {
				code += 4;
			}
		}
		if (type == _Key.MODE1) {
			boolean on = ((_mode1 & 2) != 0);
			_prt.onOff(on);
			code = _Key.MODE1 | _mode1;
		}
		if (type == 0 && _shift && (code & 0x0f0) == 0x080) {
			code |= 0x010;
		}
		if (type == _Key.META) {
			code &= 0x00f;
			code |= ((_meta | _metas) << 4);
			if (_shift) {
				code |= 0x010;
			}
		}
		if (!shifted) setShift(false);

		do_keycode(code);
	}

	JFrame _frame;
	JEditorPane _text;
	JScrollPane _scroll;
	int _xoff, _yoff;

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
		_metas = 0;
		_fout = fo;
		_defreg = 15;

		_prt.getFrame().addWindowListener(this);

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
			if (kbd._keys[x].code == _Key.GROUP(6,_Key.MODE1_CHG(2,2))) {
				_print_kbd = _nkbds;
				_print_btn = x;
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
		// assume the meta keys 00-15 are in order...
		_meta_kbd = _nkbds;
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

		kbd = new Wang600_Keyboard_main();
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
if (e.isActionKey()) {
System.err.println("action");
}
		char c = e.getKeyChar();
		if (c >= '0' && c <= '9') {
			do_keycode(c - '0');
		} else if (c == 'e' || c == 'E') {
			do_keycode(11);
		} else if (c == '.') {
			do_keycode(10);
		} else if (c == '-') {
			do_keycode(12);
		} else if (c == '\b') {
			do_keycode(15);
		} else if (c == 't' || c == 'T') {
			do_keycode(0x0010 | _defreg);
		} else if (c == '+') {
			do_keycode(0x0020 | _defreg);
		} else if (c == '_') {
			do_keycode(0x0030 | _defreg);
		} else if (c == '*') {
			do_keycode(0x0040 | _defreg);
		} else if (c == '/') {
			do_keycode(0x0050 | _defreg);
		} else if (c == 's' || c == 'S') {
			do_keycode(0x0060 | _defreg);
		} else if (c == 'r' || c == 'R') {
			do_keycode(0x0070 | _defreg);
		} else if (c == 'i' || c == 'I') {
			do_keycode(0x00fb);
		} else if (c == 'x' || c == 'X') {
			do_keycode(0x00e0 | _defreg);
		}
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(true);
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			setDefReg(_defreg - 1);
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			setDefReg(_defreg + 1);
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
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
		if (e.getWindow() == _prt.getFrame()) {
			do_button(false, _print_kbd, _print_btn);
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
		}
	}
}

class Wang600_Keyboards extends JComponent
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
		// butt.setHorizontalAlignment(SwingConstants.CENTER); // didn't help...

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
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 54;

	public Wang600_Keyboard_main() {
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
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(75, 20));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		_col = 0;
		_row += 4;
	}
}

class Wang600_Keyboard_meta extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
	final String ident = "$Id: w600_fe.java,v 1.105 2012/01/21 19:34:58 drmiller Exp $";
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
			new _Key(_Key.white1, _Key.GROUP(3,_Key.META_SPL(8,8))));
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
