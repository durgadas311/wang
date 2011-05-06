import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.io.*;

class _Key {

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color white2 = new Color(200, 200, 200, 255);
	static final Color illum1 = new Color(255, 255, 230, 255);
	static final Color red1 = new Color(255, 128, 128, 255);
	static final Color red2 = new Color(192, 96, 96, 255);

	static final int SPCL = 0x0100;
	static final int MODE0 = 0x0200;
	static final int MODE1 = 0x0300;
	static final int META = 0x0400;
	static final int METAP = 0x0500;

	public _Key(String l, Color sl, int c) {
		this.icon = l;
		this.color = sl;
		this.code = c;
	}

	static final int SHIFT = -1;

	static final int PROG_CODE(int a, int b) {
		// shift is += 01 00...
		return ((a << 4) | b);
	}
	static final int SPCL_KEY(int b) {
		// shift is += 4...
		return (SPCL | b);
	}
	static final int MODE0_CHG(int b) {
		return (MODE0 | b);
	}
	static final int MODE1_CHG(int b) {
		return (MODE1 | b);
	}
	static final int META_KEY(int b) {
		return (META | b);
	}
	static final int META_PRE(int b) {
		return (METAP | (b << 4));
	}

	String icon;
	Color color;
	int code;
}

// (red) CLEAR button is 00 14...
// f(x) is 10 xx
// F(x) is 11 xx
// XCHG is 14 xx
// I/O, etc is 15 xx

public class w600_fe {
	public static void main(String[] args) {
		java.io.FileOutputStream fout = null;

		if (args.length > 0) {
			String fd = "/proc/self/fd/" + args[0];
			try {
				fout = new FileOutputStream(fd);
			} catch (FileNotFoundException e) {
				System.out.println("No pipe: " + fd);
				System.exit(1);
			}
		}
		JFrame front_end = new JFrame("Wang 600 Keyboard");

		Wang600_Keyboard kbd = new Wang600_Keyboard(fout);
		front_end.add(kbd);

		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1000,500);
		front_end.setVisible(true);
	}
}

class Wang600_Keyboard extends JComponent
	implements ActionListener
{
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
	FileOutputStream _fout;

	private void setShift(boolean _new) {
		_shift = _new;
		if (_shift) {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_Key.illum1);
		} else {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_kbds[_shift_kbd]._keys[_shift_btn].color);
		}
	}

	public Wang600_Keyboard(FileOutputStream fo) {
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

		kbd = new Wang600_Keyboard_meta();
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
	}

	public void actionPerformed(ActionEvent e) {
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					int code = _kbds[y]._keys[x].code;
					if (code == _Key.SHIFT) {
						setShift(!_shift);
						continue;
					}
					int type = code & ~0x0ff;
					if (type == _Key.METAP) {
						code &= 0x0ff;
						if (_meta == code) {
							_meta = 0; // simple toggle
						} else {
							_meta = code;
						}
						continue;
					}
					if (type == _Key.META) {
						code |= _meta;
						code &= 0x0ff;
						type = 0;
					}
					if (type == _Key.SPCL) {
						if (_shift) {
							code += 4;
							setShift(false);
						}
					}
					if (type == 0) {
						if (_shift) {
							code |= 0x010;
							setShift(false);
						}
					}

					if (_fout == null) {
						int t = code >> 8;
						int h = (code >> 4) & 0x0f;
						int l = code & 0x0f;
						System.out.format("%d %02d %02d\n", t, h, l);
					} else {
						byte[] b = new byte[2];
						b[0] = (byte)(code & 0x0ff);
						b[1] = (byte)(code >> 8);
						try {
							_fout.write(b);
						} catch (IOException ee) {
						}
					}
					break;
				}
			}
		}
	}
}

class Wang600_Keyboards extends JComponent
{
	static final long serialVersionUID = 311457692032L;
	public Wang600_Keyboards() { }

	int _nkeys;
	_Key[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, int lx, int ly, int px, int py, _Key key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		ImageIcon ic = new ImageIcon(key.icon);
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

	void addPushButton(GridBagConstraints c, int lx, int ly, int px, int py, _Key key) {
		final Dimension dim = new Dimension(15, 30);
		JButton butt;

		butt = new JButton();

		butt.setPreferredSize(dim);
		butt.setBackground(key.color);

		c.insets.top = 0;
		c.insets.bottom = 0;
		c.insets.left = ly; // stupid warnings
		c.insets.left = lx;
		c.insets.right = lx;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = _col + px;
		c.gridy = _row + py;
		gridbag.setConstraints(butt, c);

		add(butt);
		_buttons[_nkeys] = butt;
		_keys[_nkeys] = key;
		++_nkeys;
	}
}

class Wang600_Keyboard_main extends Wang600_Keyboards
{
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
		c.anchor = GridBagConstraints.SOUTHWEST;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 4;

		setLayout(gridbag);

		addButton(c, 1, 1, 0, 0,
			new _Key("icons/prime.gif",_Key.orange1, _Key.SPCL_KEY(0)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/rad_deg.gif",_Key.green1,_Key.PROG_CODE(8,9)));
		addButton(c,1, 2, 0, 2, 
			new _Key("icons/shift.gif",_Key.white1, _Key.SHIFT));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/sin.gif",	_Key.green1, _Key.PROG_CODE(8,6)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/tan.gif",	_Key.green1, _Key.PROG_CODE(8,8)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/logex.gif",_Key.green1, _Key.PROG_CODE(8,10)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/x2.gif",	_Key.green1, _Key.PROG_CODE(8,12)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/cos.gif",	_Key.green1, _Key.PROG_CODE(8,7)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/inv.gif",	_Key.green1, _Key.PROG_CODE(8,15)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/ex.gif",	_Key.green1, _Key.PROG_CODE(8,11)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/sqrt.gif",	_Key.green1, _Key.PROG_CODE(8,13)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 
			new _Key("icons/total.gif",_Key.blue1, _Key.PROG_CODE(1,15)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/div.gif",	_Key.blue1, _Key.PROG_CODE(5,15)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/mult.gif",	_Key.blue1, _Key.PROG_CODE(4,15)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/store.gif",_Key.blue1, _Key.PROG_CODE(6,15)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/minus.gif",_Key.white1, _Key.PROG_CODE(3,15)));
		addButton(c,1, 2, 0, 1, 
			new _Key("icons/plus.gif",	_Key.white1, _Key.PROG_CODE(2,15)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/recall.gif",_Key.blue1, _Key.PROG_CODE(7,15)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 
			new _Key("icons/chg_sign.gif",_Key.green1, _Key.PROG_CODE(0,12)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/clear_disp.gif",_Key.green1, _Key.PROG_CODE(0,15)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/set_exp.gif",_Key.green1, _Key.PROG_CODE(0,11)));
		addButton(c,2, 1, 0, 3, 
			new _Key("icons/zero.gif",	_Key.white1, _Key.PROG_CODE(0,0)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/seven.gif",_Key.white1, _Key.PROG_CODE(0,7)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/four.gif",	_Key.white1, _Key.PROG_CODE(0,4)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/one.gif",	_Key.white1, _Key.PROG_CODE(0,1)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/eight.gif",_Key.white1, _Key.PROG_CODE(0,8)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/five.gif",	_Key.white1, _Key.PROG_CODE(0,5)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/two.gif",	_Key.white1, _Key.PROG_CODE(0,2)));
		addButton(c,2, 1, 0, 3, 
			new _Key("icons/dp.gif",	_Key.white1, _Key.PROG_CODE(0,10)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/nine.gif",	_Key.white1, _Key.PROG_CODE(0,9)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/six.gif",	_Key.white1, _Key.PROG_CODE(0,6)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/three.gif",_Key.white1, _Key.PROG_CODE(0,3)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 
			new _Key("icons/minus.gif",_Key.white1, _Key.PROG_CODE(3,14)));
		addButton(c,1, 2, 0, 1, 
			new _Key("icons/plus.gif",	_Key.white1, _Key.PROG_CODE(2,14)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/recall.gif",_Key.blue1, _Key.PROG_CODE(7,14)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/total.gif",_Key.blue1, _Key.PROG_CODE(1,14)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/div.gif",	_Key.blue1, _Key.PROG_CODE(5,14)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/mult.gif",	_Key.blue1, _Key.PROG_CODE(4,14)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/store.gif",_Key.blue1, _Key.PROG_CODE(6,14)));
		++_col;

		s.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, s);
		add(pan);
		++_col;

		addButton(c,1, 1, 0, 0, 
			new _Key("icons/ld_prog.gif",_Key.orange1, _Key.PROG_CODE(8,14)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/search.gif",_Key.green1, _Key.PROG_CODE(8,0)));
		addButton(c,1, 2, 0, 2, 
			new _Key("icons/go.gif",	_Key.white1, _Key.PROG_CODE(8,3)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/jif0.gif",	_Key.green1, _Key.PROG_CODE(8,4)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/jifplus.gif",_Key.green1, _Key.PROG_CODE(8,5)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/recall_xx.gif",_Key.green1, _Key.PROG_CODE(8,1)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/print.gif",_Key.green1, _Key.PROG_CODE(8,2)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/i_o.gif",	_Key.pink1, _Key.PROG_CODE(15,0)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/group1.gif",_Key.pink1, _Key.PROG_CODE(15,1)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/group2.gif",_Key.pink1, _Key.PROG_CODE(15,2)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/indir.gif",_Key.orange1, _Key.PROG_CODE(15,3)));
		++_col;
		addButton(c,1, 1, 0, 0, 
			new _Key("icons/set_pc.gif",_Key.green1, _Key.SPCL_KEY(2)));
		addButton(c,1, 1, 0, 1, 
			new _Key("icons/verif_prog.gif",_Key.green1, _Key.SPCL_KEY(1)));
		addButton(c,1, 1, 0, 2, 
			new _Key("icons/rec_prog.gif",_Key.orange1, _Key.SPCL_KEY(3)));
		addButton(c,1, 1, 0, 3, 
			new _Key("icons/step.gif",	_Key.green1, _Key.MODE0_CHG(8)));
		_col = 0;
		_row += 4;
	}
}

class Wang600_Keyboard_meta extends Wang600_Keyboards
{
	static final long serialVersionUID = 311457692032L;
	static final int num_keys = 16;

	public Wang600_Keyboard_meta() {
		_buttons = new JButton[num_keys];
		_keys = new _Key[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;

		setLayout(gridbag);

		addButton(c,1, 1, 0, 0, 
			new _Key("icons/k00.gif",_Key.white1, _Key.META_KEY(0)));
		addButton(c,1, 1, 1, 0, 
			new _Key("icons/k01.gif",_Key.white1, _Key.META_KEY(1)));
		addButton(c,1, 1, 2, 0, 
			new _Key("icons/k02.gif",_Key.white1, _Key.META_KEY(2)));
		addButton(c,1, 1, 3, 0, 
			new _Key("icons/k03.gif",_Key.white1, _Key.META_KEY(3)));
		addButton(c,1, 1, 4, 0, 
			new _Key("icons/k04.gif",_Key.white1, _Key.META_KEY(4)));
		addButton(c,1, 1, 5, 0, 
			new _Key("icons/k05.gif",_Key.white1, _Key.META_KEY(5)));
		addButton(c,1, 1, 6, 0, 
			new _Key("icons/k06.gif",_Key.white1, _Key.META_KEY(6)));
		addButton(c,1, 1, 7, 0, 
			new _Key("icons/k07.gif",_Key.white1, _Key.META_KEY(7)));
		addButton(c,1, 1, 8, 0, 
			new _Key("icons/k08.gif",_Key.white1, _Key.META_KEY(8)));
		addButton(c,1, 1, 9, 0, 
			new _Key("icons/k09.gif",_Key.white1, _Key.META_KEY(9)));
		addButton(c,1, 1, 10, 0, 
			new _Key("icons/k10.gif",_Key.white1, _Key.META_KEY(10)));
		addButton(c,1, 1, 11, 0, 
			new _Key("icons/k11.gif",_Key.white1, _Key.META_KEY(11)));
		addButton(c,1, 1, 12, 0, 
			new _Key("icons/k12.gif",_Key.white1, _Key.META_KEY(12)));
		addButton(c,1, 1, 13, 0, 
			new _Key("icons/k13.gif",_Key.white1, _Key.META_KEY(13)));
		addButton(c,1, 1, 14, 0, 
			new _Key("icons/k14.gif",_Key.white1, _Key.META_KEY(14)));
		addButton(c,1, 1, 15, 0, 
			new _Key("icons/k15.gif",_Key.white1, _Key.META_KEY(15)));
		_col = 0;
		_row += 1;

	}
}

class Wang600_Keyboard_stick extends Wang600_Keyboards
{
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
		c.anchor = GridBagConstraints.SOUTHWEST;

		setLayout(gridbag);

		addPushButton(c, 15, 1, 0, 0,
			new _Key("Run",_Key.white1, _Key.MODE0_CHG(0)));
		addPushButton(c, 15, 1, 1, 0,
			new _Key("Learn",_Key.white1, _Key.MODE0_CHG(4)));
		addPushButton(c, 15, 1, 2, 0,
			new _Key("L+P",_Key.white1, _Key.MODE0_CHG(6)));
		addPushButton(c, 15, 1, 3, 0,
			new _Key("List",_Key.white1, _Key.MODE0_CHG(2)));
		_col += 4;

		c.gridx = _col;
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		addPushButton(c, 30, 1, 0, 0,
			new _Key("Clear",_Key.red1, _Key.PROG_CODE(0,14)));

		addPushButton(c, 5, 1, 1, 0,
			new _Key("T",_Key.white1, _Key.META_PRE(1)));
		addPushButton(c, 5, 1, 2, 0,
			new _Key("+",_Key.white1, _Key.META_PRE(2)));
		addPushButton(c, 5, 1, 3, 0,
			new _Key("-",_Key.white1, _Key.META_PRE(3)));
		addPushButton(c, 5, 1, 4, 0,
			new _Key("*",_Key.white1, _Key.META_PRE(4)));
		addPushButton(c, 5, 1, 5, 0,
			new _Key("/",_Key.white1, _Key.META_PRE(5)));
		addPushButton(c, 5, 1, 6, 0,
			new _Key("St",_Key.white1, _Key.META_PRE(6)));
		addPushButton(c, 5, 1, 7, 0,
			new _Key("Re",_Key.white1, _Key.META_PRE(7)));
		addPushButton(c, 5, 1, 8, 0,
			new _Key("f(x)",_Key.white1, _Key.META_PRE(10)));
		addPushButton(c, 5, 1, 9, 0,
			new _Key("Sp",_Key.white1, _Key.META_PRE(8)));
		addPushButton(c, 5, 1, 10, 0,
			new _Key("Fl",_Key.white1, _Key.MODE0_CHG(1)));
		addPushButton(c, 5, 1, 11, 0,
			new _Key("Deg",_Key.white1, _Key.MODE1_CHG(1)));
		addPushButton(c, 5, 1, 12, 0,
			new _Key("Printer",_Key.white1, _Key.MODE1_CHG(2)));
		addPushButton(c, 5, 1, 13, 0,
			new _Key("Feed",_Key.white1, _Key.MODE1_CHG(0)));
		_col += 14;

		c.gridx = _col;
		dim = new Dimension(340, 30);
		pan = new JPanel();
		pan.setPreferredSize(dim);
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		++_col;

		_col = 0;
		_row += 1;

	}
}
