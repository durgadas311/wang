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

_KeyArray[] LeftLeft = new _KeyArray[11];
LeftLeft[0] = new _KeyArray("icons/prime.gif",	_KeyArray.orange1,		1, 1, 0, 0, _KeyArray.SPCL_KEY(0));
LeftLeft[1] = new _KeyArray("icons/rad_deg.gif",_KeyArray.green1,	1, 1, 0, 1, _KeyArray.PROG_CODE(8,9));
LeftLeft[2] = new _KeyArray("icons/shift.gif",	_KeyArray.white1,		1, 2, 0, 2, _KeyArray.SHIFT);
LeftLeft[3] = new _KeyArray("icons/sin.gif",	_KeyArray.green1,	1, 1, 1, 0, _KeyArray.PROG_CODE(8,6));
LeftLeft[4] = new _KeyArray("icons/tan.gif",	_KeyArray.green1,	1, 1, 1, 1, _KeyArray.PROG_CODE(8,8));
LeftLeft[5] = new _KeyArray("icons/logex.gif",	_KeyArray.green1,	1, 1, 1, 2, _KeyArray.PROG_CODE(8,10));
LeftLeft[6] = new _KeyArray("icons/x2.gif",	_KeyArray.green1,	1, 1, 1, 3, _KeyArray.PROG_CODE(8,12));
LeftLeft[7] = new _KeyArray("icons/cos.gif",	_KeyArray.green1,	1, 1, 2, 0, _KeyArray.PROG_CODE(8,7));
LeftLeft[8] = new _KeyArray("icons/inv.gif",	_KeyArray.green1,	1, 1, 2, 1, _KeyArray.PROG_CODE(8,15));
LeftLeft[9] = new _KeyArray("icons/ex.gif",	_KeyArray.green1,		1, 1, 2, 2, _KeyArray.PROG_CODE(8,11));
LeftLeft[10] = new _KeyArray("icons/sqrt.gif",	_KeyArray.green1,		1, 1, 2, 3, _KeyArray.PROG_CODE(8,13));

_KeyArray[] Left = new _KeyArray[7];
Left[0] = new _KeyArray("icons/total.gif",	_KeyArray.blue1,		1, 1, 0, 0, _KeyArray.PROG_CODE(1,15));
Left[1] = new _KeyArray("icons/div.gif",	_KeyArray.blue1,		1, 1, 0, 1, _KeyArray.PROG_CODE(5,15));
Left[2] = new _KeyArray("icons/mult.gif",	_KeyArray.blue1,		1, 1, 0, 2, _KeyArray.PROG_CODE(4,15));
Left[3] = new _KeyArray("icons/store.gif",	_KeyArray.blue1,		1, 1, 0, 3, _KeyArray.PROG_CODE(6,15));
Left[4] = new _KeyArray("icons/minus.gif",	_KeyArray.white1,		1, 1, 1, 0, _KeyArray.PROG_CODE(3,15));
Left[5] = new _KeyArray("icons/plus.gif",	_KeyArray.white1,		1, 2, 1, 1, _KeyArray.PROG_CODE(2,15));
Left[6] = new _KeyArray("icons/recall.gif",	_KeyArray.blue1,		1, 1, 1, 3, _KeyArray.PROG_CODE(7,15));
// (red) CLEAR button is 00 14...
// f(x) is 10 xx
// F(x) is 11 xx
// XCHG is 14 xx
// I/O, etc is 15 xx
_KeyArray[] Center = new _KeyArray[14];
Center[0] = new _KeyArray("icons/chg_sign.gif",_KeyArray.green1,		1, 1, 0, 0, _KeyArray.PROG_CODE(0,12));
Center[1] = new _KeyArray("icons/clear_disp.gif",_KeyArray.green1,		1, 1, 0, 1, _KeyArray.PROG_CODE(0,15));
Center[2] = new _KeyArray("icons/set_exp.gif",	_KeyArray.green1,		1, 1, 0, 2, _KeyArray.PROG_CODE(0,11));
Center[3] = new _KeyArray("icons/zero.gif",	_KeyArray.white1,		2, 1, 0, 3, _KeyArray.PROG_CODE(0,0));
Center[4] = new _KeyArray("icons/seven.gif",	_KeyArray.white1,		1, 1, 1, 0, _KeyArray.PROG_CODE(0,7));
Center[5] = new _KeyArray("icons/four.gif",	_KeyArray.white1,		1, 1, 1, 1, _KeyArray.PROG_CODE(0,4));
Center[6] = new _KeyArray("icons/one.gif",	_KeyArray.white1,		1, 1, 1, 2, _KeyArray.PROG_CODE(0,1));
Center[7] = new _KeyArray("icons/eight.gif",	_KeyArray.white1,		1, 1, 2, 0, _KeyArray.PROG_CODE(0,8));
Center[8] = new _KeyArray("icons/five.gif",	_KeyArray.white1,		1, 1, 2, 1, _KeyArray.PROG_CODE(0,5));
Center[9] = new _KeyArray("icons/two.gif",	_KeyArray.white1,		1, 1, 2, 2, _KeyArray.PROG_CODE(0,2));
Center[10] = new _KeyArray("icons/dp.gif",	_KeyArray.white1,		2, 1, 2, 3, _KeyArray.PROG_CODE(0,10));
Center[11] = new _KeyArray("icons/nine.gif",	_KeyArray.white1,		1, 1, 3, 0, _KeyArray.PROG_CODE(0,9));
Center[12] = new _KeyArray("icons/six.gif",	_KeyArray.white1,		1, 1, 3, 1, _KeyArray.PROG_CODE(0,6));
Center[13] = new _KeyArray("icons/three.gif",	_KeyArray.white1,		1, 1, 3, 2, _KeyArray.PROG_CODE(0,3));

_KeyArray[] Right = new _KeyArray[7];
Right[0] = new _KeyArray("icons/minus.gif",	_KeyArray.white1,		1, 1, 0, 0, _KeyArray.PROG_CODE(3,14));
Right[1] = new _KeyArray("icons/plus.gif",	_KeyArray.white1,		1, 2, 0, 1, _KeyArray.PROG_CODE(2,14));
Right[2] = new _KeyArray("icons/recall.gif",	_KeyArray.blue1,		1, 1, 0, 3, _KeyArray.PROG_CODE(7,14));
Right[3] = new _KeyArray("icons/total.gif",	_KeyArray.blue1,		1, 1, 1, 0, _KeyArray.PROG_CODE(1,14));
Right[4] = new _KeyArray("icons/div.gif",	_KeyArray.blue1,		1, 1, 1, 1, _KeyArray.PROG_CODE(5,14));
Right[5] = new _KeyArray("icons/mult.gif",	_KeyArray.blue1,		1, 1, 1, 2, _KeyArray.PROG_CODE(4,14));
Right[6] = new _KeyArray("icons/store.gif",	_KeyArray.blue1,		1, 1, 1, 3, _KeyArray.PROG_CODE(6,14));

_KeyArray[] RightRight = new _KeyArray[15];
RightRight[0] = new _KeyArray("icons/ld_prog.gif",	_KeyArray.orange1,		1, 1, 0, 0, _KeyArray.PROG_CODE(8,14));
RightRight[1] = new _KeyArray("icons/search.gif",	_KeyArray.green1,		1, 1, 0, 1, _KeyArray.PROG_CODE(8,0));
RightRight[2] = new _KeyArray("icons/go.gif",		_KeyArray.white1,		1, 2, 0, 2, _KeyArray.PROG_CODE(8,3));
RightRight[3] = new _KeyArray("icons/jif0.gif",		_KeyArray.green1,		1, 1, 1, 0, _KeyArray.PROG_CODE(8,4));
RightRight[4] = new _KeyArray("icons/jifplus.gif",	_KeyArray.green1,	1, 1, 1, 1, _KeyArray.PROG_CODE(8,5));
RightRight[5] = new _KeyArray("icons/recall_xx.gif",	_KeyArray.green1,	1, 1, 1, 2, _KeyArray.PROG_CODE(8,1));
RightRight[6] = new _KeyArray("icons/print.gif",	_KeyArray.green1,		1, 1, 1, 3, _KeyArray.PROG_CODE(8,2));
RightRight[7] = new _KeyArray("icons/i_o.gif",		_KeyArray.pink1,		1, 1, 2, 0, _KeyArray.PROG_CODE(15,0));
RightRight[8] = new _KeyArray("icons/group1.gif",	_KeyArray.pink1,		1, 1, 2, 1, _KeyArray.PROG_CODE(15,1));
RightRight[9] = new _KeyArray("icons/group2.gif",	_KeyArray.pink1,		1, 1, 2, 2, _KeyArray.PROG_CODE(15,2));
RightRight[10] = new _KeyArray("icons/indir.gif",	_KeyArray.orange1,		1, 1, 2, 3, _KeyArray.PROG_CODE(15,3));
RightRight[11] = new _KeyArray("icons/set_pc.gif",	_KeyArray.green1,		1, 1, 3, 0, _KeyArray.SPCL_KEY(2));
RightRight[12] = new _KeyArray("icons/verif_prog.gif",	_KeyArray.green1,		1, 1, 3, 1, _KeyArray.SPCL_KEY(1));
RightRight[13] = new _KeyArray("icons/rec_prog.gif",	_KeyArray.orange1,		1, 1, 3, 2, _KeyArray.SPCL_KEY(3));
RightRight[14] = new _KeyArray("icons/step.gif",	_KeyArray.green1,		1, 1, 3, 3, _KeyArray.STEP);

		Dimension dim = new Dimension(25, 200);
		JFrame front_end = new JFrame("Wang 600 Keyboard");
		GridBagLayout gridbag = new GridBagLayout();
		front_end.setLayout(gridbag);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;

		Wang600_Keyboard kbd = new Wang600_Keyboard(LeftLeft);
		gridbag.setConstraints(kbd, c);
		front_end.add(kbd);

		++c.gridx;
		JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, c);
		front_end.add(sep);

		++c.gridx;
		kbd = new Wang600_Keyboard(Left);
		gridbag.setConstraints(kbd, c);
		front_end.add(kbd);

		++c.gridx;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, c);
		front_end.add(sep);

		++c.gridx;
		kbd = new Wang600_Keyboard(Center);
		gridbag.setConstraints(kbd, c);
		front_end.add(kbd);

		++c.gridx;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, c);
		front_end.add(sep);

		++c.gridx;
		kbd = new Wang600_Keyboard(Right);
		gridbag.setConstraints(kbd, c);
		front_end.add(kbd);

		++c.gridx;
		sep = new JSeparator(SwingConstants.VERTICAL);
		sep.setPreferredSize(dim);
		gridbag.setConstraints(sep, c);
		front_end.add(sep);

		++c.gridx;
		kbd = new Wang600_Keyboard(RightRight);
		gridbag.setConstraints(kbd, c);
		front_end.add(kbd);

		front_end.getContentPane().setBackground(Color.black);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1000,300);
		front_end.setVisible(true);
	}
}

class Wang600_Keyboard extends JComponent
	implements ActionListener
{
	static final long serialVersionUID = 31145769203L;

	GridBagLayout gridbag = new GridBagLayout();
	_KeyArray[] _keys;
	JButton[] _buttons;

	private JButton addButton(GridBagConstraints c, _KeyArray key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
				//orange2, orange3);
		ImageIcon ic = new ImageIcon(key.icon);
		butt = new JButton(ic);
		butt.setBackground(key.color);
		butt.setBorder(lb);

		dim.width = 50 * key.lx;
		dim.height = 50 * key.ly;
		butt.setPreferredSize(dim);
		butt.setMargin(inset);
		gridbag.setConstraints(butt, c);
		add(butt);
		butt.addActionListener(this);
		return butt;
	}

	public Wang600_Keyboard(_KeyArray[] keys) {
		int x;
		GridBagConstraints c = new GridBagConstraints();
		_buttons = new JButton[keys.length];
		_keys = keys;

		setLayout(gridbag);

		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;

		for (x = 0; x < keys.length; ++x) {
			c.gridwidth = keys[x].lx;
			c.gridheight = keys[x].ly;
			c.gridx = keys[x].px;
			c.gridy = keys[x].py;
			_buttons[x] = addButton(c, keys[x]);
		}
	}

	public void actionPerformed(ActionEvent e) {
		int x;
		for (x = 0; x < _keys.length; ++x) {
			if (e.getSource() == _buttons[x]) {
				if (_keys[x].code < 0) {
					System.out.println(_keys[x].code);
				} else {
					int h = _keys[x].code >> 4;
					int l = _keys[x].code & 0x0f;
					System.out.format("%02d %02d\n", h, l);
				}
				break;
			}
		}
	}
}
