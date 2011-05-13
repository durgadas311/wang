// Copyright (c) 2011 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

class _Key {
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color white2 = new Color(150, 150, 150, 255);
	static final Color illum1 = new Color(255, 255, 100, 255);
	static final Color red1 = new Color(255, 128, 128, 255);
	static final Color neon = new Color(244,157,33);
	static final Color neon2 = new Color(214,127,13);
	static final Color empty = new Color(50,50,50);

	static final int SPCL = 0x0100;
	static final int MODE0 = 0x0200;
	static final int MODE1 = 0x0300;
	static final int META = 0x0400;		// never sent
	static final int METAP = 0x0500;	// never sent

	public _Key(Color sl, int c) {
		this.color = sl;
		this.altcolor = sl;
		this.code = c;
		this.state = false;
	}

	static final int SHIFT = -1;
	static final int FEED = -2;

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
	static final int META_KEY(int b) {
		return (META | b);
	}
	// a = mask
	static final int META_PRE(int a, int b) {
		return (METAP | (a << 4) | b);
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

	Color color;
	Color altcolor;
	int code;
	boolean state;
}

// (red) CLEAR button is 00 14...
// f(x) is 10 xx
// F(x) is 11 xx
// XCHG is 14 xx
// I/O, etc is 15 xx

public class w600_fe {
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	public static void main(String[] args) {
		java.io.FileOutputStream fout = null;
		java.io.FileInputStream fin = null;

		if (args.length > 0) {
			if (args[0].compareTo("-t") != 0) {
// to be removed, once "./w600-sim -b" works...
				String fd = "/proc/self/fd/" + args[0];
				try {
					fout = new FileOutputStream(fd);
				} catch (FileNotFoundException e) {
					System.out.println("No pipe: " + fd);
					System.exit(1);
				}
				if (args.length > 1) {
					fd = "/proc/self/fd/" + args[1];
					try {
						fin = new FileInputStream(fd);
					} catch (FileNotFoundException e) {
						System.out.println("No pipe: " + fd);
						System.exit(1);
					}
				}
// ---------
			}
		}
		JFrame front_end = new JFrame("Wang 600");
		// need better layout...
		FlowLayout layout = new FlowLayout();
		front_end.setLayout(layout);

		Wang600_Display dsp = new Wang600_Display(fin);
		front_end.add(dsp);

		Wang600_Keyboard kbd = new Wang600_Keyboard(fout, dsp.pe, dsp.me);
		front_end.add(kbd);

		Wang600_SimInput inp = new Wang600_SimInput(fin, dsp);

		if (inp == null) System.out.println("damn warnings");
		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1000,500);
		front_end.setVisible(true);
	}
}

class Wang600_ProgErr extends JComponent {
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

	GridBagLayout gridbag = new GridBagLayout();
	JPanel pan;

	public Wang600_ProgErr(String label) {
		GridBagConstraints s = new GridBagConstraints();

		setLayout(gridbag);

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
		lab.setFont(new Font("Monospaced", Font.PLAIN, 8));
		lab.setPreferredSize(new Dimension(30, 25));
		lab.setForeground(Color.white);
		lab.setOpaque(false);
		s.gridx = 0;
		s.gridy = 0;
		gridbag.setConstraints(lab, s);
		add(lab);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		pan.setOpaque(true);
		pan.setBackground(_Key.empty);
		s.gridy = 1;
		gridbag.setConstraints(pan, s);
		add(pan);
	}

	public void setOn(boolean on) {
		if (on) {
			pan.setBackground(_Key.neon);
		} else {
			pan.setBackground(_Key.empty);
		}
	}
}

class Wang600_SimInput
		implements Runnable
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	Wang600_Display _dsp;
	Wang600_Printer _prt;
	Wang600_Tape _tape;
	Wang600_CN24 _cn24;

	FileInputStream _fin;

	public Wang600_SimInput(FileInputStream f, Wang600_Display dsp) {
		_dsp = dsp;
		_fin = f;
		if (f != null) {
			Thread t = new Thread(this);
			t.start();
		}
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	public void run() {
		int n = 0;
		byte[] b = new byte[2];

		while (true) {
			try {
				n = _fin.read(b);
			} catch (IOException ee) {
				System.err.println("Broken pipe for display!");
				System.exit(1);
			} finally {
			}
			if (n == 0) {
				continue;
			}
			if ((b[1]  & ~7) == 0x00) {
				_dsp.do_display(b);
			} else if ((b[1]  & ~1) == 0x80) {
				_prt.do_printer(b);
			} else if ((b[1]  & ~3) == 0x0c) {
				_tape.do_tape(b);
			} else if (b[1] == 0x10) {
				_cn24.do_cn24(b);
			} else {
				System.err.println("Unexpected traffic "+b[1]);
			}
		}
	}
}

class Wang600_Printer
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
//	final int PR_NUM_COL = 20;
//	final int PR_XCOL_WID = 3;
//	final int PR_XCOL_STRT = 15;
//
//	// these will be replaced with some translation to a special font...
//	final String[] pr_16 = {
//		" E ", " T ", " + ", " - ", " x ", " / ", " ST", " RE",
//		" * ", " * ", " f ", " F ", " A ", " B ", " C ", " D "
//	};
//	final String[] pr_17 = {
//		"0  ", "1  ", "2  ", "3  ", "4  ", "5  ", "6  ", "7  ",
//		"8  ", "9  ", "10 ", "11 ", "12 ", "13 ", "14 ", "15 "
//	};
//	final String[] pr_18 = {
//		" S ", " RE", " W ", " Go", " Jo", " J+", " SN", " CS",
//		" TN", " RD", " LN", " eX", " x2", " vX", " LP", "1/x"
//	};
//	final String[] pr_19 = {
//		" M ", " ST", " a ", " Sp", " Jn", " Je", " S1", " C1",
//		" T1", " DR", " LG", "10X", " I ", "|x|", " EP", " RT"
//	};
//	final String[] pr_20 = {
//		" X ", " Y ", " Z ", " A ", " B ", " C ", " D ", " E ",
//		" F ", " G ", " H ", " I ", " J ", " K ", " L ", " M "
//	};
//	final String[][] pr_16_20 = {
//		pr_16, pr_17, pr_18, pr_19, pr_20
//	}
//	final String[] pr_ovr = { "....OVERFLOW...." };
//	final String[] pr_0_15 = { "0123456789.o\0+- " };
//	final byte[] _pr_ovr = pr_ovr.getBytes();
//	final byte[] _pr_0_15 = pr_0_15.getBytes();
//
//	byte[] _pr_line;
//
//	private void clear_buf() {
//		_pr_line = new byte(16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16,16);
//	}
//
//	public Wang600_Printer() {
//		clear_buf();
//	}
//
	public void do_printer(byte[] b) {
		if (b[1] == 0) return; // dumb warnings
//		int col = ((b[1] << 4) | (b[0] >> 4)) & 0x1f;
//		if (col == 0x1f) {
//			String s = _pr_line.toString();
//			# replace with output to printer panel...
//			System.err.println(s);
//			clear_buf();
//			return;
//		}
//		int drm = (b[0] & 0x0f);
//		byte chr;
//		if (col < PR_XCOL_STRT) {
//			chr = _pr_0_15[drm];
//			if (chr == 0) {
//				chr = _pr_ovr[col];
//			}
//			_pr_line[col] = chr;
//		} else {
//			col -= PR_XCOL_STRT;
//			String yy = pr_16_20[col];
//			byte[] xx = yy.getBytes();
//			for (x = 0; x < PR_XCOL_WID; ++x) {
//			}
//		}
//
	}
}

class Wang600_Tape
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	public void do_tape(byte[] b) {
		if (b[1] == 0) return;
	}
}

class Wang600_CN24
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	public void do_cn24(byte[] b) {
		if (b[1] == 0) return;
	}
}

class Wang600_Display extends JComponent
		implements ActionListener
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692037L;
	final byte[] sign_chr = new byte[]{'+','-','+','-','+','-','+','-','+','-','+','-','+','-','+',' '};
	final byte[] disp_chr = new byte[]{'0','1','2','3','4','5','6','7','8','9','.','>','u','<','t',' '};

	byte[] disp_a;
	JLabel disp;
	FileInputStream _fin;
	GridBagLayout gridbag = new GridBagLayout();

	Wang600_ProgErr pe;
	Wang600_ProgErr me;
	boolean flashing;
	boolean state;
	javax.swing.Timer timer;

	private void flasher() {
		if (!flashing) {
			state = false;
			disp.setForeground(_Key.neon);
			return;
		}
		state = !state;
		if (state) {
			disp.setForeground(_Key.neon2);
		} else {
			disp.setForeground(_Key.neon);
		}
	}

	public void actionPerformed(ActionEvent e) {
		// verify the action is for the timer?
		if (e.getSource() == timer) {
			flasher();
//		} else if (e.getSource() == timer2) {
//			blanker();
		} else {
			// what was it? e.getSource().stop()???
		}
	}

	public Wang600_Display(FileInputStream f) {
		String blank = "--- ++++++++ ---";
		disp_a = new byte[16];
		disp_a = blank.getBytes();
		flashing = false;
		state = false;
		timer = new Timer(100, this);

		_fin = f;
		setLayout(gridbag);

		GridBagConstraints s = new GridBagConstraints();

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		disp = new JLabel(blank, SwingConstants.CENTER);
		disp.setForeground(_Key.neon);
		disp.setBackground(_Key.empty);
		disp.setOpaque(true);
		Font font;
		font = new Font("Wang600Display", Font.PLAIN, 28);
		if (font.getFontName() == "Wang600Display") {
			disp.setPreferredSize(new Dimension(475, 75));
		} else {
			System.err.println("Missing font \"Wang600Display.ttf\", using default");
			disp.setPreferredSize(new Dimension(450, 75));
			font = new Font("Monospaced", Font.PLAIN, 40);
		}
		disp.setFont(font);

		s.gridx = 0;
		s.gridy = 0;
		gridbag.setConstraints(disp, s);
		add(disp);

		JPanel pan;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(400, 25));
		pan.setOpaque(false);
		s.gridx = 1;
		gridbag.setConstraints(pan, s);
		add(pan);

		pe = new Wang600_ProgErr("Prog<BR>Err");
		pe.setOn(false);
		me = new Wang600_ProgErr("Mach<BR>Err");
		me.setOn(false);

	}

	private void setFlashing(boolean on) {
		if (on) {
			if (flashing) return;
			flashing = true;
			timer.start();
		} else {
			if (!flashing) return;
			flashing = false;
			timer.stop();
			flasher();
		}
	}

	// this really should be set aside in a neutral class, which is given
	// access to display, tape, printer, etc...
	public void do_display(byte[] b) {
		int ds;
		int dc;
		byte c;

		if ((b[1] & 2) != 0) {
			me.setOn(true);
			setFlashing(true);
		} else {
			me.setOn(false);
		}
		if ((b[1] & 1) != 0) {
			pe.setOn(true);
			setFlashing(true);
		} else {
			pe.setOn(false);
		}
		if ((b[1] & 3) == 0) {
			setFlashing(false);
		}
		String s;
		if ((b[1] & 4) != 0) {
			// blank-out display while Wang is not refreshing...
			s = new String("                ");
		} else {
			ds = (b[0] >> 4) & 0x0f;
			dc = b[0] & 0x0f;
			if (ds == 0 || ds == 13) {
				c = sign_chr[dc];
			} else {
				c = disp_chr[dc];
			}
			disp_a[ds] = c;
			s = new String(disp_a);
		}
		disp.setText(s);
		repaint();
	}
}

class Wang600_Keyboard extends JComponent
	implements ActionListener, KeyListener
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 3;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang600_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _shift;
	int _shift_kbd;
	int _shift_btn;
	int _meta;
	int _mode0;
	int _mode1;
	FileOutputStream _fout;

	private void setShift(boolean _new) {
		_shift = _new;
		if (_shift) {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_Key.illum1);
		} else {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_kbds[_shift_kbd]._keys[_shift_btn].color);
		}
	}

	private void setToggle(boolean on, _Key key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == _Key.METAP) {
			_meta &= ~key.getMask();
		} else if (key.getType() == _Key.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == _Key.MODE1) {
			_mode1 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == _Key.METAP) {
				_meta |= key.getMode();
			} else if (key.getType() == _Key.MODE0) {
				_mode0 |= key.getMode();
			} else if (key.getType() == _Key.MODE1) {
				_mode1 |= key.getMode();
			}
		} else {
			btn.setBackground(key.color);
		}
		key.state = on;
	}

	private void set_group(int g, int y, int x) {
		int z;
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			int tg = _kbds[y]._keys[z].getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			if (_kbds[y]._keys[z].state) {
				setToggle(false, _kbds[y]._keys[z], _kbds[y]._buttons[z]);
			}
		}
		setToggle(!_kbds[y]._keys[x].state, _kbds[y]._keys[x], _kbds[y]._buttons[x]);
	}

	private void do_button(ActionEvent e, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0) {
			// we really want to trap the "key down" event and set _shift then...
			_shift = true;
		}
		if (_kbds[y]._keys[x].isSHIFT()) {
			setShift(!_shift);
			return;
		}
		if (_kbds[y]._keys[x].isFEED()) {
			// TBD: advance printer paper...
			return;
		}
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (g != 0) {
			set_group(g, y, x);
		}
		if (type == _Key.METAP) {
			return;
		}
		// _mode0, _mode1, _meta were already updated above...
		if (type == _Key.MODE0) {
			code = _Key.MODE0 | _mode0;
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				code |= _kbds[y]._keys[x].getMode();
			}
		}
		if (type == _Key.SPCL) {
			code |= _Key.SPCL;
		}
		if (type == _Key.MODE1) {
			code = _Key.MODE1 | _mode1;
		}
		if (type == _Key.META) {
			code &= 0x00f;
			code |= (_meta << 4);
			type = 0;
		}
		if (_shift) {
			if (type == _Key.SPCL) {
				code += 4;
			} else if (type == 0) {
				code |= 0x010;
			}
			setShift(false);
		}

		if (_fout == null) {
			int t = code >> 8;
			int h = (code >> 4) & 0x0f;
			int l = code & 0x0f;
			System.out.format("%d %02d %02d (%04x)\n", t, h, l, code);
		} else {
			byte[] b = new byte[2];
			b[0] = (byte)(code & 0x0ff);
			b[1] = (byte)(code >> 8);
			try {
				_fout.write(b);
			} catch (IOException ee) {
				System.err.println("Broken pipe for keyboard!");
				_fout = null;
			}
		}
	}

	public Wang600_Keyboard(FileOutputStream fo, Wang600_ProgErr pe, Wang600_ProgErr me) {
		int x;
		_kbds = new Wang600_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_shift = false;
		_meta = 0;
		_fout = fo;
		Dimension dim = new Dimension(500, 25);
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;
		Wang600_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

		kbd = new Wang600_Keyboard_stick();
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_row;

		s.gridx = _col;
		s.gridy = _row;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		_col = 0;
		_row += 1;

		kbd = new Wang600_Keyboard_meta(pe, me);
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_row;

		s.gridx = _col;
		s.gridy = _row;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		_col = 0;
		_row += 1;

		kbd = new Wang600_Keyboard_main();
		for (x = 0; x < kbd._nkeys; ++x) {
			if (kbd._keys[x].code == _Key.SHIFT) {
				_shift_kbd = _nkbds;
				_shift_btn = x;
			}
			kbd._buttons[x].addActionListener(this);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		// This doesn't work... perhaps 'this' is not an "input object"?
		// addKeyListener(this);
		// setFocusTraversalKeysEnabled(false);
	}

	public void keyTyped(KeyEvent e) {
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(true);
		}
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
			setShift(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(e, y, x);
					return;
				}
			}
		}
	}
}

class Wang600_Keyboards extends JComponent
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	public Wang600_Keyboards() { }

	int _nkeys;
	_Key[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py,
						String icon, _Key key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		ImageIcon ic = new ImageIcon(icon);
		butt = new JButton(ic);
		butt.setBackground(key.color);
		butt.setBorder(lb);

		dim.width = 50 * lx;
		dim.height = 50 * ly;
		butt.setPreferredSize(dim);
		butt.setMargin(inset);

		c.gridwidth = lx;
		c.gridheight = ly;
		c.gridx = _col + px;
		c.gridy = _row + py;
		gridbag.setConstraints(butt, c);

		add(butt);
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}

	void addPushButton(GridBagConstraints c, int lx, int ly, int px, int py,
				String toplab, String botlab, Color alt, boolean init, _Key key) {
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
			lab.setFont(new Font("Monospaced", Font.PLAIN, 8));
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
			lab.setFont(new Font("Monospaced", Font.PLAIN, 8));
			lab.setForeground(Color.white);
			lab.setOpaque(false);
			c.insets.left = 0;
			c.insets.right = 0;
			c.gridx = _col + px;
			c.gridy = _row + 2;
			gridbag.setConstraints(lab, c);
			add(lab);
		}

		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}
}

class Wang600_Keyboard_main extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 54;

	public Wang600_Keyboard_main() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		Dimension dim = new Dimension(25, 200);
		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints s = new GridBagConstraints();
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 4;

		setLayout(gridbag);

		addButton(c, 1, 1, 0, 0,"icons/prime.gif",
			new _Key(_Key.orange1, _Key.SPCL_KEY(0)));
		addButton(c,1, 1, 0, 1, "icons/rad_deg.gif",
			new _Key(_Key.green1,_Key.PROG_CODE(8,9)));
		addButton(c,1, 2, 0, 2, "icons/shift.gif",
			new _Key(_Key.white1, _Key.SHIFT));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/sin.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,6)));
		addButton(c,1, 1, 0, 1, "icons/tan.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,8)));
		addButton(c,1, 1, 0, 2, "icons/logex.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,10)));
		addButton(c,1, 1, 0, 3, "icons/x2.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,12)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/cos.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,7)));
		addButton(c,1, 1, 0, 1, "icons/inv.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,15)));
		addButton(c,1, 1, 0, 2, "icons/ex.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,11)));
		addButton(c,1, 1, 0, 3, "icons/sqrt.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,13)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(1,15)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(5,15)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,15)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,15)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(3,15)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(2,15)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(7,15)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/chg_sign.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,12)));
		addButton(c,1, 1, 0, 1, "icons/clear_disp.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,15)));
		addButton(c,1, 1, 0, 2, "icons/set_exp.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(0,11)));
		addButton(c,2, 1, 0, 3, "icons/zero.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,0)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/seven.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,7)));
		addButton(c,1, 1, 0, 1, "icons/four.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,4)));
		addButton(c,1, 1, 0, 2, "icons/one.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,1)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/eight.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,8)));
		addButton(c,1, 1, 0, 1, "icons/five.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,5)));
		addButton(c,1, 1, 0, 2, "icons/two.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,2)));
		addButton(c,2, 1, 0, 3, "icons/dp.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,10)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/nine.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,9)));
		addButton(c,1, 1, 0, 1, "icons/six.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,6)));
		addButton(c,1, 1, 0, 2, "icons/three.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(0,3)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/minus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(3,14)));
		addButton(c,1, 2, 0, 1, "icons/plus.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(2,14)));
		addButton(c,1, 1, 0, 3, "icons/recall.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(7,14)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/total.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(1,14)));
		addButton(c,1, 1, 0, 1, "icons/div.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(5,14)));
		addButton(c,1, 1, 0, 2, "icons/mult.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(4,14)));
		addButton(c,1, 1, 0, 3, "icons/store.gif",
			new _Key(_Key.blue1, _Key.PROG_CODE(6,14)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, "icons/ld_prog.gif",
			new _Key(_Key.orange1, _Key.PROG_CODE(8,14)));
		addButton(c,1, 1, 0, 1, "icons/search.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,0)));
		addButton(c,1, 2, 0, 2, "icons/go.gif",
			new _Key(_Key.white1, _Key.PROG_CODE(8,3)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/jif0.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,4)));
		addButton(c,1, 1, 0, 1, "icons/jifplus.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,5)));
		addButton(c,1, 1, 0, 2, "icons/recall_xx.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,1)));
		addButton(c,1, 1, 0, 3, "icons/print.gif",
			new _Key(_Key.green1, _Key.PROG_CODE(8,2)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/i_o.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,2)));
		addButton(c,1, 1, 0, 1, "icons/group1.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,13)));
		addButton(c,1, 1, 0, 2, "icons/group2.gif",
			new _Key(_Key.pink1, _Key.PROG_CODE(15,14)));
		addButton(c,1, 1, 0, 3, "icons/indir.gif",
			new _Key(_Key.orange1, _Key.PROG_CODE(15,11)));
		++_col;
		addButton(c,1, 1, 0, 0, "icons/set_pc.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(2)));
		addButton(c,1, 1, 0, 1, "icons/verif_prog.gif",
			new _Key(_Key.green1, _Key.SPCL_KEY(1)));
		addButton(c,1, 1, 0, 2, "icons/rec_prog.gif",
			new _Key(_Key.orange1, _Key.SPCL_KEY(3)));
		addButton(c,1, 1, 0, 3, "icons/step.gif",
			new _Key(_Key.green1, _Key.MODE0_CHG(8,8)));
		_col = 0;
		_row += 4;
	}
}

class Wang600_Keyboard_meta extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 16;

	public Wang600_Keyboard_meta(Wang600_ProgErr pe, Wang600_ProgErr me) {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(70, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		setLayout(gridbag);

		addButton(c,1, 1, 0, 0, "icons/k00.gif",
			new _Key(_Key.white1, _Key.META_KEY(0)));
		addButton(c,1, 1, 1, 0, "icons/k01.gif",
			new _Key(_Key.white1, _Key.META_KEY(1)));
		addButton(c,1, 1, 2, 0, "icons/k02.gif",
			new _Key(_Key.white1, _Key.META_KEY(2)));
		addButton(c,1, 1, 3, 0, "icons/k03.gif",
			new _Key(_Key.white1, _Key.META_KEY(3)));
		addButton(c,1, 1, 4, 0, "icons/k04.gif",
			new _Key(_Key.white1, _Key.META_KEY(4)));
		addButton(c,1, 1, 5, 0, "icons/k05.gif",
			new _Key(_Key.white1, _Key.META_KEY(5)));
		addButton(c,1, 1, 6, 0, "icons/k06.gif",
			new _Key(_Key.white1, _Key.META_KEY(6)));
		addButton(c,1, 1, 7, 0, "icons/k07.gif",
			new _Key(_Key.white1, _Key.META_KEY(7)));
		addButton(c,1, 1, 8, 0, "icons/k08.gif",
			new _Key(_Key.white1, _Key.META_KEY(8)));
		addButton(c,1, 1, 9, 0, "icons/k09.gif",
			new _Key(_Key.white1, _Key.META_KEY(9)));
		addButton(c,1, 1, 10, 0, "icons/k10.gif",
			new _Key(_Key.white1, _Key.META_KEY(10)));
		addButton(c,1, 1, 11, 0, "icons/k11.gif",
			new _Key(_Key.white1, _Key.META_KEY(11)));
		addButton(c,1, 1, 12, 0, "icons/k12.gif",
			new _Key(_Key.white1, _Key.META_KEY(12)));
		addButton(c,1, 1, 13, 0, "icons/k13.gif",
			new _Key(_Key.white1, _Key.META_KEY(13)));
		addButton(c,1, 1, 14, 0, "icons/k14.gif",
			new _Key(_Key.white1, _Key.META_KEY(14)));
		addButton(c,1, 1, 15, 0, "icons/k15.gif",
			new _Key(_Key.white1, _Key.META_KEY(15)));
		_col += 16;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		c.gridx = _col;
		gridbag.setConstraints(pe, c);
		add(pe);
		++_col;
		c.gridx = _col;
		gridbag.setConstraints(me, c);
		add(me);

		++_col;
		_col = 0;
		_row += 1;

	}
}

class Wang600_Keyboard_stick extends Wang600_Keyboards
{
	final String ident = "$Id: w600_fe.java,v 1.24 2011/05/13 12:51:44 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 18;

	public Wang600_Keyboard_stick() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();
		Dimension dim = new Dimension(20, 30);
		JPanel pan;

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;

		setLayout(gridbag);

		addPushButton(c, 15, 1, 0, 0,"Run","",_Key.white2, true,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,0))));
		addPushButton(c, 15, 1, 1, 0,"Learn","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,4))));
		addPushButton(c, 15, 1, 2, 0,"Learn<BR>and<BR>Print","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,6))));
		addPushButton(c, 15, 1, 3, 0,"List<BR>Program","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(1,_Key.MODE0_CHG(6,2))));
		_col += 4;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 30, 1, 0, 0,"Clear","",null, false,
			new _Key(_Key.red1, _Key.PROG_CODE(0,14)));

		addPushButton(c, 5, 1, 1, 0,"T","1",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,1))));
		addPushButton(c, 5, 1, 2, 0,"+","2",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,2))));
		addPushButton(c, 5, 1, 3, 0,"-","3",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,3))));
		addPushButton(c, 5, 1, 4, 0,"X","4",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,4))));
		addPushButton(c, 5, 1, 5, 0,"&divide;","5",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,5))));
		addPushButton(c, 5, 1, 6, 0,"St","6",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,6))));
		addPushButton(c, 5, 1, 7, 0,"Re","7",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(7,7))));
		addPushButton(c, 5, 1, 8, 0,"f(x)","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(2,_Key.META_PRE(15,10))));
		addPushButton(c, 5, 1, 9, 0,"Sp<BR>\u2193<BR>On", "8",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(3,_Key.META_PRE(8,8))));
		addPushButton(c, 5, 1, 10, 0,"Fl<BR>\u2195<BR>Sc","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(4,_Key.MODE0_CHG(1,1))));
		addPushButton(c, 5, 1, 11, 0,"Deg<BR>\u2195<BR>Rad","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(5,_Key.MODE1_CHG(1,1))));
		addPushButton(c, 5, 1, 12, 0,"Printer<BR>\u2193<BR>On","",_Key.white2, false,
			new _Key(_Key.white1, _Key.GROUP(6,_Key.MODE1_CHG(2,2))));
		addPushButton(c, 5, 1, 13, 0,"Paper<BR>Feed","",null, false,
			new _Key(_Key.white1, _Key.FEED));
		_col += 14;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(340, 30));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		_col = 0;
		_row += 1;

	}
}
