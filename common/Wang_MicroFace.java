// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang_MicroFace.java,v 1.3 2013/12/29 16:53:13 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.Arrays;

class Wang_MicroFace
		implements Wang_InputDevice
{
	final String ident = "$Id: Wang_MicroFace.java,v 1.3 2013/12/29 16:53:13 drmiller Exp $";

	public static final String Model = "05";
	public static final String Description = "Micro Face";

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

	public void reset() {
		_input = false;
		_iob = 0;
	}

	private boolean execute(int num) {
		// fork/exec command in _intfs[n]...
		// capturing stdout... (and...?)
		_sample = null;
		String failed = null;
		int x = -1;
		try {
			String[] args = Arrays.copyOf(_shell, _shell.length + 1);
			args[_shell.length] = _intfs[num];
			ProcessBuilder cmd = new ProcessBuilder(args);
			cmd.redirectErrorStream(true);
			// eventually want:
			//cmd.redirectError(cmd.Redirect.INHERIT);
			//cmd.redirectOutput(cmd.Redirect.INHERIT);
			// but instead have to get stream and copy to stdout...
			// yuk!
			Process proc = cmd.start();
			java.io.InputStream out = proc.getInputStream();
			byte[] buf = new byte[256];
			while (out != null) {
				try {
					int n = out.read(buf);
					if (n > 0) {
						_sample = new String(Arrays.copyOfRange(buf, 0, n));
					} else if (n < 0) {
						out.close();
						out = null;
						proc.destroy();
					}
				} catch(Exception ee) {
					out.close();
					out = null;
					proc.destroy();
				}
			}
			x = proc.waitFor();
			if (_cygwin && x == 1) { x = 0; } // todo: investigate this
			if (x != 0) {
				failed = "Exited " + Integer.toString(x);
				if (_sample != null) {
					failed += "\n" + _sample;
				}
			}
		} catch(Exception ee) {
			x = 1;
			failed = ee.getMessage();
		}
		if (x == 0) {
			_input = true;
			_sampix = 0;
			do_ack(_iob);
			return true;
		} else {
			// does the user already know it failed?
			System.err.format("GROUP 1 07 %02d failed: %s\n", num, failed);
			return false;
		}
	}

	public boolean start_cn36(int iob, int c) {
		// currently, don't care if running program or not...
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
				return true;
			}
		}
		return false;
	}

	public void pickFile(JMenuItem m) {
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

	private boolean sendNum() {
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
				Wang_UI.getCore().replyIO(_iob, b);
				return true;
			}
		}
		return false;
	}

	public void do_ack(int iob) {
		// should be ACK for previous code... should enable next...
		// TODO
		// check mode of operation and send next code...
		// What about pass-through of data?
		if (_input) {
			if (!sendNum()) {
				Wang_UI.getCore().replyIO(_iob, GO);
				_input = false;
			}
		}
	}

	public void do_dev(int iob, int b) {
		// right now only ACK happens
		// TODO
	}

	public int getGLRN() { return 0; }

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
		_cygwin = false;
		boolean windows = (System.getProperty("os.name").indexOf("Windows") >= 0);
		String sh = System.getenv("SHELL");
		if (sh == null) {
			if (windows) {
				_shell = new String[]{ "cmd.exe", "/c" };
			} else {
				// what else to do?
				_shell = new String[]{ "sh", "-c" };
			}
		} else {
			// try to interpret it? for now, assume *nix...
			// including cygwin, so turn off "windows"...
			if (windows) { // assume cygwin...
				File shell = new File("c:\\cygwin64\\bin\\bash.exe");
				if (!shell.exists()) {
					shell = new File("c:\\cygwin32\\bin\\bash.exe");
				}
				if (!shell.exists()) {
					shell = new File("c:\\cygwin\\bin\\bash.exe");
				}
				_shell = new String[]{ shell.getAbsolutePath(),
							"--login", "-i", "-c" };
				_cygwin = true;
				windows = false;
			} else {
				_shell = new String[]{ sh, "-c" };
			}
		}

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
			if (windows) {
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

		Wang_UI.registerCN36(this);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
