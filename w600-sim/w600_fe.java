import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class _KeyArray {

	static final Color orange1 = new Color(255, 210, 180, 255);
	static final Color blue1 = new Color(190, 230, 255, 255);
	static final Color green1 = new Color(230, 240, 220, 255);
	static final Color pink1 = new Color(255, 220, 220, 255);
	static final Color white1 = new Color(250, 250, 250, 255);
	static final Color illum1 = new Color(255, 255, 250, 255);

	public _KeyArray(String l, Color sl, int lx, int ly, int px, int py, int c) {
		this.icon = l;
		this.color = sl;
		this.lx = lx;
		this.ly = ly;
		this.px = px;
		this.py = py;
		this.code = c;
	}

	static final int SHIFT = -10;
	static final int STEP = -11;

	static final int PROG_CODE(int a, int b) {
		// shift is += 01 00...
		return ((a << 4) | b);
	}
	static final int SPCL_KEY(int b) {
		// shift is += 4...
		return (0x0100 | b);
	}
	static final int MODE0_CHG(int b) {
		return (0x0200 | b);
	}
	static final int MODE1_CHG(int b) {
		return (0x0300 | b);
	}
	static final int META_KEY(int b) {
		return (0x0400 | b);
	}

	String icon;
	Color color;
	int lx;
	int ly;
	int px;
	int py;
	int code;
}

public class w600_fe {
	public static void main(String[] args) {

// (red) CLEAR button is 00 14...
// f(x) is 10 xx
// F(x) is 11 xx
// XCHG is 14 xx
// I/O, etc is 15 xx
		JFrame front_end = new JFrame("Wang 600 Keyboard");

		Wang600_Keyboard kbd = new Wang600_Keyboard();
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
	static final int num_kbds = 2;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang600_Keyboards[] _kbds;
	int _row;
	int _col;
	boolean _shift;
	int _shift_kbd;
	int _shift_btn;

	private void setShift(boolean _new) {
		_shift = _new;
		if (_shift) {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_KeyArray.orange1);
		} else {
			_kbds[_shift_kbd]._buttons[_shift_btn].setBackground(_kbds[_shift_kbd]._keys[_shift_btn].color);
		}
	}

	public Wang600_Keyboard() {
		int x;
		_kbds = new Wang600_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_shift = false;
		Dimension dim = new Dimension(500, 25);
		GridBagConstraints s = new GridBagConstraints();
		JSeparator sep;
		Wang600_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

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
		sep = new JSeparator(SwingConstants.HORIZONTAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, s);
		add(sep);
		_col = 0;
		_row += 1;

		kbd = new Wang600_Keyboard_main();
		for (x = 0; x < kbd._nkeys; ++x) {
			if (kbd._keys[x].code == _KeyArray.SHIFT) {
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
					if (y == _shift_kbd && x == _shift_btn) {
						setShift(!_shift);
					} else if (_kbds[y]._keys[x].code < 0) {
						System.out.println(_kbds[y]._keys[x].code);
					} else {
						int code = _kbds[y]._keys[x].code;
						if (_shift) {
							code += 0x10;
							setShift(false);
						}
						int h = code >> 4;
						int l = code & 0x0f;
						System.out.format("%02d %02d\n", h, l);
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
	_KeyArray[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, _KeyArray key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		ImageIcon ic = new ImageIcon(key.icon);
		butt = new JButton(ic);
		butt.setBackground(key.color);
		butt.setBorder(lb);

		dim.width = 50 * key.lx;
		dim.height = 50 * key.ly;
		butt.setPreferredSize(dim);
		butt.setMargin(inset);

		c.gridwidth = key.lx;
		c.gridheight = key.ly;
		c.gridx = _col + key.px;
		c.gridy = _row + key.py;
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
		_keys = new _KeyArray[num_keys];
		_nkeys = 0;
		_row = 0;
		_col = 0;
		Dimension dim = new Dimension(25, 200);
		GridBagConstraints c = new GridBagConstraints();
		GridBagConstraints s = new GridBagConstraints();
		JSeparator sep;

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

		addButton(c,
			new _KeyArray("icons/prime.gif",_KeyArray.orange1, 1, 1, 0, 0, _KeyArray.SPCL_KEY(0)));
		addButton(c,
			new _KeyArray("icons/rad_deg.gif",_KeyArray.green1,1, 1, 0, 1, _KeyArray.PROG_CODE(8,9)));
		addButton(c,
			new _KeyArray("icons/shift.gif",_KeyArray.white1, 1, 2, 0, 2, _KeyArray.SHIFT));
		++_col;
		addButton(c,
			new _KeyArray("icons/sin.gif",	_KeyArray.green1, 1, 1, 0, 0, _KeyArray.PROG_CODE(8,6)));
		addButton(c,
			new _KeyArray("icons/tan.gif",	_KeyArray.green1, 1, 1, 0, 1, _KeyArray.PROG_CODE(8,8)));
		addButton(c,
			new _KeyArray("icons/logex.gif",_KeyArray.green1, 1, 1, 0, 2, _KeyArray.PROG_CODE(8,10)));
		addButton(c,
			new _KeyArray("icons/x2.gif",	_KeyArray.green1, 1, 1, 0, 3, _KeyArray.PROG_CODE(8,12)));
		++_col;
		addButton(c,
			new _KeyArray("icons/cos.gif",	_KeyArray.green1, 1, 1, 0, 0, _KeyArray.PROG_CODE(8,7)));
		addButton(c,
			new _KeyArray("icons/inv.gif",	_KeyArray.green1, 1, 1, 0, 1, _KeyArray.PROG_CODE(8,15)));
		addButton(c,
			new _KeyArray("icons/ex.gif",	_KeyArray.green1, 1, 1, 0, 2, _KeyArray.PROG_CODE(8,11)));
		addButton(c,
			new _KeyArray("icons/sqrt.gif",	_KeyArray.green1, 1, 1, 0, 3, _KeyArray.PROG_CODE(8,13)));
		++_col;

		s.gridx = _col;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, s);
		add(sep);
		++_col;

		addButton(c,
			new _KeyArray("icons/total.gif",_KeyArray.blue1, 1, 1, 0, 0, _KeyArray.PROG_CODE(1,15)));
		addButton(c,
			new _KeyArray("icons/div.gif",	_KeyArray.blue1, 1, 1, 0, 1, _KeyArray.PROG_CODE(5,15)));
		addButton(c,
			new _KeyArray("icons/mult.gif",	_KeyArray.blue1, 1, 1, 0, 2, _KeyArray.PROG_CODE(4,15)));
		addButton(c,
			new _KeyArray("icons/store.gif",_KeyArray.blue1, 1, 1, 0, 3, _KeyArray.PROG_CODE(6,15)));
		++_col;
		addButton(c,
			new _KeyArray("icons/minus.gif",_KeyArray.white1, 1, 1, 0, 0, _KeyArray.PROG_CODE(3,15)));
		addButton(c,
			new _KeyArray("icons/plus.gif",	_KeyArray.white1, 1, 2, 0, 1, _KeyArray.PROG_CODE(2,15)));
		addButton(c,
			new _KeyArray("icons/recall.gif",_KeyArray.blue1, 1, 1, 0, 3, _KeyArray.PROG_CODE(7,15)));
		++_col;

		s.gridx = _col;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, s);
		add(sep);
		++_col;

		addButton(c,
			new _KeyArray("icons/chg_sign.gif",_KeyArray.green1, 1, 1, 0, 0, _KeyArray.PROG_CODE(0,12)));
		addButton(c,
			new _KeyArray("icons/clear_disp.gif",_KeyArray.green1, 1, 1, 0, 1, _KeyArray.PROG_CODE(0,15)));
		addButton(c,
			new _KeyArray("icons/set_exp.gif",_KeyArray.green1, 1, 1, 0, 2, _KeyArray.PROG_CODE(0,11)));
		addButton(c,
			new _KeyArray("icons/zero.gif",	_KeyArray.white1, 2, 1, 0, 3, _KeyArray.PROG_CODE(0,0)));
		++_col;
		addButton(c,
			new _KeyArray("icons/seven.gif",_KeyArray.white1, 1, 1, 0, 0, _KeyArray.PROG_CODE(0,7)));
		addButton(c,
			new _KeyArray("icons/four.gif",	_KeyArray.white1, 1, 1, 0, 1, _KeyArray.PROG_CODE(0,4)));
		addButton(c,
			new _KeyArray("icons/one.gif",	_KeyArray.white1, 1, 1, 0, 2, _KeyArray.PROG_CODE(0,1)));
		++_col;
		addButton(c,
			new _KeyArray("icons/eight.gif",_KeyArray.white1, 1, 1, 0, 0, _KeyArray.PROG_CODE(0,8)));
		addButton(c,
			new _KeyArray("icons/five.gif",	_KeyArray.white1, 1, 1, 0, 1, _KeyArray.PROG_CODE(0,5)));
		addButton(c,
			new _KeyArray("icons/two.gif",	_KeyArray.white1, 1, 1, 0, 2, _KeyArray.PROG_CODE(0,2)));
		addButton(c,
			new _KeyArray("icons/dp.gif",	_KeyArray.white1, 2, 1, 0, 3, _KeyArray.PROG_CODE(0,10)));
		++_col;
		addButton(c,
			new _KeyArray("icons/nine.gif",	_KeyArray.white1, 1, 1, 0, 0, _KeyArray.PROG_CODE(0,9)));
		addButton(c,
			new _KeyArray("icons/six.gif",	_KeyArray.white1, 1, 1, 0, 1, _KeyArray.PROG_CODE(0,6)));
		addButton(c,
			new _KeyArray("icons/three.gif",_KeyArray.white1, 1, 1, 0, 2, _KeyArray.PROG_CODE(0,3)));
		++_col;

		s.gridx = _col;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, s);
		add(sep);
		++_col;

		addButton(c,
			new _KeyArray("icons/minus.gif",_KeyArray.white1, 1, 1, 0, 0, _KeyArray.PROG_CODE(3,14)));
		addButton(c,
			new _KeyArray("icons/plus.gif",	_KeyArray.white1, 1, 2, 0, 1, _KeyArray.PROG_CODE(2,14)));
		addButton(c,
			new _KeyArray("icons/recall.gif",_KeyArray.blue1, 1, 1, 0, 3, _KeyArray.PROG_CODE(7,14)));
		++_col;
		addButton(c,
			new _KeyArray("icons/total.gif",_KeyArray.blue1, 1, 1, 0, 0, _KeyArray.PROG_CODE(1,14)));
		addButton(c,
			new _KeyArray("icons/div.gif",	_KeyArray.blue1, 1, 1, 0, 1, _KeyArray.PROG_CODE(5,14)));
		addButton(c,
			new _KeyArray("icons/mult.gif",	_KeyArray.blue1, 1, 1, 0, 2, _KeyArray.PROG_CODE(4,14)));
		addButton(c,
			new _KeyArray("icons/store.gif",_KeyArray.blue1, 1, 1, 0, 3, _KeyArray.PROG_CODE(6,14)));
		++_col;

		s.gridx = _col;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, s);
		add(sep);
		++_col;

		addButton(c,
			new _KeyArray("icons/ld_prog.gif",_KeyArray.orange1, 1, 1, 0, 0, _KeyArray.PROG_CODE(8,14)));
		addButton(c,
			new _KeyArray("icons/search.gif",_KeyArray.green1, 1, 1, 0, 1, _KeyArray.PROG_CODE(8,0)));
		addButton(c,
			new _KeyArray("icons/go.gif",	_KeyArray.white1, 1, 2, 0, 2, _KeyArray.PROG_CODE(8,3)));
		++_col;
		addButton(c,
			new _KeyArray("icons/jif0.gif",	_KeyArray.green1, 1, 1, 0, 0, _KeyArray.PROG_CODE(8,4)));
		addButton(c,
			new _KeyArray("icons/jifplus.gif",_KeyArray.green1, 1, 1, 0, 1, _KeyArray.PROG_CODE(8,5)));
		addButton(c,
			new _KeyArray("icons/recall_xx.gif",_KeyArray.green1, 1, 1, 0, 2, _KeyArray.PROG_CODE(8,1)));
		addButton(c,
			new _KeyArray("icons/print.gif",_KeyArray.green1, 1, 1, 0, 3, _KeyArray.PROG_CODE(8,2)));
		++_col;
		addButton(c,
			new _KeyArray("icons/i_o.gif",	_KeyArray.pink1, 1, 1, 0, 0, _KeyArray.PROG_CODE(15,0)));
		addButton(c,
			new _KeyArray("icons/group1.gif",_KeyArray.pink1, 1, 1, 0, 1, _KeyArray.PROG_CODE(15,1)));
		addButton(c,
			new _KeyArray("icons/group2.gif",_KeyArray.pink1, 1, 1, 0, 2, _KeyArray.PROG_CODE(15,2)));
		addButton(c,
			new _KeyArray("icons/indir.gif",_KeyArray.orange1, 1, 1, 0, 3, _KeyArray.PROG_CODE(15,3)));
		++_col;
		addButton(c,
			new _KeyArray("icons/set_pc.gif",_KeyArray.green1, 1, 1, 0, 0, _KeyArray.SPCL_KEY(2)));
		addButton(c,
			new _KeyArray("icons/verif_prog.gif",_KeyArray.green1, 1, 1, 0, 1, _KeyArray.SPCL_KEY(1)));
		addButton(c,
			new _KeyArray("icons/rec_prog.gif",_KeyArray.orange1, 1, 1, 0, 2, _KeyArray.SPCL_KEY(3)));
		addButton(c,
			new _KeyArray("icons/step.gif",	_KeyArray.green1, 1, 1, 0, 3, _KeyArray.STEP));
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
		_keys = new _KeyArray[num_keys];
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

		addButton(c,
			new _KeyArray("icons/k00.gif",_KeyArray.white1, 1, 1, 0, 0, _KeyArray.META_KEY(0)));
		addButton(c,
			new _KeyArray("icons/k01.gif",_KeyArray.white1, 1, 1, 1, 0, _KeyArray.META_KEY(1)));
		addButton(c,
			new _KeyArray("icons/k02.gif",_KeyArray.white1, 1, 1, 2, 0, _KeyArray.META_KEY(2)));
		addButton(c,
			new _KeyArray("icons/k03.gif",_KeyArray.white1, 1, 1, 3, 0, _KeyArray.META_KEY(3)));
		addButton(c,
			new _KeyArray("icons/k04.gif",_KeyArray.white1, 1, 1, 4, 0, _KeyArray.META_KEY(4)));
		addButton(c,
			new _KeyArray("icons/k05.gif",_KeyArray.white1, 1, 1, 5, 0, _KeyArray.META_KEY(5)));
		addButton(c,
			new _KeyArray("icons/k06.gif",_KeyArray.white1, 1, 1, 6, 0, _KeyArray.META_KEY(6)));
		addButton(c,
			new _KeyArray("icons/k07.gif",_KeyArray.white1, 1, 1, 7, 0, _KeyArray.META_KEY(7)));
		addButton(c,
			new _KeyArray("icons/k08.gif",_KeyArray.white1, 1, 1, 8, 0, _KeyArray.META_KEY(8)));
		addButton(c,
			new _KeyArray("icons/k09.gif",_KeyArray.white1, 1, 1, 9, 0, _KeyArray.META_KEY(9)));
		addButton(c,
			new _KeyArray("icons/k10.gif",_KeyArray.white1, 1, 1, 10, 0, _KeyArray.META_KEY(10)));
		addButton(c,
			new _KeyArray("icons/k11.gif",_KeyArray.white1, 1, 1, 11, 0, _KeyArray.META_KEY(11)));
		addButton(c,
			new _KeyArray("icons/k12.gif",_KeyArray.white1, 1, 1, 12, 0, _KeyArray.META_KEY(12)));
		addButton(c,
			new _KeyArray("icons/k13.gif",_KeyArray.white1, 1, 1, 13, 0, _KeyArray.META_KEY(13)));
		addButton(c,
			new _KeyArray("icons/k14.gif",_KeyArray.white1, 1, 1, 14, 0, _KeyArray.META_KEY(14)));
		addButton(c,
			new _KeyArray("icons/k15.gif",_KeyArray.white1, 1, 1, 15, 0, _KeyArray.META_KEY(15)));
		_col = 0;
		_row += 1;

	}
}
