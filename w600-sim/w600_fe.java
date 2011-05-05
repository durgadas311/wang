import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class _KeyArray {
	public _KeyArray(String l, String sl, int lx, int ly, int px, int py, int c) {
		this.label = l;
		this.slabel = sl;
		this.lx = lx;
		this.ly = ly;
		this.px = px;
		this.py = py;
		this.code = c;
	}

	static final int PRIME = -1;
	static final int S_M_ = -2;
	static final int SET_P_C_ = -3;
	static final int VER_PROG = -4;
	static final int REC_PROG = -5;
	static final int INS = -6;
	static final int B_S_ = -7;
	static final int DEL = -8;

	static final int SHIFT = -10;
	static final int STEP = -11;

	static final int RUN = -100;
	static final int LEARN = -101;
	static final int L_P_ = -102;
	static final int LIST = -103;

	static final int PROG_CODE(int a, int b) {
		return ((a << 4) | b);
	}

	String label;
	String slabel;
	int lx;
	int ly;
	int px;
	int py;
	int code;
}

public class w600_fe {
	public static void main(String[] args) {

_KeyArray[] LeftLeft = new _KeyArray[11];
LeftLeft[0] = new _KeyArray("PRIME",	"S.M.",		1, 1, 0, 0, _KeyArray.PRIME);
LeftLeft[1] = new _KeyArray("RAD-DEG",	"DEG-RAD",	1, 1, 0, 1, _KeyArray.PROG_CODE(8,9));
LeftLeft[2] = new _KeyArray("SHIFT",	"",		1, 2, 0, 2, _KeyArray.SHIFT);
LeftLeft[3] = new _KeyArray("SIN",	"SIN-1",	1, 1, 1, 0, _KeyArray.PROG_CODE(8,6));
LeftLeft[4] = new _KeyArray("TAN",	"TAN-1",	1, 1, 1, 1, _KeyArray.PROG_CODE(8,8));
LeftLeft[5] = new _KeyArray("LOGeX",	"LOG10X",	1, 1, 1, 2, _KeyArray.PROG_CODE(8,10));
LeftLeft[6] = new _KeyArray("X2",	"INT X",	1, 1, 1, 3, _KeyArray.PROG_CODE(8,12));
LeftLeft[7] = new _KeyArray("COS",	"COS-1",	1, 1, 2, 0, _KeyArray.PROG_CODE(8,7));
LeftLeft[8] = new _KeyArray("1/X",	"RETURN",	1, 1, 2, 1, _KeyArray.PROG_CODE(8,15));
LeftLeft[9] = new _KeyArray("eX",	"10X",		1, 1, 2, 2, _KeyArray.PROG_CODE(8,11));
LeftLeft[10] = new _KeyArray("vX",	"|X|",		1, 1, 2, 3, _KeyArray.PROG_CODE(8,13));

_KeyArray[] Left = new _KeyArray[7];
Left[0] = new _KeyArray("TOTAL",	"",		1, 1, 0, 0, _KeyArray.PROG_CODE(1,14));
Left[1] = new _KeyArray("/=",		"",		1, 1, 0, 1, _KeyArray.PROG_CODE(5,14));
Left[2] = new _KeyArray("*=",		"",		1, 1, 0, 2, _KeyArray.PROG_CODE(4,14));
Left[3] = new _KeyArray("STORE",	"",		1, 1, 0, 3, _KeyArray.PROG_CODE(6,14));
Left[4] = new _KeyArray("-",		"",		1, 1, 1, 0, _KeyArray.PROG_CODE(3,14));
Left[5] = new _KeyArray("+",		"",		1, 2, 1, 1, _KeyArray.PROG_CODE(2,14));
Left[6] = new _KeyArray("RECALL",	"",		1, 1, 1, 3, _KeyArray.PROG_CODE(7,14));

_KeyArray[] Center = new _KeyArray[14];
Center[0] = new _KeyArray("CHANGE\nSIGN","",		1, 1, 0, 0, _KeyArray.PROG_CODE(0,15));
Center[1] = new _KeyArray("CLEAR\nDISPLAY","",		1, 1, 0, 1, _KeyArray.PROG_CODE(0,15));
Center[2] = new _KeyArray("SET\nEXP",	"",		1, 1, 0, 2, _KeyArray.PROG_CODE(0,15));
Center[3] = new _KeyArray("0",		"",		2, 1, 0, 3, _KeyArray.PROG_CODE(0,0));
Center[4] = new _KeyArray("7",		"",		1, 1, 1, 0, _KeyArray.PROG_CODE(0,7));
Center[5] = new _KeyArray("4",		"",		1, 1, 1, 1, _KeyArray.PROG_CODE(0,4));
Center[6] = new _KeyArray("1",		"",		1, 1, 1, 2, _KeyArray.PROG_CODE(0,1));
Center[7] = new _KeyArray("8",		"",		1, 1, 2, 0, _KeyArray.PROG_CODE(0,8));
Center[8] = new _KeyArray("5",		"",		1, 1, 2, 1, _KeyArray.PROG_CODE(0,5));
Center[9] = new _KeyArray("2",		"",		1, 1, 2, 2, _KeyArray.PROG_CODE(0,2));
Center[10] = new _KeyArray(".",		"",		2, 1, 2, 3, _KeyArray.PROG_CODE(0,0));
Center[11] = new _KeyArray("9",		"",		1, 1, 3, 0, _KeyArray.PROG_CODE(0,9));
Center[12] = new _KeyArray("6",		"",		1, 1, 3, 1, _KeyArray.PROG_CODE(0,6));
Center[13] = new _KeyArray("3",		"",		1, 1, 3, 2, _KeyArray.PROG_CODE(0,3));

_KeyArray[] Right = new _KeyArray[7];
Right[0] = new _KeyArray("-",		"",		1, 1, 0, 0, _KeyArray.PROG_CODE(3,15));
Right[1] = new _KeyArray("+",		"",		1, 2, 0, 1, _KeyArray.PROG_CODE(2,15));
Right[2] = new _KeyArray("RECALL",	"",		1, 1, 0, 3, _KeyArray.PROG_CODE(7,15));
Right[3] = new _KeyArray("TOTAL",	"",		1, 1, 1, 0, _KeyArray.PROG_CODE(1,15));
Right[4] = new _KeyArray("/=",		"",		1, 1, 1, 1, _KeyArray.PROG_CODE(5,15));
Right[5] = new _KeyArray("*=",		"",		1, 1, 1, 2, _KeyArray.PROG_CODE(4,15));
Right[6] = new _KeyArray("STORE",	"",		1, 1, 1, 3, _KeyArray.PROG_CODE(6,15));

_KeyArray[] RightRight = new _KeyArray[15];
RightRight[0] = new _KeyArray("LOAD\nPROG",	"END",		1, 1, 0, 0, _KeyArray.PROG_CODE(8,14));
RightRight[1] = new _KeyArray("SEARCH",		"MARK",		1, 1, 0, 1, _KeyArray.PROG_CODE(8,0));
RightRight[2] = new _KeyArray("GO",		"STOP",		1, 2, 0, 2, _KeyArray.PROG_CODE(8,3));
RightRight[3] = new _KeyArray("J IF 0",		"!= 0",		1, 1, 1, 0, _KeyArray.PROG_CODE(8,4));
RightRight[4] = new _KeyArray("J IF +",		"ERROR",	1, 1, 1, 1, _KeyArray.PROG_CODE(8,5));
RightRight[5] = new _KeyArray("RECALL",		"STORE",	1, 1, 1, 2, _KeyArray.PROG_CODE(8,1));
RightRight[6] = new _KeyArray("PRINT",		"a",		1, 1, 1, 3, _KeyArray.PROG_CODE(8,2));
RightRight[7] = new _KeyArray("I/O",		"",		1, 1, 2, 0, _KeyArray.PROG_CODE(15,0));
RightRight[8] = new _KeyArray("GROUP\n1",	"",		1, 1, 2, 1, _KeyArray.PROG_CODE(15,1));
RightRight[9] = new _KeyArray("GROUP\n2",	"",		1, 1, 2, 2, _KeyArray.PROG_CODE(15,2));
RightRight[10] = new _KeyArray("INDIR",		"",		1, 1, 2, 3, _KeyArray.PROG_CODE(15,3));
RightRight[11] = new _KeyArray("SET\nP.C.",	"INS",		1, 1, 3, 0, _KeyArray.SET_P_C_);
RightRight[12] = new _KeyArray("VERIFY\nPROG",	"B.S.",		1, 1, 3, 1, _KeyArray.VER_PROG);
RightRight[13] = new _KeyArray("RECORD\nPROG",	"DEL",		1, 1, 3, 2, _KeyArray.REC_PROG);
RightRight[14] = new _KeyArray("STEP",		"",		1, 1, 3, 3, _KeyArray.STEP);

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

		JButton butt = new JButton(key.label);
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
				System.out.println(_keys[x].label);
				break;
			}
		}
	}
}
