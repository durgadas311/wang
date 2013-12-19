// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputOutputWriter.java,v 1.18 2013/12/19 22:34:33 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_InputOutputWriter extends IBM_Selectric
		implements Wang_InputDevice, KeyListener
{
	final String ident = "$Id: Wang_InputOutputWriter.java,v 1.18 2013/12/19 22:34:33 drmiller Exp $";

	public static final String Model = "11";
	public static final String Description = "Input/Output Writer";

	// Group 2 04 12 = Enter program steps (GLRN)
	// Group 2 04 13 = Type (echo back to OutputWriter) (!GLRN)

	private int _glrn;

	public void reset() {
		_glrn = 0;
		_input = false;
		_indOUTPUT.setOn(true);
		_indTYPE.setOn(false);
		_indINPUT.setOn(false);
		super.getPaper().removeKeyListener(this);
	}

	public boolean start_cn36(int iob, int c) {
		if (iob != 5) return false;
		if (c == 0x4c) {
			_glrn = 1;
			_input = true;
			_indOUTPUT.setOn(false);
			_indINPUT.setOn(true);
			super.getPaper().addKeyListener(this);
			super.onOff(true);
			return true;
		}
		if (c == 0x4d) {
			_glrn = 0;
			_input = false; // do not send codes to Calculator...
			_indOUTPUT.setOn(false);
			_indTYPE.setOn(true);
			super.getPaper().addKeyListener(this);
			super.onOff(true);
			return true;
		}
		return false;
	}

	public void do_ack(int iob) {
		// should be ACK for previous code... should enable next...
		// TODO
	}

	public void do_dev(int iob, int b) {
		// right now only ACK happens
		// TODO
	}

	public int getGLRN() { return _glrn; }

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang611.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.18 $ $Date: 2013/12/19 22:34:33 $<BR>"+
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

//	private void sendACK() {
//		Wang_UI.getCore().ackIO(5);
//	}

	private void sendCode(byte b) {
		if (!_input) return;
		Wang_UI.getCore().replyIO(5, (b & 0x0ff));
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton butt = (JButton)e.getSource();
			if (butt.getMnemonic() == KeyEvent.VK_G) {
				// TODO: must release GLRN first... timing...
				_glrn = 0;
				// might have to notify Simulator?
				if (!_input) return;
				Wang_UI.getCore().replyIO(5, GO);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_E) {
				if (!_input) return;
				Wang_UI.getCore().replyIO(5, END);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_A) {
				if (!_input) return;
				Wang_UI.getCore().replyIO(5, START);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_L) {
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
					super.onOff(true);
				}
				return;
			}
		}
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
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
		static final long serialVersionUID = 311603000004L;
		Wang_InputOutputWriter _parent;

		public ProxyKeyHandler(Wang_InputOutputWriter parent) {
			_parent = parent;
		}

		public void keyTyped(KeyEvent e) {
			_parent.onOff(true);
			_parent.keyTyped(e);
		}
		public void keyReleased(KeyEvent e) { }
		public void keyPressed(KeyEvent e) { }
	}

	private class ControlPanel extends JComponent
	{
		static final long serialVersionUID = 311602000004L;

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

	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		e.consume(); // prevent JTextArea from seeing it
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

		_input = false;

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

		JFrame frame = new JFrame();
		frame.add(new ControlPanel(this));
		frame.getContentPane().setBackground(Color.black);
		frame.pack();
		frame.addKeyListener(new ProxyKeyHandler(this));

		Wang_UI.registerCN36(this);

		// Not initially...
		frame.setVisible(true);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
