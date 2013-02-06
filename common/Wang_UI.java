// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_UI.java,v 1.3 2013/02/06 22:28:33 drmiller Exp $

import javax.swing.*;
import java.io.*;

public class Wang_UI
{
	final String ident = "$Id: Wang_UI.java,v 1.3 2013/02/06 22:28:33 drmiller Exp $";

	private static ImageIcon _icon;
	private static File _dir;
	private static Wang_Properties _props;
	private static java.text.SimpleDateFormat _timestamp =
			new java.text.SimpleDateFormat("MMMM d, yyyy HH:mm:ss");
	private static String _series;

//	public Wang_UI(Wang_Properties props, ImageIcon icon) {
//		_props = props;
//		_icon = icon;
//	}

	public static Wang_Properties getProperties() { return _props; }

	public static ImageIcon getIcon() { return _icon; }

	public static File getDir() { return _dir; }

	public static java.text.SimpleDateFormat getTimestamp() { return _timestamp; }

	public static String getSeries() { return _series; }

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
}
