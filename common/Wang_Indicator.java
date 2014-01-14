// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Indicator.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang_Indicator extends JLabel {
	final String ident = "$Id: Wang_Indicator.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $";
	static final long serialVersionUID = 311611692038L;

	JLabel lab;

	public Wang_Indicator(String label) {
		setText("<HTML><CENTER>"+label+"</CENTER></HTML>");
		setFont(new Font("Sans-serif", Font.PLAIN, 8));
		setPreferredSize(new Dimension(50, 25));
		setHorizontalAlignment(SwingConstants.CENTER);
		setForeground(Color.black);
		setBackground(Wang_Colors.empty);
		setOpaque(true);
	}

	public void setOn(boolean on) {
		if (on) {
			setBackground(Wang_Colors.neon);
		} else {
			setBackground(Wang_Colors.empty);
		}
	}
}
