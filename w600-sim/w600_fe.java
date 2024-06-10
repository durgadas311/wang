// Copyright (c) 2011,2014 Douglas Miller
// $Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.util.concurrent.Semaphore;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.Desktop;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.datatransfer.StringSelection;

public class w600_fe
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";

	private static JFrame front_end;

	public static void main(String[] args) {
		Wang_SplashScreen.starting();
		boolean test = false;
		boolean dbg = false;
		boolean stop = false;
		String model = null;

		GridBagLayout gridbag = new GridBagLayout();

		Wang_UI.setProperties(new Wang600_Properties());
		if (Wang_UI.getProperties().isNew()) {
			// since this file should have been create during INSTALL,
			// go ahead and nag the user.
			Wang_UI.warning("Load Setup",
				"Wang600_Properties file not found - using defaults");
		}

		for (String arg : args) {
			if (arg.equals("-t")) {
				test = true;
			} else if (arg.equals("-i")) {
				dbg = true;
			} else if (arg.equals("-I")) {
				stop = true;
			} else if (arg.equalsIgnoreCase("600-2TP") ||
					arg.equalsIgnoreCase("600-6TP") ||
					arg.equalsIgnoreCase("600-14TP")) {
				model = arg.toUpperCase();
			} else if (arg.matches(".*=.*")) {
				String[] ss = arg.split("=");
				Wang_UI.getProperties().setProperty("wang600_" + ss[0],
					ss[1]);
			} else {
				System.err.format("Unrecognized arg \"%s\"\n", arg);
			}
		}

		java.net.URL url = w600_fe.class.getResource("icons/wang600-48x48.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang600_home"));
		Wang_UI.setSeries("6");
		if (model != null) {
			Wang_UI.getProperties().setProperty("wang600_model", model);
		} else {
			model = Wang_UI.getProperties().getProperty("wang600_model");
			if (model == null) {
				model = "600-14TP";
				Wang_UI.getProperties().setProperty("wang600_model", model);
			}
		}

		front_end = new JFrame("Wang " + model + " Advanced Programmable Calculator");
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

		Wang_Keys ej = Wang600_Keyboard_stick.getEject();
		Wang600.Tape = new Wang_TapeDrive(ej,
					"<HTML><BR><FONT SIZE=+2><B>WANG</B></FONT>" +
					" 600 SERIES</HTML>",
					Wang_Colors.ivory, Wang_Colors.aquaGlass,
					null, "tape image",
					Wang_UI.getProperties().getProperty("wang600_tape_file_suffix"),
					"File", (byte)0x9e, 0, false, "wang600_tape_image");
		s.gridx = 3;
		s.gridheight = 2;
		gridbag.setConstraints(Wang600.Tape, s);
		s.gridheight = 1;
		front_end.add(Wang600.Tape);

		Wang600.Prt = new Wang600_Printer();

		Wang_FunctionLabelBar fbar = null;
		if (Wang_UI.getProperties().getProperty("wang_function_labels") != null) {
			fbar = new Wang_FunctionLabelBar();
		}

		// Must be after Display, Tape, and Printer are created.
		Wang600.Kbd = new Wang600_Keyboard(fbar);
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
		} else if (cn24 != null && cn24.equals(Wang_Teletype.getModel())) {
			Wang600.CN24 = new Wang_Teletype("wang600_607_");
		}
		// Must be after Keyboard created.
		Wang600.M630 = new Wang600_Model630();
		Wang600.M603 = new Wang_PaperTapeReader("wang600_603_image", front_end);
		Wang600.M605 = new Wang_MicroFace("wang600_605_", front_end);

		Wang600.Help = new Wang600_Help(front_end);

		// Must be alfter all components created.
		Wang600_SimInput inp = new Wang600_SimInput(test, dbg || stop, stop);
		// Wang600.Core is now setup...
 
		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Devices");
		mb.add(mu);
		JMenuItem mi;
		mi = ((Wang600_Simulator)Wang600.Core).getXRomMenu(KeyEvent.VK_R);
		mi.addActionListener(inp);
		mu.add(mi);

		mu.add(inp.getOutputMenu()); // CN-24 output devices

		mi = Wang600.M630.getMenu(KeyEvent.VK_D);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang600.M603.getMenu(KeyEvent.VK_P);
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang600.M605.getMenu(KeyEvent.VK_M);
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

		Wang_SplashScreen.finished();
	}
}

class Wang600_SimInput
		implements WindowListener, ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";

	private JMenuItem _mi601;
	private JMenuItem _mi602;
	private JMenuItem _mi611;
	private JMenuItem _mi612;
	private JMenuItem _mi607;
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
		} else if (Wang600.CN24 instanceof Wang_Teletype) {
			_mi607.setText(Wang_Teletype.getName() +
				" (not installed)");
			Wang600.CN24.onOff(false);
		}
	}

	private void setupDevice() {
		if (Wang600.CN24.getFrame() != null) Wang600.CN24.getFrame().addWindowListener(this);
		String model = "";
		if (Wang600.CN24 instanceof Wang_Plotter) {
			model = Wang_Plotter.getModel();
		} else if (Wang600.CN24 instanceof Wang_OutputWriter) {
			model = Wang_OutputWriter.getModel();
		} else if (Wang600.CN24 instanceof Wang_PlottingOutputWriter) {
			model = Wang_PlottingOutputWriter.getModel();
		} else if (Wang600.CN24 instanceof Wang_InputOutputWriter) {
			model = Wang_InputOutputWriter.getModel();
		} else if (Wang600.CN24 instanceof Wang_Teletype) {
			model = Wang_Teletype.getModel();
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
		if (m.getMnemonic() == KeyEvent.VK_5) {
			if (!(Wang600.CN24 instanceof Wang_Teletype)) {
				disposeDevice();
				Wang600.CN24 = new Wang_Teletype("wang600_607_");
				_mi607.setText(Wang_Teletype.getName() +
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
		if (m.getMnemonic() == KeyEvent.VK_P) {
			Wang600.M603.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_M) {
			Wang600.M605.pickFile(m);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_R) {
			((Wang600_Simulator)Wang600.Core).pickXRomFile(m);
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

	public Wang600_SimInput(boolean test, boolean dbg, boolean stop) {
		if (Wang600.CN24 != null) {
			if (Wang600.CN24.getFrame() != null) Wang600.CN24.getFrame().addWindowListener(this);
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
		status = " (not installed)";
		if (Wang600.CN24 instanceof Wang_Teletype) status = " (installed)";
		_mi607 = new JMenuItem(Wang_Teletype.getName() + status,
					KeyEvent.VK_5);
		_mi607.addActionListener(this);
		_mu.add(_mi607);
		_miNone = new JMenuItem("None",
					KeyEvent.VK_0);
		_miNone.addActionListener(this);
		_mu.add(_miNone);

		if (!test) {
			Wang600.Core = new Wang600_Simulator(dbg, stop);
			Wang_UI.setCore(Wang600.Core);
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
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
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

class Wang600_Model630 implements Wang_RandIODevice {
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

	public int getGLRN() { return 0; }
}

class Wang600_Display extends Wang_Display
		implements ActionListener, Runnable
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','B','C','D','E',' '};

	byte[] disp_a;
	short[] disp_b;
	JLabel disp;
	boolean _d12;	// is digit 12 enabled?

	private Wang_ErrorLight pe;
	private Wang_ErrorLight me;
	private boolean flashing;
	private boolean state;
	private javax.swing.Timer timer;
	private Semaphore sem;

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
			// flasher();
			sem.release();
//		} else if (e.getSource() == timer2) {
//			blanker();
		} else {
			// what was it? e.getSource().stop()???
		}
	}

	public Wang600_Display() {
		String blank = "--- Wang 600 ---";
		disp_a = new byte[16];
		disp_b = new short[16];	// replaced before used?
		flashing = false;
		state = false;
		timer = new Timer(100, this);
		sem = new Semaphore(0);
		new Thread(this).start();

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

	public void do_display(short[] b) {
		// b[*] is actually more like uint8_t b[16][2]!
		int ds;
		disp_b = b;
		ds = 0;
		disp_a[ds] = sign_chr[b[ds] & 0x0f]; // mant sign
		++ds;
		do {
			disp_a[ds] = disp_chr[b[ds] & 0x0f];
			++ds;
		} while (ds < 13);
		disp_a[ds] = sign_chr[b[ds] & 0x0f]; // exp sign
		++ds;
		disp_a[ds] = disp_chr[b[ds] & 0x0f];
		++ds;
		disp_a[ds] = disp_chr[b[ds] & 0x0f];
		++ds;
		if (!_d12) {
			disp_a[12] = ' ';
		}

		String s = new String(disp_a);
		disp.setText(s);
		repaint();
	}

	public void run() {
		Rectangle r = null;
		while (true) {
			try {
				sem.acquire();
			} catch (Exception ee) {}
			if (r == null) {
				r = disp.getBounds(null);
//System.err.format("disp %d %d %d %d\n", r.x, r.y, r.width, r.height);
			}
			flasher();
			//disp.paintImmediately(r);
			disp.paint(disp.getGraphics());
		}
	}
}

class Wang600_Keyboard extends Wang_Keyboard
	implements ActionListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _shift;
	boolean _adf;	// "/ADF" is the signal that selects alt func spcl keys
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

	public int getMode0(boolean clear) {
		int code = _mode0;
		if (clear) _mode0 &= ~_mode0r; // STEP is reset on read
		return code;
	}
	public int getMode1(boolean clear) { return _mode1; }

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

	private void set_group_mode0(int g, int y, int x, boolean alt) {
		int z;
		Wang_Keys key = _kbds[y]._keys[x];
		int mode = key.getMode();
		int numon = 0;
		boolean run = false; // RUN also down?
		boolean couldbe = (alt && (mode == 0 || mode == 4));
		// The RUN button will only be encountered once, either
		// it is the button currently pressed or it will be
		// seen in this 'for' loop (but not both):
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			if (_kbds[y]._keys[z] == null) continue;
			Wang_Keys key2 = _kbds[y]._keys[z];
			int tg = key2.getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			int mode2 = key2.getMode();
			boolean dbldown = (couldbe && mode2 != mode &&
				(mode2 == 0 || mode2 == 4));
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
			if (key2.getMode() == 0) run = key2.state;
		}
		// never toggle?
		key.state = !key.state || (numon == 0);
		if (key.getMode() == 0) run = key.state; // is RUN (now) down?
		if (key.state) {
			_mode0 |= key.getMode();
			_kbds[y]._buttons[x].setBackground(key.altcolor);
		} else {
			_mode0 &= ~key.getMask();
			_kbds[y]._buttons[x].setBackground(key.color);
		}
		_adf = (run && _mode0 == 4);
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
			// group must be "1" for RUN/LEARN/... group
			if (g == 1 && type == Wang_Keys.MODE0) {
				set_group_mode0(g, y, x, shifted);
			} else {
				set_group(g, y, x);
			}
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
			// TODO: RUN+LEARN only? never _shift?
			if (_adf || _shift) {
				code |= 4;
			}
			Wang600.Core.pressCmd(code);
			if (!shifted) setShift(false);
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

	public Wang600_Keyboard(Wang_FunctionLabelBar fbar) {
		int x;
		_kbds = new Wang_Keyboards[num_kbds];
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
		Wang_Keyboards kbd;

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
		} else if (c == 0x04) {	// Ctrl-D
			Wang600.Core.debugIntr();
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

		_frame.addWindowListener(this);
		_frame.addComponentListener(this);
		_frame.pack();

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
			Release.ident.replaceAll("\\$","")+"<BR>"+
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

class Wang600_Keyboard_main extends Wang_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
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
		setBackground(Color.black);
	}
}

class Wang600_Keyboard_meta extends Wang_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
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

		setBackground(Color.black);
	}
}

class Wang600_Keyboard_stick extends Wang_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.189 2014/01/26 14:52:56 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 22;

	static public Wang_Keys getEject() {
		return new Wang_Keys(Wang_Colors.ivory,
				Wang_Keys.GROUP(7,Wang_Keys.TAPE_EJECT));
	}

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

		// group must be "1" for RUN/LEARN/... group
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
			Wang600.Tape.ejectKey());
	
		addTapeButton(c, 5, 1, 1, 0, "FORWARD", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_FF)));

		addTapeButton(c, 5, 1, 2, 0, "TAPE READY", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_READY)));

		addTapeButton(c, 5, 1, 3, 0, "REWIND", Wang_Colors.white2,
			new Wang_Keys(Wang_Colors.ivory, Wang_Keys.GROUP(7,Wang_Keys.TAPE_REW)));

		_col = 0;
		_row += 1;

		setBackground(Color.black);
	}
}
