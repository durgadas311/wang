// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Properties.java,v 1.5 2013/12/05 22:31:57 drmiller Exp $

import java.util.Properties;
import javax.swing.*;
import java.io.*;
import java.awt.*;

class Wang_Properties extends Properties
{
	static final long serialVersionUID = 311000000014L;
	static final int OPTION_APPLY = 0;
	static final int OPTION_SAVE = 1;
	static final int OPTION_CANCEL = 2;
	static final int OPTION_NONE = 3;
	private String _name;
	private String _cfg;
	private JOptionPane _prefs;
	private Object[] _btns;

	int doDialog() {
		Dialog dlg = _prefs.createDialog(null, "Set " + _name + " Options");
		dlg.setVisible(true);
		Object res = _prefs.getValue();
		if (_btns[OPTION_APPLY].equals(res)) return OPTION_APPLY;
		if (_btns[OPTION_SAVE].equals(res)) return OPTION_SAVE;
		if (_btns[OPTION_CANCEL].equals(res)) return OPTION_CANCEL;
		return OPTION_NONE;
	}

	void setupDialog(Object dialog, Icon icon) {
		_btns = new Object[3];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_SAVE] = "Save";
		_btns[OPTION_CANCEL] = "Cancel";

		_prefs = new JOptionPane(dialog, JOptionPane.QUESTION_MESSAGE,
				JOptionPane.YES_NO_CANCEL_OPTION, icon, _btns);
	}

	void initProperties(String name, String cfgFile) throws Exception {
		_name = name;
		if (cfgFile.startsWith("~/")) {
			_cfg = System.getProperty("user.home") + cfgFile.substring(1);
		} else {
			_cfg = cfgFile;
		}
		FileInputStream cfg = new FileInputStream(_cfg);
		load(cfg);
		cfg.close();
		// save, and force existence of file if not exists?
	}

	int getInteger(String prop) {
		try {
			return Integer.valueOf(getProperty(prop));
		} catch (Exception e) {
			return 0;
		}
	}

	double getDouble(String prop) {
		try {
			return Double.valueOf(getProperty(prop));
		} catch (Exception e) {
			return 0.0;
		}
	}

	boolean getBoolean(String prop) {
		try {
			return Boolean.valueOf(getProperty(prop));
		} catch (Exception e) {
			return false;
		}
	}

	File getFile(String prop, boolean must_exist, File dir) {
		File f = null;
		String s = getProperty(prop);
		if (s != null) {
			if (dir != null) {
				s = dir + "/" + s;
			}
			f = new File(s);
			if (must_exist && !f.exists()) {
				f = null;
			}
		}
		return f;
	}

	// sets a single property in both the current set and the saved set.
	void setAndSaveProperty(Wang_Properties temp, String prop, String value)
	throws Exception {
		setProperty(prop, value);
		temp.setProperty(prop, value);
		temp.save();
	}

	// transfers some properties from the current set to the saved set.
	void saveSome(Wang_Properties temp, String[] props) throws Exception {
		int x;
		for (x = 0; x < props.length; ++x) {
			temp.setProperty(props[x], getProperty(props[x]));
		}
		temp.save();
	}

	void save() throws Exception {
		FileOutputStream cfg = new FileOutputStream(_cfg);
		store(cfg, "Saved by " + _name);
		cfg.close();
	}
}
