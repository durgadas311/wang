// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.concurrent.LinkedBlockingDeque;

class Wang_InputOutputWriter extends IBM_Selectric
		implements Wang_GroupIODevice, KeyListener, ActionListener, Runnable
{
	public static final String Model = "11";
	public static final String Description = "Input/Output Writer";
	private static JMenuItem pmi = null;
	private static Wang_InputOutputWriter thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Description;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_InputOutputWriter s_getInstance() {
		if (thus != null) return thus;
		thus = new Wang_InputOutputWriter();
		return thus;
	}


	// Group 2 04 12 = Enter program steps (GLRN)
	// Group 2 04 13 = Type (echo back to OutputWriter) (!GLRN)

	private int _glrn;
	private JFrame _frame;
	static JMenuItem dev_mi = null;
	private boolean plugged = false;

	public String getModel() { return s_getModel(); }
	public String getName() { return s_getName(); }
	public void plugIn(JMenu mu) {
		if (plugged) return;
		plugged = true;
		if (pmi != null) {
			pmi.setText(s_getName() + " (installed)");
		}
		if (mu != null) {
			mu.add(getMenu());
		}
		Wang_CN24_dev.connect(this);
		Wang_CN36_Bus.registerCN36(this);
		onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		if (Wang_CN24_dev.get() == this) {
			Wang_CN24_dev.connect(null);
		}
		Wang_CN36_Bus.deregisterCN36(this);
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		onOff(false);
	}
	public boolean isPlugged() { return plugged; }
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName(), KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}

	public void reset() {
		_glrn = 0;
		_input = false;
		_indOUTPUT.setOn(true);
		_indTYPE.setOn(false);
		_indINPUT.setOn(false);
		super.getPaper().removeKeyListener(this);
	}

	public void onOff(boolean on) {
		// need to trap when we're "disconnected", shut off, etc.
		_frame.setVisible(on);
		super.onOff(on);
	}

	public boolean start_cn36(int iob, int c) {
		// don't care about run vs. keyboard modes?
		// how would this work from a running program?!
		// especially if GLRN is asserted...
		_input = ((c & 0xfe) == 0x4c);
		if (!_input) return _input;
		if (c == 0x4c) { // "INPUT" - standard input mode
			_glrn = 1;
			_indOUTPUT.setOn(false);
			_indINPUT.setOn(true);
			super.getPaper().addKeyListener(this);
			onOff(true);
		} else if (c == 0x4d) { // "TYPE" - local echo but no send
			_glrn = 0;
			_indOUTPUT.setOn(false);
			_indTYPE.setOn(true);
			super.getPaper().addKeyListener(this);
			onOff(true);
		}
		return _input;
	}

	public void do_ack(int iob) { } // not used

	public int getGLRN() { return _glrn; }
	public void setGKBD(boolean state) { gkbd = !state; }

	public boolean isBlockIO() { return false; }
	public boolean isDevEnabled() { return _input; }

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang611.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.21 $ $Date: 2014/01/26 14:52:57 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	ImageIcon _togL;
	ImageIcon _togR;
	Wang_Indicator _indTYPE;
	Wang_Indicator _indOUTPUT;
	Wang_Indicator _indINPUT;
	boolean _input;
	boolean gkbd;
	LinkedBlockingDeque<Integer> giChr;

	private void sendCode(byte b) {
		if (_glrn == 0) return;
		giChr.add(b & 0x0ff);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton butt = (JButton)e.getSource();
			if (butt.getMnemonic() == KeyEvent.VK_G) {
				// regardless if TYPE mode, need to send GO
				giChr.add(GO);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_E) {
				if (_glrn == 0) return;
				giChr.add(END);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_A) {
				if (_glrn == 0) return;
				giChr.add(START);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_L) { // "LOCAL"
				boolean on = !butt.isSelected();
				butt.setSelected(on);
				if (on) {
					// I/O mode...
					butt.setIcon(_togL);
					_indOUTPUT.setOn(true);
					super.getPaper().removeKeyListener(this);
				} else {
					// LOCAL mode...
					butt.setIcon(_togR);
					_indOUTPUT.setOn(false);
					super.getPaper().addKeyListener(this);
					onOff(true);
				}
				return;
			}
		}
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();

			if (m.getMnemonic() == KeyEvent.VK_D) {
				onOff(true);
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_U) {
				//setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_T) {
				//return; fall through and perform base class too...
			}
			if (m.getMnemonic() == KeyEvent.VK_B) { 
				showAbout();
				return;
			}
		}
		super.actionPerformed(e);
	}

	private class ProxyKeyHandler
			implements KeyListener
	{
		Wang_InputOutputWriter _parent;

		public ProxyKeyHandler(Wang_InputOutputWriter parent) {
			_parent = parent;
		}

		public void keyTyped(KeyEvent e) {
		}
		public void keyReleased(KeyEvent e) { }
		public void keyPressed(KeyEvent e) {
			_parent.onOff(true);
			_parent.keyTyped(e);
		}
	}

	private class ControlPanel extends JComponent
	{
		public ControlPanel(Wang_InputOutputWriter parent) {
			Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
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
			s.insets.left = 2;
			s.insets.right = 2;
			s.anchor = GridBagConstraints.CENTER;

			s.gridx = 0;
			s.gridwidth = 3;
			JPanel pan = new JPanel();
			pan.setPreferredSize(new Dimension(50, 10));
			pan.setOpaque(false);
			gridbag.setConstraints(pan, s);
			add(pan);
			s.gridwidth = 1;
			++s.gridy;

			_indOUTPUT = new Wang_Indicator("OUTPUT");
			gridbag.setConstraints(_indOUTPUT, s);
			add(_indOUTPUT);
			++s.gridx;

			_indTYPE = new Wang_Indicator("TYPE");
			gridbag.setConstraints(_indTYPE, s);
			add(_indTYPE);
			++s.gridx;

			_indINPUT = new Wang_Indicator("INPUT");
			gridbag.setConstraints(_indINPUT, s);
			add(_indINPUT);

			s.insets.left = 0;
			s.insets.right = 0;

			++s.gridy;
			s.gridx = 0;
			s.gridwidth = 3;
			pan = new JPanel();
			pan.setPreferredSize(new Dimension(50, 20));
			pan.setOpaque(false);
			gridbag.setConstraints(pan, s);
			add(pan);

			s.gridwidth = 1;
			s.gridx = 1;
			++s.gridy;

			JButton butt = new JButton("<HTML><CENTER>GO</CENTER></HTML>");
			butt.setFocusable(false);
			butt.setBackground(Wang_Colors.white1);
			butt.setBorder(lb);
			butt.setFocusPainted(false);
			butt.setOpaque(true);
			butt.setPreferredSize(new Dimension(50, 50));
			butt.setMargin(new Insets(2,2,2,2));
			butt.setMnemonic(KeyEvent.VK_G);
			butt.addActionListener(parent);
			gridbag.setConstraints(butt, s);
			add(butt);
			++s.gridy;

			butt = new JButton("<HTML><CENTER>END<BR>ALPHA</CENTER></HTML>");
			butt.setFocusable(false);
			butt.setBackground(Wang_Colors.orange1);
			butt.setBorder(lb);
			butt.setFocusPainted(false);
			butt.setOpaque(true);
			butt.setPreferredSize(new Dimension(50, 50));
			butt.setMargin(new Insets(2,2,2,2));
			butt.setMnemonic(KeyEvent.VK_E);
			butt.addActionListener(parent);
			gridbag.setConstraints(butt, s);
			add(butt);
			++s.gridy;

			butt = new JButton("<HTML><CENTER>ALPHA</CENTER></HTML>");
			butt.setFocusable(false);
			butt.setBackground(Wang_Colors.green1);
			butt.setBorder(lb);
			butt.setFocusPainted(false);
			butt.setOpaque(true);
			butt.setPreferredSize(new Dimension(50, 50));
			butt.setMargin(new Insets(2,2,2,2));
			butt.setMnemonic(KeyEvent.VK_A);
			butt.addActionListener(parent);
			gridbag.setConstraints(butt, s);
			add(butt);
			++s.gridy;

			s.gridx = 0;
			s.gridwidth = 3;
			pan = new JPanel();
			pan.setPreferredSize(new Dimension(50, 20));
			pan.setOpaque(false);
			gridbag.setConstraints(pan, s);
			add(pan);
			++s.gridy;
			s.gridwidth = 1;

			JLabel lab = new JLabel("I/O");
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			s.anchor = GridBagConstraints.EAST;
			gridbag.setConstraints(lab, s);
			add(lab);
			++s.gridx;
			s.anchor = GridBagConstraints.CENTER;

			_togL = new ImageIcon(this.getClass().getResource("icons/toggle_L.png"));
			_togR = new ImageIcon(this.getClass().getResource("icons/toggle_R.png"));
			butt = new JButton();
			butt.setFocusable(false);
			butt.setOpaque(false);
			butt.setBackground(Color.black);
			butt.setFocusPainted(false);
			butt.setBorderPainted(false);
			butt.setPreferredSize(new Dimension(34, 20));
			butt.setMnemonic(KeyEvent.VK_L);
			butt.addActionListener(parent);
			butt.setIcon(_togL);
			butt.setSelected(true);
			_indOUTPUT.setOn(true);
			gridbag.setConstraints(butt, s);
			add(butt);
			++s.gridx;

			lab = new JLabel("LOCAL");
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			s.anchor = GridBagConstraints.WEST;
			gridbag.setConstraints(lab, s);
			add(lab);
			++s.gridy;
			s.anchor = GridBagConstraints.CENTER;

			s.gridx = 0;
			s.gridwidth = 3;
			pan = new JPanel();
			pan.setPreferredSize(new Dimension(50, 10));
			pan.setOpaque(false);
			gridbag.setConstraints(pan, s);
			add(pan);
			++s.gridy;
			s.gridwidth = 1;
			s.gridx = 1;

			setFocusCycleRoot(true);
			setRequestFocusEnabled(true);
		}
	}

	private byte _shifted = (byte)0x12;	// default to (start with) Shift Down

	// This is called from keyPressed(), so event must be scrutinized
	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		if (c > 0x7f) return; // reject meta keys, etc.
		// Convert ENTER to RETURN+INDEX, and allow CTRL+ for just INDEX.
		int k = e.getKeyCode();
		int m = e.getModifiersEx();
		e.consume(); // prevent JTextArea from seeing it
		if (k == KeyEvent.VK_ENTER && (m & InputEvent.CTRL_DOWN_MASK) == 0) {
			c = '\r'; // this will translate to CR+LF
		}
		byte b = (byte)c;
		byte[] tr = Wang_UI.getCharConv().asciiToTiltrotate(b);
		if (tr[0] != _shifted) {
			sendCode(tr[0]);
			_shifted = tr[0];
			do_cn24(tr[0]);
		}
		sendCode(tr[1]); // ignored in TYPE mode?
		do_cn24(tr[1]);
	}
	public void keyReleased(KeyEvent e) { }
	public void keyPressed(KeyEvent e) { }

	public Wang_InputOutputWriter() {
		super(Wang_UI.getSeries() + Model, Description);

		giChr = new LinkedBlockingDeque<Integer>();
		_input = false;
		_glrn = 0;

		JMenu mu;
		mu = new JMenu("Typewriter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);
		mu = new JMenu("Keyboard");
		super.addMenu(mu);

		mu = new JMenu("Help");
		mi = new JMenuItem("About", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);

		// now create control panel...

		_frame = new JFrame();
		_frame.add(new ControlPanel(this));
		_frame.getContentPane().setBackground(Color.black);
		_frame.pack();
		_frame.addKeyListener(new ProxyKeyHandler(this));

		// Not initially...
		//_frame.setVisible(true);

		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giChr.take();
				while (!gkbd) {
					Thread.sleep(10);
				}
			} catch (Exception ee) {}
			if (c < 0) continue; // or break?
			if (!_input) continue; // PRIME, etc.
			if (c == GO) {
				// This is identical to PRIME
				reset();
			}
			Wang_UI.getCore().replyIO(5, c);
		}
	}
}
