// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Keyboards.java,v 1.3 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_Keyboards extends JPanel
{
	final String ident = "$Id: Wang_Keyboards.java,v 1.3 2014/01/14 21:53:51 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang_Keyboards() { }

	public int _nkeys;
	public Wang_Keys[] _keys;
	public JButton[] _buttons;
	protected GridBagLayout gridbag = new GridBagLayout();
	protected int _row;
	protected int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py,
						String icon, Wang_Keys key) {
		addButton(c, gridbag, this, lx, ly, px, py, 1, 1, icon, key);
	}

	void addButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
						int lx, int ly, int px, int py,
						int gx, int gy,
						String icon, Wang_Keys key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		java.net.URL url = Wang_Keyboards.class.getResource(icon);
		if (url != null) {
			ImageIcon ic = new ImageIcon(url);
			butt = new JButton(ic);
		} else {
			butt = new JButton("<HTML><CENTER>"+icon+"</CENTER></HTML>");
		}
		butt.setBackground(key.color);
		butt.setBorder(lb);
		butt.setOpaque(true);
		// butt.setHorizontalAlignment(SwingConstants.CENTER); // didn't help...

		dim.width = 50 * lx;
		if (ly < 0) {
			ly = -ly;
			dim.height = 50;
		} else {
			dim.height = 50 * ly;
		}
		butt.setPreferredSize(dim);
		butt.setMargin(inset);

		c.gridwidth = lx * gx;
		c.gridheight = ly * gy;
		c.gridx = _col + px;
		c.gridy = _row + py;
		gb.setConstraints(butt, c);

		ct.add(butt);
		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addPushButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String toplab, String botlab, Color alt, boolean init, Wang_Keys key) {
		final Dimension dim = new Dimension(15, 30);
		JButton butt;
		if (alt != null) {
			key.altcolor = alt;
		}

		butt = new JButton();

		butt.setPreferredSize(dim);
		if (init) {
			butt.setBackground(key.altcolor);
		} else {
			butt.setBackground(key.color);
		}
		key.state = init;
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		butt.setBorder(lb);
		butt.setOpaque(true);

		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = ly; // stupid warnings
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.CENTER;

		JLabel lab ;
		if (toplab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+toplab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 0;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		if (botlab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+botlab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 2;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}


	void addPushButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
				int lx, int ly, int px, int py,
				String botlab, Color alt, boolean init, Wang_Keys key) {
		final Dimension dim1 = new Dimension(20, 30); // button
		final Dimension dim2 = new Dimension(30, 10); // label
		final Font font1 = new Font("Sans-serif", Font.PLAIN, 7);
		JButton butt;
		if (alt != null) {
			key.altcolor = alt;
		}

		butt = new JButton();

		butt.setPreferredSize(dim1);
		if (init) {
			butt.setBackground(key.altcolor);
		} else {
			butt.setBackground(key.color);
		}
		key.state = init;
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		butt.setBorder(lb);
		butt.setOpaque(true);

		c.insets.left = lx; // stupid warnings
		c.gridx = _col + px;
		c.gridy = _row;
		c.gridwidth = ly;
		c.gridheight = py - 1;

		gb.setConstraints(butt, c);
		ct.add(butt);
		c.gridy += c.gridheight;

		if (botlab.length() > 0) {
			JLabel lab;
			lab = new JLabel(botlab);
			lab.setFont(font1);
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			lab.setPreferredSize(dim2);
			lab.setHorizontalAlignment(SwingConstants.CENTER);
			c.gridheight = 1;
			gb.setConstraints(lab, c);
			ct.add(lab);
		}

		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addToggleButton(GridBagConstraints c, int lx, int ly, int px, int py,
			String toplab,
			boolean init, Wang_Keys key) {
		final Dimension dim = new Dimension(15, 30);
		JButton butt;
		if (init) {
			butt = new JButton(Wang_Keys.toggle_on);
		} else {
			butt = new JButton(Wang_Keys.toggle_off);
		}
		butt.setPreferredSize(dim);
		key.state = init;
		butt.setOpaque(false);
		butt.setFocusPainted(false);
		butt.setBorderPainted(false);
		butt.setBackground(Color.black);


		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = ly; // stupid warnings
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = 1;

		JLabel lab;
		if (toplab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+toplab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 0;
			c.anchor = GridBagConstraints.SOUTH;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addTapeButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String toplab, Color alt, Wang_Keys key) {
		final Dimension dim = new Dimension(60, 30);
		JButton butt;
		if (alt != null) {
			key.altcolor = alt;
		}

		butt = new JButton();

		butt.setPreferredSize(dim);
		butt.setBackground(key.color);
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		butt.setBorder(lb);
		butt.setOpaque(true);

		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = ly; // stupid warnings
		c.insets.left = py; // stupid warnings
		c.gridheight = 1;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.CENTER;

		JLabel lab ;
		if (toplab.length() > 0) {
			lab = new JLabel("<HTML><CENTER>"+toplab+"</CENTER></HTML>");
			lab.setFont(new Font("Sans-serif", Font.PLAIN, 11));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 0;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		c.gridx = _col + px;
		c.gridy = _row + 1;
		c.insets.left = lx;
		c.insets.right = lx;
		gridbag.setConstraints(butt, c);
		add(butt);

		key.button = butt;
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}
}
