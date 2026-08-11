// Copyright (c) 2011,2014 Douglas Miller
// $Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

import java.awt.Desktop;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;

public class w700_fe
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";

	private static JFrame front_end;

	public static void main(String[] args) {
		Wang_SplashScreen.starting();
		boolean test = false;
		boolean dbg = false;
		boolean stop = false;
		String model = null;

		GridBagLayout gridbag = new GridBagLayout();

		Wang_UI.setProperties(new Wang700_Properties());
		if (Wang_UI.getProperties().isNew()) {
			// since this file should have been create during INSTALL,
			// go ahead and nag the user.
			Wang_UI.warning("Load Setup",
				"Wang700_Properties file not found - using defaults");
		}

		for (String arg : args) {
			if (arg.equals("-t")) {
				test = true;
			} else if (arg.equals("-i")) {
				dbg = true;
			} else if (arg.equals("-I")) {
				stop = true;
			} else if (arg.matches("7[02]0[AaBbCc]")) {
				model = arg.toUpperCase();
			} else if (arg.matches(".*=.*")) {
				String[] ss = arg.split("=");
				Wang_UI.getProperties().setProperty("wang700_" + ss[0],
					ss[1]);
			} else {
				System.err.format("Unrecognized arg \"%s\"\n", arg);
			}
		}

		java.net.URL url = w700_fe.class.getResource("icons/wang700-48x48.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang700_home"));
		Wang_UI.setSeries("7");
		if (model != null) {
			Wang_UI.getProperties().setProperty("wang700_model", model);
		} else {
			model = Wang_UI.getProperties().getProperty("wang700_model");
			if (model == null) {
				model = "720C";
				Wang_UI.getProperties().setProperty("wang700_model", model);
			}
		}

		front_end = new JFrame("Wang " + model + " Advanced Programmable Calculator");
		front_end.setIconImage(img);

		Wang_Keys.toggle_on = new ImageIcon(w700_fe.class.getResource("icons/toggle_on.gif"));
		Wang_Keys.toggle_off = new ImageIcon(w700_fe.class.getResource("icons/toggle_off.gif"));

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

		lab = new JLabel(" Y ");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 24));
		lab.setForeground(Color.white);
		lab.setPreferredSize(new Dimension(35, 75));
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(35, 35));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 1;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		lab = new JLabel(" X ");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 24));
		lab.setForeground(Color.white);
		lab.setPreferredSize(new Dimension(35, 75));
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 2;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);

		JPanel dpan = new JPanel();
		GridBagLayout dgb = new GridBagLayout();
		dpan.setLayout(dgb);

		Wang700.DispY = new Wang700_Display(null);
		s.gridx = 0;
		s.gridy = 0;
		s.gridheight = 1;
		s.gridwidth = 1;
		dgb.setConstraints(Wang700.DispY, s);
		dpan.add(Wang700.DispY);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(560, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 1;
		s.gridheight = 1;
		s.gridwidth = 1;
		dgb.setConstraints(pan, s);
		dpan.add(pan);

		Wang700.DispX = new Wang700_Display(Wang700.DispY);
		s.gridx = 0;
		s.gridy = 2;
		s.gridheight = 1;
		s.gridwidth = 1;
		dgb.setConstraints(Wang700.DispX, s);
		dpan.add(Wang700.DispX);

		dpan.setOpaque(true);
		dpan.setBackground(Wang_Colors.empty);

		s.gridx = 1;
		s.gridy = 0;
		s.gridheight = 3;
		s.gridwidth = 1;
		gridbag.setConstraints(dpan, s);
		front_end.add(dpan);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(60, 25));
		pan.setOpaque(false);
		s.gridx = 2;
		s.gridy = 0;
		s.gridheight = 3;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		String tapeDoor = "<HTML><BR>" +
			"<FONT STYLE=\"font-family: serif; font-size: 22pt; font-weight: bold;\">" +
			"WANG </FONT>" +
			"<FONT STYLE=\"font-family: sans-serif; font-size: 14pt;\">700 SERIES</FONT><BR>" +
			"<FONT STYLE=\"font-family: sans-serif; font-size: 12pt;\">" +
			"ADVANCED PROGRAMMING CALCULATOR</FONT></HTML>";
		Wang_Keys ej = Wang700_Keyboard_stick.getEject();
		Wang700.Tape = new Wang_TapeDrive(ej, tapeDoor,
					Wang_Colors.ivory, Wang_Colors.aquaGlass,
					null, "tape image",
					Wang_UI.getProperties().getProperty("wang700_tape_file_suffix"),
					"File", (byte)0x5c, 0, false, "wang700_tape_image");
		s.gridx = 3;
		s.gridy = 0;
		s.gridheight = 4;
		s.gridwidth = 1;
		gridbag.setConstraints(Wang700.Tape, s);
		front_end.add(Wang700.Tape);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 3;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 25));
		pan.setOpaque(false);
		s.gridx = 4;
		s.gridy = 0;
		s.gridheight = 3;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);


		Wang_FunctionLabelBar fbar = null;
		if (Wang_UI.getProperties().getProperty("wang_function_labels") != null) {
			fbar = new Wang_FunctionLabelBar();
		}

		Wang700.Kbd = new Wang700_Keyboard(fbar);
		s.gridx = 0;
		s.gridy = 4;
		s.gridheight = 1;
		s.gridwidth = 5;
		gridbag.setConstraints(Wang700.Kbd, s);
		front_end.add(Wang700.Kbd);
		front_end.addKeyListener(Wang700.Kbd);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(110, 5));
		pan.setOpaque(false);
		s.gridx = 0;
		s.gridy = 5;
		s.gridheight = 1;
		s.gridheight = 5;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		String cn24 = Wang_UI.getProperties().getProperty("wang700_cn24_device");
		Wang700.CN24 = null;
		if (cn24 != null && cn24.equals(Wang_PlottingOutputWriter.getModel())) {
			Wang700.CN24 = new Wang_PlottingOutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_OutputWriter.getModel())) {
			Wang700.CN24 = new Wang_OutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_InputOutputWriter.getModel())) {
			Wang700.CN24 = new Wang_InputOutputWriter();
		} else if (cn24 != null && cn24.equals(Wang_Plotter.getModel())) {
			Wang700.CN24 = new Wang_Plotter();
		} else if (cn24 != null && cn24.equals(Wang_Teletype.getModel())) {
			Wang700.CN24 = new Wang_Teletype("wang700_707_");
		}
		// Must be after Keyboard created.
		Wang700.M730 = new Wang700_Model730();
		Wang700.M703 = new Wang_PaperTapeReader("wang700_703_image", front_end);
		Wang700.M705 = new Wang_MicroFace("wang700_705_", front_end);

		Wang700.Help = new Wang700_Help(front_end);

		// Must be alfter all components created.
		Wang700_SimInput inp = new Wang700_SimInput(test, dbg || stop, stop);
		// Wang700.Core is now setup...

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mu.add(inp.getOutputMenu()); // CN-24 output devices

		mi = Wang700.M730.getMenu(KeyEvent.VK_D);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang700.M703.getMenu(KeyEvent.VK_P);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang700.M705.getMenu(KeyEvent.VK_M);
		mi.addActionListener(inp);
		mu.add(mi);

		mu.add(Wang_Teletype.getMenu()); // TTY sub-menu, when active

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
		mi.addActionListener((Wang700_Keyboard)Wang700.Kbd);
		mu.add(mi);
		mi = new JMenuItem("Preferences", KeyEvent.VK_E);
		mi.addActionListener(inp);
		mu.add(mi);

		mu = new JMenu("Help");
		mb.add(mu);
		mi = Wang700.Help.getMenuItemHelp();
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang700.Help.getMenuItemAbout();
		mi.addActionListener(inp);
		mu.add(mi);

 		front_end.setJMenuBar(mb);
 
 		pan = new JPanel();

		front_end.setJMenuBar(mb);

		Wang700.DispX.setProperties(Wang_UI.getProperties());
		Wang700.DispY.setProperties(Wang_UI.getProperties());

		if (inp == null) System.err.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1025,690);
		front_end.pack();
		front_end.setVisible(true);

		Wang_SplashScreen.finished();
	}
}

class Wang700_SimInput
		implements WindowListener, ActionListener
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	private JMenuItem _mi701;
	private JMenuItem _mi702;
	private JMenuItem _mi711;
	private JMenuItem _mi712;
	private JMenuItem _mi707;
	private JMenuItem _miNone;
	private JMenu _mu;
	public JMenu getOutputMenu() { return _mu; }

	private void disposeDevice() {
		if (Wang700.CN24 instanceof Wang_Plotter) {
			_mi712.setText(Wang_Plotter.getName() +
				" (not installed)");
			Wang700.CN24.onOff(false);
		} else if (Wang700.CN24 instanceof Wang_OutputWriter) {
			_mi701.setText(Wang_OutputWriter.getName() +
				" (not installed)");
			Wang700.CN24.onOff(false);
		} else if (Wang700.CN24 instanceof Wang_PlottingOutputWriter) {
			_mi702.setText(Wang_PlottingOutputWriter.getName() +
				" (not installed)");
			Wang700.CN24.onOff(false);
		} else if (Wang700.CN24 instanceof Wang_InputOutputWriter) {
			_mi711.setText(Wang_InputOutputWriter.getName() +
				" (not installed)");
			Wang700.CN24.onOff(false);
		} else if (Wang700.CN24 instanceof Wang_Teletype) {
			_mi707.setText(Wang_Teletype.getName() +
				" (not installed)");
			Wang700.CN24.onOff(false);
		}
	}

	private void setupDevice() {
		if (Wang700.CN24.getFrame() != null) Wang700.CN24.getFrame().addWindowListener(this);
		String model = "";
		if (Wang700.CN24 instanceof Wang_Plotter) {
			model = Wang_Plotter.getModel();
		} else if (Wang700.CN24 instanceof Wang_OutputWriter) {
			model = Wang_OutputWriter.getModel();
		} else if (Wang700.CN24 instanceof Wang_PlottingOutputWriter) {
			model = Wang_PlottingOutputWriter.getModel();
		} else if (Wang700.CN24 instanceof Wang_InputOutputWriter) {
			model = Wang_InputOutputWriter.getModel();
		} else if (Wang700.CN24 instanceof Wang_Teletype) {
			model = Wang_Teletype.getModel();
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang700_Properties(),
				"wang700_cn24_device",
				model);
		} catch(Exception ee) {}
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Menu event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_1) {
			if (!(Wang700.CN24 instanceof Wang_OutputWriter)) {
				disposeDevice();
				Wang700.CN24 = new Wang_OutputWriter();
				_mi701.setText(Wang_OutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang700.CN24.onOff(!Wang700.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_2) {
			if (!(Wang700.CN24 instanceof Wang_PlottingOutputWriter)) {
				disposeDevice();
				Wang700.CN24 = new Wang_PlottingOutputWriter();
				_mi702.setText(Wang_PlottingOutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang700.CN24.onOff(!Wang700.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_3) {
			if (!(Wang700.CN24 instanceof Wang_InputOutputWriter)) {
				disposeDevice();
				Wang700.CN24 = new Wang_InputOutputWriter();
				_mi711.setText(Wang_InputOutputWriter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang700.CN24.onOff(!Wang700.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_4) {
			if (!(Wang700.CN24 instanceof Wang_Plotter)) {
				disposeDevice();
				Wang700.CN24 = new Wang_Plotter();
				_mi712.setText(Wang_Plotter.getName() +
						" (installed)");
				setupDevice();
			} else {
				Wang700.CN24.onOff(!Wang700.CN24.onOff());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_5) {
			if (!(Wang700.CN24 instanceof Wang_Teletype)) {
				disposeDevice();
				Wang700.CN24 = new Wang_Teletype("wang700_707_");
				_mi707.setText(Wang_Teletype.getName() +
						" (installed)");
				setupDevice();
			} else {
				// no on/off here for this device...
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_0) {
			disposeDevice();
			try { // if this fails, oh well.
				Wang_UI.getProperties().setAndSaveProperty(
					new Wang700_Properties(),
					"wang700_cn24_device",
					"");
			} catch(Exception ee) {}
			Wang700.CN24 = null;
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_D) {
			Wang700.M730.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_P) {
			Wang700.M703.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_M) {
			Wang700.M705.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_COPY) {
			// might try to figure out where mouse pointer is???
			Wang700.DispX.copy();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_H) {
			Wang700.Help.toggle();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_A) {
			Wang700.Help.showAbout();
 			return;
 		}
		if (m.getMnemonic() == KeyEvent.VK_E) {
			Wang700_Properties props = (Wang700_Properties)Wang_UI.getProperties();
			boolean changed = props.editPreferences();
			if (changed) {
				// Apply properties...
				Wang_UI.setDir(props.getProperty("wang700_home"));
				Wang700.DispX.setProperties(props);
				Wang700.DispY.setProperties(props);
				// <others>.setProperties(props);
			}
			return;
		}
	}

	public Wang700_SimInput(boolean test, boolean dbg, boolean stop) {
		if (Wang700.CN24 != null) {
			if (Wang700.CN24.getFrame() != null) Wang700.CN24.getFrame().addWindowListener(this);
		}

		_mu = new JMenu("Output Device...");
		// todo: make this a radio-button sub-menu
		String status = " (not installed)";
		if (Wang700.CN24 instanceof Wang_OutputWriter) status = " (installed)";
		_mi701 = new JMenuItem(Wang_OutputWriter.getName() + status,
					KeyEvent.VK_1);
		_mi701.addActionListener(this);
		_mu.add(_mi701);
		status = " (not installed)";
		if (Wang700.CN24 instanceof Wang_PlottingOutputWriter) status = " (installed)";
		_mi702 = new JMenuItem(Wang_PlottingOutputWriter.getName() + status,
					KeyEvent.VK_2);
		_mi702.addActionListener(this);
		_mu.add(_mi702);
		status = " (not installed)";
		if (Wang700.CN24 instanceof Wang_InputOutputWriter) status = " (installed)";
		_mi711 = new JMenuItem(Wang_InputOutputWriter.getName() + status,
					KeyEvent.VK_3);
		_mi711.addActionListener(this);
		_mu.add(_mi711);
		status = " (not installed)";
		if (Wang700.CN24 instanceof Wang_Plotter) status = " (installed)";
		_mi712 = new JMenuItem(Wang_Plotter.getName() + status,
					KeyEvent.VK_4);
		_mi712.addActionListener(this);
		_mu.add(_mi712);
		status = " (not installed)";
		if (Wang700.CN24 instanceof Wang_Teletype) status = " (installed)";
		_mi707 = new JMenuItem(Wang_Teletype.getName() + status,
					KeyEvent.VK_5);
		_mi707.addActionListener(this);
		_mu.add(_mi707);
		_miNone = new JMenuItem("None",
					KeyEvent.VK_0);
		_miNone.addActionListener(this);
		_mu.add(_miNone);

		if (!test) {
			Wang700.Core = new Wang700_Simulator(dbg, stop);
			Wang_UI.setCore(Wang700.Core);
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (Wang700.CN24 != null && e.getWindow() == Wang700.CN24.getFrame()) {
			Wang700.CN24.onOff(false);
			return;
		}
	}
}

class Wang700_Model730 implements Wang_RandIODevice {
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
		return new JMenuItem("730 Disk - " + status, key);
	}

	public Wang700_Model730() {
		reset();
		_buf = new byte[256]; // largest transfer
		_file = Wang_UI.getProperties().getFile("wang700_730_image",
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
					Wang_UI.getProperties().getProperty("wang700_disk_file_suffix"),
					"Wang disk image files", Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(Wang700.Kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			m.setText("730 Disk - " + _file.getName());
		} else {
			_file = null;
			m.setText("730 Disk - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang700_Properties(),
				"wang700_730_image",
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}

		disk_open();
	}

	public boolean start_cn36(int iob, int c) {
		// TODO: implement device selection.
		// ...
		// Wang_UI.getCore().replyIO(iob, GO);
		return false;
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
//System.err.println("dev 2 ["+_cmd+"] "+b[0]);
		boolean dat = ((iob & 1) != 0);
		if (_cmd <= 4 && dat || _cmd > 4 && !dat) {
System.err.println("sync error");
			return;
		}
//try{
// Thread.currentThread().sleep(50);
//}
//catch(InterruptedException ie){
//}
		if (_cmd < 4) {
			_adr <<= 8;
			_adr |= (c & 0x00ff);
			Wang700.Core.ackIO(iob);
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
				Wang700.Core.replyIO(iob, res);
//System.err.println("rd result "+res+" ("+_len+")");
			} else {
				Wang700.Core.ackIO(iob);
			}
		} else {
			if (_idx < _len) {
				if (_wr) {
					_buf[_idx] = (byte)c;
					Wang700.Core.ackIO(iob);
				} else {
					Wang700.Core.replyIO(iob, _buf[_idx]);
				}
			} else {
				if (_wr) {
					res = disk_write(_len);
					Wang700.Core.replyIO(iob, res);
				} else {
					Wang700.Core.ackIO(iob);
				}
				_cmd = 0;
//System.err.println("result "+res+" ("+_idx+")");
			}
			++_idx;
		}
//System.err.printf("got %02x%02x put %04x\n", b[1], b[0], bb);
	}

	public void reset() {
//System.err.println("clear ("+_len+")");
		_cmd = 0;
		_adr = 0;
		_len = 0;
		_wr = false;
		// cancel anything...
	}

	public int getGLRN() { return 0; }
}

class Wang700_Display extends Wang_Display
		implements ActionListener
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	// Nixie tubes can't display invalid characters
	final byte[] sign_chr = new byte[]{'+','-',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' ',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9',' ',' ',' ',' ',' ',' '};

	byte[] disp_a;
	short[] disp_b;
	JLabel disp;
	byte _dpc;
	byte _gap;

	private Wang_ErrorLight pe;
	private Wang_ErrorLight me;
	private boolean flashing;
	private boolean state;
	private boolean ismain;
	private Wang700_Display _other;
	javax.swing.Timer timer;

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
		s = s.replaceAll("\004","");	// half-width gap
		s = s.replaceAll("\005","1");	// special "1"
		s = s.replace("\006",".");	// special "."
		s = s.replace("\007",".");	// zero-width "."
		if (s.length() > 14) {
			e = s.substring(14); // keep "+"
			if (e.equals("+00")) e = null;
			s = s.substring(0,14);
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
			// only happens in main...
			flasher();
			//_other.flasher();
		} else {
			// what was it? e.getSource().stop()???
		}
	}

	public Wang700_Display(Wang_Display other) {
		String blank = "--- Wang 700 ----";
		disp_a = new byte[18];
		disp_b = new short[18];
		flashing = false;
		state = false;
		ismain = (other != null);
		_other = (Wang700_Display)other;
		if (ismain) {
			timer = new Timer(100, this);
		}

		setLayout(new FlowLayout());
		disp = new JLabel(blank, SwingConstants.LEFT);
		disp.setForeground(Wang_Colors.neon);
		disp.setBackground(Wang_Colors.empty);
		disp.setOpaque(true);

		disp.setPreferredSize(new Dimension(560, 75));
		// font setup later... in setProperties()...

		add(disp);

		if (ismain) {
			pe = new Wang_ErrorLight("Prog<BR>Error");
			pe.setOn(false);
			me = new Wang_ErrorLight("Mach<BR>Error");
			me.setOn(false);
		}

	}

	public void setProperties(Wang_Properties p) {
		Wang700_Properties prop = (Wang700_Properties)p;
		// TODO: reconfig/redraw display...
		String f = prop.getProperty("wang700_displayfont");
		Font font = null;
		java.io.InputStream ttf = this.getClass().getResourceAsStream(f);
		if (ttf != null) {
			try {
				font = Font.createFont(Font.TRUETYPE_FONT, ttf);
			} catch (FontFormatException ee) {
			} catch (IOException ee) {
			}
			font = font.deriveFont(40f);
			if (font.canDisplay('\007')) _dpc = '\007'; else _dpc = '.';
			if (font.canDisplay('\004')) _gap = '\004'; else _gap = ' ';
			if (prop.getBoolean("wang700_special1") && font.canDisplay('\005')) {
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
		do_display(disp_b);
	}

	private void setFlashing(boolean on) {
		if (on) {
			if (flashing) return;
			flashing = true;
			if (ismain) {
				timer.start();
				_other.setFlashing(on);
			}
		} else {
			if (!flashing) return;
			flashing = false;
			if (ismain) {
				timer.stop();
				_other.setFlashing(on);
			}
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
		String s = new String("                  ");
//System.err.println("blanking ("+ismain+")");
		disp.setText(" "+s);
		repaint();
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	// m[15] contains S1 (or !S0) in bit 8... i.e. FLD/FXD
	public void do_display(short[] m) {
		int ds;
		int dc;
		int dp;
		byte dx;
		disp_b = m;

//System.err.println("refreshed ("+ismain+")");
		// m[] is columns 0-15...
		String s;
		// first check FXD/FLD...
		dc = m[15] & 0x0f;
		boolean fxd = ((m[15] & 0x0100) == 0);
		if (dc == 15) { // intentional blanking
			dp = 18; // infinity
		} else if (fxd) { // FXD - fixed DP at pos 0
			dp = 0;
		} else { // FLD - floating DP at pos m[15]
			dp = dc;
		}
		ds = 0;
		dx = 0;
		// sign always goes straight into place...
		disp_a[ds] = sign_chr[m[dx] & 0x0f];
		++dx;
		do {
			if (ds == dp) {
				++ds;
				disp_a[ds] = _dpc;
			}
			++ds;
			dc = m[dx] & 0x0f;
			disp_a[ds] = disp_chr[dc];
			++dx;
		} while (dx < 13);
		++ds;
		disp_a[ds] = _gap;
		if (fxd) {
			++ds;
			disp_a[ds] = sign_chr[m[13] & 0x0f];
			++ds;
			disp_a[ds] = disp_chr[m[14] & 0x0f];
			++ds;
			disp_a[ds] = disp_chr[m[15] & 0x0f];
		}
		while (ds < 17) {
			++ds;
			disp_a[ds] = ' ';
		}
		s = new String(disp_a);
		disp.setText(" "+s);
		repaint();
	}
}
 
class Wang700_Help extends JComponent
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

	public Wang700_Help(JFrame frame) {
		_main = frame;
		_help = new JMenuItem("Show Help", KeyEvent.VK_H);;
		_about = new JMenuItem("About", KeyEvent.VK_A);
		_help_on = false;

		java.net.URL url = w700_fe.class.getResource("docs/wang700.html");
		_frame = new JFrame("Wang 700 Help");
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (IOException ee) {
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
		mi = new JMenuItem("About the Simulator", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Functions by Code", KeyEvent.VK_K);
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
		java.net.URL url = w700_fe.class.getResource("icons/wang700.gif");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang 700 Advanced Programming Calculator<BR>"+
			"Simulator<BR>"+
			Release.ident.replaceAll("\\$","")+"<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang700.durgadas.com<BR>"+
			"<BR>"+
			"With Rick Bensene<BR>"+
			"http://www.oldcalculatormuseum.com/wang720.html<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(_main, lab,
			"About: Wang 700 Simulator", JOptionPane.PLAIN_MESSAGE);
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
				url = this.getClass().getResource("docs/wang700.html");
			} else if (m.getMnemonic() == KeyEvent.VK_U) {
				url = this.getClass().getResource("docs/wang700calc.html");
			} else if (m.getMnemonic() == KeyEvent.VK_A) {
				url = this.getClass().getResource("docs/wang700samp.html");
			} else if (m.getMnemonic() == KeyEvent.VK_D) {
				url = this.getClass().getResource("docs/wang700tape.html");
			} else if (m.getMnemonic() == KeyEvent.VK_P) {
				url = this.getClass().getResource("docs/wang700prog.html");
			} else if (m.getMnemonic() == KeyEvent.VK_F) {
				url = this.getClass().getResource("docs/wang700func.html");
			} else if (m.getMnemonic() == KeyEvent.VK_T) {
				url = this.getClass().getResource("docs/wang700tech.html");
			} else if (m.getMnemonic() == KeyEvent.VK_C) {
				url = this.getClass().getResource("docs/wang700codes.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = this.getClass().getResource("docs/wang700sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_K) {
				url = this.getClass().getResource("docs/wang700bycode.html");
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
					doc = doc.replaceFirst("/wang700\\.jar!/","/");
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

class Wang700_Keyboard extends Wang_Keyboard
	implements ActionListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang_Keyboards[] _kbds;
	int _row;
	int _col;
	int _meta_kbd;
	int _meta;
	int _mode0;
	int _mode0r;
	int _defreg;
	boolean _run; // _mode0 doesn't show RUN down, only NOT learn/list/etc
	boolean modelC;

	public int getMode0(boolean clear) {
		int code = _mode0;
		if (clear) _mode0 &= ~_mode0r; // STEP is reset on read
		return code;
	}
	public int getMode1(boolean clear) { return 0; }

	private void setDefReg(int _new) {
		if (_new > 15) _new = -1;
		if (_new < -1) _new = 15;
		if (_defreg >= 0) {
			_kbds[_meta_kbd]._buttons[_defreg + 4].setBackground(Wang_Colors.white1);
		}
		_defreg = _new;
		if (_defreg >= 0) {
			_kbds[_meta_kbd]._buttons[_defreg + 4].setBackground(Wang_Colors.white3);
		}
	}

	// never called for MODE0 switches...
	private void setToggle(boolean on, Wang_Keys key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == Wang_Keys.METAP) {
			_meta &= ~key.getMode();
		} else if (key.getType() == Wang_Keys.MODE0) {
			_mode0 &= ~key.getMask();
		}
		if (on) {
			if (key.getType() == Wang_Keys.METAP) {
				_meta |= key.getMode();
				btn.setIcon(Wang_Keys.toggle_on);
			} else {
				btn.setBackground(key.altcolor);
			}
		} else {
			if (key.getType() == Wang_Keys.METAP) {
				btn.setIcon(Wang_Keys.toggle_off);
			} else {
				btn.setBackground(key.color);
			}
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

	private void set_group_mode0(int g, int y, int x, boolean alt) {
		int z;
		Wang_Keys key = _kbds[y]._keys[x];
		int mode = key.getMode();
		int numon = 0;
		boolean couldbe = (alt && (mode == 0 || (mode & 4) != 0));
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			if (_kbds[y]._keys[z] == null) continue;
			Wang_Keys key2 = _kbds[y]._keys[z];
			int tg = key2.getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			int mode2 = key2.getMode();
			boolean dbldown = (couldbe && mode2 != mode &&
				(mode2 == 0 || (mode2 & 4) != 0));
			if (key2.state) {
				if (dbldown) {
					// leave button down...
					++numon;
				} else {
					key2.state = false;
					_mode0 &= ~key2.getMask();
					_kbds[y]._buttons[z].setBackground(key2.color);
				}
			}
			// is RUN button (still) down?
			if (modelC && key2.getMode() == 0) {
				_run = key2.state;
			}
		}
		// never toggle?
		key.state = !key.state || (numon == 0);
		if (modelC && key.getMode() == 0) _run = key.state; // is RUN (now) down?
		if (key.state) {
			_mode0 |= key.getMode();
			_kbds[y]._buttons[x].setBackground(key.altcolor);
		} else {
			_mode0 &= ~key.getMask();
			_kbds[y]._buttons[x].setBackground(key.color);
		}
	}

	private void do_button(int y, int x, boolean alt) {
		int code = _kbds[y]._keys[x].getCode();
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x].isTAPE()) {
			set_group(g, y, x);
			boolean st = Wang700.Tape.do_button(_kbds[y]._keys[x]);
			if (st) {
				setToggle(!_kbds[y]._keys[x].state,
					_kbds[y]._keys[x], _kbds[y]._buttons[x]);
			}
			return;
		}
		if (g != 0) {
			if (type == Wang_Keys.MODE0) {
				set_group_mode0(g, y, x, alt);
			} else {
				set_group(g, y, x);
			}
		}
		if (_kbds[y]._keys[x].isMETA()) {
			return;
		}
		// _mode0, _meta were already updated above...
		if (type == Wang_Keys.MODE0) {
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				_mode0 |= _kbds[y]._keys[x].getMode();
				_mode0r |= _kbds[y]._keys[x].getMode();
			}
			Wang700.Core.chgMode0();
			return;
		}
		if (type == Wang_Keys.SPCL) {
			if (_run && (_mode0 & 4) != 0) {
				code += 4;
			}
			Wang700.Core.pressCmd(code);
			return;
		}
		if (type == Wang_Keys.META) {
			code &= 0x00f;
			code |= (_meta << 4);
		}
		Wang700.Core.pressKey(code);
	}

	public Wang700_Keyboard(Wang_FunctionLabelBar fbar) {
		int x;
		_kbds = new Wang_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_meta = 0;
		_defreg = -1;	// "Y" register is target

		modelC = Wang_UI.getProperties().getProperty("wang700_model").endsWith("C");
		Dimension dim = new Dimension(500, 25);
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;
		Wang_Keyboards kbd;

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

		kbd = new Wang700_Keyboard_meta();
		// assume the meta keys 00-15 are in order...
		// But, toggle switches (4) are first.
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
		if (fbar == null) {
			pan = new JPanel();
			pan.setPreferredSize(dim);
			pan.setOpaque(false);
			gridbag.setConstraints(pan, s);
			add(pan);
		} else {
			s.insets = new Insets(5, 0, 5, 0);
			gridbag.setConstraints(fbar, s);
			add(fbar);
			s.insets = new Insets(0, 0, 0, 0);
		}
		_col = 0;
		_row += 1;

		kbd = new Wang700_Keyboard_main(modelC);
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
			// format(%.13g) ensures no more than 13 digits.
			// The exponent will not error-out on overflow.
			s = String.format("%.13g", d);
			s = s.replaceAll("^0*", ""); // trim leading zero...
			int i = s.indexOf('e');
			String exp = new String();
			String mant;
			if (i < 0) {
				mant = s;
				if (mant.length() > 13) {
					mant = mant.substring(0, 13);
				}
			} else {
				if (i > 13) {
					mant = s.substring(0, 13);
				} else {
					mant = s.substring(0, i);
				}
				mant = mant.replaceAll("0*$", "");
				// The 700 is different about scientific notation,
				// it forces the mantissa to be < 1. So, have to adjust
				// the exponent accordingly.
				Integer ei = Integer.valueOf(s.substring(i + 1).replaceAll("^[+]", ""));
				ei += 1;
				ei = ei % 100;
				exp = "e" + Integer.toString(ei);
			}
			mant = mant.replaceAll("0*$", "");
			mant = mant.replaceAll("[.]$", "");
			if (mant.length() == 0) {
				mant = "0";
			}
			s = mant + exp;
		} catch (NumberFormatException e) {
			// give some indication
			s = "";
		}
		// Let Wang700_Core buffer up input...
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

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			setDefReg(_defreg - 1);
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			setDefReg(_defreg + 1);
		}
	}
	public void keyReleased(KeyEvent e) { }

	private void do_regop(int cmd) {
		if (_defreg >= 0) {
			Wang700.Core.pressKey(cmd | 0x40);
			Wang700.Core.pressKey(_defreg);
		} else {
			Wang700.Core.pressKey(cmd | 0x60);
		}
	}

	private void do_key(char c) {
		if (c >= '0' && c <= '9') {
			Wang700.Core.pressKey(0x70 + (c - '0'));
		} else if (c == 'e' || c == 'E') {
			Wang700.Core.pressKey(0x7a);
		} else if (c == '.') {
			Wang700.Core.pressKey(0x7c);
		} else if (c == '-') {
			Wang700.Core.pressKey(0x7b);
		} else if (c == '\b') {
			Wang700.Core.pressKey(0x7f);
//		} else if (c == 't' || c == 'T') {
//			Wang700.Core.pressKey(0x0010 | _defreg);
		} else if (c == '+') {
			do_regop(0x00);
		} else if (c == '_') {
			do_regop(0x01);
		} else if (c == '*') {
			do_regop(0x02);
		} else if (c == '/') {
			do_regop(0x03);
		} else if (c == 's' || c == 'S') {
			do_regop(0x04);
		} else if (c == 'r' || c == 'R') {
			do_regop(0x05);
//		} else if (c == 'i' || c == 'I') {
//			Wang700.Core.pressKey(0x00fb);
		} else if (c == 'x' || c == 'X') {
			do_regop(0x06);
		} else if (c == 0x04) {	// Ctrl-D
			Wang700.Core.debugIntr();
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
		// must be a button, find out which
		boolean alt = ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(y, x, alt);
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

	public void componentResized(ComponentEvent e) { }
}

class Wang700_Keyboard_main extends Wang_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 67;

	public Wang700_Keyboard_main(boolean modelC) {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(4,12)));
		addButton(c,1, 1, 0, 1, "icons/wr.gif",
			new Wang_Keys(Wang_Colors.blue1,Wang_Keys.PROG_CODE(4,11)));
		addButton(c,1, 1, 0, 2, "icons/int_x.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,8)));
		addButton(c,1, 1, 0, 3, "icons/e10x.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,13)));
		addButton(c,1, 1, 0, 4, "icons/ex.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/end_a.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(4,13)));
		addButton(c,1, 1, 0, 1, "icons/inv.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,15)));
		addButton(c,1, 1, 0, 2, "icons/abs_x.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,7)));
		addButton(c,1, 1, 0, 3, "icons/log10x.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,10)));
		addButton(c,1, 1, 0, 4, "icons/logex.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(6,11)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/re_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,5)));
		addButton(c,1, 1, 0, 1, "icons/st_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,4)));
		addButton(c,1, 1, 0, 2, "icons/re_res.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,14)));
		addButton(c,1, 1, 0, 3, "icons/pi.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(6,9)));
		addButton(c,1, 1, 0, 4, "icons/xchg_xy.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(6,6)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/xchg_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,6)));
		addButton(c,1, 1, 0, 1, "icons/div_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,3)));
		addButton(c,1, 1, 0, 2, "icons/mult_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,2)));
		addButton(c,1, 1, 0, 3, "icons/minus_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,1)));
		addButton(c,1, 1, 0, 4, "icons/plus_ind.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/xchg_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,6)));
		addButton(c,1, 1, 0, 1, "icons/div_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,3)));
		addButton(c,1, 1, 0, 2, "icons/mult_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,2)));
		addButton(c,1, 1, 0, 3, "icons/minus_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,1)));
		addButton(c,1, 1, 0, 4, "icons/plus_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/re_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,5)));
		addButton(c,1, 1, 0, 1, "icons/st_dir.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.PROG_CODE(4,4)));
		addButton(c,1, 1, 0, 2, "icons/y_to_x.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(6,5)));
		addButton(c,1, 2, 0, 3, "icons/x_to_y.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(6,4)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/chg_sign.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,11)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(6,3)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(6,2)));
		addButton(c,1, 1, 0, 3, "icons/minus.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(6,1)));
		addButton(c,1, 1, 0, 4, "icons/plus.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(6,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/sqrt.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(6,12)));
		addButton(c,1, 1, 0, 1, "icons/seven.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,7)));
		addButton(c,1, 1, 0, 2, "icons/four.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,4)));
		addButton(c,1, 1, 0, 3, "icons/one.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,1)));
		addButton(c,1, 1, 0, 4, "icons/zero.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/x2.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(7,13)));
		addButton(c,1, 1, 0, 1, "icons/eight.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,8)));
		addButton(c,1, 1, 0, 2, "icons/five.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,5)));
		addButton(c,1, 1, 0, 3, "icons/two.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,2)));
		addButton(c,1, 1, 0, 4, "icons/dp.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,12)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/clear_x.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,15)));
		addButton(c,1, 1, 0, 1, "icons/nine.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,9)));
		addButton(c,1, 1, 0, 2, "icons/six.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,6)));
		addButton(c,1, 1, 0, 3, "icons/three.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,3)));
		addButton(c,1, 1, 0, 4, "icons/set_exp.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(7,10)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/ld_prog.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,13)));
		addButton(c,1, 1, 0, 1, "icons/end_prog.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,12)));
		addButton(c,1, 1, 0, 2, "icons/stop.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,15)));
		addButton(c,1, 2, 0, 3, "icons/go.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(5,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/skip_err.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,10)));
		addButton(c,1, 1, 0, 1, "icons/skip_ge.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,7)));
		addButton(c,1, 1, 0, 2, "icons/skip_eq.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,9)));
		addButton(c,1, 1, 0, 3, "icons/skip_lt.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,8)));
		addButton(c,2, 1, 0, 4, "icons/search.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(4,7)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/mark.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(4,8)));
		addButton(c,1, 1, 0, 1, "icons/return.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(5,11)));
		addButton(c,1, 1, 0, 2, "icons/group1.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(4,9)));
		addButton(c,1, 1, 0, 3, "icons/group2.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.PROG_CODE(4,10)));
		++_col;
		if (modelC) {
			addButton(c,1, 1, 0, 0, "icons/prime.gif",
				new Wang_Keys(Wang_Colors.white1, Wang_Keys.SPCL_KEY(0)));
			addButton(c,1, 1, 0, 1, "icons/verif_prog.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(1)));
			addButton(c,1, 1, 0, 2, "icons/set_pc.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(2)));
			addButton(c,1, 1, 0, 3, "icons/rec_prog.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(3)));
		} else {
			addButton(c,1, 1, 0, 0, "icons/prime-0.gif",
				new Wang_Keys(Wang_Colors.white1, Wang_Keys.SPCL_KEY(0)));
			addButton(c,1, 1, 0, 1, "icons/verif_prog-0.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(1)));
			addButton(c,1, 1, 0, 2, "icons/set_pc-0.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(2)));
			addButton(c,1, 1, 0, 3, "icons/rec_prog-0.gif",
				new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(3)));
		}
		addButton(c,1, 1, 0, 4, "icons/step.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.MODE0_CHG(8,8)));
		++_col;

		_col = 0;
		_row += 5;
		setBackground(Color.black);
	}
}

class Wang700_Keyboard_meta extends Wang_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 20;

	public Wang700_Keyboard_meta() {
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

		setLayout(gridbag);

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(29, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addToggleButton(c, 1, 1, 0, 1,"80", false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.META_PRE(15,8))));
		addToggleButton(c, 1, 1, 1, 1,"40", false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(3,Wang_Keys.META_PRE(15,4))));
		addToggleButton(c, 1, 1, 2, 1,"20", false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(4,Wang_Keys.META_PRE(15,2))));
		addToggleButton(c, 1, 1, 3, 1,"10", false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(5,Wang_Keys.META_PRE(15,1))));
		_col += 4;

		// need to reset after pushbuttons!
		c.insets.left = 0;
		c.insets.right = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		c.anchor = GridBagConstraints.SOUTH;
		addButton(c,1, -2, 0, 0, "icons/k00.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(0)));
		addButton(c,1, -2, 1, 0, "icons/k01.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(1)));
		addButton(c,1, -2, 2, 0, "icons/k02.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(2)));
		addButton(c,1, -2, 3, 0, "icons/k03.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(3)));
		addButton(c,1, -2, 4, 0, "icons/k04.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(4)));
		addButton(c,1, -2, 5, 0, "icons/k05.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(5)));
		addButton(c,1, -2, 6, 0, "icons/k06.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(6)));
		addButton(c,1, -2, 7, 0, "icons/k07.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(7)));
		addButton(c,1, -2, 8, 0, "icons/k08.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(8)));
		addButton(c,1, -2, 9, 0, "icons/k09.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(9)));
		addButton(c,1, -2, 10, 0, "icons/k10.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(10)));
		addButton(c,1, -2, 11, 0, "icons/k11.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(11)));
		addButton(c,1, -2, 12, 0, "icons/k12.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(12)));
		addButton(c,1, -2, 13, 0, "icons/k13.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(13)));
		addButton(c,1, -2, 14, 0, "icons/k14.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(14)));
		addButton(c,1, -2, 15, 0, "icons/k15.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.META_KEY(15)));
		_col += 16;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(Wang700.DispX.getOv(), c);
		add(Wang700.DispX.getOv());
		++_col;
		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(Wang700.DispX.getErr(), c);
		add(Wang700.DispX.getErr());

		++_col;
		_col = 0;
		_row += 2;

		setBackground(Color.black);
	}
}

class Wang700_Keyboard_stick extends Wang_Keyboards
{
	final String ident = "$Id: w700_fe.java,v 1.73 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 22;

	static public Wang_Keys getEject() {
		return new Wang_Keys(Wang_Colors.ivory,
				Wang_Keys.GROUP(6,Wang_Keys.TAPE_EJECT));
	}

	public Wang700_Keyboard_stick() {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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

		c.gridx = _col;
		c.gridy = _row;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10,25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		java.net.URL url;
		url = w700_fe.class.getResource("icons/logo-2.gif");
		ImageIcon ic = new ImageIcon(url);
		JLabel lab = new JLabel(ic);
		c.gridx = _col;
		c.gridy = _row;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(lab, c);
		c.anchor = GridBagConstraints.NORTH;
		add(lab);
		_col += 1;

		c.gridx = _col;
		c.gridy = _row;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20,25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 12, 1, 0, 0,"Run","",Wang_Colors.white2, true,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,0))));
		addPushButton(c, 12, 1, 1, 0,"Learn","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,4))));
		addPushButton(c, 12, 1, 2, 0,"Learn and<BR>Print","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,6))));
		addPushButton(c, 12, 1, 3, 0,"List<BR>Program","",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(6,2))));
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(353, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addTapeButton(c, 5, 1, 0, 0, "RELEASE", Wang_Colors.white2,
			Wang700.Tape.ejectKey());

		addTapeButton(c, 5, 1, 1, 0, "FORWARD", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(6,Wang_Keys.TAPE_FF)));

		addTapeButton(c, 5, 1, 2, 0, "TAPE READY", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(6,Wang_Keys.TAPE_READY)));

		addTapeButton(c, 5, 1, 3, 0, "REWIND", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(6,Wang_Keys.TAPE_REW)));
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(35,25));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		_col = 0;
		_row += 1;

		setBackground(Color.black);
	}
}
