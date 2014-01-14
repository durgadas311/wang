// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Keys.java,v 1.5 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang_Keys {
	final String ident = "$Id: Wang_Keys.java,v 1.5 2014/01/14 21:53:51 drmiller Exp $";

	static final int SPCL = 0x0100;
	static final int MODE0 = 0x0200;
	static final int MODE1 = 0x0300;
	static final int MODE2 = 0x0700;
	static final int META = 0x0400;		// never sent
	static final int ALT = META;		// never sent
	static final int METAP = 0x0500;	// never sent
	static final int METAS = 0x0600;	// never sent

	public Wang_Keys(Color sl, int c) {
		this.color = sl;
		this.altcolor = sl;
		this.code = c;
		this.state = false;
	}

	public Wang_Keys(Color sl, Color xl, int c) {
		this.color = sl;
		this.altcolor = xl;
		this.code = c;
		this.state = false;
	}

	static final int SHIFT = -1;
	static final int FEED = -2;
	static final int TAPE_EJECT = -3;
	static final int TAPE_REW = -4;
	static final int TAPE_FF = -5;
	static final int TAPE_READY = -6;

	static final int PROG_CODE(int a, int b) {
		// shift is += 01 00...
		return ((a << 4) | b);
	}
	static final int SPCL_KEY(int b) {
		// shift is += 4...
		return (SPCL | b);
	}
	// 'a' is mask of bits that change
	static final int MODE0_CHG(int a, int b) {
		return (MODE0 | (a << 4) | b);
	}
	static final int MODE1_CHG(int a, int b) {
		return (MODE1 | (a << 4) | b);
	}
	static final int MODE2_CHG(int a, int b) {
		return (MODE2 | (a << 4) | b);
	}
	static final int META_KEY(int b) {
		return (META | b);
	}
	static final int ALT_KEY(int b) {
		return (ALT | b);
	}
	// a = mask
	static final int META_PRE(int a, int b) {
		return (METAP | (a << 4) | b);
	}
	static final int META_SPL(int a, int b) {
		return (METAS | (a << 4) | b);
	}
	// group is never sent.
	// group=-1 is toggle (no group)
	// group=0 is momentary switch (no group)
	// group=N is ganged bank N (radio buttons)
	static final int GROUP(int a, int b) {
		return ((a << 12) | b);
	}

	public int getCode() {
		return code & 0x0ff;
	}
	public int getMode() {
		return code & 0x0f;
	}
	public int getMask() {
		return (code >> 4) & 0x0f;
	}
	public int getType() {
		return code & (0x0f << 8);
	}
	public int getGroup() {
		return (code >> 12);
	}
	public boolean isSHIFT() {
		return (code == SHIFT);
	}
	public boolean isFEED() {
		return (code == FEED);
	}
	public boolean isTAPE() {
		return (code <= TAPE_EJECT);
	}
	public boolean isMETA() {
		return (getType() == METAP || getType() == METAS);
	}
	public void setOn(boolean on) {
		state = on;
		if (on) {
			button.setBackground(altcolor);
		} else {
			button.setBackground(color);
		}
		button.repaint();
	}

	// null unless Wang700 toggle switches...
	static public ImageIcon toggle_on;
	static public ImageIcon toggle_off;

	Color color;
	Color altcolor;
	int code;
	boolean state;
	JButton button;
}
