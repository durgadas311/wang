// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_UI.java,v 1.13 2014/01/26 14:19:02 drmiller Exp $

import javax.swing.*;
import java.io.*;

public class Wang_UI
{
	final String ident = "$Id: Wang_UI.java,v 1.13 2014/01/26 14:19:02 drmiller Exp $";

	private static ImageIcon _icon;
	private static File _dir;
	private static Wang_Properties _props;
	private static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
	private static String _series;
	private static Wang_CharConverter _conv;
	private static Wang_InputDevice[] _cn36;
	private static Wang_Core _core;

//	public Wang_UI(Wang_Properties props, ImageIcon icon) {
//		_props = props;
//		_icon = icon;
//	}

	public static void Initialize() {
		Wang_RunCommand.Initialize();
	}

	// run command using same stdio as calling process...
	public static int runCommand(String cmd) {
		return Wang_RunCommand.runCommand(cmd);
	}

	// run command with stdout+stderr collected in "out[0]".
	// Also, on failure additional error messages may be appended to "out[1]".
	// "out" must have been initialized to (at least) new String[2] before call.
	// This is intended for short-running, deterministic, commands producing
	// little (or no) output. Does not support interactive commands (stdin).
	public static int runCommand(String cmd, String[] out) {
		return Wang_RunCommand.runCommand(cmd, out);
	}

	public static boolean isWindows() { return Wang_RunCommand.isWindows(); }

	public static Wang_Properties getProperties() { return _props; }

	public static ImageIcon getIcon() { return _icon; }

	public static File getDir() { return _dir; }

	public static java.text.SimpleDateFormat getTimestamp() { return _timestamp; }

	public static String getSeries() { return _series; }

	public static Wang_Core getCore() { return _core; }

	public static void setDir(String dir) {
		_dir = new File(dir);
		_dir.mkdir();
	}
	public static void setProperties(Wang_Properties props) {
		_props = props;
	}
	public static void setIcon(ImageIcon icon) {
		_icon = icon;
	}
	public static void setSeries(String series) {
		_series = series;
	}

	public static void setCore(Wang_Core core) { _core = core; }

	public static Wang_CharConverter getCharConv() {
		if (_conv == null) {
			_conv = new Wang_CharConverter(_series.equals("12"));
		}
		return _conv;
	}

	static public void fatal(String op, String err) {
		JOptionPane.showMessageDialog(null,
			new JLabel(err),
			op + " Error", JOptionPane.ERROR_MESSAGE);
		System.exit(1);
	}

	static public void warning(String op, String err) {
		JOptionPane.showMessageDialog(null,
			new JLabel(err),
			op + " Warning", JOptionPane.WARNING_MESSAGE);
	}

	static public void inform(String op, String err) {
		JOptionPane.showMessageDialog(null,
			new JLabel(err),
			op + " Information", JOptionPane.INFORMATION_MESSAGE);
	}

	static public int confirm(String op, String err) {
		int res = JOptionPane.showConfirmDialog(null,
			new JLabel(err),
			op + " Confirmation", JOptionPane.YES_NO_OPTION);
		return res;
	}

	static public void registerCN36(Wang_InputDevice dev) {
		Wang_InputDevice[] newdevs;
		if (_cn36 == null) {
			newdevs = new Wang_InputDevice[1];
			newdevs[0] = dev;
		} else {
			int oldnum = _cn36.length;
			newdevs = new Wang_InputDevice[oldnum + 1];	
			System.arraycopy(_cn36, 0, newdevs, 0, oldnum);
			newdevs[oldnum] = dev;
		}
		_cn36 = newdevs;
	}
	static public void deregisterCN36(Wang_InputDevice dev) {
		int ix = -1;
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				if (_cn36[x].equals(dev)) {
					ix = x;
					break;
				}
			}
		}
		if (ix < 0) {
			return;
		}
		Wang_InputDevice[] newdevs;
		int oldnum = _cn36.length;
		newdevs = new Wang_InputDevice[oldnum - 1];
		if (ix > 0) {
			System.arraycopy(_cn36, 0, newdevs, 0, ix);
		}
		if (ix < oldnum - 1) {
			System.arraycopy(_cn36, ix + 1, newdevs, ix, oldnum - 1 - ix);
		}
		_cn36 = newdevs;
	}
	static public void resetCN36() {
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				_cn36[x].reset();
			}
		}
	}
	static public Wang_InputDevice startCN36(int iob, int c) {
		Wang_InputDevice dev = null;
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				if (_cn36[x].start_cn36(iob, c)) {
					dev = _cn36[x];
					break;
				}
			}
		}
		return dev;
	}
}
