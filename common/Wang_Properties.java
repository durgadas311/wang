// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_Properties.java,v 1.1 2013/01/26 02:47:13 drmiller Exp $

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

	void initProperties(String name, String cfgFile) {
		_name = name;
		if (cfgFile.startsWith("~/")) {
			_cfg = System.getProperty("user.home") + cfgFile.substring(1);
		} else {
			_cfg = cfgFile;
		}
		try {
			FileInputStream cfg = new FileInputStream(_cfg);
			load(cfg);
			cfg.close();
		} catch (Exception e) {
			//w600_fe.warning("Load Setup", e.getMessage());
			// set defaults later, just leave all empty...
			// save, and force existence of file?
		}
	}

	int getInteger(String prop) {
		try {
			return Integer.valueOf(getProperty(prop));
		} catch (Exception e) {
			return 0;
		}
	}

	boolean getBoolean(String prop) {
		try {
			return Boolean.valueOf(getProperty(prop));
		} catch (Exception e) {
			return false;
		}
	}

	void save() throws Exception {
		FileOutputStream cfg = new FileOutputStream(_cfg);
		store(cfg, "Saved by " + _name);
		cfg.close();
	}
}
