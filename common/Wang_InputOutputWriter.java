// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputOutputWriter.java,v 1.7 2013/02/19 22:50:19 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_InputOutputWriter extends IBM_Selectric
{
	final String ident = "$Id: Wang_InputOutputWriter.java,v 1.7 2013/02/19 22:50:19 drmiller Exp $";

	public static final String Model = "11";
	public static final String Description = "Input/Output Writer";

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang611.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.7 $ $Date: 2013/02/19 22:50:19 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	public void actionPerformed(ActionEvent e) {
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

	public Wang_InputOutputWriter() {
		super(Wang_UI.getSeries() + Model, Description);

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

		Wang_Indicator ind = new Wang_Indicator("OUTPUT");
		gridbag.setConstraints(ind, s);
		frame.add(ind);
		++s.gridx;

		ind = new Wang_Indicator("TYPE");
		gridbag.setConstraints(ind, s);
		frame.add(ind);
		++s.gridx;

		ind = new Wang_Indicator("INPUT");
		gridbag.setConstraints(ind, s);
		frame.add(ind);

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

		JButton butt = new JButton("GO");
		butt.setBackground(Wang_Colors.white1);
		butt.setBorder(lb);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridy;

		butt = new JButton("<HTML><CENTER>END<BR>ALPHA</CENTER></HTML>");
		butt.setBackground(Wang_Colors.orange1);
		butt.setBorder(lb);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridy;

		butt = new JButton("ALPHA");
		butt.setBackground(Wang_Colors.green1);
		butt.setBorder(lb);
		butt.setOpaque(true);
		butt.setPreferredSize(new Dimension(50, 50));
		butt.setMargin(new Insets(2,2,2,2));
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
		gridbag.setConstraints(lab, s);
		frame.add(lab);
		++s.gridx;

		ImageIcon togL = new ImageIcon(this.getClass().getResource("icons/toggle_L.png"));
		//ImageIcon togR = new ImageIcon(this.getClass().getResource("icons/toggle_R.png"));
		butt = new JButton(togL);
		butt.setBackground(Wang_Colors.green1);
		butt.setBorder(lb);
		butt.setOpaque(false);
		butt.setFocusPainted(false);
		butt.setBorderPainted(false);
		butt.setPreferredSize(new Dimension(34, 20));
		gridbag.setConstraints(butt, s);
		frame.add(butt);
		++s.gridx;

		lab = new JLabel("LOCAL");
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		gridbag.setConstraints(lab, s);
		frame.add(lab);
		++s.gridy;

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
		frame.setVisible(true);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
