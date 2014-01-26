// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_UI.java,v 1.14 2014/01/26 14:52:57 drmiller Exp $

import javax.swing.*;
import java.io.*;

public class Wang_UI
{
	final String ident = "$Id: Wang_UI.java,v 1.14 2014/01/26 14:52:57 drmiller Exp $";

	private static ImageIcon _icon;
	private static File _dir;
	private static Wang_Properties _props;
	private static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
	private static String _series;
	private static Wang_CharConverter _conv;
	private static Wang_Core _core;

//	public Wang_UI(Wang_Properties props, ImageIcon icon) {
//		_props = props;
//		_icon = icon;
//	}

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
}
