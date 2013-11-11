// Copyright (c) 2011,2012 Douglas Miller
// $Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.Desktop;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;
import javax.swing.JCheckBox;

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
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";

	private static JFrame front_end;

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
//		java.io.BufferedReader ferr = null;
		GridBagLayout gridbag = new GridBagLayout();


		boolean test = (args.length > 0 && args[0].compareTo("-t") == 0);
		boolean back = (args.length > 0 && args[0].compareTo("-b") == 0);
		boolean web = (args.length > 0 && args[0].compareTo("-w") == 0);

		java.net.URL url = w600_fe.class.getResource("icons/wang600-48x48.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);

		Wang_UI.setProperties(new Wang600_Properties());
		Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang600_home"));
		Wang_UI.setSeries("6");

		if (test) {
			// Test front-end stand-alone - nothing special
		} else if (back) {
			// Used for debugging? Need adapter class to convert
			// multi-stream comm to fin/fout.
			fout = System.out;
			fin = System.in;
		} else if (web || Wang_UI.getProperties().getBoolean("wang600_remote")) {
			// Not needed any more
			String host = Wang_UI.getProperties().getProperty("wang600_host");
			String port = Wang_UI.getProperties().getProperty("wang600_port");
			if (web && args.length >= 3) {
				// should these set properties?
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
			} catch (Exception ee) {
				Wang_UI.fatal("Startup", ee.getMessage());
			}
		} else {
//			try {
//				Process _be = null;
//				_be = Runtime.getRuntime().exec("./w600-sim -b");
//				fout = _be.getOutputStream();
//				fin = _be.getInputStream();
//				ferr = new BufferedReader(new InputStreamReader(_be.getErrorStream()));
//				Runtime.getRuntime().addShutdownHook(new FEexit(_be));
//				new Wang600_SimError(ferr);
//			} catch (IOException ee) {
//				System.err.println("Unable to exec back-end!");
//				System.exit(1);
//			}
		}
		Wang_UI.setSimIO(fin, fout);
		front_end = new JFrame("Wang 600 Advanced Programmable Calculator");
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

		Wang600.Disp = new Wang600_Display();
		s.gridx = 1;
		s.gridy = 0;
		gridbag.setConstraints(Wang600.Disp, s);
		front_end.add(Wang600.Disp);
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

		Wang600.Tape = new Wang_TapeDrive(
					"<HTML><BR><FONT SIZE=+2><B>WANG</B></FONT>" +
					" 600 SERIES</HTML>",
					Wang_Colors.ivory, Wang_Colors.aqua,
					null, "tape image",
					Wang_UI.getProperties().getProperty("wang600_tape_file_suffix"),
					"File", (byte)0x9e, "wang600_tape_image");
		s.gridx = 3;
		s.gridheight = 2;
		gridbag.setConstraints(Wang600.Tape, s);
		s.gridheight = 1;
		front_end.add(Wang600.Tape);

		Wang600.Prt = new Wang600_Printer();

		// Must be after Display, Tape, and Printer are created.
		Wang600.Kbd = new Wang600_Keyboard();
		s.gridx = 0;
		s.gridy = 2;
		s.gridwidth = 4;
		s.gridheight = 1;
		gridbag.setConstraints(Wang600.Kbd, s);
		front_end.add(Wang600.Kbd);
		front_end.addKeyListener(Wang600.Kbd);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(110, 5));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 3;
		s.gridwidth = 4;
		s.gridheight = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		String cn24 = Wang_UI.getProperties().getProperty("wang600_cn24_device");
		Wang600.CN24 = null;
		if (cn24 != null && cn24.equals(Wang_PlottingOutputWriter.getModel())) {
			Wang600.CN24 = new Wang_PlottingOutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_OutputWriter.getModel())) {
			Wang600.CN24 = new Wang_OutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_InputOutputWriter.getModel())) {
			Wang600.CN24 = new Wang_InputOutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_Plotter.getModel())) {
			Wang600.CN24 = new Wang_Plotter();
		}
		// Must be after Keyboard created.
		Wang600.M630 = new Wang600_Model630();
		Wang600.XROM = new Wang600_XROM();

		Wang600.Help = new Wang600_Help(front_end);

		// Must be alfter all components created.
		Wang600_SimInput inp = new Wang600_SimInput();
		Wang600.XROM.Initialize();
 
		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = Wang600.XROM.getMenu(KeyEvent.VK_R);
		mi.addActionListener(inp);
		mu.add(mi);

		mu.add(inp.getOutputMenu()); // CN-24 output devices

		mi = Wang600.M630.getMenu(KeyEvent.VK_D);
		mi.addActionListener(inp);
		mu.add(mi);

		mu = new JMenu("Edit");
		mb.add(mu);
		mi = new JMenuItem("Copy", KeyEvent.VK_COPY);
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK);
		mi.setAccelerator(ks);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = new JMenuItem("Paste", KeyEvent.VK_PASTE);
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK);
		mi.setAccelerator(ks);
		mi.addActionListener((Wang600_Keyboard)Wang600.Kbd);
		mu.add(mi);
		mi = new JMenuItem("Preferences", KeyEvent.VK_E);
		mi.addActionListener(inp);
		mu.add(mi);

		mu = new JMenu("Help");
		mb.add(mu);
		mi = Wang600.Help.getMenuItemHelp();
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang600.Help.getMenuItemAbout();
		mi.addActionListener(inp);
		mu.add(mi);

		front_end.setJMenuBar(mb);

		Wang600.Disp.setProperties(Wang_UI.getProperties());
		// others?

		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,640);
		front_end.pack();
		front_end.setVisible(true);
	}
}

class Wang600_Properties extends Wang_Properties
		implements Wang_PropertyEditor
{
	static final long serialVersionUID = 311000000015L;
	JCheckBox _d12_cb;
	JCheckBox _cdp_cb;
	JCheckBox _sp1_cb;
	JCheckBox _rem_cb;
	JRadioButton _f_rb1;
	JRadioButton _f_rb2;
	JRadioButton _f_rb3;
	ButtonGroup _f_bg;
	JLabel _f_lb;
	JTextArea _home_tx;
	JTextArea _host_tx;
	JTextArea _port_tx;
	JPanel _home_pn;
	JPanel _host_pn;
	JPanel _port_pn;
	JPanel _dia_pn;

	public Wang600_Properties() {
		try {
			initProperties("Wang600", "~/.wang600.rc");
		} catch (Exception e) {
			Wang_UI.warning("Load Setup", e.getMessage());
		}
		processDefaults();

		// Edit Properties...
		_d12_cb = new JCheckBox("Enable Column 12");
		_cdp_cb = new JCheckBox("Center DP");
		_sp1_cb = new JCheckBox("Enable PanaPlex '1'");
		_f_rb1 = new JRadioButton("PanaPlex 9-Segment");
		_f_rb1.setActionCommand("Panaplex9seg.ttf");
		_f_rb2 = new JRadioButton("Nixie Tubes");
		_f_rb2.setActionCommand("NixieZM1336.ttf");
		_f_rb3 = new JRadioButton("(not set)");
		_f_rb3.setActionCommand("nothing");
		_f_rb3.setEnabled(false);
		_f_bg = new ButtonGroup();
		_f_bg.add(_f_rb1);
		_f_bg.add(_f_rb2);
		_f_bg.add(_f_rb3);
		_f_lb = new JLabel("Display style:");
		_home_tx = new JTextArea();
		_home_tx.setPreferredSize(new Dimension(200, 20));
		_home_pn = new JPanel();
		_home_pn.add(new JLabel("Home:"));
		_home_pn.add(_home_tx);
		_rem_cb = new JCheckBox("Use remote server");
		_host_tx = new JTextArea();
		_host_tx.setPreferredSize(new Dimension(200, 20));
		_host_pn = new JPanel();
		_host_pn.add(new JLabel("Remote host:"));
		_host_pn.add(_host_tx);
		_port_tx = new JTextArea();
		_port_tx.setPreferredSize(new Dimension(50, 20));
		_port_pn = new JPanel();
		_port_pn.add(new JLabel("Remote port:"));
		_port_pn.add(_port_tx);
		_dia_pn = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		_dia_pn.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 1;
		s.gridy = 1;
		s.weightx = 1;
		s.weighty = 1;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(_d12_cb, s);
		_dia_pn.add(_d12_cb);
		s.gridy += 1;
		gridbag.setConstraints(_cdp_cb, s);
		_dia_pn.add(_cdp_cb);
		s.gridy += 1;
		gridbag.setConstraints(_sp1_cb, s);
		_dia_pn.add(_sp1_cb);
		s.gridy += 1;
		gridbag.setConstraints(_f_lb, s);
		_dia_pn.add(_f_lb);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb1, s);
		_dia_pn.add(_f_rb1);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb2, s);
		_dia_pn.add(_f_rb2);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb3, s);
		_dia_pn.add(_f_rb3);
		s.gridy += 1;
		gridbag.setConstraints(_home_pn, s);
		_dia_pn.add(_home_pn);
		s.gridy += 1;
		gridbag.setConstraints(_rem_cb, s);
		_dia_pn.add(_rem_cb);
		s.gridy += 1;
		gridbag.setConstraints(_host_pn, s);
		_dia_pn.add(_host_pn);
		s.gridy += 1;
		gridbag.setConstraints(_port_pn, s);
		_dia_pn.add(_port_pn);

		setupDialog(_dia_pn, Wang_UI.getIcon());
	}

	public void processDefaults() {
		// setup defaults for everything...
		String s;
		s = getProperty("wang600_digit12");
		if (s == null || s.length() == 0) {
			setProperty("wang600_digit12", "false");
		}
		s = getProperty("wang600_centerDP");
		if (s == null || s.length() == 0) {
			setProperty("wang600_centerDP", "false");
		}
		s = getProperty("wang600_special1");
		if (s == null || s.length() == 0) {
			setProperty("wang600_special1", "true");
		}
		s = getProperty("wang600_displayfont");
		if (s == null || s.length() == 0) {
			setProperty("wang600_displayfont", "Panaplex9seg.ttf");
		}
		s = getProperty("wang600_home");
		if (s == null || s.length() == 0) {
			setProperty("wang600_home", "~/Wang600Files");
		}
		s = getProperty("wang600_tape_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_tape_file_suffix", "w6t");
		}
		s = getProperty("wang600_rom_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_rom_file_suffix", "w6x");
		}
		s = getProperty("wang600_disk_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_disk_file_suffix", "w6d");
		}
		s = getProperty("wang600_remote");
		if (s == null || s.length() == 0) {
			setProperty("wang600_remote", "false");
		}
//	do we have defaults for these?
//		s = getProperty("wang600_host");
//		if (s == null || s.length() == 0) {
//			setProperty("wang600_host", "localhost");
//		}
//		s = getProperty("wang600_port");
//		if (s == null || s.length() == 0) {
//			setProperty("wang600_port", "10311");
//		}

		// process (obsolete?) env vars...
		s = System.getenv("WANG600HOME");
		if (s != null) {
			setProperty("wang600_home", s);
		}
		s = System.getenv("WANG600_FONT");
		if (s != null) {
			setProperty("wang600_displayfont", s);
		}
		s = System.getenv("WANG600_HOST");
		if (s != null) {
			setProperty("wang600_host", s);
		}
		s = System.getenv("WANG600_PORT");
		if (s != null) {
			setProperty("wang600_port", s);
		}

		// special processing for any required...
		s = getProperty("wang600_home");
		if (s.startsWith("~/")) {
			s = System.getProperty("user.home") + s.substring(1);
			setProperty("wang600_home", s);
		}
	}

	public boolean editPreferences() {
		_cdp_cb.setSelected(getBoolean("wang600_centerDP"));
		_d12_cb.setSelected(getBoolean("wang600_digit12"));
		_sp1_cb.setSelected(getBoolean("wang600_special1"));
		String f = getProperty("wang600_displayfont");
		if (f.equals(_f_rb1.getActionCommand())) {
			_f_rb1.setSelected(true);
		} else if (f.equals(_f_rb2.getActionCommand())) {
			_f_rb2.setSelected(true);
		} else {
			_f_rb3.setText(f);
			_f_rb3.setActionCommand(f);
			_f_rb3.setEnabled(true);
			_f_rb3.setSelected(true);
			// Need something user-editable...
		}
		_rem_cb.setSelected(getBoolean("wang600_remote"));
		_home_tx.setText(getProperty("wang600_home"));
		_host_tx.setText(getProperty("wang600_host"));
		_port_tx.setText(getProperty("wang600_port"));

		int ret = doDialog();
		if (ret != OPTION_APPLY && ret != OPTION_SAVE) return false;

		// TBD: change parameters and restart?
		// TBD: do validation?
		ButtonModel bm = _f_bg.getSelection();
		setProperty("wang600_displayfont", bm.getActionCommand());
		setProperty("wang600_digit12", Boolean.toString(_d12_cb.isSelected()));
		setProperty("wang600_centerDP", Boolean.toString(_cdp_cb.isSelected()));
		setProperty("wang600_special1", Boolean.toString(_sp1_cb.isSelected()));
		setProperty("wang600_home", _home_tx.getText());
		setProperty("wang600_remote", Boolean.toString(_rem_cb.isSelected()));
		setProperty("wang600_host", _host_tx.getText());
		setProperty("wang600_port", _port_tx.getText());
		processDefaults();

		if (ret == OPTION_SAVE) {
			try {
				save();
			} catch (Exception e) {
				Wang_UI.warning("Save Setup", e.getMessage());
			}
		}
		return true;
	}
}

// Receives single byte-stream input from BackEnd simulator and directs
// messages to components.
class Wang600_SimulatorPipe
	implements Wang_Core, ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";

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

	public void ackIO(int iob) {
		int code = ((iob << 12) | 0x0100);
		if ((iob & ~1) == 4 && _cn36 != null) {
			code |= _cn36.getGLRN();
		}
		do_keycode(code);
	}

	public void replyIO(int iob, int rep) {
		do_keycode((iob << 12) | 0x0100 | (rep & 0x0ff));
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
				Wang600.Disp.setOv((byte)(b[0] & 1));
				Wang600.Disp.setErr((byte)(b[0] & 2));
			} else if ((b[1] & 0xfe) == 0x06) {
				Wang600.Disp.do_blanking();
			} else if ((b[1] & ~1) == 0x08) {
				int col = (((b[1] & 1) << 4) | ((b[0] & 0xf0) >> 4)) & 0x1f;
				int drm = (b[0] & 0x0f);
				if (col == 0x1f) {
					Wang600.Prt.do_line();
				} else {
					Wang600.Prt.do_printer(col, drm);
				}
			} else if ((b[1]  & ~3) == 0x0c) {
				if (b[1] == 0x0d) { // tape on - read
					if (b[0] == 0) { // tape-on
						Wang600.Tape.tape_on(0);
					} else { // request for next byte
						int i = Wang600.Tape.tape_play();
						if (i < 0) {
							do_keycode(0x0e00);
						} else {
							do_keycode(0x0c00 | i);
						}
					}
				} else if (b[1] == 0x0f) { // tape on - write
					Wang600.Tape.tape_on(1);
				} else if (b[1] == 0x0e) { // tape off
					Wang600.Tape.tape_off(0);
				} else if (b[1] == 0x0c) { // tape write
					Wang600.Tape.tape_record(b[0]);
				} else {
					System.err.format("invalid tape command (%04x)\n", (b[1] << 8) | b[0]);
				}
			} else if ((b[1] & 0x0ff) == 0x7f) {
				_cn36 = null;
				if (Wang600.CN24 != null) {
					Wang600.CN24.reset();
				}
				Wang600.M630.reset();
				Wang_UI.resetCN36();
			} else if (b[1] == 0x10) {
				if (Wang600.CN24 != null) {
					Wang600.CN24.do_cn24(b[0]);
				}
			} else if ((b[1] & ~0x1f) == 0x20) { // IOB = 2,3
				// Random-access devices on CN-36
				// might need to support daisy-chained devices?
				if ((b[1] & 0x0f) == 1) { // ACK
					Wang600.M630.do_ack(b[1] >> 4);
				} else {
					Wang600.M630.do_dev(b[1] >> 4, b[0]);
				}
			} else if ((b[1] & ~0x1f) == 0x40) { // IOB = 4,5
				// Group 1 / Group 2 devices on CN-36
				// need to find which device "wants" this...
				// ACK, gets directed to the device "currently
				// in charge", as determined by (prior) start command.
				if (_cn36 != null) {
					// this should only happen for ACKs... (?)
					_cn36.do_ack(b[1] >> 4);
				} else {
					// find a device that "wants" this code
					_cn36 = Wang_UI.startCN36(b[1] >> 4, b[0]);
				}
			} else if ((b[1] & 0x80) != 0) {
				int x;

				// now, the back-end is waiting for us...
				// dump the whole ROM image...
				// back-end is expecting reversed image, but XROM
				// is reversing it already, so have to re-reverse.
				for (x = 0x7ff; x >= 0; --x) {
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

// Implements the Wang600 hardware. Does not provide any debug/trace support.

class Wang600_SimulatorJava
	implements Wang_Core
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	// CPU registers.
	// ucode accessible
	byte s;
	byte t;
	byte u;
	byte v;
	byte ca;
	byte cb;
	byte ka;
	byte kb;
	// internal hardware accessible
	byte l;
	byte m;
	byte n;
	byte rb;
	byte gioa;
	byte giob;
	byte iob;

	// status flags (1 bit)
	byte zo;
	byte cc;
	byte sc;
	byte kbd;
	byte ov;
	byte err;

	// ucode subroutine stack
	int stk1;
	int stk2;

	// simulator (no direct h/w relation)
	int jam;
	int next;
	int pc;
	int cycles;
	int cylimit; // debug, not used

	byte pr_drum;
	int pr_hammers;
	byte pr_tach;
	int pr_col;

	static final int D10_FP = 0x01;
	static final int D11_LST_L_P = 0x02;
	static final int D12_LRN_L_P = 0x04;
	static final int D13_STEP = 0x08;

	static final int D20_DEGREES = 0x01;
	static final int D21_PRT_ON = 0x02;

	byte[] _ram;

	private class Wang600_Ucode {
		public byte jl;
		public byte jh;
		public int jad;
		public byte sub;
		public byte st;
		public byte kk;
		public byte mop;
		public byte bc;
		public byte ac;
		public byte aop;
		public byte zo;
		public byte bi;
		public byte ai;

		public Wang600_Ucode(byte[] instr) {
			// "LE", i.e. "jl" in byte[0]
			jl = (byte)((instr[0] >> 2) & 0x07);
			jh = (byte)((instr[0] >> 5) & 0x07);
			jad = ((instr[1] & 0x00ff) | ((instr[2] & 1) << 8));
			sub = (byte)((instr[2] >> 1) & 1);
			st = (byte)((instr[2] >> 2) & 0x0f);
			kk = (byte)(((instr[2] >> 6) & 0x03) | ((instr[3] & 0x03) << 2));
			mop = (byte)((instr[3] >> 2) & 0x0f);
			bc = (byte)((instr[3] >> 6) & 1);
			ac = (byte)((instr[3] >> 7) & 1);
			aop = (byte)(instr[4] & 0x07);
			zo = (byte)((instr[4] >> 3) & 0x07);
			bi = (byte)(((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2));
			ai = (byte)((instr[5] >> 1) & 0x07);
		}
	}
	private class Wang600_UcodeRom {
		private byte[] _ucode; // raw ucode from file, 64-bit words
		// right now, the only override is for mem size, so just hardcode
		// all that.

		public Wang600_UcodeRom(File img, int memsize) {
			// Can't change _ucode after initial setup (i.e. while running).
			// Can't run if _ucode is null... need to check
			// (right now, will throw NULL pointer exception when fetching)
			// Enforce fixed-size 2048-word x 64-bit ucode.
			if (_ucode == null && img != null) {
				FileInputStream f;
				try {
					f = new FileInputStream(img);
				} catch (FileNotFoundException ee) {
					return;
				}
				int n = 0;
				byte[] buf = new byte[16384];
				try {
					n = f.read(buf);
				} catch (IOException ee) {
					n = -1;
				}
				try {
					f.close();
				} catch (IOException ee) {
				}
				if (n == 16384) {
					_ucode = buf;

					// patch mem-size override instruction...
					int idx = 0x008 * 8;
					byte kk = (byte)(((memsize - 1) >> 8) & 0x0f);
					_ucode[idx + 2] |= ((kk & 0x03) << 6);
					_ucode[idx + 3] |= ((kk >> 2) & 0x03);
				}
			}
		}

		public Wang600_Ucode fetch(int adr) {
			int idx = adr * 8;
			return new Wang600_Ucode(Arrays.copyOfRange(_ucode, idx, idx + 7));
//Wang600_Ucode u = new Wang600_Ucode(Arrays.copyOfRange(_ucode, idx, idx + 7));
//System.err.format("%03x: %d %d %03x\n", adr, u.jl, u.jh, u.jad << 2);
//return u;
		}
	}

	private Wang600_UcodeRom _rom;

	public void chgMode0() {
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode1() {
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void pressCmd(int cmd) {
		jam = 0x1000 | cmd;
		// needs other side-effects... display? clear key buffer?
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void ackIO(int iob) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		pressKey(0);
	}

	public void replyIO(int iob, int rep) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		pressKey(rep);
	}

	public void chgXROM() { }	// don't care

	java.util.concurrent.LinkedBlockingDeque<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
		// needs other side-effects... display?
	}

	boolean _canSleep;

	public Wang600_SimulatorJava() {
		// at some point, get these from properties...
		int memsize = 2048; // based on Model (2TP, 6TP, 14TP, ...)
		_rom = new Wang600_UcodeRom(new File("wang600.rom"), memsize);
		_ram = new byte[memsize];

		_canSleep = false;
		pr_drum = 0;
		pr_hammers = 0;
		pr_tach = 0;
		pr_col = 0;
		disp = new byte[32];	// compatable with Wang700's dual display
		odd_parity = new byte[] { 1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1 };
		keyCodes = new java.util.concurrent.LinkedBlockingDeque<Integer>();

		Thread t = new Thread(this);
		t.start();
	}

	private byte[] odd_parity;

	byte to_last;
	byte to_data;
	byte to_byte;
	int to_nibc;
	int to_bitc;
	int to_sigc;

	private void tape_write(int dat) {
		to_last <<= 1;
		to_last |= dat;
		to_sigc ^= 1;
		if (to_sigc != 0) return;
		byte bit = 0; 
		byte h = (byte)(to_last & 0x03);
		if (h == 0x02 || h == 0x01) bit = 1;
		if (++to_bitc == 5) {
			to_nibc ^= 1;
			if (to_nibc != 0) {
				to_byte = (byte)((to_byte & 0x0f) | (to_data << 4));
			} else {
				to_byte = (byte)((to_byte & 0xf0) | to_data);
				Wang600.Tape.tape_record(to_byte);
			}
			to_data = 0;
			to_bitc = 0;
		} else {
			to_data <<= 1;
			to_data |= bit;
		}
	}

	byte ti_last;
	int ti_data;
	int ti_bitc;
	int ti_sigc;
	long ti_repc;
	byte ti_bit;

	private int do_repc() {
		return ti_bit;
	}
	private int do_sigc() {
		--ti_sigc;
		ti_bit = (byte)(ti_last & 1);
		ti_last >>= 1;
		ti_repc = cycles + 97;	// very sensitive...
		return do_repc();
	}
	private int do_bitc() {
		--ti_bitc;
		ti_data <<= 1;
		if ((ti_data & 0x400) != 0) {
			ti_last = 0x05;	// lsb first out...
		} else {
			ti_last = 0x01;	// lsb first out...
		}
		ti_sigc = 4;
		return do_sigc();
	}

	private int tape_read() {
		// wait for TD 0->1
		// delay 56 cycles
		// wait 220 cycles (sample TD for end of loop)
		// [15,15,6] ^= DL	; compute parity?
		// CY = 0 - DL		; CY = bit0
		// [15,15,5] <<= 1	; make space
		// [15,15,5] += CY	; insert new bit
		// ACC += 1		; count bits
		// wait up to 256 cycles for TD 0->1
		//         __    __
		// "1" = _|  |__|  |_
		//         __
		// "0" = _|  |_______
		//
		if (cycles < ti_repc) {
			return do_repc();
		}
		if (ti_sigc > 0) {
			return do_sigc();
		}
		if (ti_bitc > 0) {
			return do_bitc();
		}
		int ti = Wang600.Tape.tape_play();
		if (ti < 0) { // EOF
			ti_repc = cycles + 700;	// expects at least 650?
			ti_bit = 0;
			return do_repc();
		}
		int x = ((ti >> 4) & 0x0f);
		ti_data = (x << 1) | odd_parity[x];
		x = (ti & 0x0f);
		ti_data <<= 5;
		ti_data |= (x << 1) | odd_parity[x];
		ti_bitc = 10;
		return do_bitc();
	}

	private void tape_on(int wr) {
		Wang600.Tape.tape_on(wr);
		if (wr == 0) {
			ti_bit = 0;
			ti_last = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_repc = 0;
		}
	}

	private void tape_off() {
		Wang600.Tape.tape_off(0);
	}

	private void dev_out() {
		byte c = (byte)((ka << 4) | kb);
		if (iob == 0) {
			_cn36 = null;
			if (Wang600.CN24 != null) {
				Wang600.CN24.reset();
			}
			Wang600.M630.reset();
			Wang_UI.resetCN36();
		} else if (iob == 1) { // CN24 output only, 6 bits
			c &= 0x3f;
			if (Wang600.CN24 != null) {
				Wang600.CN24.do_cn24(c);
			}
		} else if (iob == 2 || iob == 3) { // CN36 Model 630
			Wang600.M630.do_dev(iob, c);
		} else if (iob == 4 || iob == 5) { // CN36 Group 1/2 devices
			if (_cn36 != null) {
				// All known devices are ACK only
				_cn36.do_ack(iob);
			} else {
				_cn36 = Wang_UI.startCN36(iob, c);
			}
		}
	}

	private byte add3_i(byte a, byte b, byte c) {
		byte s = (byte)(a + b + c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte sub3_i(byte a, byte b, byte c) {
		byte s = (byte)(a - b - c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte and2(byte a, byte b) {
		byte s = (byte)(a & b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte or2(byte a, byte b) {
		byte s = (byte)(a | b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte xor2(byte a, byte b) {
		byte s = (byte)(a ^ b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte add3_c(byte a, byte b, byte c) {
		byte s = add3_i(a, b, c);
		sc = cc;
		return s;
	}

	private byte sub3_c(byte a, byte b, byte c) {
		byte s = sub3_i(a, b, c);
		sc = cc;
		return s;
	}

	private void printer_status() {
		// we don't want to do this unless it is really the
		// drum printer we're looking at... can't tell?
		if ((Wang600.Kbd.getMode1() & D21_PRT_ON) == 0) {
			// only if running program doesn't get here...
			// printer is off, tach will never pulse, so don't spin
			if (pc == 0x6db) {
				// sleep until key event... incl PRT ON...
			}
			return;
		}
		if (pr_tach != 0) {
			pr_col = 0;
			pr_drum = (byte)((pr_drum + 1) & 0x0f);
			pr_hammers = 0;
		}
		pr_tach ^= 0x08;
		ka = pr_drum;
		kb = pr_tach;
	}

	private void printer_hammers() {
		int x;
		int h;

		pr_hammers <<= 1;
		pr_hammers &= 0x0fffff;
		pr_hammers |= kb & 1;
		if (++pr_col >= 20) {
			h = pr_hammers;
			for (x = 0; h != 0; ++x) {
				if ((h & 1) != 0) {
					Wang600.Prt.do_printer(x, pr_drum);
				}
				h >>= 1;
			}
			pr_col = 0;
		}
	}

	private void printer_feed() {
		// now, actually print it...
		Wang600.Prt.do_line();
	}

	private void rd_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		//adr &= ram_mask;
		byte b = _ram[adr >> 1];
		if ((adr & 1) != 0) {
			b >>= 4;
		}
		rb = ca = (byte)(b & 0x0f);
		b = Wang600.XROM.getByte(adr >> 1);
		if ((adr & 1) != 0) {
			b >>= 4;
		}
		cb = (byte)(b & 0x0f);
//if (adr < 0x7b8) System.err.format("%03x: %02x %02x\n", adr, ca, cb);
	}

	private void wr_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		//adr &= ram_mask;
		byte a = ca;
		byte b = _ram[adr >> 1];
		if ((adr & 1) != 0) {
			a <<= 4;
			b &= 0x0f;
		} else {
			b &= 0xf0;
		}
		_ram[adr >> 1] = (byte)(b | a);
	}

	private byte[] disp;
	int good;
	int lastx;

	// CN-36 "Input" devices (Group 1/2 I/O Protocol)
	private Wang_InputDevice _cn36;	// current active device

	private void refresh() {
		byte x = (byte)((n << 4) | rb);
		if (disp[n * 2 + 0] != x) {
			disp[n * 2 + 0] = x;
			good = 0;
		}
		if (++lastx >= 16) {
			lastx = 0;
			++good;
			Wang600.Disp.do_display(disp);
		}
	}

	private void display_check() {
		if (pc == 0x252) {
		}
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if (pc == 0x51c) {	// display refresh routine...
			next = 0x51f;	// update some regs too?
			cycles += 272;
			refresh();
			if (good > 4) {
				// OK to sleep now... but only have take() to
				// wait for key press. So, have to signal the
				// next ucode instruction to test keyboard
				// that it should sleep.
//				_canSleep = true;
				int k = -1;
				try {
					k = keyCodes.take();
				} catch(Exception ee) {
					k = -1;
				}
				if (k >= 0) {
					keyCodes.addFirst(k);
				}
				good = 0;
			}
		// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
		} else if (pc == 0x5c0) {	// alpha-stop refresh routine...
			next = 0x5c3;
			cycles += 272;
			refresh();
		} else if (pc == 0x5c6) {	// alpha-stop done... "return"...
			if (next == 0x27b) { // alpha-stop in running program...
				// observed 211975 cycles or about 0.53 second
				try {
					Thread.sleep(530);
				} catch(Exception ee) {
				}
			}
			Wang600.Disp.do_blanking();
		}
	}

	private int instr_exec() {
		Wang600_Ucode uu = _rom.fetch(pc);
		int nxt;
		int rc = 0;

		// F==7 && J==0:
		//	PC <= STK1, STK1 <= PC, STK2 <= STK1
		//
		// F==7 && J==1:
		//	PC <= STK1, STK1 <= STK2, STK2 <= STK1
		//
		// F!=7 && J==0:
		//	PC <= NEXT**
		//
		// F!=7 && J==1:
		//	STK2 = STK1, STK1 <= PC, PC <= NEXT**
		//
		// For conditional jump/call, these bits are latched early...
		byte br_acc = s;
		byte br_c = sc;
		byte br_k = uu.kk;
		int opf7 = (uu.jl == 7 ? 1 : 0);
		if (opf7 != 0) {
			nxt = stk1 | 1;
			if (uu.sub != 0) {
				stk1 = stk2;
			} else {
				stk1 = stk2; // bugfix?
				//stk1 = pc;	// bad?
				// rc = 1;
			}
		} else {
			nxt = uu.jad << 2;
		}

		if (uu.mop >= 1 && uu.mop <= 6) {
			l = t;
			m = u;
			n = v;
		}

		byte g = 0;
		byte h = 0;
		switch(uu.ai) {
		case 0: h = s; break;
		case 1: h = t; break;
		case 2: h = u; break;
		case 3: h = v; break;
		case 4: h = ka; break;
		case 5: h = kb; break;
		case 6: h = ca; break;
		case 7: h = cb; break;
		}

		switch(uu.bi) {
		case 0: g = 0; break;
		case 1: g = br_k; break;
		case 2:
			g = (byte)Wang600.Kbd.getMode0();
			// clear 0010 if glrn?
			if (_cn36 != null) {
				g |= (byte)((_cn36.getGLRN() & 1) << 2);
			}
			break;
		case 3: g = (byte)(Wang600.Kbd.getMode1() ^ D20_DEGREES); break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		if (uu.ac == 0) h = 0; // "15"? "0"? ???
		switch (uu.aop) {
		case 0:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)0);
			else alu = add3_i(h, g, (byte)0);
			break;
		case 1:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)1);
			else alu = add3_i(h, g, (byte)1);
			break;
		case 2:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)0);
			else alu = add3_c(h, g, (byte)0);
			break;
		case 3:
			if (uu.bc != 0) alu = sub3_c(h, g, sc);
			else alu = add3_c(h, g, sc);
			break;
		case 4:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)1);
			else alu = add3_c(h, g, (byte)1);
			break;
		case 5:
			alu = and2(h, g);
			break;
		case 6:
			if (uu.bc != 0) alu = xor2(h, g);
			else alu = or2(h, g);
			break;
		case 7:
			// alu = 0;
			break;
		}

		switch(uu.zo) {
		case 0:	if (uu.st == 15) s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		case 4:	ka = alu; break;
		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		}

		switch(uu.st) {
		case 0:
			// nop
			break;
		case 1:
			s |= 1;
			break;
		case 2:
			s |= 2;
			break;
		case 3:
			s |= 4;
			break;
		case 4:
			s |= 8;
			break;
		case 5:
			s &= ~1;
			break;
		case 6:
			s &= ~2;
			break;
		case 7:
			s &= ~4;
			break;
		case 8:
			s &= ~8;
			break;
		case 9:
			// T.B.D. reset 6184...
	//fprintf(stderr, "%03x: res (%04x)\n", pc, key);
			kbd = 0;
			break;
		case 10:
			s = (byte)((s & 0x0e) | (zo ^ 1));
			break;
		case 11:
			s = (byte)((s & 0x0d) | (zo << 1));
			break;
		case 12:
			ov = 1;
			Wang600.Disp.setOv(ov);
			break;
		case 13:
			s = 0;
			break;
		case 14:
			err = 1;
			Wang600.Disp.setErr(err);
			break;
		}

		switch(uu.mop) {
		case 1:	wr_ram_i(l, m, n); break;
		case 2:	wr_ram_i((byte)15, br_k, n); break;
		case 3:	wr_ram_i((byte)15, (byte)15, br_k); break;
		case 4:	rd_ram_i(l, m, n); break;
		case 5:	rd_ram_i((byte)15, br_k, n); break;
		case 6:	rd_ram_i((byte)15, (byte)15, br_k); break;
		case 7:	printer_hammers(); break;
		case 8:	printer_feed(); break;
		case 9:	rc = 2; break;
		case 10:
			kb = (byte)((kb & ~1) | tape_read());
			break;
		case 11:
			tape_write(kb & 1);
			break;
		case 12:
			printer_status();
			// not just printer, but CN-24 as well...
			kb |= 2;
			break;
		case 13:
			tape_on(uu.bi & 1);
			break;
		case 14:
			tape_off(); // uu.bi & 1 affects this...
			break;
		case 15:
			gioa = ka;	// gioa = g;
			giob = kb;	// giob = h;
			iob = (byte)(br_k & 0x07);
			dev_out();
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		if (opf7 == 0) {
			if (uu.sub != 0) {
				stk2 = stk1;
				stk1 = pc;
			}
			switch(uu.jh) {
			case 0: nxt |= (0 << 1); break;
			case 1: nxt |= (1 << 1); break;
			case 2: nxt |= ((br_acc & 2) >> 0); break;
			case 3: nxt |= ((br_acc & 8) >> 2); break;
			case 4:
				nxt |= (ov << 1);
				ov = 0;
				Wang600.Disp.setOv(ov);
				break;
			case 5: nxt |= (cc << 1); break;
			case 6:
				int key = -1;
				if (_canSleep) {
					_canSleep = false;
					try {
						// problem: need to also wake on special
						key = keyCodes.take();
					} catch(Exception ee) {
						key = -1;
					}
					good = 0;
				} else if (keyCodes.size() > 0) {
					key = keyCodes.remove();
				}
				if (key >= 0) {
//fprintf(stderr,"%03x: chk pe\n", pc, key);
//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
					kbd = 1;
					ka = (byte)((key >> 4) & 0x0f);
					kb = (byte)(key & 0x0f);
					if ((iob & ~1) == 2) {
						Wang600.M630.do_ack(iob);
					} else if (_cn36 != null) {
						_cn36.do_ack(iob);
					}
				}
				nxt |= (kbd << 1);
				if (kbd != 0) {
					good = 0;
					kbd = 0;
					Wang600.Disp.do_blanking();
				}
				break;
			case 7: rc = 3; break;
			}
			switch(uu.jl) {
			case 0: nxt |= (0 << 0); break;
			case 1: nxt |= (1 << 0); break;
			case 2: nxt |= ((br_acc & 1) >> 0); break;
			case 3: nxt |= ((br_acc & 4) >> 2); break;
			case 4: nxt |= (zo << 0); break;
			case 5: rc = 4; break;
			case 6: nxt |= (br_c << 0); break;
			case 7: rc = 5; break;
			}
		}

		++cycles;
		next = nxt;
		// the following are called in specific order...
		// keyboard injection of next pc must override all, so is last.

		display_check();	// this might sleep until UI event...

		//sys->keyboard(sys, &key, 0);

		if (jam != 0) {
			next = jam & 0x0fff;
			jam = 0;
			ov = 0;
			Wang600.Disp.setOv(ov);
			if (next == 0) { // PRIME
				err = 0;
				Wang600.Disp.setErr(err);
			}
		}

		pc = next;
		return rc;
	}

	public void run() {
		// Run the simulator...
		int rc = 0;
		while (rc == 0) {
			rc = instr_exec();
		}
		Wang_UI.fatal("Wang600 Core", "Simulation error");
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
		implements WindowListener, ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";

	private JMenuItem _mi601;
	private JMenuItem _mi602;
	private JMenuItem _mi611;
	private JMenuItem _mi612;
	private JMenuItem _miNone;
	private JMenu _mu;
	public JMenu getOutputMenu() { return _mu; }

	private void disposeDevice() {
		if (Wang600.CN24 instanceof Wang_Plotter) {
			_mi612.setText(Wang_Plotter.getName() +
				" (not installed)");
			Wang600.CN24.onOff(false);
		} else if (Wang600.CN24 instanceof Wang_OutputWriter) {
			_mi601.setText(Wang_OutputWriter.getName() +
				" (not installed)");
			Wang600.CN24.onOff(false);
		} else if (Wang600.CN24 instanceof Wang_PlottingOutputWriter) {
			_mi602.setText(Wang_PlottingOutputWriter.getName() +
				" (not installed)");
			Wang600.CN24.onOff(false);
		} else if (Wang600.CN24 instanceof Wang_InputOutputWriter) {
			_mi611.setText(Wang_InputOutputWriter.getName() +
				" (not installed)");
			Wang600.CN24.onOff(false);
		}
	}

	private void setupDevice() {
		Wang600.CN24.getFrame().addWindowListener(this);
		String model = "";
		if (Wang600.CN24 instanceof Wang_Plotter) {
			model = Wang_Plotter.getModel();
		} else if (Wang600.CN24 instanceof Wang_OutputWriter) {
			model = Wang_OutputWriter.getModel();
		} else if (Wang600.CN24 instanceof Wang_PlottingOutputWriter) {
			model = Wang_PlottingOutputWriter.getModel();
		} else if (Wang600.CN24 instanceof Wang_InputOutputWriter) {
			model = Wang_InputOutputWriter.getModel();
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang600_Properties(),
				"wang600_cn24_device",
				model);
		} catch(Exception ee) {}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_1) {
			if (!(Wang600.CN24 instanceof Wang_OutputWriter)) {
				disposeDevice();
				Wang600.CN24 = new Wang_OutputWriter();
				_mi601.setText(Wang_OutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang600.CN24.onOff(!Wang600.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_2) {
			if (!(Wang600.CN24 instanceof Wang_PlottingOutputWriter)) {
				disposeDevice();
				Wang600.CN24 = new Wang_PlottingOutputWriter();
				_mi602.setText(Wang_PlottingOutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang600.CN24.onOff(!Wang600.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_3) {
			if (!(Wang600.CN24 instanceof Wang_InputOutputWriter)) {
				disposeDevice();
				Wang600.CN24 = new Wang_InputOutputWriter();
				_mi611.setText(Wang_InputOutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang600.CN24.onOff(!Wang600.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_4) {
			if (!(Wang600.CN24 instanceof Wang_Plotter)) {
				disposeDevice();
				Wang600.CN24 = new Wang_Plotter();
				_mi612.setText(Wang_Plotter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang600.CN24.onOff(!Wang600.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_0) {
			disposeDevice();
			try { // if this fails, oh well.
				Wang_UI.getProperties().setAndSaveProperty(
					new Wang600_Properties(),
					"wang600_cn24_device",
					"");
			} catch(Exception ee) {}
			Wang600.CN24 = null;
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_D) {
			Wang600.M630.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_R) {
			Wang600.XROM.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_COPY) {
			Wang600.Disp.copy();
			return;
		}
		// note: potential conflicts with Devices and Help menus...
		if (m.getMnemonic() == KeyEvent.VK_H) {
			Wang600.Help.toggle();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_A) {
			Wang600.Help.showAbout();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_E) {
			Wang600_Properties props = (Wang600_Properties)Wang_UI.getProperties();
			boolean changed = props.editPreferences();
			if (changed) {
				// Apply properties...
				Wang_UI.setDir(props.getProperty("wang600_home"));
				Wang600.Disp.setProperties(props);
				// <others>.setProperties(props);
			}
			return;
		}
	}

	public Wang600_SimInput() {
		if (Wang600.CN24 != null) {
			Wang600.CN24.getFrame().addWindowListener(this);
		}

		_mu = new JMenu("Output Device...");
		// todo: make this a radio-button sub-menu
		String status = " (not installed)";
		if (Wang600.CN24 instanceof Wang_OutputWriter) status = " (installed)";
		_mi601 = new JMenuItem(Wang_OutputWriter.getName() + status,
					KeyEvent.VK_1);
		_mi601.addActionListener(this);
		_mu.add(_mi601);
		status = " (not installed)";
		if (Wang600.CN24 instanceof Wang_PlottingOutputWriter) status = " (installed)";
		_mi602 = new JMenuItem(Wang_PlottingOutputWriter.getName() + status,
					KeyEvent.VK_2);
		_mi602.addActionListener(this);
		_mu.add(_mi602);
		status = " (not installed)";
		if (Wang600.CN24 instanceof Wang_InputOutputWriter) status = " (installed)";
		_mi611 = new JMenuItem(Wang_InputOutputWriter.getName() + status,
					KeyEvent.VK_3);
		_mi611.addActionListener(this);
		_mu.add(_mi611);
		status = " (not installed)";
		if (Wang600.CN24 instanceof Wang_Plotter) status = " (installed)";
		_mi612 = new JMenuItem(Wang_Plotter.getName() + status,
					KeyEvent.VK_4);
		_mi612.addActionListener(this);
		_mu.add(_mi612);
		_miNone = new JMenuItem("None",
					KeyEvent.VK_0);
		_miNone.addActionListener(this);
		_mu.add(_miNone);

		if (Wang_UI.getFin() == null) { // && getFout() == null
			Wang600.Core = new Wang600_SimulatorJava();
		} else {
			Wang600.Core = new Wang600_SimulatorPipe();
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (Wang600.CN24 != null && e.getWindow() == Wang600.CN24.getFrame()) {
			Wang600.CN24.onOff(false);
			return;
		}
	}
}

class Wang600_Printer
	implements Wang_Printer, ActionListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
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
		//_scroll.getViewport().setBackground(Wang_Colors.empty);
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
					"lst", "Wang list files", Wang_UI.getDir());
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
					Wang_UI.getTimestamp().format(dt));
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
	public void do_printer(int col, int drm) {
		_pr_buf[col] = (byte)drm;
	}

	public void do_line() {
		int x, y, z;
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
	}
}

class Wang600_Model630 implements Wang_CN36_Type2 {
	private int _cmd;
	private int _adr;
	private boolean _wr;
	private int _len;
	private int _idx;
	java.io.RandomAccessFile _f;
	File _file;
	byte[] _buf;

	public JMenuItem getMenu(int key) {
		String status = "not mounted";
		if (_file != null) {
			status = _file.getName();
		}
		return new JMenuItem("630 Disk - " + status, key);
	}

	public Wang600_Model630() {
		reset();
		_buf = new byte[256]; // largest transfer
		_file = Wang_UI.getProperties().getFile("wang600_630_image",
							true, Wang_UI.getDir());
		if (_file != null) {
			disk_open();
		}
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
					Wang_UI.getProperties().getProperty("wang600_disk_file_suffix"),
					"Wang disk image files", Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(Wang600.Kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			m.setText("630 Disk - " + _file.getName());
		} else {
			_file = null;
			m.setText("630 Disk - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang600_Properties(),
				"wang600_630_image",
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}

		disk_open();
	}

	public void do_ack(int iob) {
		// only respond to ACK if in a command already
		// and don't respond to an ACK of an ACK
		if (_cmd > 4 && !_wr) {
			do_dev(iob, 0);
		}
	}

	public void do_dev(int iob, int c) {
		int res;
		++_cmd;
//System.err.println("dev 2 ["+_cmd+"] "+c);
		boolean dat = ((iob & 1) != 0);
		if (_cmd <= 4 && dat || _cmd > 4 && !dat) {
System.err.println("sync error");
			return;
		}
//try{
// Thread.sleep(50);
//}
//catch(InterruptedException ie){
//}
		if (_cmd < 4) {
			_adr <<= 8;
			_adr |= (c & 0x00ff);
			Wang600.Core.ackIO(iob);
		} else if (_cmd == 4) {
			_wr = ((c & 0x80) != 0);
			_len = (c & 0x7f);
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
				Wang600.Core.replyIO(iob, res);
//System.err.println("rd result "+res+" ("+_len+")");
			} else {
				Wang600.Core.ackIO(iob);
			}
		} else {
			if (_idx < _len) {
				if (_wr) {
					_buf[_idx] = (byte)c;
					Wang600.Core.ackIO(iob);
				} else {
					Wang600.Core.replyIO(iob, _buf[_idx]);
				}
			} else {
				if (_wr) {
					res = disk_write(_len);
					Wang600.Core.replyIO(iob, res);
				} else {
					Wang600.Core.ackIO(iob);
				}
				_cmd = 0;
//System.err.println("result "+res+" ("+_idx+")");
			}
			++_idx;
		}
//System.err.printf("got %02x%02x put %04x\n", iob, c, bb);
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

class Wang600_XROM implements Wang_XROM {
	File _file;
	byte[] _xrom;

	public JMenuItem getMenu(int key) {
		String status = "none installed";
		if (_file != null) {
			status = _file.getName();
		}
		return new JMenuItem("Expansion ROM - " + status, key);
	}

	private void loadXROM(File img) {
		_xrom = null;
		if (img != null) {
			FileInputStream f;
			try {
				f = new FileInputStream(img);
			} catch (FileNotFoundException ee) {
				return;
			}
			int n = 0;
			byte[] buf = new byte[2048];
			try {
				n = f.read(buf);
			} catch (IOException ee) {
				n = -1;
			}
			try {
				f.close();
			} catch (IOException ee) {
			}
			if (n > 0) {
				_xrom = buf;
				// inform back-end we have a new image...
				// this is only done if not running embedded sim
				Wang600.Core.chgXROM();
			}
		}
	}

	public Wang600_XROM() {
		_file = Wang_UI.getProperties().getFile("wang600_rom_image", true, Wang_UI.getDir());
	}

	public void Initialize() {
		loadXROM(_file);
	}

	public byte getByte(int adr) {
		if (_xrom != null && _xrom.length > adr) {
			// ROM image needs to be reversed... do it here
			// (but some users need to reverse it back...)
			return _xrom[0x7ff - adr];
		} else {
			return (byte)0xff;
		}
	}

	public void pickFile(JMenuItem m) {
		SuffFileChooser ch = new SuffFileChooser("Install",
					Wang_UI.getProperties().getProperty("wang600_rom_file_suffix"),
					"Wang ROM image files", Wang_UI.getDir());
		File file = Wang_UI.getProperties().getFile("wang600_rom_image",
							true, Wang_UI.getDir());
		if (file != null) {
			ch.setSelectedFile(file);
		}
		int rv = ch.showDialog(Wang600.Kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
			// are we being too optimistic? maybe wait until
			// download succeeds?
			m.setText("Expansion ROM - " + _file.getName());
		} else {
			file = null;
			m.setText("Expansion ROM - none installed");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang600_Properties(),
				"wang600_rom_image",
				file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		// NOTE: on real hardware, you can't change ROMs without
		// risking severe damage to ROM cartridge or calculator!
		// We could just save the property and wait for restart?
		loadXROM(file);
	}
}

class Wang600_Display extends Wang_Display
		implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','B','C','D','E',' '};

	byte[] disp_a;
	byte[] disp_b;
	JLabel disp;
	boolean _d12;	// is digit 12 enabled?

	private Wang_ErrorLight pe;
	private Wang_ErrorLight me;
	private boolean flashing;
	private boolean state;
	private javax.swing.Timer timer;

	public Wang_ErrorLight getOv() { return pe; }
	public Wang_ErrorLight getErr() { return me; }

	private void flasher() {
		if (!flashing) {
			state = false;
			disp.setForeground(Wang_Colors.neon);
			return;
		}
		state = !state;
		if (state) {
			disp.setForeground(Wang_Colors.neon2);
		} else {
			disp.setForeground(Wang_Colors.neon);
		}
	}

	private static void setClipboard(String str) {
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	public void copy() {
		// e.g. "+0.0000000000+00"
		// e.g. "+0.0000000000   "
		String s = disp.getText();
		String e = null;
		s = s.replaceAll(" ","");
		s = s.replaceAll("\005","1");	// special "1"
		s = s.replace("\006",".");	// special "."
		if (s.length() > 13) {
			e = s.substring(13); // keep "+"
			if (e.equals("+00")) e = null;
			s = s.substring(0,13);
		}
		s = s.replaceAll("0*$", ""); // cut trailing zeroes
		if (s.length() == 0) s = "0";
		s = s.replaceAll("\\.$", ""); // cut trailing decimalpoint
		s = s.replaceAll("^\\+", ""); // cut + sign
		if (e != null) {
			s = s + "e" + e;
		}
		setClipboard(s);
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

	public Wang600_Display() {
		String blank = "--- Wang 600 ---";
		disp_a = new byte[16];
		disp_b = new byte[32];	// replaced before used?
		flashing = false;
		state = false;
		timer = new Timer(100, this);

		setLayout(new FlowLayout());
		disp = new JLabel(blank, SwingConstants.CENTER);
		disp.setForeground(Wang_Colors.neon);
		disp.setBackground(Wang_Colors.empty);
		disp.setOpaque(true);
		disp.setPreferredSize(new Dimension(475, 75));
		// font setup later... in setProperties()...

		add(disp);

		pe = new Wang_ErrorLight("Prog<BR>Error");
		pe.setOn(false);
		me = new Wang_ErrorLight("Mach<BR>Error");
		me.setOn(false);

	}

	public void setProperties(Wang_Properties p) {
		Wang600_Properties prop = (Wang600_Properties)p;
		// TODO: reconfig/redraw display...
		String f = prop.getProperty("wang600_displayfont");
		Font font = null;
		java.io.InputStream ttf = this.getClass().getResourceAsStream(f);
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
System.err.println("FontFormatException for " + f);
			} catch (IOException ee) {
System.err.println("IOException for " + f);
			}
			font = font.deriveFont(40f);
			// special decimal point, optimal placement...
			if (prop.getBoolean("wang600_centerDP") && font.canDisplay('\006')) {
				disp_chr[10] = '\006';
			} else  {
				disp_chr[10] = '.';
			}
			// special one digit, optimal placement...
			// should this also be a preference?
			if (prop.getBoolean("wang600_special1") && font.canDisplay('\005')) {
				disp_chr[1] = '\005';
			} else {
				disp_chr[1] = '1';
			}
		}
		if (font == null) {
			System.err.println("Missing font \"" +
					f + "\", using default");
			font = new Font("Monospaced", Font.PLAIN, 40);
		}
		disp.setFont(font);
		_d12 = prop.getBoolean("wang600_digit12");
		do_display(disp_b);
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

	public void setErr(byte on) {
		if (on != 0) {
			me.setOn(true);
			setFlashing(true);
		} else {
			me.setOn(false);
			if (!pe.isOn()) {
				setFlashing(false);
			}
		}
	}

	public void setOv(byte on) {
		if (on != 0) {
			pe.setOn(true);
			setFlashing(true);
		} else {
			pe.setOn(false);
			if (!me.isOn()) {
				setFlashing(false);
			}
		}
	}

	public void do_blanking() {
		// blank-out display while Wang is not refreshing...
		String s = new String("                ");
		disp.setText(s);
		repaint();
	}

	public void do_display(byte[] b) {
		// b[*] is actually more like uint8_t b[16][2]!
		int ds;
		disp_b = b;
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
		if (!_d12) {
			disp_a[12] = ' ';
		}

		String s = new String(disp_a);
		disp.setText(s);
		repaint();
	}
}

class Wang600_Keyboard extends Wang_Keyboard
	implements ActionListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
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
	int _mode0r;
	int _mode1;
	int _defreg;

	public int getMode0() {
		int code = _mode0;
		_mode0 &= ~_mode0r; // STEP is reset on read
		return code;
	}
	public int getMode1() { return _mode1; }

	private void setShift(boolean _new) {
		_shift = _new;
		if (_shift) {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(Wang_Colors.illum1);
		} else {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_kbds[_shift_kbd]._keys[_shift_btn].color);
		}
	}

	private void setDefReg(int _new) {
		_kbds[_meta_kbd]._buttons[_defreg].setBackground(Wang_Colors.white1);
		_defreg = _new & 0x0f;
		_kbds[_meta_kbd]._buttons[_defreg].setBackground(Wang_Colors.white3);
	}

	private void setToggle(boolean on, Wang_Keys key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == Wang_Keys.METAP) {
			_meta &= ~key.getMask();
		} else if (key.getType() == Wang_Keys.METAS) {
			_metas &= ~key.getMask();
		} else if (key.getType() == Wang_Keys.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == Wang_Keys.MODE1) {
			_mode1 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == Wang_Keys.METAP) {
				_meta |= key.getMode();
			} else if (key.getType() == Wang_Keys.METAS) {
				_metas |= key.getMode();
			} else if (key.getType() == Wang_Keys.MODE0) {
				_mode0 |= key.getMode();
			} else if (key.getType() == Wang_Keys.MODE1) {
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

	private void do_button(boolean shifted, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if (_kbds[y]._keys[x].isSHIFT()) {
			if (!shifted) setShift(!_shift);
			return;
		}
		if (_kbds[y]._keys[x].isFEED()) {
			Wang600.Prt.feed();
			return;
		}
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x].isTAPE()) {
			set_group(g, y, x);
			boolean st = Wang600.Tape.do_button(_kbds[y]._keys[x]);
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
		if (type == Wang_Keys.MODE0) {
			if (g == 0) {
				// did not previously update things...
				// not a toggle... Reading must clear it...
				_mode0 |= _kbds[y]._keys[x].getMode();
				_mode0r |= _kbds[y]._keys[x].getMode();
			}
			Wang600.Core.chgMode0();
			return;
		}
		if (type == Wang_Keys.SPCL) {
			if (_shift) {
				code += 4;
			}
			Wang600.Core.pressCmd(code);
			return;
		}
		if (type == Wang_Keys.MODE1) {
			boolean on = ((_mode1 & 2) != 0);
			Wang600.Prt.onOff(on);
			Wang600.Core.chgMode1();
			return;
		}
		// From here on, generate a "normal" keyboard code...
		if (type == 0 && _shift && (code & 0x0f0) == 0x080) {
			code |= 0x010;
		}
		if (type == Wang_Keys.META) {
			code &= 0x00f;
			code |= ((_meta | _metas) << 4);
			if (_shift) {
				code |= 0x010;
			}
		}
		if (!shifted) setShift(false);

		Wang600.Core.pressKey(code);
	}

	JFrame _frame;
	JScrollPane _scroll;
	int _xoff, _yoff;

	public Wang600_Keyboard() {
		int x;
		_kbds = new Wang600_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_shift = false;
		_meta = 0;
		_metas = 0;
		_defreg = 15;

		Wang600.Prt.getFrame().addWindowListener(this);

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
			if (kbd._keys[x].code == Wang_Keys.GROUP(6,Wang_Keys.MODE1_CHG(2,2))) {
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

		kbd = new Wang600_Keyboard_meta();
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
			if (kbd._keys[x].code == Wang_Keys.SHIFT) {
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

	private static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String)t.getTransferData(DataFlavor.stringFlavor);
				return text;
			}
		} catch (UnsupportedFlavorException e) {
		} catch (IOException e) {
		}
		return null;
	}

	private void paste() {
		String s = getClipboard();
		// even strip off trailing manitissa '0'...
		try {
			Double d = Double.valueOf(s);
			// format(%.12g) ensures no more than 12 digits.
			// however, decimal point makes 13, if present...
			// and will cause error.
			// The exponent will not error-out on overflow.
			s = String.format("%.12g", d);
			s = s.replaceAll("^0", ""); // trim leading zero...
			int i = s.indexOf('e');
			if (i < 0 && s.length() > 12) {
				s = s.substring(0, 12);
			} else if (i > 12) {
				s = s.substring(0, 12) + s.substring(i);
			}
		} catch (NumberFormatException e) {
			// give some indication
			s = "";
		}
		// Let Wang600_Core buffer up input...
		char[] keys = s.toCharArray();
		int x = 0;
		do_key('\b');
		while (x < keys.length) {
			char c = keys[x++];
			do_key(c);
		}
	}

	public void keyTyped(KeyEvent e) {
//System.err.println("key pressed "+e.getKeyCode()+" "+e.getKeyChar());
if (e.isActionKey()) {
System.err.println("action");
}
		char c = e.getKeyChar();
		do_key(c);
	}

	private void do_key(char c) {
		if (c >= '0' && c <= '9') {
			Wang600.Core.pressKey(c - '0');
		} else if (c == 'e' || c == 'E') {
			Wang600.Core.pressKey(11);
		} else if (c == '.') {
			Wang600.Core.pressKey(10);
		} else if (c == '-') {
			Wang600.Core.pressKey(12);
		} else if (c == '\b') {
			Wang600.Core.pressKey(15);
		} else if (c == 't' || c == 'T') {
			Wang600.Core.pressKey(0x0010 | _defreg);
		} else if (c == '+') {
			Wang600.Core.pressKey(0x0020 | _defreg);
		} else if (c == '_') {
			Wang600.Core.pressKey(0x0030 | _defreg);
		} else if (c == '*') {
			Wang600.Core.pressKey(0x0040 | _defreg);
		} else if (c == '/') {
			Wang600.Core.pressKey(0x0050 | _defreg);
		} else if (c == 's' || c == 'S') {
			Wang600.Core.pressKey(0x0060 | _defreg);
		} else if (c == 'r' || c == 'R') {
			Wang600.Core.pressKey(0x0070 | _defreg);
		} else if (c == 'i' || c == 'I') {
			Wang600.Core.pressKey(0x00fb);
		} else if (c == 'x' || c == 'X') {
			Wang600.Core.pressKey(0x00e0 | _defreg);
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
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_PASTE) {
				paste();
				return;
			}
			System.err.println("Unknown menu event on keyboard");
			return;
		}
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
		if (e.getWindow() == Wang600.Prt.getFrame()) {
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
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang600_Keyboards() { }

	int _nkeys;
	Wang_Keys[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py,
						String icon, Wang_Keys key) {
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
				String toplab, String botlab, Color alt, boolean init, Wang_Keys key) {
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
				String toplab, Color alt, Wang_Keys key) {
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
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 11));
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

class Wang600_Help extends JComponent
	implements Wang_Help, ActionListener, WindowListener, ComponentListener, HyperlinkListener
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

	public Wang600_Help(JFrame frame) {
		_main = frame;
		_help = new JMenuItem("Show Help", KeyEvent.VK_H);;
		_about = new JMenuItem("About", KeyEvent.VK_A);
		_help_on = false;

		java.net.URL url = this.getClass().getResource("docs/wang600.html");
		_frame = new JFrame("Wang 600 Help");
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (Exception ee) {
			Wang_UI.fatal("Help Setup", ee.getMessage());
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

		_frame.addWindowListener(this);
		_frame.addComponentListener(this);

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
	}

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang600.gif");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang 600 Advanced Programmable Calculator<BR>"+
			"Simulator<BR>"+
			"$Revision: 1.158 $ $Date: 2013/11/11 23:02:50 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"<BR>"+
			"With Rick Bensene<BR>"+
			"http://www.oldcalculatormuseum.com/wang600.html<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(_main, lab,
			"About: Wang 600 Simulator", JOptionPane.PLAIN_MESSAGE);
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
				url = this.getClass().getResource("docs/wang600.html");
			} else if (m.getMnemonic() == KeyEvent.VK_U) {
				url = this.getClass().getResource("docs/wang600calc.html");
			} else if (m.getMnemonic() == KeyEvent.VK_D) {
				url = this.getClass().getResource("docs/wang600tape.html");
			} else if (m.getMnemonic() == KeyEvent.VK_A) {
				url = this.getClass().getResource("docs/wang600samp.html");
			} else if (m.getMnemonic() == KeyEvent.VK_P) {
				url = this.getClass().getResource("docs/wang600prog.html");
			} else if (m.getMnemonic() == KeyEvent.VK_F) {
				url = this.getClass().getResource("docs/wang600func.html");
			} else if (m.getMnemonic() == KeyEvent.VK_T) {
				url = this.getClass().getResource("docs/wang600tech.html");
			} else if (m.getMnemonic() == KeyEvent.VK_C) {
				url = this.getClass().getResource("docs/wang600codes.html");
			} else if (m.getMnemonic() == KeyEvent.VK_K) {
				url = this.getClass().getResource("docs/wang600bycode.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = this.getClass().getResource("docs/wang600sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_G) {
				url = this.getClass().getResource("docs/wang600bugs.html");
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
					doc = doc.replaceFirst("/wang600\\.jar!/","/");
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

class Wang600_Keyboard_main extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 54;

	public Wang600_Keyboard_main() {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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
			new Wang_Keys(Wang_Colors.orange1, Wang_Keys.SPCL_KEY(0)));
		addButton(c,1, 1, 0, 1, "icons/rad_deg.gif",
			new Wang_Keys(Wang_Colors.green1,Wang_Keys.PROG_CODE(8,9)));
		addButton(c,1, 2, 0, 2, "icons/shift.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.SHIFT));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/sin.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,6)));
		addButton(c,1, 1, 0, 1, "icons/tan.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,8)));
		addButton(c,1, 1, 0, 2, "icons/logex.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,10)));
		addButton(c,1, 1, 0, 3, "icons/x2.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,12)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/cos.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,7)));
		addButton(c,1, 1, 0, 1, "icons/inv.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,15)));
		addButton(c,1, 1, 0, 2, "icons/ex.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,11)));
		addButton(c,1, 1, 0, 3, "icons/sqrt.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,13)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(1,15)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(5,15)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(4,15)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,15)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(3,15)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(2,15)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(7,15)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/chg_sign.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(0,12)));
		addButton(c,1, 1, 0, 1, "icons/clear_disp.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(0,15)));
		addButton(c,1, 1, 0, 2, "icons/set_exp.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(0,11)));
		addButton(c,2, 1, 0, 3, "icons/zero.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/seven.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,7)));
		addButton(c,1, 1, 0, 1, "icons/four.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,4)));
		addButton(c,1, 1, 0, 2, "icons/one.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,1)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/eight.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,8)));
		addButton(c,1, 1, 0, 1, "icons/five.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,5)));
		addButton(c,1, 1, 0, 2, "icons/two.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,2)));
		addButton(c,2, 1, 0, 3, "icons/dp.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,10)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/nine.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,9)));
		addButton(c,1, 1, 0, 1, "icons/six.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,6)));
		addButton(c,1, 1, 0, 2, "icons/three.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(0,3)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(3,14)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(2,14)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(7,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(1,14)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(5,14)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(4,14)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,14)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/ld_prog.gif",
			new Wang_Keys(Wang_Colors.orange1, Wang_Keys.PROG_CODE(8,14)));
		addButton(c,1, 1, 0, 1, "icons/search.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,0)));
		addButton(c,1, 2, 0, 2, "icons/go.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(8,3)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/jif0.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,4)));
		addButton(c,1, 1, 0, 1, "icons/jifplus.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,5)));
		addButton(c,1, 1, 0, 2, "icons/recall_xx.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,1)));
		addButton(c,1, 1, 0, 3, "icons/print.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(8,2)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/i_o.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(15,2)));
		addButton(c,1, 1, 0, 1, "icons/group1.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(15,13)));
		addButton(c,1, 1, 0, 2, "icons/group2.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(15,14)));
		addButton(c,1, 1, 0, 3, "icons/indir.gif",
			new Wang_Keys(Wang_Colors.orange1, Wang_Keys.PROG_CODE(15,11)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/set_pc.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(2)));
		addButton(c,1, 1, 0, 1, "icons/verif_prog.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(1)));
		addButton(c,1, 1, 0, 2, "icons/rec_prog.gif",
			new Wang_Keys(Wang_Colors.orange1, Wang_Keys.SPCL_KEY(3)));
		addButton(c,1, 1, 0, 3, "icons/step.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.MODE0_CHG(8,8)));
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
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 16;

	public Wang600_Keyboard_meta() {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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
		pan.setPreferredSize(new Dimension(80, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		setLayout(gridbag);

		addButton(c,1, 1, 0, 0, "icons/k00.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(0)));
		addButton(c,1, 1, 1, 0, "icons/k01.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(1)));
		addButton(c,1, 1, 2, 0, "icons/k02.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(2)));
		addButton(c,1, 1, 3, 0, "icons/k03.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(3)));
		addButton(c,1, 1, 4, 0, "icons/k04.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(4)));
		addButton(c,1, 1, 5, 0, "icons/k05.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(5)));
		addButton(c,1, 1, 6, 0, "icons/k06.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(6)));
		addButton(c,1, 1, 7, 0, "icons/k07.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(7)));
		addButton(c,1, 1, 8, 0, "icons/k08.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(8)));
		addButton(c,1, 1, 9, 0, "icons/k09.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(9)));
		addButton(c,1, 1, 10, 0, "icons/k10.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(10)));
		addButton(c,1, 1, 11, 0, "icons/k11.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(11)));
		addButton(c,1, 1, 12, 0, "icons/k12.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(12)));
		addButton(c,1, 1, 13, 0, "icons/k13.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(13)));
		addButton(c,1, 1, 14, 0, "icons/k14.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(14)));
		addButton(c,1, 1, 15, 0, "icons/k15.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(15)));
		_col += 16;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		c.gridx = _col;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(Wang600.Disp.getOv(), c);
		add(Wang600.Disp.getOv());
		++_col;
		c.gridx = _col;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(Wang600.Disp.getErr(), c);
		add(Wang600.Disp.getErr());

		++_col;
		_col = 0;
		_row += 1;

	}
}

class Wang600_Keyboard_stick extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.158 2013/11/11 23:02:50 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 22;

	public Wang600_Keyboard_stick() {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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

		addPushButton(c, 15, 1, 0, 0,"Run","",Wang_Colors.white2, true,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,0))));
		addPushButton(c, 15, 1, 1, 0,"Learn","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,4))));
		addPushButton(c, 15, 1, 2, 0,"Learn<BR>and<BR>Print","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,6))));
		addPushButton(c, 15, 1, 3, 0,"List<BR>Program","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,2))));
		_col += 4;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 30, 1, 0, 0,"Clear","",null, false,
			new Wang_Keys(Wang_Colors.red1, Wang_Keys.PROG_CODE(0,14)));

		addPushButton(c, 5, 1, 1, 0,"T","1",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,1))));
		addPushButton(c, 5, 1, 2, 0,"+","2",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,2))));
		addPushButton(c, 5, 1, 3, 0,"-","3",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,3))));
		addPushButton(c, 5, 1, 4, 0,"X","4",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,4))));
		addPushButton(c, 5, 1, 5, 0,"&divide;","5",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,5))));
		addPushButton(c, 5, 1, 6, 0,"St","6",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,6))));
		addPushButton(c, 5, 1, 7, 0,"Re","7",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(7,7))));
		addPushButton(c, 5, 1, 8, 0,"f(x)","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(15,10))));
		addPushButton(c, 5, 1, 9, 0,"Sp<BR>\u2193<BR>On", "8",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(3,Wang_Keys.META_SPL(8,8))));
		addPushButton(c, 5, 1, 10, 0,"Fl<BR>\u2195<BR>Sc","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(4,Wang_Keys.MODE0_CHG(1,1))));
		addPushButton(c, 5, 1, 11, 0,"Deg<BR>\u2195<BR>Rad","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(5,Wang_Keys.MODE1_CHG(1,1))));
		addPushButton(c, 5, 1, 12, 0,"Printer<BR>\u2193<BR>On","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(6,Wang_Keys.MODE1_CHG(2,2))));
		addPushButton(c, 5, 1, 13, 0,"Paper<BR>Feed","",null, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.FEED));
		_col += 14;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(30, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addTapeButton(c, 5, 1, 0, 0, "RELEASE", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_EJECT)));

		addTapeButton(c, 5, 1, 1, 0, "FORWARD", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_FF)));

		addTapeButton(c, 5, 1, 2, 0, "TAPE READY", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_READY)));

		addTapeButton(c, 5, 1, 3, 0, "REWIND", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_REW)));

		_col = 0;
		_row += 1;

	}
}
