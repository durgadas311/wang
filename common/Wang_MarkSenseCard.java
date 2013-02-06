// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_MarkSenseCard.java,v 1.1 2013/02/06 00:39:24 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.awt.geom.AffineTransform;

public class w614_fe
{
	final String ident = "$Id: Wang_MarkSenseCard.java,v 1.1 2013/02/06 00:39:24 drmiller Exp $";

	public static void main(String[] args) {

		JFrame frame = new JFrame("Wang 614 Mark Sense Card");
		frame.setLayout(new FlowLayout());

		Wang_MarkSenseCard card = new Wang_MarkSenseCard();

		JScrollPane scroll = new JScrollPane(card);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(new Dimension(512,800));
		frame.add(scroll);

		frame.getContentPane().setBackground(Color.black);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}

class Wang_MarkSenseCard extends JLabel {
	static final long serialVersionUID = 311614000000L;

	Font font1 = new Font("Sans-serif", Font.PLAIN, 18);
	Font font2 = new Font("Sans-serif", Font.PLAIN, 11);

	byte[] code = new byte[] {
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e,
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e,
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e,
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e,
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e,
		(byte)0x80, (byte)0x01, (byte)0x92, (byte)0xa0, (byte)0x82, (byte)0x9e
	};
	String title = new String("my_prog_3");

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);
		g2d.setColor(Color.black);
		g2d.setFont(font2);
		int s;
		for (s = 0; s < 40 && s < code.length; ++s) {
			byte c = code[s];
			double ry = s * 28.8 + 48.0;
			String step = String.format("%03d", s / 10);
			g2d.drawString(step, 90, (int)Math.round(ry + 8));
			int b;
			for (b = 0; b < 8; ++b) {
				double rx = (b * 38.4) + 169.5;
				boolean m = ((c & 0x80) != 0);
				c <<= 1;
				if (m) {
					g2d.fillRect((int)Math.round(rx), (int)Math.round(ry), 20, 10);
				}
			}
		}
		g2d.setFont(font1);
		AffineTransform orig = g2d.getTransform();
		g2d.rotate(-Math.PI/2);
		g2d.drawString(title, -1100, 20);
		g2d.setTransform(orig);
	}

	public Wang_MarkSenseCard() {
		super(new ImageIcon(w614_fe.class.getResource("icons/Wang_MarkSenseCard.gif")));
		setBackground(new Color(236,226,190));
		setOpaque(true);
		setPreferredSize(new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight()));
	}
}

