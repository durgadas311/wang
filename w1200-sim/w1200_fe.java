// Copyright (c) 2011,2014 Douglas Miller
// $Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.lang.Math;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.awt.Desktop;

public class w1200_fe
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";

	public static File _dir;
	public static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
	private static JFrame front_end;

	public static void main(String[] args) {
		GridBagLayout gridbag = new GridBagLayout();
		String dir;

		dir = System.getenv("WANG1200HOME");
		if (dir == null) {
			dir = System.getProperty("user.home") + "/Wang1200Files";
		} else if (dir.startsWith("~/")) {
			dir = System.getProperty("user.home") + dir.substring(1);
		}
		_dir = new File(dir);

		boolean test = (args.length > 0 && args[0].compareTo("-t") == 0);
		boolean dbg = (args.length > 0 && args[0].compareTo("-i") == 0);
		boolean stop = (args.length > 0 && args[0].compareTo("-I") == 0);

		java.net.URL url = w1200_fe.class.getResource("icons/wang1200-48x48.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);

		Wang_UI.setProperties(new Wang1200_Properties());
		Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang1200_home"));
		if (Wang_UI.getProperties().isNew()) {
			// since this file should have been create during INSTALL,
			// go ahead and nag the user.
			Wang_UI.warning("Load Setup",
				"Wang1200_Properties file not found - using defaults");
		}
		Wang_UI.setSeries("12");

		front_end = new JFrame("Wang 1200 Word Processing System");
		front_end.setIconImage(img);
		front_end.setFocusTraversalKeysEnabled(false);	// allows TAB key to work...
		int row = 0, col = 0;

		front_end.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = col;
		s.gridy = row;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.anchor = GridBagConstraints.NORTH;
		JPanel pan;

		url = w1200_fe.class.getResource("icons/logo-sm.gif");
		ImageIcon ic = new ImageIcon(url);
		JLabel lab = new JLabel(ic);
		lab.setPreferredSize(new Dimension(150, 100));
		lab.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(lab, s);
		front_end.add(lab);
		++col;
		s.anchor = GridBagConstraints.NORTH;

		Wang_Keys ej = new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(7,Wang_Keys.TAPE_EJECT));
		Wang1200.TapeL = new Wang_TapeDrive(ej, null,
					Color.black, Wang_Colors.aquaGlass,
					"Left", "word processing image",
					Wang_UI.getProperties().getProperty("wang1200_tape_file_suffix"),
					"Block", (byte)0, 108, true, "wang1200_left_tape_image");
		ej = new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(7,Wang_Keys.TAPE_EJECT));
		Wang1200.TapeR = new Wang_TapeDrive(ej, null,
					Color.black, Wang_Colors.aquaGlass,
					"Right", "word processing image",
					Wang_UI.getProperties().getProperty("wang1200_tape_file_suffix"),
					"Block", (byte)0, 108, true, "wang1200_right_tape_image");

		// This now creates a widget for insertion in main frame...
		Wang1200_Model611 m611 = new Wang1200_Model611();
		m611.setProperties(Wang_UI.getProperties());
		Wang1200.CN24 = m611;

		Wang1200_KeyboardInst kbd = new Wang1200_KeyboardInst();
		Wang1200.Kbd = kbd;
		// Now we have "real" eject buttons...

		// Do left tape drive...
		Wang1200_Keyboards ejb = kbd.ejectLeft();
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 2;
		gridbag.setConstraints(Wang1200.TapeL, s);
		s.gridheight = 1;
		front_end.add(Wang1200.TapeL);
		col += 2;
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.insets.top = 0;
		s.insets.bottom = 50;
		s.insets.left = 5;
		s.insets.right = 5;
		s.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(ejb, s);
		front_end.add(ejb);
		++col;
		s.anchor = GridBagConstraints.NORTH;
		s.insets.top = 0;
		s.insets.bottom = 0;
		s.insets.left = 0;
		s.insets.right = 0;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(50, 25));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);
		++col;

		// Now do right tape drive...
		ejb = kbd.ejectRight();
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 2;
		gridbag.setConstraints(Wang1200.TapeR, s);
		front_end.add(Wang1200.TapeR);
		col += 2;
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 1;
		s.insets.top = 0;
		s.insets.bottom = 50;
		s.insets.left = 5;
		s.insets.right = 5;
		s.anchor = GridBagConstraints.SOUTHWEST;
		gridbag.setConstraints(ejb, s);
		front_end.add(ejb);
		++col;
		s.anchor = GridBagConstraints.NORTH;
		s.insets.top = 0;
		s.insets.bottom = 0;
		s.insets.left = 0;
		s.insets.right = 0;
		col = 0;
		++row;

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 5));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 8;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);
		++row;

		// Now add "keyboard"...
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 8;
		gridbag.setConstraints(Wang1200.Kbd, s);
		front_end.add(Wang1200.Kbd);
		++row;
		front_end.addKeyListener(Wang1200.Kbd);

		Wang1200.Help = new Wang1200_Help(front_end);
		Wang1200_SimInput inp = new Wang1200_SimInput(test, dbg || stop, stop);

		JMenuBar mb = new JMenuBar();
		JMenu mu = m611.getMenu();
		mb.add(mu);

		JMenuItem mi;
		mu = new JMenu("Help");
		mb.add(mu);
		mi = Wang1200.Help.getMenuItemHelp();
		mi.addActionListener(inp);
		mu.add(mi);
		mi = Wang1200.Help.getMenuItemAbout();
		mi.addActionListener(inp);
		mu.add(mi);

		front_end.setJMenuBar(mb);

		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 5));
		pan.setOpaque(false);
		s.gridx = col;
		s.gridy = row;
		s.gridheight = 1;
		s.gridwidth = 3;
		gridbag.setConstraints(pan, s);
		front_end.add(pan);

		front_end.getContentPane().setBackground(Wang_Colors.beige);
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1024,640);

		front_end.pack();	// set size according to content...

		front_end.setVisible(true);
	}

	static public void fatal(String op, String err) {
		JOptionPane.showMessageDialog(front_end,
			new JLabel(err),
			op + " Error", JOptionPane.ERROR_MESSAGE);
		System.exit(1);
	}

	static public void warning(String op, String err) {
		JOptionPane.showMessageDialog(front_end,
			new JLabel(err),
			op + " Warning", JOptionPane.WARNING_MESSAGE);
	}
}

class Wang1200_Indicator extends JLabel {
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 311457692038L;

//	GridBagLayout gridbag = new GridBagLayout();
	JLabel lab;

	public Wang1200_Indicator(String label) {
//		GridBagConstraints s = new GridBagConstraints();
//
//		setLayout(gridbag);
//
//		s.fill = GridBagConstraints.NONE;
//		s.gridx = 0;
//		s.gridy = 0;
//		s.weightx = 0;
//		s.weighty = 0;
//		s.gridwidth = 1;
//		s.gridheight = 1;
//		s.insets.left = 0;
//		s.insets.right = 0;
//		s.anchor = GridBagConstraints.CENTER;

		setText("<HTML><CENTER>"+label+"</CENTER></HTML>");
		setFont(new Font("Sans-serif", Font.PLAIN, 6));
		setPreferredSize(new Dimension(40, 20));
		setHorizontalAlignment(SwingConstants.CENTER);
		setForeground(Color.black);
		setBackground(Wang_Colors.gray);
		setOpaque(true);
//		s.gridx = 0;
//		s.gridy = 0;
//		gridbag.setConstraints(lab, s);
////		add(lab);
	}

	public void setOn(boolean on) {
		if (on) {
			setBackground(Wang_Colors.neon);
		} else {
			setBackground(Wang_Colors.gray);
		}
	}
}

class Wang1200_SimInput
		implements ActionListener
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown Menu event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_H) {
			Wang1200.Help.toggle();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_A) {
			Wang1200.Help.showAbout();
			return;
		}
	}

	public Wang1200_SimInput(boolean test, boolean dbg, boolean stop) {
		if (!test) {
			Wang1200.Core = new Wang1200_Simulator(dbg, stop);
			Wang_UI.setCore(Wang1200.Core);
		}
	}
}

class Wang1200_TapeEject extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 311057692031L;
	static final int num_keys = 1;

	private class Wang1200_TapeEjectButton extends JButton {
		static final long serialVersionUID = 311057692131L;

		Wang_Keys _key;

		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.addRenderingHints(new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON));
			//super.paint(g2d);
			Dimension d = getSize();
			Point p = new Point(0, 0);
			g2d.setColor(Color.black);
			g2d.fillOval(p.x, p.y, d.width, d.height);
			p.x += 4;
			p.y += 4;
			d.width -= 8;
			d.height -= 8;
			if (!_key.state) {
				g2d.setColor(Color.white);
				g2d.fillArc(p.x,  p.y, d.width, d.height, 45, 180);
				g2d.setColor(Wang_Colors.white3);
				g2d.fillArc(p.x,  p.y, d.width, d.height, -135, 180);
				p.x += 2;
				p.y += 2;
				d.width -= 4;
				d.height -= 4;
				g2d.setColor(Wang_Colors.white1);
				g2d.fillOval(p.x, p.y, d.width, d.height);
			} else {
				g2d.setColor(Wang_Colors.white1);
				g2d.fillArc(p.x,  p.y, d.width, d.height, 45, 180);
				g2d.setColor(Wang_Colors.white2);
				g2d.fillArc(p.x,  p.y, d.width, d.height, -135, 180);
				p.x += 2;
				p.y += 2;
				d.width -= 4;
				d.height -= 4;
				g2d.setColor(Wang_Colors.white3);
				g2d.fillOval(p.x, p.y, d.width, d.height);
			}
		}

		public Wang1200_TapeEjectButton(Wang_Keys key) {
			_key = key;
			setPreferredSize(new Dimension(30,30));
			setOpaque(false);
		}
	}

	public Wang1200_TapeEject(Wang_Keys key) {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
		_nkeys = 0;
		_keys[_nkeys] = key;

		_buttons[_nkeys] = new Wang1200_TapeEjectButton(_keys[0]);
		++_nkeys;

		_keys[0].button = _buttons[0];
		add(_buttons[0]);
		//setPreferredSize(new Dimension(30,30));
		setOpaque(false);
	}
}

class Wang1200_Model611 extends JComponent
	implements Wang_OutputDevice, ActionListener, ComponentListener
{
	static final long serialVersionUID = 31140769203L;
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	private byte[] cn24_tabstops;
	private int _max;	// last active tab stop, or zero if none

	public void reset() {
		// anything?
	}

	public boolean onOff() {
		return true;
	}

	public void onOff(boolean on) {
	}

	public JFrame getFrame() { return null; }
	public Component getComponent() { return this; }

	private PlotTextArea _text;
	private JScrollPane _scroll;
	private JLabel _carriage;
	private JLabel _bell;
	private JLabel _lock;
	private javax.swing.Timer timer;

//	private int _xoff, _yoff;
	private int _eop;	// position of user character
	private int _eoc;	// position of internal cursor
	private int _eol;	// position of internal newline
	//
	// Typewriter emulation of carriage and non-destructive space/backspace:
	//            +--- "_eop"
	//            |               +-- "_eol"
	//            v               v
	// user typed characters here.\n
	// -----------@
	//            ^
	//            +--- "_eoc"
	//
	// Where '-' is blank spaces, '@' is the cursor character (\u25b2),
	// and '\n' is the internally-maintain newline - not part of user typed text.
	//
	// Actions:
	//
	// SP:   ++_eop;
	//       _text.insert(" ",_eoc); ++_eoc;
	//
	// BS:   --_eop;
	//       --_eoc; _text.replaceRange(null, _eoc, _eoc + 1);
	//
	// CR:   _text.replaceRange(null, _eop, _eoc); _text.insert("\n", _eop); _eol = _eop + 1; _eoc = _eol + 1;
	//
	// char: if (_eop < _eol) { _text.replaceRange(char, _eop, _eop + 1); }
	//       else { _text.insert(char, _eol); ++_eol; }
	//       ++_eop;
	//       _text.insert(" ",_eoc); ++_eoc;
	//
	// CLEAR: _text.setText(""); _eop = _eol = _eoc = 0; call(CR);
	//
	//
	double _fx;
	int _fy, _fa;
	double _gx, _gy;

	private void clear() {
		_text.setText("\n\u25b2");
		_eop = _eol = 0;
		_eoc = 1;
		_shifted = false;
		_text.clear();
		_text.setCaretPosition(_eoc + 1);
	}

	String _footer;

	public boolean cursor_left() {
		if (_eoc - 1 <= _eol) {
			return false;
		}
		--_eoc;
		_text.replaceRange(null, _eoc, _eoc + 1);
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
		return true;
	}

	public boolean cursor_right() {
		_text.insert(" ", _eoc);
		++_eoc;
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
		return true;
	}

	public void do_crlf() {
		_text.replaceRange(null, _eol + 1, _eoc);	// this leaves cursor in buffer...
		_text.replaceRange(null, _eop, _eol);		// this leaves \n in buffer...
		_text.insert("\n", _eop);
		++_eop;
		_eol = _eop;
		_eoc = _eol + 1;
		_text.setCaretPosition(_eoc + 1);
		_carriage.setText(Integer.toString(_eoc - _eol));
	}

	public void do_index() {
		do_crlf(); // should it be different?
	}

	public void do_space() {
		if (_eop >= _eol) {
			_text.insert(" ", _eol);
			++_eol;
			++_eoc;
		}
		++_eop;
		cursor_right();
	}

	public void do_backspace() {
//System.err.println("BS");
		// TODO: need to prevent reverse-wrap to previous line?
		// The Wang1200 prevents that...
		if (cursor_left()) {
			--_eop;
		}
	}

	private void do_char(char c) {
		String s = Character.toString(c);
		do_char(s);
	}

	private void do_char(String s) {
		if (_eop < _eol) {
			if (s.equals("_")) {
				_text.addUnderline(_eop, _eop + 1);
				_text.repaint();
			} else {
				_text.replaceRange(s, _eop, _eop + 1);
				_text.rmUnderline(_eop, _eop + 1); // might repaint()
			}
		} else {
			_text.insert(s, _eol);
			++_eol;
			++_eoc;
		}
		++_eop;
		cursor_right();
	}

	public void do_tab() {
		int col;
		if (_eoc - _eol < _max) {
			do {
				do_space();
				col = _eoc - _eol;
			} while (col <= _max && cn24_tabstops[col - 1] == 0);
		}
	}

	public void do_settab() {
		int col = _eoc - _eol;
		cn24_tabstops[col - 1] = 1;
		if (col > _max) _max = col;
//System.err.println("set tab @ "+col);
	}

	public void do_clrtab() {
		int col = _eoc - _eol;
		cn24_tabstops[col - 1] = 0;
//System.err.println("clear tab @ "+col);
		if (_max == col) {
			while (col > 0 && cn24_tabstops[col - 1] == 0) --col;
			_max = col;
		}
	}

	public void do_lock(int lk) {
		if (lk != 0) {
			_lock.setBackground(Wang_Colors.neon);
		} else {
			_lock.setBackground(Wang_Colors.aqua);
		}
	}

	public void do_bell() {
		_bell.setBackground(Wang_Colors.neon);
		timer.start();
		Toolkit.getDefaultToolkit().beep();
	}

	public void do_shift_dn() {
		_shifted = false;
	}

	public void do_shift_up() {
		_shifted = true;
	}

	private double _cpi;
	private double _cpl;
	private double _lpi;
	private double _lpp;
	private boolean _fte;
	private String _ftt;
	private OrientationRequested _ort;
	private double _pfz;

//	private int[] _cursor_x;
//	private int[] _cursor_y;
//	private int _cursor_n;
	JMenu _menu;

	public JMenu getMenu() { return _menu; }

	public void setProperties(Wang_Properties p) {
		Wang1200_Properties prop = (Wang1200_Properties)p;
		_cpi = prop.getDouble("wang1200_page_cpi");
		_cpl = prop.getDouble("wang1200_page_cpl");
		_lpi = prop.getDouble("wang1200_page_lpi");
		_lpp = prop.getDouble("wang1200_page_lpp");
		_fte = prop.getBoolean("wang1200_page_footers");
		_ftt = prop.getProperty("wang1200_page_footertext");
		// orientation, ...
	}

	public Wang1200_Model611() {
		cn24_tabstops = new byte[256];
		int x;
		for (x = 0; x < 256; ++x) cn24_tabstops[x] = 0;
		_max = 0;

		GridBagLayout gridbag = new GridBagLayout();
		setLayout(gridbag);

		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		_carriage = new JLabel();
		_carriage.setVerticalAlignment(SwingConstants.CENTER);
		_carriage.setHorizontalAlignment(SwingConstants.CENTER);
		_carriage.setPreferredSize(new Dimension(30, 15));
		_carriage.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_carriage.setOpaque(true);
		_carriage.setForeground(Color.black);
		_carriage.setBackground(Wang_Colors.aqua);
		_carriage.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.weightx = 1;
		s.weighty = 0;
		s.anchor = GridBagConstraints.EAST;
		s.insets = new Insets(0, 0, 0, 3);
		gridbag.setConstraints(_carriage, s);
		add(_carriage);

		_bell = new JLabel("bell");
		_bell.setVerticalAlignment(SwingConstants.CENTER);
		_bell.setHorizontalAlignment(SwingConstants.CENTER);
		_bell.setPreferredSize(new Dimension(30, 15));
		_bell.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_bell.setOpaque(true);
		_bell.setForeground(Color.black);
		_bell.setBackground(Wang_Colors.aqua);
		_bell.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.gridx = 1;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.anchor = GridBagConstraints.CENTER;
		s.insets = new Insets(0, 3, 0, 3);
		gridbag.setConstraints(_bell, s);
		add(_bell);
		timer = new Timer(500, this);

		_lock = new JLabel("lock");
		_lock.setVerticalAlignment(SwingConstants.CENTER);
		_lock.setHorizontalAlignment(SwingConstants.CENTER);
		_lock.setPreferredSize(new Dimension(30, 15));
		_lock.setFont(new Font("SansSerif", Font.PLAIN, 10));
		_lock.setOpaque(true);
		_lock.setForeground(Color.black);
		_lock.setBackground(Wang_Colors.aqua);
		_lock.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		s.gridx = 2;
		s.gridy = 0;
		s.weightx = 1;
		s.weighty = 0;
		s.anchor = GridBagConstraints.WEST;
		s.insets = new Insets(0, 3, 0, 0);
		gridbag.setConstraints(_lock, s);
		add(_lock);

		_text = new PlotTextArea();
		_text.setFont(new Font("Monospaced", Font.PLAIN, 10));

		// setting this messes up horiz scrollbar...
		//_text.setPreferredSize(new Dimension(60 * _fx, 32 * _fy));
		// doing this prevents "auto warp" when printing...
		//_text.setEditable(false);
		_text.setFocusable(false);

		clear();

		FontMetrics fm = _text.getFontMetrics(_text.getFont());
		_fa = fm.getAscent();
		char[] cc = { 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M' };
		int wi = fm.charsWidth(cc, 0, 10);
		_fx = wi / 10.0;
		_fy = fm.getHeight();
		_gx = (12.0 * _fx) / 100.0; // 12 cpi into 1/100th in.
		_gy = (6.0 * _fy) / 100.0; // 6 lpi into 1/100th in.

//		_cursor_x = new int[8];
//		_cursor_y = new int[8];
//		_cursor_n = 0;
//
//		x = (int)Math.round(_fx / 2.0);
//		y = 0;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		x *= 2;
//		y += (int)Math.round(_fy / 2.0);
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		y *= 2;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		x = 0;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;
//		y /= 2;
//		_cursor_x[_cursor_n] = x;
//		_cursor_y[_cursor_n] = y;
//		++_cursor_n;

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension((int)Math.round(96 * _fx), 32 * _fy + 24));
		s.gridx = 0;
		s.gridy = 1;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 3;
		s.insets = new Insets(0, 0, 0, 0);
		s.anchor = GridBagConstraints.CENTER;
		gridbag.setConstraints(_scroll, s);
		add(_scroll);

		//parent.pack();	// set size according to content...

//		Dimension fdim = getSize();
//		Dimension sdim = _scroll.getSize();
//		_xoff = fdim.width - sdim.width;
//		_yoff = fdim.height - sdim.height;

		// Portrait/Landscape? Left/Right margin? Top/Bottom?
		_ort = OrientationRequested.PORTRAIT;

		_menu = new JMenu("System");
		JMenuItem mi;
		mi = new JMenuItem("Setup", KeyEvent.VK_U);
		mi.addActionListener(this);
		_menu.add(mi);
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(this);
		_menu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(this);
		_menu.add(mi);
		mi = new JMenuItem("Tear Off", KeyEvent.VK_T);
		mi.addActionListener(this);
		_menu.add(mi);

		addComponentListener(this);
	}

	private void save611(File file) {
		FileOutputStream fo;
		try {
			fo = new FileOutputStream(file);
		} catch (FileNotFoundException ee) {
			System.err.println("chosen 611 file not found?");
			return;
		}
		_text.save(fo);
		try {
			fo.close();
		} catch (IOException ee) {
			System.err.println("error writing 611 TXT");
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == timer) {
			timer.stop();
			_bell.setBackground(Wang_Colors.aqua);
			return;
		}
		if (!(e.getSource() instanceof JMenuItem)) {
			System.err.println("unknown 611 event source type");
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_T) {
			clear();
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			String sfx, dsc;
			sfx = "txt";
			dsc = "Text files";
			SuffFileChooser ch = new SuffFileChooser("Save", sfx, dsc, Wang_UI.getDir());
			int rv = ch.showDialog(this);
			if (rv == JFileChooser.APPROVE_OPTION) {
				save611(ch.getSelectedFile());
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_U) {
			Wang1200_Properties props = (Wang1200_Properties)Wang_UI.getProperties();
			boolean changed = props.editPreferences();
			if (changed) {
				// Apply properties...
				Wang_UI.setDir(props.getProperty("wang1200_home"));
				Wang1200.CN24.setProperties(props);
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(_ort);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.5, (float)0.5, (float)7.5, (float)10.0, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(_text);
			boolean print = pj.printDialog(aset);
			if (print) {
				// Would like to setup page here, only once, but for some
				// idiotic reason PrinterJob won't tell me the PageFormat.

				java.util.Date dt = new java.util.Date();
				_footer = new String(_ftt + " - " +
					w1200_fe._timestamp.format(dt));
				try {
					pj.print(aset);
				} catch (PrinterException ee) { 
					System.out.println("print failed");
				}
			}
			return;
		}
		System.err.println("1200 menu " + e.getActionCommand() +
						" not implemented yet");
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
//		if (e.getComponent() == this) {
//			Dimension fdim = getSize(); 
//			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff - 24);
//System.err.println("_scroll.setSize " + _scroll.getSize());
//			_scroll.setPreferredSize(_scroll.getSize());
//			setSize(fdim.width, fdim.height); // redundant?
//System.err.println("this.setSize " + getSize());
//			setPreferredSize(getSize());
//		}
	}

	private boolean _shifted;

	class PlotTextArea extends JTextArea
			implements Printable {
		static final long serialVersionUID = 311457692040L;

		class underlines {
			underlines(int s, int e) {
				start = s;
				end = e;
			}
			public boolean extend(int s, int e) {
				if (e == start) {
					start = s;
					return true;
				}
				if (end == s) {
					end = e;
					return true;
				}
				return false;
			}
			public int trim(int s, int e) {
				// assert (s < e)
				if (e <= start || s >= end) return 0;
				if (s <= start) {
					start = e;
					return 1;
				}
				if (e >= end) {
					end = s;
					return 1;
				}
				return 2;
			}
			public int start;
			public int end;
		}

		public void clear() {
			_nund = 0;
			_und = null;
		}

//		public void drawCursor(Graphics g) {
//			Polygon p = new Polygon(_cursor_x, _cursor_y, _cursor_n);
//			int x = (_eoc - _eol) * (int)Math.round(_fx);
//			try {
//				int ln = _text.getLineOfOffset(_eoc);
//				int y = ln * _fy;
//				p.translate(x, y);
//				g.fillPolygon(p);
//			} catch(javax.swing.text.BadLocationException ee) {
//			}
//		}

		private underlines[] _und;
		private int _nund;

		private void do_1underline(Graphics g, int ls, int le, int x, int pos, double w) {
			int ls_ = ls;
			int le_ = le;
			if (ls_ < _und[x].start) ls_ = _und[x].start;
			if (le_ > _und[x].end) le_ = _und[x].end;
			int x1 = (int)Math.round((ls_ - ls) * w);
			int x2 = (int)Math.round((le_ - ls) * w);
			int y = pos + 1;
			g.drawLine(x1, y, x2, y);
		}

		private void do_underline(Graphics g, int ls, int ll, int pos, double w) {
			int le = ls + ll;
			int x;
			for (x = 0; x < _nund; ++x) {
				if (_und[x].start < le && _und[x].end >= ls) {
					do_1underline(g, ls, le, x, pos, w);
				}
			}
		}

		public void addUnderline(int s, int e) {
			int x;
			for (x = 0; x < _nund; ++x) {
				if (_und[x].extend(s, e)) return;
			}
			++_nund;
			underlines[] u = new underlines[_nund];
			if (x > 0) {
				System.arraycopy(_und, 0, u, 0, x);
			}
			u[x] = new underlines(s, e);
			_und = u;
		}

		public void rmUnderline(int s, int e) {
			int x, r = 0;
			for (x = 0; x < _nund; ++x) {
				r = _und[x].trim(s, e);
				if (r != 0) break;
			}
			if (r == 0) return;
			// either case, display needs to repaint...
			if (r == 2) {
				// must split _und[x]...
				// _und[x].start ... s ... e ... _und[x].end
				int t = _und[x].end;
				_und[x].end = s;
				s = e;
				e = t;
				x = _nund;
				++_nund;
				underlines[] u = new underlines[_nund];
				if (x > 0) {
					System.arraycopy(_und, 0, u, 0, x);
				}
				u[x] = new underlines(s, e);
				_und = u;
			}
			repaint();
		}

		private String strUnderline(String s) {
			int i;
			int n = s.length();
			String r = new String("");
			i = 0;
			while (i < n) {
				r += s.substring(i, i + 1) + "\b_";
				++i;
			}
			return r;
		}

		public void save(FileOutputStream fo) {
			int a, b, l;
			int x;

		try {
			a = 0;
			x = 0;
			while (a < _eop) {
				if (x >= _nund) {
					b = _eop;
				} else {
					b = _und[x].start;
				}
				l = b - a;
				if (l > 0) {
					fo.write(getText(a, l).getBytes());
				}
				if (x < _nund) {
					// can this ever be zero length?
					a = _und[x].end;
					String s;
					s = getText(b, a - b);
					// too bad this doesn't work...
					//s = s.replaceAll("(.)", "\\1\b_");
					s = strUnderline(s);
					fo.write(s.getBytes());
					++x;
				} else {
					a = b;
				}
			}
			//fo.write('\n');
		} catch (javax.swing.text.BadLocationException ee) {
			System.err.println("error extracting 611 TXT");
		} catch (IOException ee) {
			System.err.println("error writing 611 TXT");
		}

		}

		public void paint(Graphics g) {
			super.paint(g);
			int x;
			for (x = 0; x < _nund; ++x) {
				try {
					int ln = _text.getLineOfOffset(_und[x].start);
					int ls = _text.getLineStartOffset(ln);
					int le = _text.getLineEndOffset(ln);
					do_1underline(g, ls, le, x, ln * _fy + _fa, _fx);
				} catch(javax.swing.text.BadLocationException ee) {
				}
			}
			// drawCursor(g);
		}

		public int print(Graphics g, PageFormat pf, int pageIndex) {
			double x0 = pf.getImageableX();
			double y0 = pf.getImageableY();
			double w0 = pf.getImageableWidth();
			double h0 = pf.getImageableHeight();

			int fl = 0;
			if (_fte) {
				fl = 2;
			}

			// This should be done after the printDialog, only
			// once per job.  But for some stupid reason the
			// PrinterJob object doesn't allow getting PageFormat.
			double nf, d;
			nf = 0.0;
			if (_lpp > 0) {
				nf = Math.floor(h0 / (_lpp + fl));// start with LPP sized font...
			}
			if (_lpi > 0.0) {
				d = 72.0 / _lpi;		// see if LPI requires smaller font...
				if (nf == 0.0 || d < nf) nf = d;
			}
			FontMetrics fm;
			if (_cpi > 0.0) {
				d = _fy *		// see if CPI requires smaller font...
					((72.0 / _cpi) / _fx);
				if (nf == 0.0 || d < nf) nf = d;
			}
			if (_cpl > 0.0) {
				d = Math.floor(w0 / _cpl);	// see if CPL requires smaller font...
				if (nf == 0.0 || d < nf) nf = d;
			}
			_pfz = nf;

			int pg = 0;
			Graphics2D g2d = (Graphics2D)g;
			g2d.translate(x0, y0);

			if (_pfz > 0.0) {
				g2d.setFont(_text.getFont().deriveFont((float)_pfz));
			} else {
				g2d.setFont(_text.getFont());
			}
			fm = g2d.getFontMetrics(g2d.getFont());
			char[] cc = { 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M', 'M' };
			int wi = fm.charsWidth(cc, 0, 10);
			double w = wi / 10.0;
//System.err.println("Got printer font "+g2d.getFont().getFontName()+" size "+g2d.getFont().getSize()+" width(M) "+w);

			int lpp = (int)_lpp;
			if (lpp == 0.0) {
				lpp = (int)Math.floor(h0 / g2d.getFont().getSize());
			}
//System.err.println("Using font size "+g2d.getFont().getSize()+"pt, L/P="+lpp);

			int did = 0;
			String s;
			g2d.setColor(Color.white);
			g2d.fillRect(0, 0, (int)w0, (int)h0);
			g2d.setColor(Color.black);
			g2d.setStroke(new BasicStroke((float)0.5));
			int l = g2d.getFont().getSize();
			int max = getLineCount();
			while (pg <= pageIndex) {
				int ln;
				for (ln = 0; ln < lpp; ++ln) {
					int nn = ln + pg * lpp;
					if (nn >= max) break;
					int ls, ll;
					try {
						ls = getLineStartOffset(nn);
						ll = getLineEndOffset(nn) - ls;
						if (ls >= _eop) break;
						s = getText(ls, ll);
					} catch(javax.swing.text.BadLocationException ee) {
						break;
					}
					if (pg == pageIndex) {
						++did;
						if (s.length() > 0) { // not blank line...
							g2d.drawString(s, 0, ln * l + l);
							do_underline(g2d, ls, ll, ln * l + l, w);
						}
					}
				}
				if (pg == pageIndex) {
				}
				++pg;
			}
			if (did > 0) {
				if (fl > 0) {
					s = new String("Page " + pg +
						" - " + _footer);
					g2d.drawString(s, 0, (lpp + 1) * l + l); // consumes 2 lines...
				}
				return Printable.PAGE_EXISTS;
			} else {
				return Printable.NO_SUCH_PAGE;
			}
		}
	}

	public void do_cn24(byte b) {
		String p;
		p = Wang_UI.getCharConv().tiltrotateToAscii(b, _shifted);
//if (c == '_') System.err.println("UL");
//System.err.println("ch="+c);
		do_char(p);
	}

	public void do_cn24_direct(char c) {
		// ugh... beats going through xlate tables...
		if (c == '^') c = '\u00A2';		// cent
		else if (c == '[') c = '\u00BD';	// 1/2
		else if (c == '{') c = '\u00BC';	// 1/4
		else if (c == '|') {
			do_settab();
			return;
		// clear tab, too?
		}
		if (c == '\b') {
			do_backspace();
		} else if (c == '\n') {
			do_crlf();
		} else if (c == '\t') {
			do_tab();
		} else if (c == ' ') {
			do_space();
		} else if (c != '\0') {
			do_char(c);
		}
	}
}

class Wang1200_Help extends JComponent
	implements Wang_Help, ActionListener, WindowListener, ComponentListener, HyperlinkListener
{
	static final long serialVersionUID = 311857692031L;
	private JFrame _frame;
	private JEditorPane _text;
	private JScrollPane _scroll;
	private int _xoff, _yoff;
	private JMenuItem _help;
	private JMenuItem _about;
	private boolean _help_on;
	private JFrame _main;

	public JMenuItem getMenuItemHelp() {
		return _help;
	}

	public JMenuItem getMenuItemAbout() {
		return _about;
	}

	public Wang1200_Help(JFrame frame) {
		_main = frame;
		_help = new JMenuItem("Show Help", KeyEvent.VK_H);;
		_about = new JMenuItem("About", KeyEvent.VK_A);
		_help_on = false;

		java.net.URL url = w1200_fe.class.getResource("docs/wang1200.html");
		_frame = new JFrame("Wang 1200 Help");
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (Exception ee) {
			w1200_fe.fatal("Help Setup", ee.getMessage());
		}
		_text.setEditable(false);
		_text.setFont(new Font("Sans-serif", Font.PLAIN, 12));
		int z = _text.getFont().getSize();
		// for some reason, this randomly messes up scroll size...
		//_text.setPreferredSize(new Dimension(60 * z, 32 * z));
		_text.addHyperlinkListener(this);

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(60 * z, 32 * z));

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Topic");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Basic Operation", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("About the Simulator", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Resources and Links", KeyEvent.VK_L);
		mi.addActionListener(this);
		mu.add(mi);

		_frame.setJMenuBar(mb);
		_frame.add(_scroll);
		_frame.pack();

		_frame.addWindowListener(this);
		_frame.addComponentListener(this);

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
	}

	public void showAbout() {
		java.net.URL url = w1200_fe.class.getResource("icons/wang1200.gif");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang 1200 Word Processor System<BR>"+
			"Simulator<BR>"+
			"$Revision: 1.78 $ $Date: 2014/01/27 21:11:47 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang1200.durgadas.com<BR>"+
			"<BR>"+
			"With Jim Battle<BR>"+
			"http://wang1200.org<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(_main, lab,
			"About: Wang 1200 Simulator", JOptionPane.PLAIN_MESSAGE);
	}

	public void toggle() {
		setOn(!_help_on);
	}

	private void setOn(boolean on) {
		_help_on = on;
		if (on) {
			_frame.pack();
			_help.setText("Hide Help");
		} else {
			_help.setText("Show Help");
		}
		_frame.setVisible(on);
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (e.getWindow() == _frame) {
			setOn(false);
			return;
		}
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
		if (e.getComponent() == _frame) {
			Dimension fdim = _frame.getSize();
			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
			_scroll.setPreferredSize(_scroll.getSize());
			_frame.setSize(fdim.width, fdim.height); // redundant?
			_frame.setPreferredSize(_frame.getSize());
			return;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			java.net.URL url = null;
			// should use a table to lookup url?
			if (m.getMnemonic() == KeyEvent.VK_B) {
				url = w1200_fe.class.getResource("docs/wang1200.html");
			} else if (m.getMnemonic() == KeyEvent.VK_S) {
				url = w1200_fe.class.getResource("docs/wang1200sim.html");
			} else if (m.getMnemonic() == KeyEvent.VK_L) {
				url = w1200_fe.class.getResource("docs/wang1200links.html");
			} else {
				System.err.println("help menu " + e.getActionCommand() +
						" not implemented yet");
				return;
			}
			try {
				_text.setPage(url);
			} catch (IOException ee) {
			}
			return;
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent r) {
		if (r.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			if (r.getURL().getProtocol().compareTo("file") == 0 ||
			    r.getURL().getProtocol().compareTo("jar") == 0) {
				String doc = r.getURL().getFile();
				if (r.getURL().getProtocol().compareTo("jar") == 0) {
					// ugh! must be a better way...
					doc = doc.replaceFirst("/wang1200\\.jar!/","/");
					doc = doc.replaceFirst("file:","");
				}
				try {
					Desktop.getDesktop().open(new File(doc));
				} catch (IOException e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				} catch(Exception e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				}
			} else {
				try {
					Desktop.getDesktop().browse(r.getURL().toURI());
				} catch (IOException e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				} catch(Exception e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				}
			}
		}
	}
}

class Wang1200_KeyboardInst extends Wang1200_Keyboard
	implements ActionListener, KeyListener, WindowListener, ComponentListener
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 31145769203L;
	static final int num_kbds = 4;

	GridBagLayout gridbag = new GridBagLayout();
	int _nkbds;
	Wang1200_Keyboards[] _kbds;
	private int _ejLeft;
	private int _ejRight;
	int _row;
	int _col;
	boolean _code;
	int _code_kbd;
	int _code_btn;
	int _mode0;	// a.k.a D1
	int _mode1;	// a.k.a D2
	int _mode2;	// a.k.a D3

	public Wang1200_Keyboards ejectLeft() { return _kbds[_ejLeft]; }
	public Wang1200_Keyboards ejectRight() { return _kbds[_ejRight]; }

	// indicator lamps - need to control illumination
	Wang1200_Indicator _tml;
	Wang1200_Indicator _er;
	Wang1200_Indicator _tmr;
	Wang1200_Indicator _na;
	Wang1200_Indicator _el;
	// illuminated keys - need to control illumination
	Wang_Keys _skl;
	Wang_Keys _shl;
	Wang_Keys _csl;

	public int getMode0(boolean clear) { return _mode0; }
	public int getMode1(boolean clear) {
		int m = _mode1;
		if (clear) _mode1 = 0;
		return m;
	}
	public int getMode2(boolean clear) { return _mode2; }

	public void setRECORD(boolean on) {
		_er.setOn(on);
	}

	public void setTAPE_MOV_L(boolean on) {
		_tml.setOn(on);
	}

	public void setTAPE_MOV_R(boolean on) {
		_tmr.setOn(on);
	}

	public void setNO_ADJUST(boolean on) {
		_na.setOn(on);
	}

	public void setEND_DOC(boolean on) {
		_el.setOn(on);
	}

	public void setSKIP(boolean on) {
		_skl.setOn(on);
	}

	public void setSEARCH(boolean on) {
		_shl.setOn(on);
	}

	public void setCHAR_STOP(boolean on) {
		_csl.setOn(on);
	}

	public Wang_Keys locateKey(int code) {
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (_kbds[y]._keys[x].code == code) {
					return _kbds[y]._keys[x];
				}
			}
		}
		// fatal error?
		return null;
	}

	private void setCode(boolean _new) {
		_code = _new;
		if (_code) {
			_kbds[_code_kbd]._buttons[_code_btn].setBackground(Wang_Colors.illum1);
		} else {
			_kbds[_code_kbd]._buttons[_code_btn].setBackground(_kbds[_code_kbd]._keys[_code_btn].color);
		}
	}

	private void setToggle(boolean on, Wang_Keys key, JButton btn) {
		if (key.state == on) return;
		if (key.getType() == Wang_Keys.MODE0) {
			_mode0 &= ~key.getMask();
		} else if (key.getType() == Wang_Keys.MODE1) {
			_mode1 &= ~key.getMask();
		} else if (key.getType() == Wang_Keys.MODE2) {
			_mode2 &= ~key.getMask();
		}
		if (on) {
			btn.setBackground(key.altcolor);
			if (key.getType() == Wang_Keys.MODE0) {
				_mode0 |= key.getMode();
			} else if (key.getType() == Wang_Keys.MODE1) {
				_mode1 |= key.getMode();
			} else if (key.getType() == Wang_Keys.MODE2) {
				_mode2 |= key.getMode();
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

	private void set_group_mode0(int g, int y, int x, boolean alt) {
		int z;
		Wang_Keys key = _kbds[y]._keys[x];
		int mode = key.getMode();
		int numon = 0;
		boolean couldbe = (alt && (mode == 0 || (mode & 4) != 0));
		for (z = 0; z < _kbds[y]._keys.length; ++z) {
			if (z == x) continue;
			if (_kbds[y]._keys[z] == null) continue;
			Wang_Keys key2 = _kbds[y]._keys[z];
			int tg = _kbds[y]._keys[z].getGroup();
			if (tg != g) continue;
			// might check event modifiers to see if multiple-downs allowed...
			int mode2 = key2.getMode();
			boolean dbldown = (couldbe && mode2 != mode &&
				(mode2 == 0 || (mode2 & 4) != 0));
			if (key2.state) {
				if (dbldown) {
					// leave button down...
					_mode0 = key2.getMask(); // all on...
					++numon;
				} else {
					key2.state = false;
					_mode0 &= ~key2.getMask();
					_kbds[y]._buttons[z].setBackground(key2.color);
				}
			}
		}
		// never toggle?
		key.state = !key.state || (numon == 0);
		if (key.state) {
			_mode0 |= key.getMode();
			_kbds[y]._buttons[x].setBackground(key.altcolor);
		} else {
			_mode0 &= ~key.getMask();
			_kbds[y]._buttons[x].setBackground(key.color);
		}
	}

	private void do_button(boolean alt, boolean coded, int y, int x) {
		int code = _kbds[y]._keys[x].getCode();
		if (_kbds[y]._keys[x].isSHIFT()) {
			if (!coded) setCode(!_code);
			return;
		}
		int type = _kbds[y]._keys[x].getType();
		int g = _kbds[y]._keys[x].getGroup();
		if (_kbds[y]._keys[x] == Wang1200.TapeL.ejectKey()) {
			Wang1200.TapeL.do_button(_kbds[y]._keys[x]);
			return;
		} else if (_kbds[y]._keys[x] == Wang1200.TapeR.ejectKey()) {
			Wang1200.TapeR.do_button(_kbds[y]._keys[x]);
			return;
		}
		if (g != 0) {
			if (type == Wang_Keys.MODE0) {
				set_group_mode0(g, y, x, alt);
			} else {
				set_group(g, y, x);
			}
		}
		// _mode0, _mode1, _mode2 were already updated above...
		if (type == Wang_Keys.MODE0) {
			if (g == 0) {
				// did not previously update things...
				// not a toggle...
				code |= _kbds[y]._keys[x].getMode();
			}
			Wang1200.Core.chgMode0();
			return;
		}
		if (type == Wang_Keys.MODE1) {
			// was not handled above!
			// these bits should not really be static...
			_mode1 &= ~_kbds[y]._keys[x].getMask();
			_mode1 |= _kbds[y]._keys[x].getMode();
			Wang1200.Core.chgMode1();
			return;
		}
		if (type == Wang_Keys.MODE2) {
			Wang1200.Core.chgMode2();
			return;
		}
		if (type == Wang_Keys.SPCL) {
			Wang1200.Core.pressCmd(code);
			return;
		}
		// none of the above affect CODE state (?)
		if (type == Wang_Keys.ALT) {
			// SEARCH or SKIP...
			code |= Wang_Keys.ALT;
		} else {
			// all other cases eliminated... only simple key codes...
			code = Wang_UI.getCharConv().tiltrotateToCodedTiltrotate((byte)code, _code) & 0x0ff;
		}
		if (!coded) setCode(false);
		Wang1200.Core.pressKey(code);
	}

	public Wang1200_KeyboardInst() {
		int x;
		_kbds = new Wang1200_Keyboards[num_kbds];
		_nkbds = 0;
		_row = 0;
		_col = 0;
		_code = false;
		_mode0 = 0;
		_mode1 = 0;
		_mode2 = 1;	// initial value... we just know it...

		_tml = new Wang1200_Indicator("TAPE<BR>MOVING");
		_er = new Wang1200_Indicator("RECORD");
		_tmr = new Wang1200_Indicator("TAPE<BR>MOVING");
		_na = new Wang1200_Indicator("NO<BR>ADJUST");
		_el = new Wang1200_Indicator("END OF<BR>DOCUMENT");

		GridBagConstraints s = new GridBagConstraints();
//		JPanel pan;
		Wang1200_Keyboards kbd;

		s.fill = GridBagConstraints.NONE;
		s.gridx = 0;
		s.gridy = 0;
		s.weightx = 0;
		s.weighty = 0;
		s.gridwidth = 1;
		s.gridheight = 1;

		setLayout(gridbag);

		kbd = new Wang1200_Keyboard_left(_tml, _er, _tmr);
		for (x = 0; x < kbd._nkeys; ++x) {
			if (kbd._keys[x].code == Wang_Keys.SHIFT) {
				_code_kbd = _nkbds;
				_code_btn = x;
			}
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		s.gridx = _col;
		s.gridy = _row;
//		pan = new JPanel();
//		pan.setPreferredSize(new Dimension(400, 25));
//		pan.setOpaque(false);
//		gridbag.setConstraints(pan, s);
//		add(pan);
		gridbag.setConstraints(Wang1200.CN24.getComponent(), s);
		add(Wang1200.CN24.getComponent());
		++_col;

		kbd = new Wang1200_Keyboard_right(_na, _el);
		for (x = 0; x < kbd._nkeys; ++x) {
			kbd._buttons[x].addActionListener(this);
			kbd._buttons[x].setFocusable(false);
		}
		s.gridx = _col;
		s.gridy = _row;
		gridbag.setConstraints(kbd, s);
		add(kbd);
		_kbds[_nkbds] = kbd;
		++_nkbds;
		++_col;

		//setFocusTraversalKeysEnabled(false);
		setFocusCycleRoot(true);
		setRequestFocusEnabled(true);
		// setTransferHandler(TransferHandler newHandler) 

		_kbds[_nkbds] = new Wang1200_TapeEject(Wang1200.TapeL.ejectKey());
		_kbds[_nkbds]._buttons[0].addActionListener(this);
		_kbds[_nkbds]._buttons[0].setFocusable(false);
		_ejLeft = _nkbds;
		++_nkbds;
		_kbds[_nkbds] = new Wang1200_TapeEject(Wang1200.TapeR.ejectKey());
		_kbds[_nkbds]._buttons[0].addActionListener(this);
		_kbds[_nkbds]._buttons[0].setFocusable(false);
		_ejRight = _nkbds;
		++_nkbds;

		_skl = locateKey(Wang_Keys.ALT_KEY(1));
		_shl = locateKey(Wang_Keys.ALT_KEY(2));
		_csl = locateKey(Wang_Keys.MODE1_CHG(7, 7));
	}

	private void process_keychar(char c, boolean coded) {
		// every key gets printed...
		if (c == ']') c = '1';	// feable attempt to handle type elements confusion
		if (c == '}') c = '!';	// feable attempt to handle type elements confusion
		Wang1200.CN24.do_cn24_direct(c);
		int i= Wang_UI.getCharConv().asciiToCodedTiltrotate((byte)c, _code) & 0x0ff;
		if (!coded) setCode(false);
		Wang1200.Core.pressKey(i);
	}

	public void keyTyped(KeyEvent e) {
//System.err.println("key pressed "+e.getKeyCode()+" "+e.getKeyChar());
if (e.isActionKey()) {
System.err.println("action");
}
		// on some platforms, ALT causes extended character codes...
		// so we have to handle ALT (as CODE) in the keyPressed event.
		int m = e.getModifiers();
		char c = e.getKeyChar();
		if (c == 0x04) {
			e.consume();
			Wang1200.Core.debugIntr();
			return;
		}
		if ((m & InputEvent.ALT_MASK) != 0) return;
		e.consume();
		process_keychar(c, false);
	}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			e.consume();
			setCode(true);
			return;
		}
		int m = e.getModifiers();
		if ((m & InputEvent.ALT_MASK) == 0) return;
		int k = e.getKeyCode();
		if (k != KeyEvent.VK_BACK_SPACE &&
			k != KeyEvent.VK_ENTER &&
			k != KeyEvent.VK_TAB &&
			(k < KeyEvent.VK_SPACE || k >= KeyEvent.VK_UNDERSCORE)) return;
		e.consume();
		process_keychar(Character.toLowerCase((char)k), true);
	}

	public void keyReleased(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ALT) {
			e.consume();
			setCode(false);
		}
	}

	public void actionPerformed(ActionEvent e) {
		boolean alt = ((e.getModifiers() & InputEvent.SHIFT_MASK) != 0);
		boolean coded = ((e.getModifiers() & InputEvent.ALT_MASK) != 0);
		// must be a button, find out which
		int x, y;
		for (y = 0; y < _nkbds; ++y) {
			for (x = 0; x < _kbds[y]._keys.length; ++x) {
				if (e.getSource() == _kbds[y]._buttons[x]) {
					do_button(alt, coded, y, x);
					return;
				}
			}
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
//		if (e.getWindow() == _frame) {
//			_help_on = false;
//			_help.setBackground(Wang_Colors.empty);
//			_frame.setVisible(_help_on);
//			return;
//		}
//		if (e.getWindow() == _prt.getFrame()) {
//			do_button(false, _print_kbd, _print_btn);
//			return;
//		}
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
//		if (e.getComponent() == _frame) {
//			Dimension fdim = _frame.getSize(); 
//			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
//			_scroll.setPreferredSize(_scroll.getSize());
//			_frame.setSize(fdim.width, fdim.height); // redundant?
//			_frame.setPreferredSize(_frame.getSize());
//		}
	}
}

class Wang1200_Keyboards extends JPanel
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 311457692034L;
	public Wang1200_Keyboards() { }

	int _nkeys;
	Wang_Keys[] _keys;
	JButton[] _buttons;
// private:
	GridBagLayout gridbag = new GridBagLayout();
	int _row;
	int _col;

	void addButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
						int lx, int ly, int px, int py,
						int gx, int gy,
						String icon, Wang_Keys key) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim = new Dimension(50, 50);
		JButton butt;

		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		java.net.URL url = w1200_fe.class.getResource(icon);
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
		dim.height = 50 * ly;
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

	void addPushButton(GridBagConstraints c, GridBagLayout gb, JComponent ct,
				int lx, int ly, int px, int py,
				String botlab, Color alt, boolean init, Wang_Keys key) {
		final Dimension dim = new Dimension(20, 30); // button
		final Dimension dim2 = new Dimension(30, 10); // label
		final Font font = new Font("Sans-serif", Font.PLAIN, 7);
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

		lx = py; // stupid warnings
		py = lx; // stupid warnings

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
			lab.setFont(font);
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

	void addTapeButton(GridBagConstraints c, int lx, int ly, int px, int py,
				Color alt, Wang_Keys key) {
		final Dimension dim = new Dimension(20, 20);
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

		c.gridx = _col + px;
		c.gridy = _row + 0;
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

class Wang1200_Keyboard_left extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 311457692031L;
	static final int num_keys = 10;

	public Wang1200_Keyboard_left(Wang1200_Indicator tml,
				Wang1200_Indicator er,
				Wang1200_Indicator tmr) {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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

		setLayout(gridbag);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		// do upper panel separately... stupid gridbag
		JPanel upper = new JPanel();
		GridBagLayout ugb = new GridBagLayout();
		upper.setLayout(ugb);
		upper.setOpaque(false);

		_row = 0;
		_col = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 40));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		++_col;

		addPushButton(c,ugb,upper,
			5, 3, 0, 4,"LEFT",Wang_Colors.white2, true,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(1,0))));
		addPushButton(c,ugb,upper,
			5, 3, 3, 4,"RIGHT",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(1,Wang_Keys.MODE0_CHG(1,1))));
		addPushButton(c,ugb,upper,
			5, 3, 6, 4,"TRANS.",Wang_Colors.red2, false,
			new Wang_Keys(Wang_Colors.red1, Wang_Keys.GROUP(2,Wang_Keys.MODE0_CHG(12,8))));
		addPushButton(c,ugb,upper,
			5, 3, 9, 4,"PLAY",Wang_Colors.white2, true,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(2,Wang_Keys.MODE0_CHG(12,0))));
		addPushButton(c,ugb,upper,
			5, 3, 12, 4,"RECORD",Wang_Colors.red2, false,
			new Wang_Keys(Wang_Colors.red1, Wang_Keys.GROUP(2,Wang_Keys.MODE0_CHG(12,4))));
		_col += 15;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 4;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(1, 40));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		++_col;

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(upper, c);
		add(upper);

		JPanel middle = new JPanel();
		GridBagLayout mgb = new GridBagLayout();
		middle.setLayout(mgb);
		middle.setOpaque(false);

		// don't bother with SINGLE/DOUBLE ?

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(tml, c);
		middle.add(tml);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(er, c);
		middle.add(er);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		mgb.setConstraints(tmr, c);
		middle.add(tmr);
		_col += 4;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 2;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 20));
		pan.setOpaque(false);
		mgb.setConstraints(pan, c);
		middle.add(pan);
		++_col;

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(middle, c);
		add(middle);

		_col = 0;
		_row += 2;

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 50));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);
		_col += 16;

		JPanel lower = new JPanel();
		GridBagLayout lgb = new GridBagLayout();
		lower.setLayout(lgb);
		lower.setOpaque(false);

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5,"icons/rewind.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.SPCL_KEY(1)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/forward.gif",
			new Wang_Keys(Wang_Colors.orange1,Wang_Keys.SPCL_KEY(2)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/reset.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Keys.SPCL_KEY(0)));
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/back_line.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Keys.PROG_CODE(7,3)));
		addButton(c,lgb,lower,
			1, 2, 0, 5, 5, 5, "icons/code.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Colors.illum1, Wang_Keys.SHIFT));
		_col += 5;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 15;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 150));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += 2;

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(lower, c);
		add(lower);

		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
				Color.white, Color.gray));
		//setPreferredSize(new Dimension(160, 260));
		setBackground(Wang_Colors.slate);
		setOpaque(true);
	}
}

class Wang1200_Keyboard_right extends Wang1200_Keyboards
{
	final String ident = "$Id: w1200_fe.java,v 1.78 2014/01/27 21:11:47 drmiller Exp $";
	static final long serialVersionUID = 311457692033L;
	static final int num_keys = 11;

	public Wang1200_Keyboard_right(Wang1200_Indicator na,
				Wang1200_Indicator el) {
		_buttons = new JButton[num_keys];
		_keys = new Wang_Keys[num_keys];
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
		setLayout(gridbag);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		// do upper panel separately... stupid gridbag
		JPanel upper = new JPanel();
		GridBagLayout ugb = new GridBagLayout();
		upper.setLayout(ugb);
		upper.setOpaque(false);

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 4;
		c.gridheight = 2;
		ugb.setConstraints(na, c);
		upper.add(na);
		c.gridy = _row + 2;
		c.gridwidth = 4;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(40, 1));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		c.gridy = _row + 3;
		c.gridheight = 2;
		ugb.setConstraints(el, c);
		upper.add(el);
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		c.insets = new Insets(0,0,0,0);
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(5, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		addPushButton(c,ugb,upper,
			5, 3, 0, 5,"SAME",Wang_Colors.white2, true,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(3,Wang_Keys.MODE2_CHG(3,1))));
		addPushButton(c,ugb,upper,
			5, 3, 3, 5,"ADJUST",Wang_Colors.white2, false,
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.GROUP(3,Wang_Keys.MODE2_CHG(3,2))));
		addPushButton(c,ugb,upper,
			5, 3, 6, 5,"JUSTIFY",Wang_Colors.red2, false,
			new Wang_Keys(Wang_Colors.red1, Wang_Keys.GROUP(3,Wang_Keys.MODE2_CHG(3,3))));
		_col += 9;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 1;
		c.gridheight = 5;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 50));
		pan.setOpaque(false);
		ugb.setConstraints(pan, c);
		upper.add(pan);
		_col += c.gridwidth;

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(upper, c);
		add(upper);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 10));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		JPanel lower = new JPanel();
		GridBagLayout lgb = new GridBagLayout();
		lower.setLayout(lgb);
		lower.setOpaque(false);

		_col = 0;
		_row = 0;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15,200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/para.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.MODE1_CHG(7,4)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/line.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.MODE1_CHG(7,5)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/word.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.MODE1_CHG(7,6)));
		addButton(c,lgb,lower,
			1, 1, 0, 15, 5, 5, "icons/char-stop.gif",
			new Wang_Keys(Wang_Colors.pink1, Wang_Colors.pink2, Wang_Keys.MODE1_CHG(7,7)));
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(20, 200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		addButton(c,lgb,lower,
			1, 1, 0, 0, 5, 5, "icons/auto_start.gif",
			new Wang_Keys(Wang_Colors.green1, Wang_Keys.MODE1_CHG(7,1)));
		addButton(c,lgb,lower,
			1, 1, 0, 5, 5, 5, "icons/memo_out.gif",
			new Wang_Keys(Wang_Colors.white1, Wang_Keys.PROG_CODE(6,3)));
		addButton(c,lgb,lower,
			1, 1, 0, 10, 5, 5, "icons/search.gif",
			new Wang_Keys(Wang_Colors.blue1, Wang_Colors.blue2, Wang_Keys.ALT_KEY(2)));
		addButton(c,lgb,lower,
			1, 1, 0, 15, 5, 5, "icons/skip.gif",
			new Wang_Keys(Wang_Colors.orange1, Wang_Colors.orange2, Wang_Keys.ALT_KEY(1)));
		_col += c.gridwidth;

		c.gridx = _col;
		c.gridy = _row;
		c.gridwidth = 2;
		c.gridheight = 20;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(15, 200));
		pan.setOpaque(false);
		lgb.setConstraints(pan, c);
		lower.add(pan);
		_col += c.gridwidth;

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		gridbag.setConstraints(lower, c);
		add(lower);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(150, 5));
		pan.setOpaque(false);
		gridbag.setConstraints(pan, c);
		add(pan);

		setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED,
				Color.white, Color.gray));
		//setPreferredSize(new Dimension(160, 260));
		setBackground(Wang_Colors.slate);
		setOpaque(true);
	}
}
