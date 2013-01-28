// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Plotter.java,v 1.2 2013/01/28 02:27:34 drmiller Exp $

import java.awt.event.*;
import javax.swing.*;

class Wang_Plotter extends Wang_Paper
	implements Wang_OutputDevice
{
	final String ident = "$Id: Wang_Plotter.java,v 1.2 2013/01/28 02:27:34 drmiller Exp $";
	public static final String Model = "12";
	public static final String Description = "Plotter";

	private byte[] cn24_xlate;

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_U) { 
				setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_H) { 
				home();
				return;
			}
		}
		super.actionPerformed(e);
	}

	public void reset() {
		// anything?
	}

	private void setup() {
		System.err.println("Plotter Setup menu");
	}

	private void setup_xlate() {
		cn24_xlate = new byte[256];
		cn24_xlate[0x00] = '-';
		cn24_xlate[0x01] = 'Y';
		cn24_xlate[0x02] = ' ';
		cn24_xlate[0x03] = '/';
		cn24_xlate[0x04] = 'Q';
		cn24_xlate[0x05] = 'P';
		cn24_xlate[0x06] = '+';
		cn24_xlate[0x07] = 'J';
		cn24_xlate[0x08] = '}';
		cn24_xlate[0x09] = '?';
		cn24_xlate[0x0a] = '=';
		cn24_xlate[0x0b] = '{';
		cn24_xlate[0x0c] = ',';
		cn24_xlate[0x0d] = ':';
		cn24_xlate[0x0e] = 'F';
		cn24_xlate[0x0f] = 'G';

		cn24_xlate[0x10] = 'W';
		cn24_xlate[0x11] = 'S';
		cn24_xlate[0x12] = '\013';	// plot
		cn24_xlate[0x13] = '\014';	// move
		cn24_xlate[0x14] = 'I';
		cn24_xlate[0x15] = '\'';
		cn24_xlate[0x16] = '.';
		cn24_xlate[0x17] = '\020';	// EXTENSION: pen color/stroke
		cn24_xlate[0x18] = '\015';	// char size
		cn24_xlate[0x19] = 'O';
		cn24_xlate[0x1a] = '\016';	// char spacing
		cn24_xlate[0x1b] = '\017';	// home
		cn24_xlate[0x1c] = 'A';
		cn24_xlate[0x1d] = 'R';
		cn24_xlate[0x1e] = 'V';
		cn24_xlate[0x1f] = 'M';

		cn24_xlate[0x20] = 'B';
		cn24_xlate[0x21] = 'H';
		cn24_xlate[0x22] = '\001';	// step x+
		cn24_xlate[0x23] = '\002';	// step x-
		cn24_xlate[0x24] = 'K';
		cn24_xlate[0x25] = 'E';
		cn24_xlate[0x26] = 'N';
		cn24_xlate[0x27] = 'T';
		cn24_xlate[0x28] = '\003';	// print mode (ignored)
		cn24_xlate[0x29] = '1';
		cn24_xlate[0x2a] = '\004';	// step y+
		cn24_xlate[0x2b] = '\005';	// step y-
		cn24_xlate[0x2c] = 'C';
		cn24_xlate[0x2d] = 'D';
		cn24_xlate[0x2e] = 'U';
		cn24_xlate[0x2f] = 'X';

		cn24_xlate[0x30] = '9';
		cn24_xlate[0x31] = '0';
		cn24_xlate[0x32] = '\006';	// step x+y+
		cn24_xlate[0x33] = '\007';	// step x-y+
		cn24_xlate[0x34] = '6';
		cn24_xlate[0x35] = '5';
		cn24_xlate[0x36] = '2';
		cn24_xlate[0x37] = 'Z';
		cn24_xlate[0x38] = '\010';	// plot mode (ignored)
		cn24_xlate[0x39] = '4';
		cn24_xlate[0x3a] = '\011';	// step x+y-
		cn24_xlate[0x3b] = '\012';	// step x-y-
		cn24_xlate[0x3c] = '8';
		cn24_xlate[0x3d] = '7';
		cn24_xlate[0x3e] = '3';
		cn24_xlate[0x3f] = 'L';

		// No characters over 0x3f are ever received...
		// Those codes cause the Wang to emit move commands followed
		// by the associated code 0x00-0x3f.
	}

	public Wang_Plotter() {
		super(Wang_UI.getSeries() + Model, Description,
				null,
				0, 0, 1000, 1000);
		super.setScale(1.0, -1.0);
		super.setOrigin(0, 1000);
		JMenu mu;
		mu = new JMenu("Plotter");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Home", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);
		super.addMenu(mu);
		setup_xlate();
		_x = 0;
		_y = 0;
		_dx = 0;
		_dy = 0;
	}

	private int _x, _y;
	private int _dx, _dy;

	private boolean plotChar(byte p) {
		System.err.println("Character " + p);
		return true;
	}

	private boolean plot() {
		int xd = _x + _dx;
		if (xd < 0) xd = 0;
		if (xd >= 1000) xd = 999;
		int yd = _y + _dy;
		if (yd < 0) yd = 0;
		if (yd >= 1000) yd = 999;
//System.err.println("Plot " + _x + "," + _y + " -> " + xd + "," + yd);
		_text.addPlot(_x, _y, xd, yd);
		_x = xd;
		_y = yd;
		return true;
	}

	private boolean move() {
		System.err.println("move(" + _dx + "," + _dy + ")");
		return false;
	}

	private boolean chrSize() {
		System.err.println("chrSize(" + _dx + "," + _dy + ")");
		return false;
	}

	private boolean chrSpace() {
		System.err.println("chrSpace(" + _dx + "," + _dy + ")");
		return false;
	}

	private boolean plotMode() {
		//System.err.println("plotMode");
		return false;
	}

	private boolean printMode() {
		System.err.println("printMode");
		return false;
	}

	private boolean home() {
		//System.err.println("home");
		_x = _y = 0;
		return false;
	}

	private boolean setPen() {
		System.err.println("setPen(" + _dx + "," + _dy + ")");
		return false;
	}

	public void do_cn24(byte[] b) {
		boolean drew = false;
		byte c = b[0];
		byte p = cn24_xlate[c];
		if (p < (byte)'\040') {
			// special
			switch(p) {
			case '\013':
				drew = plot();	// plot, actually
				break;
			case '\014':
				drew = move();
				break;
			case '\015':
				drew = chrSize();	// set character size (0-15) (Y)
				break;
			case '\016':
				drew = chrSpace();	// set character spacing (X,Y)
				break;
			case '\017':
				drew = home();	// move to lower-left (0,0)
				break;
			case '\003':
				drew = printMode();
				break;
			case '\010':
				drew = plotMode();
				break;
			// extensions
			case '\020':
				drew = setPen();
				break;
			// simple position movement commands.
			// all must return here or else _dx/_dy get cleared
			case '\001':
				_dx += 1;
				return;
			case '\002':
				_dx -= 1;
				return;
			case '\004':
				_dy += 1;
				return;
			case '\005':
				_dy -= 1;
				return;
			case '\006':
				_dx += 1;
				_dy += 1;
				return;
			case '\007':
				_dx -= 1;
				_dy += 1;
				return;
			case '\011':
				_dx += 1;
				_dy -= 1;
				return;
			case '\012':
				_dx -= 1;
				_dy -= 1;
				return;
			}
			// ignore anything else
		} else {
			drew = plotChar(p);
		}
		_dx = _dy = 0;
		if (drew) {
			_text.repaint();
		}
		// "auto raise"...
		onOff(true);
	}

	static public String getName() {
		return Wang_UI.getSeries() + Model + " " + Description;
	}
}
