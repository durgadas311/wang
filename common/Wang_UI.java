// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_UI.java,v 1.6 2013/11/08 21:12:28 drmiller Exp $

import javax.swing.*;
import java.io.*;

public class Wang_UI
{
	final String ident = "$Id: Wang_UI.java,v 1.6 2013/11/08 21:12:28 drmiller Exp $";

	private static ImageIcon _icon;
	private static File _dir;
	private static Wang_Properties _props;
	private static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
	private static String _series;
	private static Wang_CharConverter _conv;
	private static Wang_InputDevice[] _cn36;
	private static java.io.InputStream _fin;
	private static java.io.OutputStream _fout;
	private static Wang_Core _core;

//	public Wang_UI(Wang_Properties props, ImageIcon icon) {
//		_props = props;
//		_icon = icon;
//	}

	public static Wang_Properties getProperties() { return _props; }

	public static ImageIcon getIcon() { return _icon; }

	public static File getDir() { return _dir; }

	public static java.text.SimpleDateFormat getTimestamp() { return _timestamp; }

	public static String getSeries() { return _series; }

	public static java.io.InputStream getFin() { return _fin; }
	public static java.io.OutputStream getFout() { return _fout; }

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
	public static void setSimIO(java.io.InputStream fin, java.io.OutputStream fout) {
		_fin = fin;
		_fout = fout;
	}

	public static void setCore(Wang_Core core) { _core = core; }

	public static Wang_CharConverter getCharConv() {
		if (_conv == null) {
			_conv = new Wang_CharConverter();
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

	static public int confirm(String op, String err) {
		int res = JOptionPane.showConfirmDialog(null,
			new JLabel(err),
			op + " Confirmation", JOptionPane.YES_NO_OPTION);
		return res;
	}

	// need de-register?
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
