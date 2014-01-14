// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_ErrorLight.java,v 1.3 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang_ErrorLight extends JPanel {
	final String ident = "$Id: Wang_ErrorLight.java,v 1.3 2014/01/14 21:53:51 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

	GridBagLayout gridbag = new GridBagLayout();
	Wang_Lamp pan;

	private class Wang_Lamp extends JPanel
	{
		static final long serialVersionUID = 311457692138L;

		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.addRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
			super.paint(g2d);
			Dimension d = getSize();
			if (_on) {
				g2d.setColor(Wang_Colors.neon);
				g2d.fillOval(0, 0, d.width - 1, d.height - 1);
				g2d.setColor(Wang_Colors.neon2);
				g2d.setStroke(new BasicStroke((float)3.0));
				g2d.drawOval(1, 1, d.width - 3, d.height - 3);
				g2d.setStroke(new BasicStroke((float)1.0));
			} else {
				g2d.setColor(Wang_Colors.empty);
				g2d.fillOval(0, 0, d.width - 1, d.height - 1);
			}
			g2d.setColor(Wang_Colors.white2);
			g2d.drawArc(1, 1, d.width - 2, d.height - 2, 80, 110);
		}

		public Wang_Lamp() {
			_on = false;
		}

		public void setOn(boolean on) {
			if (on != _on) {
				_on = on;
				repaint();
			}
		}

		private boolean _on;
	}

	public Wang_ErrorLight(String label) {
		GridBagConstraints s = new GridBagConstraints();

		setLayout(gridbag);
		//setPreferredSize(new Dimension(30, 50));
		setOpaque(false);

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.anchor = GridBagConstraints.CENTER;

		JLabel lab = new JLabel("<HTML><CENTER>"+label+"</CENTER></HTML>");
		lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
		lab.setPreferredSize(new Dimension(30, 20));
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 0;
		s.insets.left = 1;
		s.insets.right = 0;
		gridbag.setConstraints(lab, s);
		add(lab);

		pan = new Wang_Lamp();
		pan.setPreferredSize(new Dimension(20, 20));
		pan.setOpaque(true);
		pan.setBackground(Color.black);
		s.gridy = 1;
		s.insets.left = 0;
		s.insets.right = 8;
		gridbag.setConstraints(pan, s);
		add(pan);
	}

	public boolean isOn() { return pan._on; }

	public void setOn(boolean on) {
		pan.setOn(on);
	}
}
