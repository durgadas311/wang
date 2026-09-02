// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.concurrent.LinkedBlockingDeque;

class Wang_MicroFace implements Wang_GroupIODevice, ActionListener, Runnable
{
	public static final String Model = "05";
	public static final String Description = "Micro Face";

	private static JMenuItem pmi = null;
	private static Wang_MicroFace thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Description;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_MicroFace s_getInstance(Component comp) {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s05_",
			Wang_UI.getSeries(), Wang_UI.getSeries());
		thus = new Wang_MicroFace(p, comp);
		return thus;
	}

	static JMenuItem dev_mi = null;
	private boolean plugged = false;

	public String getModel() { return s_getModel(); }
	public String getName() { return s_getName(); }
	public void plugIn(JMenu mu) {
		if (plugged) return;
		plugged = true;
		if (pmi != null) {
			pmi.setText(s_getName() + " (installed)");
		}
		if (mu != null) {
			mu.add(getMenu());
		}
		Wang_CN36_Bus.registerCN36(this);
		// onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		Wang_CN36_Bus.deregisterCN36(this);
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		// onOff(false);
	}
	public boolean isPlugged() { return plugged; }
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName(), KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}


	// Group 1 07 xx = execute sampling of data on interface 'xx'

	String _prop;
	Component _comp;
	String[] _shell;
	boolean _cygwin; // yuk

	String[] _intfs = new String[16];

	int _iob;
	boolean _input;	// send to Wang vs. skip (00-00 vs. 00-07)
	String _sample;
	int _sampix;

	JPanel[] _panels;
	JTextArea[] _texts;
	JPanel _dia_pn;
	private Object[] _btns;
	private JOptionPane _prefs;
	private static final int OPTION_APPLY = 0;
	private static final int OPTION_SAVE = 1;
	private static final int OPTION_CANCEL = 2;

	LinkedBlockingDeque<Integer> giChr;
	boolean gkbd; // actually, !GKBD

	public void reset() {
		giChr.clear(); // still could be one in the chamber...
		_input = false;
		_iob = 0;
	}

	private boolean execute(int num) {
		// fork/exec command in _intfs[n]...
		// capturing stdout... (and...?)
		String[] out = new String[2];
		int x = Wang_RunCommand.runCommand(_intfs[num], out);
		if (x == 0) {
			_input = true;
			_sample = out[0];
			_sampix = 0;
			sendNum(); // queue up entire thing, incl. GO
			return true;
		} else {
			// does the user already know it failed?
			System.err.format("GROUP 1 07 %02d failed: (%d) %s\n", num, x, out[1]);
			return false;
		}
	}

	public boolean start_cn36(int iob, int c) {
		// currently, don't care if running program or not...
		_input = false;
		// We are a GROUP 1 device...
		if ((iob & 0x05) != 4) {
			return false;
		}
		_iob = iob;
		if ((c & 0x0f0) != 0x70) {
			return false;
		}
		// At this point, we are handling the I/O...
		c &= 0x0f;
		if (_intfs[c].length() > 0) {
			if (execute(c)) {
				_input = true;
			}
		}
		return _input;
	}

	public void menuClick(JMenuItem m) {
		for (int x = 0; x < 16; ++x) {
			if (_intfs[x] != null && _intfs[x].length() > 0) {
				_texts[x].setText(_intfs[x]);
			} else {
				// just in case...
				_texts[x].setText("");
			}
		}

		// todo: share code with Wang_Properties...
		// since these are properties, should be part of Wang_Properties?
		Dialog dlg = _prefs.createDialog(null, "Set " + getModel() + " Interfaces");
		dlg.setVisible(true);
		Object res = _prefs.getValue();
		if (_btns[OPTION_CANCEL].equals(res)) return;

		if (_btns[OPTION_APPLY].equals(res) ||
				_btns[OPTION_SAVE].equals(res)) {
try {
			Wang_Properties temp = Wang_UI.getProperties().getClass().newInstance();
			for (int x = 0; x < 16; ++x) {
				String p = _prop + Integer.toString(x);
				if (_texts[x].getText().length() > 0) {
					_intfs[x] = _texts[x].getText();
					temp.setProperty(p, _intfs[x]);
				} else {
					_intfs[x] = null;
					temp.remove(p);
				}
			}
			if (_btns[OPTION_SAVE].equals(res)) {
				temp.save();
			}
} catch (Exception ee) {}
		}
	}

	// queue up all output, including GO
	private void sendNum() {
		while (_sampix < _sample.length()) {
			int b = 0;
			int c = _sample.charAt(_sampix);
			++_sampix;
			if (c >= '0' && c <= '9') {
				b = (c & 0x0f) + E0;
			} else if (c == '.') {
				b = DP;
			} else if (c == '-') {
				b = CHG_SIGN;
			}
			// todo: must always send something... loop until valid numeric...
			if (b > 0) {
				giChr.add(b);
			}
		}
		giChr.add(GO);
	}

	public void do_ack(int iob) {} // not used?

	public void do_dev(int iob, int b) {} // never called: isBlockIO() is false

	public int getGLRN() { return 0; }
	public void setGKBD(boolean state) { gkbd = !state; }
	public boolean isBlockIO() { return false; }
	public boolean isDevEnabled() { return _input; }
	public void setProperties(Wang_Properties p) {}
	public boolean onOff() { return false; }
	public void onOff(boolean vis) {}
	public JFrame getFrame() { return null; }
	public Component getComponent() { return null; }

	public JMenuItem getMenu(int key) {
		return new JMenuItem(getName(), key);
	}

	private void setupDialog(Object dialog, Icon icon) {
		_btns = new Object[3];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_SAVE] = "Save";
		_btns[OPTION_CANCEL] = "Cancel";

		_prefs = new JOptionPane(dialog, JOptionPane.QUESTION_MESSAGE,
			JOptionPane.YES_NO_CANCEL_OPTION, icon, _btns);
	}

	public Wang_MicroFace(String prop, Component comp) {
		Wang_RunCommand.Initialize();
		giChr = new LinkedBlockingDeque<Integer>();
		_panels = new JPanel[16];
		_texts = new JTextArea[16];
		_dia_pn = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		_dia_pn.setLayout(gridbag);
		GridBagConstraints s = new GridBagConstraints();
		s.fill = GridBagConstraints.NONE;
		s.gridx = 1;
		s.gridy = 1;
		s.weightx = 1;
		s.weighty = 1;
		s.gridwidth = 1;
		s.gridheight = 1;
		s.insets.left = 0;
		s.insets.right = 0;
		s.anchor = GridBagConstraints.WEST;

		_input = false;
		_prop = prop;
		_comp = comp;
		int x, n = 0;
		for (x = 0; x < 16; ++x) {
			_texts[x] = new JTextArea();
			_texts[x].setPreferredSize(new Dimension(200, 20));
			_panels[x] = new JPanel();
			_panels[x].add(new JLabel(String.format("07 %02d:", x)));
			_panels[x].add(_texts[x]);
			gridbag.setConstraints(_panels[x], s);
			_dia_pn.add(_panels[x]);
			s.gridy += 1;

			String p = _prop + Integer.toString(x);
			String cmd = Wang_UI.getProperties().getProperty(p);
			if (cmd != null && cmd.length() > 0) {
				_intfs[x] = cmd;
				++n;
			}
		}
		if (n == 0) {
			// "demo" mode...
			if (Wang_UI.isWindows()) {
				// both of these spew unwanted text, but should
				// be ignored.
				_intfs[13] = "echo %RANDOM%";	// random number 0-32767
				// ugh, no seconds... HH:MM, and 12-hour clock...
				_intfs[14] = "time /t & echo 00";
				_intfs[15] = "date /t";	// ugh, 4-digit year... MM/DD/YYYY
			} else {
				_intfs[12] = "date +%s";	// seconds since epoch
				_intfs[13] = "echo $RANDOM";	// random number 0-32767
				_intfs[14] = "date +%H%M%S";	// time HHMMSS
				_intfs[15] = "date +%m%d%y";	// date MMDDYY
			}
		}
		// todo: share code with Wang_Properties...
		setupDialog(_dia_pn, Wang_UI.getIcon());
		Thread t = new Thread(this);
		t.start();
	}

	public void actionPerformed(ActionEvent e) {
		// There is only one, but decode it anyway...
		Object src = e.getSource();
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		if (mi.getMnemonic() == KeyEvent.VK_D) {
			// onOff(true);
			menuClick(mi);
		}
	}

	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giChr.take();
				while (!gkbd) {
					Thread.sleep(10);
				}
			} catch (Exception ee) {}
			if (c < 0) continue; // or break?
			if (!_input) continue; // PRIME, etc.
			if (c == GO) {
				_input = false;
			}
			Wang_UI.getCore().replyIO(_iob, c);
		}
	}
}
