// Copyright (c) 2011,2012 Douglas Miller
// $Id: w600_edit.java,v 1.2 2013/02/06 17:02:32 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class w600_edit
{
	final String ident = "$Id: w600_edit.java,v 1.2 2013/02/06 17:02:32 drmiller Exp $";

	public static void main(String[] args) {
		Wang_UI.setProperties(new Wang600_Properties());
		//Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang600_home"));
		Wang_UI.setSeries("6");

		JFrame frame = new JFrame("Wang 600-Series Card Editor");
		frame.setLayout(new FlowLayout());

		Wang_MarkSenseCard card;
		if (args.length > 0) {
			card = new Wang_MarkSenseCard(args[0]);
		} else {
			card = new Wang_MarkSenseCard(null);
		}
		frame.addKeyListener(card);

		JScrollPane scroll = new JScrollPane(card);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(new Dimension(512,800));
		frame.add(scroll);

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		JMenuItem mi;
		mu = new JMenu("File");
		mb.add(mu);
		mi = new JMenuItem("New", KeyEvent.VK_N);
		mi.addActionListener(card);
		mu.add(mi);
		mi = new JMenuItem("Open", KeyEvent.VK_O);
		mi.addActionListener(card);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(card);
		mu.add(mi);
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(card);
		mu.add(mi);
		mi = new JMenuItem("Quit", KeyEvent.VK_Q);
		mi.addActionListener(card);
		mu.add(mi);
		frame.setJMenuBar(mb);

		frame.getContentPane().setBackground(Color.black);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}
