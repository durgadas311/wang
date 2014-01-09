// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_FunctionLabelBar.java,v 1.1 2014/01/09 00:45:42 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang_FunctionLabelBar extends JPanel {

	static final long serialVersionUID = 311999692037L;

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);

		g2d.setColor(Color.black);
		for (int x = 0; x < 17; ++x) {
			g2d.drawLine(x * 50 + 87, 0, x * 50 + 87, 50);
		}
		g2d.drawLine(26, 25, 949, 25);
		g2d.setColor(Wang_Colors.white3);
		g2d.fillRect(1, 1, 23, 48);
		g2d.fillRect(951, 1, 23, 48);
		g2d.setColor(Color.black);
		g2d.fillOval(2, 14, 21, 21);
		g2d.fillOval(952, 14, 21, 21);
	}

	public Wang_FunctionLabelBar() {
		setPreferredSize(new Dimension(975, 50));
		setBackground(Wang_Colors.ivory);
	}
}
