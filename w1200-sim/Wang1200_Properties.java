// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang1200_Properties.java,v 1.4 2014/01/13 17:48:17 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.io.FileNotFoundException;

class Wang1200_Properties extends Wang_Properties
		implements Wang_PropertyEditor
{
	static final long serialVersionUID = 311000000015L;
	JTextArea _home_tx;
	JTextField _cpi_tx;
	JTextField _cpl_tx;
	JTextField _lpi_tx;
	JTextField _lpp_tx;
	JTextField _fut_tx;
	JCheckBox _fute_cb;
	JPanel _home_pn;
	JPanel _cpi_pn;
	JPanel _cpl_pn;
	JPanel _lpi_pn;
	JPanel _lpp_pn;
	JPanel _dia_pn;

	// TODO: add print setup, etc here...

	public Wang1200_Properties() {
		try {
			initProperties("Wang1200", "~/.wang1200.rc");
		} catch (Exception e) {
			if (e instanceof FileNotFoundException) {
				// Don't complain about non-existent file,
				// just work with in-memory properties.
				// Still might not be possible to create...
			} else {
				Wang_UI.warning("Load Setup", e.getMessage());
			}
		}
		processDefaults();

		// Edit Properties...
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

		_home_tx = new JTextArea();
		_home_tx.setPreferredSize(new Dimension(200, 20));
		JPanel pn = new JPanel();
		pn.add(new JLabel("Home:"));
		pn.add(_home_tx);
		gridbag.setConstraints(pn, s);
		_dia_pn.add(pn);
		s.gridy += 1;

		JLabel lb = new JLabel("--- Printer Page ---");
		gridbag.setConstraints(lb, s);
		_dia_pn.add(lb);
		s.gridy += 1;
		_cpi_tx = new JTextField();
		_cpi_tx.setPreferredSize(new Dimension(50, 20));
		pn = new JPanel();
		pn.add(new JLabel("Chars/Inch:"));
		pn.add(_cpi_tx);
		gridbag.setConstraints(pn, s);
		_dia_pn.add(pn);
		s.gridy += 1;

		_cpl_tx = new JTextField();
		_cpl_tx.setPreferredSize(new Dimension(50, 20));
		pn = new JPanel();
		pn.add(new JLabel("Chars/Line:"));
		pn.add(_cpl_tx);
		gridbag.setConstraints(pn, s);
		_dia_pn.add(pn);
		s.gridy += 1;

		_lpi_tx = new JTextField();
		_lpi_tx.setPreferredSize(new Dimension(50, 20));
		pn = new JPanel();
		pn.add(new JLabel("Lines/Inch:"));
		pn.add(_lpi_tx);
		gridbag.setConstraints(pn, s);
		_dia_pn.add(pn);
		s.gridy += 1;

		_lpp_tx = new JTextField();
		_lpp_tx.setPreferredSize(new Dimension(50, 20));
		pn = new JPanel();
		pn.add(new JLabel("Lines/Page:"));
		pn.add(_lpp_tx);
		gridbag.setConstraints(pn, s);
		_dia_pn.add(pn);
		s.gridy += 1;

		_fute_cb = new JCheckBox("Enable footers");
		gridbag.setConstraints(_fute_cb, s);
		_dia_pn.add(_fute_cb);
		s.gridy += 1;
		lb = new JLabel("Footer Text:");
		gridbag.setConstraints(lb, s);
		_dia_pn.add(lb);
		s.gridy += 1;
		_fut_tx = new JTextField(); // Footer Text
		gridbag.setConstraints(_fut_tx, s);
		_dia_pn.add(_fut_tx);
		s.gridy += 1;

		setupDialog(_dia_pn, Wang_UI.getIcon());
	}

	public void processDefaults() {
		int changes = 0;
		// setup defaults for everything...
		String s;
		s = getProperty("wang1200_home");
		boolean home_set = (s != null && s.length() != 0);
		if (!home_set) {
			++changes;
			setProperty("wang1200_home", "~/Wang1200Files");
		}
		s = getProperty("wang1200_page_cpi");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_cpi", "10.0");
		}
		s = getProperty("wang1200_page_cpl");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_cpl", "0.0");
		}
		s = getProperty("wang1200_page_lpi");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_lpi", "6.0");
		}
		s = getProperty("wang1200_page_lpp");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_lpp", "0.0");
		}
		s = getProperty("wang1200_page_footers");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_footers", Boolean.toString(false));
		}
		s = getProperty("wang1200_page_footertext");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_page_footertext", "Wang 1200 Output");
		}

		// non-settable properties (except for direct file edit)
		s = getProperty("wang1200_tape_file_suffix");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang1200_tape_file_suffix", "wpt");
		}

		// process (obsolete?) env vars...
		s = System.getenv("WANG1200HOME");
		if (s != null && !home_set) {
			setProperty("wang1200_home", s);
		}

		// special processing for any required...
		s = getProperty("wang1200_home");
		if (s.startsWith("~/")) {
			s = System.getProperty("user.home") + s.substring(1);
			setProperty("wang1200_home", s);
		}
		if (changes > 0) {
			_changed = true;
		}
	}

	public boolean editPreferences() {
		_home_tx.setText(getProperty("wang1200_home"));
		_cpi_tx.setText(getProperty("wang1200_page_cpi"));
		_cpl_tx.setText(getProperty("wang1200_page_cpl"));
		_lpi_tx.setText(getProperty("wang1200_page_lpi"));
		_lpp_tx.setText(getProperty("wang1200_page_lpp"));
		_fute_cb.setSelected(getBoolean("wang1200_page_footers"));
		_fut_tx.setText(getProperty("wang1200_page_footertext"));

		int ret = doDialog();
		if (ret != OPTION_APPLY && ret != OPTION_SAVE) return false;

		// TBD: change parameters and restart?
		// TBD: do validation?
		setProperty("wang1200_home", _home_tx.getText());
		setProperty("wang1200_page_cpi", _cpi_tx.getText());
		setProperty("wang1200_page_cpl", _cpl_tx.getText());
		setProperty("wang1200_page_lpi", _lpi_tx.getText());
		setProperty("wang1200_page_lpp", _lpp_tx.getText());
		setProperty("wang1200_page_footers", Boolean.toString(_fute_cb.isSelected()));
		setProperty("wang1200_page_footertext", _fut_tx.getText());
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
