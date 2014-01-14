// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_OutputWriter.java,v 1.15 2014/01/14 21:53:51 drmiller Exp $

import java.awt.event.*;
import javax.swing.*;

class Wang_OutputWriter extends IBM_Selectric
{
	final String ident = "$Id: Wang_OutputWriter.java,v 1.15 2014/01/14 21:53:51 drmiller Exp $";

	public static final String Model = "01";
	public static final String Description = "Output Writer";

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang601.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.15 $ $Date: 2014/01/14 21:53:51 $<BR>"+
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

	public Wang_OutputWriter() {
		super(Wang_UI.getSeries() + Model, Description);

		JMenu mu;
		mu = new JMenu("Typewriter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);

		mu = new JMenu("Help");
		mi = new JMenuItem("About", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
