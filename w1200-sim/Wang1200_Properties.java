// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang1200_Properties.java,v 1.1 2013/11/30 17:51:46 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang1200_Properties extends Wang_Properties
		implements Wang_PropertyEditor
{
	static final long serialVersionUID = 311000000015L;
	JTextArea _home_tx;
	JPanel _home_pn;
	JPanel _dia_pn;

	// TODO: add print setup, etc here...

	public Wang1200_Properties() {
		try {
			initProperties("Wang1200", "~/.wang1200.rc");
		} catch (Exception e) {
			Wang_UI.warning("Load Setup", e.getMessage());
		}
		processDefaults();

		// Edit Properties...
		_home_tx = new JTextArea();
		_home_tx.setPreferredSize(new Dimension(200, 20));
		_home_pn = new JPanel();
		_home_pn.add(new JLabel("Home:"));
		_home_pn.add(_home_tx);
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
		gridbag.setConstraints(_home_pn, s);
		_dia_pn.add(_home_pn);
		s.gridy += 1;

		setupDialog(_dia_pn, Wang_UI.getIcon());
	}

	public void processDefaults() {
		// setup defaults for everything...
		String s;
		s = getProperty("wang1200_home");
		if (s == null || s.length() == 0) {
			setProperty("wang1200_home", "~/Wang1200Files");
		}
		s = getProperty("wang1200_tape_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang1200_tape_file_suffix", "wpt");
		}

		// process (obsolete?) env vars...
		s = System.getenv("WANG1200HOME");
		if (s != null) {
			setProperty("wang1200_home", s);
		}

		// special processing for any required...
		s = getProperty("wang1200_home");
		if (s.startsWith("~/")) {
			s = System.getProperty("user.home") + s.substring(1);
			setProperty("wang1200_home", s);
		}
	}

	public boolean editPreferences() {
		_home_tx.setText(getProperty("wang1200_home"));

		int ret = doDialog();
		if (ret != OPTION_APPLY && ret != OPTION_SAVE) return false;

		// TBD: change parameters and restart?
		// TBD: do validation?
		setProperty("wang1200_home", _home_tx.getText());
		processDefaults();

		if (ret == OPTION_SAVE) {
			try {
				save();
			} catch (Exception e) {
				Wang_UI.warning("Save Setup", e.getMessage());
			}
		}
		return true;
	}
}
