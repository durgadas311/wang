// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputOutputWriter.java,v 1.9 2013/02/22 21:28:32 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_InputOutputWriter extends IBM_Selectric
		implements Wang_InputDevice
{
	final String ident = "$Id: Wang_InputOutputWriter.java,v 1.9 2013/02/22 21:28:32 drmiller Exp $";

	public static final String Model = "11";
	public static final String Description = "Input/Output Writer";

	// Group 2 04 12 = Enter program steps (GLRN)
	// Group 2 04 13 = Type (echo back to OutputWriter) (!GLRN)

	public void reset() {
		_input = false;
		_indTYPE.setOn(false);
		_indINPUT.setOn(false);
	}

	public boolean start_cn36(byte[] b) {
		if (b[1] != (byte)0x50) return false;
		if (b[0] == (byte)0x4c) {
			_input = true;
			sendACK((byte)1); // assert GLRN
			_indINPUT.setOn(true);
			return true;
		}
		if (b[0] == (byte)0x4d) {
			_input = true;
			sendACK((byte)0); // do not assert GLRN
			_indTYPE.setOn(true);
			return true;
		}
		return false;
	}

	public void do_cn36(byte[] b) {
		// should be ACK for previous code... should enable next...
		// TODO
	}

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang611.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.9 $ $Date: 2013/02/22 21:28:32 $<BR>"+
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

	private void _send(byte[] b) {
		try {
			Wang_UI.getFout().write(b);
			Wang_UI.getFout().flush();
		} catch(Exception e) {
System.err.println(e.getMessage());
		}
	}

	private void sendACK(byte b) {
		_send(new byte[] { b, (byte)0x51 });
	}

	private void sendCode(byte b) {
		if (!_input) return;
		_send(new byte[] { b, (byte)0x50 });
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton butt = (JButton)e.getSource();
			if (butt.getMnemonic() == KeyEvent.VK_G) {
				sendACK((byte)0); // force de-assert GLRN
				sendCode((byte)0x83);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_E) {
				sendCode((byte)0x22);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_A) {
				sendCode((byte)0x92);
				return;
			}
			if (butt.getMnemonic() == KeyEvent.VK_L) {
				boolean on = !butt.isSelected();
				butt.setSelected(on);
				if (on) {
					// I/O mode...
					butt.setIcon(_togL);
					_indOUTPUT.setOn(true);
				} else {
					// LOCAL mode...
					butt.setIcon(_togR);
					_indOUTPUT.setOn(false);
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

	private class MnemonicAction extends AbstractAction {
		static final long serialVersionUID = 311602000004L;
		public MnemonicAction(int key) {
			putValue(Action.MNEMONIC_KEY, key);
		}
		public void actionPerformed(ActionEvent e) { }
	}

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
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		JFrame frame = new JFrame();
		GridBagLayout gridbag = new GridBagLayout();
		frame.setLayout(gridbag);
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
		frame.add(pan);
		s.gridwidth = 1;
		++s.gridy;

		_indOUTPUT = new Wang_Indicator("OUTPUT");
		gridbag.setConstraints(_indOUTPUT, s);
		frame.add(_indOUTPUT);
		++s.gridx;

		_indTYPE = new Wang_Indicator("TYPE");
		gridbag.setConstraints(_indTYPE, s);
		frame.add(_indTYPE);
		++s.gridx;

		_indINPUT = new Wang_Indicator("INPUT");
		gridbag.setConstraints(_indINPUT, s);
		frame.add(_indINPUT);

		s.insets.left = 0;
		s.insets.right = 0;

		++s.gridy;
		s.gridx = 0;
		s.gridwidth = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 20));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		frame.add(pan);

		s.gridwidth = 1;
		s.gridx = 1;
		++s.gridy;

		JButton butt = new JButton("<HTML><CENTER>GO</CENTER></HTML>");
		butt.setBackground(Wang_Colors.white1);
		butt.setBorder(lb);
		butt.setFocusPainted(false);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
		butt.setMnemonic(KeyEvent.VK_G);
		butt.addActionListener(this);
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridy;

		butt = new JButton("<HTML><CENTER>END<BR>ALPHA</CENTER></HTML>");
		butt.setBackground(Wang_Colors.orange1);
		butt.setBorder(lb);
		butt.setFocusPainted(false);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
		butt.setMnemonic(KeyEvent.VK_E);
		butt.addActionListener(this);
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridy;

		butt = new JButton("<HTML><CENTER>ALPHA</CENTER></HTML>");
		butt.setBackground(Wang_Colors.green1);
		butt.setBorder(lb);
		butt.setFocusPainted(false);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
		butt.setMnemonic(KeyEvent.VK_A);
		butt.addActionListener(this);
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridy;

		s.gridx = 0;
		s.gridwidth = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 20));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		frame.add(pan);
		++s.gridy;
		s.gridwidth = 1;

		JLabel lab = new JLabel("I/O");
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		s.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(lab, s);
		frame.add(lab);
		++s.gridx;
		s.anchor = GridBagConstraints.CENTER;

		_togL = new ImageIcon(this.getClass().getResource("icons/toggle_L.png"));
		_togR = new ImageIcon(this.getClass().getResource("icons/toggle_R.png"));
		butt = new JButton();
		butt.setOpaque(false);
		butt.setBackground(Color.black);
		butt.setFocusPainted(false);
		butt.setBorderPainted(false);
		butt.setPreferredSize(new Dimension(34, 20));
		butt.setAction(new MnemonicAction(KeyEvent.VK_L));
		butt.addActionListener(this);
		butt.setIcon(_togL);
		butt.setSelected(true);
		_indOUTPUT.setOn(true);
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridx;

		lab = new JLabel("LOCAL");
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		s.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(lab, s);
		frame.add(lab);
		++s.gridy;
		s.anchor = GridBagConstraints.CENTER;

		s.gridx = 0;
		s.gridwidth = 3;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		frame.add(pan);
		++s.gridy;
		s.gridwidth = 1;
		s.gridx = 1;

		frame.getContentPane().setBackground(Color.black);
		frame.pack();

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
