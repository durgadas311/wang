// Copyright (c) 2011,2012 Douglas Miller
// $Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.net.Socket;

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
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";

	private static JFrame front_end;

	public static void main(String[] args) {
		java.io.OutputStream fout = null;
		java.io.InputStream fin = null;
		java.io.BufferedReader ferr = null;
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
			// nothing special
		} else if (back) {
			fout = System.out;
			fin = System.in;
		} else if (web || Wang_UI.getProperties().getBoolean("wang600_remote")) {
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

		Wang_TapeDrive tape = new Wang_TapeDrive(fout,
					"<HTML><BR><FONT SIZE=+2><B>WANG</B></FONT>" +
					" 600 SERIES</HTML>",
					Wang_Colors.ivory, Wang_Colors.aqua,
					null, "program", "wng",
					"File", (byte)0x9e);
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
		s.gridheight = 1;
		gridbag.setConstraints(kbd, s);
		front_end.add(kbd);
		front_end.addKeyListener(kbd);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(110, 5));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 3;
		s.gridwidth = 4;
		s.gridheight = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		//Wang_OutputWriter m611f = new Wang_OutputWriter();
		//Wang_Plotter m612f = new Wang_Plotter();
		Wang600_Model630 m630f = new Wang600_Model630(kbd);
		Wang600_XROM xROMf = new Wang600_XROM(kbd);

		Wang600_Help help = new Wang600_Help(front_end);
		Wang600_SimInput inp = new Wang600_SimInput(fin, dsp, kbd, help,
						prt, tape, null, m630f, xROMf);
 
		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Expansion ROM - none installed", KeyEvent.VK_R);
		mi.addActionListener(inp);
		mu.add(mi);
//		mi = new JMenuItem("601/602/611 OutputWriter", KeyEvent.VK_O);
//		mi.addActionListener(inp);
//		mu.add(mi);
		mu.add(inp.getOutputMenu());
		mi = new JMenuItem("630 Disk - not mounted", KeyEvent.VK_D);
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
		mi.addActionListener(kbd);
		mu.add(mi);
		mi = new JMenuItem("Preferences", KeyEvent.VK_E);
		mi.addActionListener(inp);
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

		dsp.setProperties((Wang600_Properties)Wang_UI.getProperties());

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
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
	Wang600_Display _dsp;
	Wang600_Keyboard _kbd;
	Wang600_Printer _prt;
	Wang_TapeDrive _tape;
	Wang_OutputDevice _cn24;
	Wang600_Model630 _m630;
	Wang600_XROM _xROM;
	private Wang600_Help _help;

	InputStream _fin;

	private JMenuItem _mi611;
	private JMenuItem _mi612;
	private JMenuItem _miNone;
	private JMenu _mu;
	public JMenu getOutputMenu() { return _mu; }

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_O) {
			if (!(_cn24 instanceof Wang_OutputWriter)) {
				if (_cn24 instanceof Wang_Plotter) {
					_mi612.setText(Wang_Plotter.getName() +
						" (not installed)");
					_cn24.onOff(false);
				}
				_cn24 = new Wang_OutputWriter();
				_mi611.setText(Wang_OutputWriter.getName() +
						" (installed)");
				_cn24.getFrame().addWindowListener(this);
			} else {
				_cn24.onOff(!_cn24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_Q) {
			if (!(_cn24 instanceof Wang_Plotter)) {
				if (_cn24 instanceof Wang_OutputWriter) {
					_mi611.setText(Wang_OutputWriter.getName() +
						" (not installed)");
					_cn24.onOff(false);
				}
				_cn24 = new Wang_Plotter();
				_mi612.setText(Wang_Plotter.getName() +
						" (installed)");
				_cn24.getFrame().addWindowListener(this);
			} else {
				_cn24.onOff(!_cn24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_Z) {
			if (_cn24 instanceof Wang_OutputWriter) {
				_mi611.setText(Wang_OutputWriter.getName() +
						" (not installed)");
				_cn24.onOff(false);
			} else if (_cn24 instanceof Wang_Plotter) {
				_mi612.setText(Wang_Plotter.getName() +
						" (not installed)");
				_cn24.onOff(false);
			}
			_cn24 = null;
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
		if (m.getMnemonic() == KeyEvent.VK_COPY) {
			_dsp.copy();
			return;
		}
		// note: potential conflicts with Devices and Help menus...
		if (m.getMnemonic() == KeyEvent.VK_H) {
			_help.toggle();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_A) {
			_help.showAbout();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_E) {
			Wang600_Properties props = (Wang600_Properties)Wang_UI.getProperties();
			boolean changed = props.editPreferences();
			if (changed) {
				// Apply properties...
				Wang_UI.setDir(props.getProperty("wang600_home"));
				_dsp.setProperties(props);
				// <others>.setProperties(props);
			}
			return;
		}
	}

	public Wang600_SimInput(InputStream f,
			Wang600_Display dsp,
			Wang600_Keyboard kbd,
			Wang600_Help help,
			Wang600_Printer prt, Wang_TapeDrive tape,
			Wang_OutputDevice cn24,
			Wang600_Model630 m630,
			Wang600_XROM xROM) {
		_kbd = kbd;
		_dsp = dsp;
		_prt = prt;
		_tape = tape;
		_cn24 = cn24;
		if (_cn24 != null) {
			_cn24.getFrame().addWindowListener(this);
		}
		_m630 = m630;
		_xROM = xROM;
		_fin = f;
		_help = help;

		_mu = new JMenu("Output Device...");
		_mi611 = new JMenuItem(Wang_OutputWriter.getName() +
						" (not installed)",
					KeyEvent.VK_O);
		_mi611.addActionListener(this);
		_mu.add(_mi611);
		_mi612 = new JMenuItem(Wang_Plotter.getName() +
						" (not installed)",
					KeyEvent.VK_Q);
		_mi612.addActionListener(this);
		_mu.add(_mi612);
		_miNone = new JMenuItem("None",
					KeyEvent.VK_Z);
		_miNone.addActionListener(this);
		_mu.add(_miNone);

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
				if (_cn24 != null) {
					_cn24.reset();
				}
				_m630.reset();
				//etc...
			} else if (b[1] == 0x10) {
				if (_cn24 != null) {
					_cn24.do_cn24(b);
				}
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
		if (_cn24 != null && e.getWindow() == _cn24.getFrame()) {
			_cn24.onOff(false);
			return;
		}
	}
}

class Wang600_Printer
	implements ActionListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
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
					"wng", "Wang program files", Wang_UI.getDir());
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
					"wng", "Wang program files", Wang_UI.getDir());
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

class Wang600_Display extends JComponent
		implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','B','C','D','E',' '};

	byte[] disp_a;
	byte[] disp_b;
	JLabel disp;
	InputStream _fin;
	boolean _d12;	// is digit 12 enabled?

	Wang_ErrorLight pe;
	Wang_ErrorLight me;
	boolean flashing;
	boolean state;
	javax.swing.Timer timer;

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

	public Wang600_Display(InputStream f) {
		String blank = "--- Wang 600 ---";
		disp_a = new byte[16];
		disp_b = new byte[16];
		flashing = false;
		state = false;
		timer = new Timer(100, this);

		_fin = f;

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

	public void setProperties(Wang600_Properties prop) {
		// TODO: reconfig/redraw display...
		String f = prop.getProperty("wang600_displayfont");
		Font font = null;
		java.io.InputStream ttf = this.getClass().getResourceAsStream(f);
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
			} catch (IOException ee) {
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

	public void do_display(byte[] b) {
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

class Wang600_Keyboard extends JComponent
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
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
	Wang_TapeDrive _tape;
	Wang600_Printer _prt;

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
		if (type == Wang_Keys.MODE0) {
			code = Wang_Keys.MODE0 | _mode0;
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				code |= _kbds[y]._keys[x].getMode();
			}
		}
		if (type == Wang_Keys.SPCL) {
			code |= Wang_Keys.SPCL;
			if (_shift) {
				code += 4;
			}
		}
		if (type == Wang_Keys.MODE1) {
			boolean on = ((_mode1 & 2) != 0);
			_prt.onOff(on);
			code = Wang_Keys.MODE1 | _mode1;
		}
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

		do_keycode(code);
	}

	JFrame _frame;
	JScrollPane _scroll;
	int _xoff, _yoff;
	char[] _paste_text;
	int _paste_pos;
	private javax.swing.Timer timer; // must regulate flow...

	public Wang600_Keyboard(OutputStream fo, Wang_ErrorLight pe, Wang_ErrorLight me,
				Wang600_Printer prt, Wang_TapeDrive tape) {
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
		_paste_text = null;
		_paste_pos = 0;
		timer = new Timer(10, this);

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

	public boolean do_paste() {
		if (_paste_text != null) {
			if (_paste_pos < _paste_text.length) {
				int p = _paste_pos++;
				char c = _paste_text[p];
				do_key(c);
			} else {
				_paste_text = null;
				_paste_pos = 0;
				// need a non-desctructive "end input"...
				//do_keycode(0x009f); // TBD
				timer.stop();
			}
			return true;
		}
		return false;
	}

	public void paste() {
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
		_paste_text = s.toCharArray();
		// simulate keyboard input from clipboard...
		_paste_pos = 0;
		do_keycode(0x000f); // clear display - start entering number...
		timer.start();
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
		if (e.getSource() == timer) {
			do_paste();
			return;
		}
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
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
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
			"$Revision: 1.141 $ $Date: 2013/01/27 23:44:06 $<BR>"+
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
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
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
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 16;

	public Wang600_Keyboard_meta(Wang_ErrorLight pe, Wang_ErrorLight me) {
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
		gridbag.setConstraints(pe, c);
		add(pe);
		++_col;
		c.gridx = _col;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(me, c);
		add(me);

		++_col;
		_col = 0;
		_row += 1;

	}
}

class Wang600_Keyboard_stick extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.141 2013/01/27 23:44:06 drmiller Exp $";
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
