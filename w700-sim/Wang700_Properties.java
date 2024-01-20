// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang700_Properties.java,v 1.6 2014/01/27 21:12:45 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.io.FileNotFoundException;

class Wang700_Properties extends Wang_Properties
		implements Wang_PropertyEditor
{
	static final long serialVersionUID = 311000000015L;
	JCheckBox _sp1_cb;
	JCheckBox _wfl_cb;
	JRadioButton _f_rb1;
	JRadioButton _f_rb2;
	JRadioButton _f_rb3;
	ButtonGroup _f_bg;
	JLabel _f_lb;
	JTextArea _home_tx;
	JPanel _home_pn;
	JTextArea _707host_tx;
	JPanel _707host_pn;
	JPanel _dia_pn;
	JComboBox<String> _mdl_kb;
	String[] _mdl_val = new String[] { "700C", "720B", "720C" };
	JPanel _mdl_pn;

	public Wang700_Properties() {
		try {
			initProperties("Wang700", "~/.wang700.rc");
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
		_mdl_kb = new JComboBox<String>(_mdl_val);
		_mdl_pn = new JPanel();
		_mdl_pn.add(new JLabel("Model:"));
		_mdl_pn.add(_mdl_kb);
		_sp1_cb = new JCheckBox("Enable PanaPlex '1'");
		_wfl_cb = new JCheckBox("Enable Function Key Labels");
		_f_rb1 = new JRadioButton("PanaPlex 9-Segment");
		_f_rb1.setActionCommand("Panaplex9seg.ttf");
		_f_rb2 = new JRadioButton("Nixie Tubes");
		_f_rb2.setActionCommand("NixieZM1336.ttf");
		_f_rb3 = new JRadioButton("(not set)");
		_f_rb3.setActionCommand("nothing");
		_f_rb3.setEnabled(false);
		_f_bg = new ButtonGroup();
		_f_bg.add(_f_rb1);
		_f_bg.add(_f_rb2);
		_f_bg.add(_f_rb3);
		_f_lb = new JLabel("Display style:");
		_home_tx = new JTextArea();
		_home_tx.setPreferredSize(new Dimension(200, 20));
		_home_pn = new JPanel();
		_home_pn.add(new JLabel("Home:"));
		_home_pn.add(_home_tx);
		_707host_tx = new JTextArea();
		_707host_tx.setPreferredSize(new Dimension(200, 20));
		_707host_pn = new JPanel();
		_707host_pn.add(new JLabel("Teletype Host:"));
		_707host_pn.add(_707host_tx);
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
		gridbag.setConstraints(_mdl_pn, s);
		_dia_pn.add(_mdl_pn);
		s.gridy += 1;
		gridbag.setConstraints(_sp1_cb, s);
		_dia_pn.add(_sp1_cb);
		s.gridy += 1;
		gridbag.setConstraints(_f_lb, s);
		_dia_pn.add(_f_lb);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb1, s);
		_dia_pn.add(_f_rb1);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb2, s);
		_dia_pn.add(_f_rb2);
		s.gridy += 1;
		gridbag.setConstraints(_f_rb3, s);
		_dia_pn.add(_f_rb3);
		s.gridy += 1;
		gridbag.setConstraints(_home_pn, s);
		_dia_pn.add(_home_pn);
		s.gridy += 1;
		gridbag.setConstraints(_707host_pn, s);
		_dia_pn.add(_707host_pn);
		s.gridy += 1;
		gridbag.setConstraints(_wfl_cb, s);
		_dia_pn.add(_wfl_cb);

		setupDialog(_dia_pn, Wang_UI.getIcon());
	}

	public void processDefaults() {
		int changes = 0;
		// setup defaults for everything...
		String s;
		s = getProperty("wang700_special1");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang700_special1", "true");
		}
		s = getProperty("wang700_displayfont");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang700_displayfont", "NixieZM1336.ttf");
		}
		s = getProperty("wang700_home");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang700_home", "~/Wang700Files");
		}
		s = getProperty("wang700_tape_file_suffix");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang700_tape_file_suffix", "w7t");
		}
		s = getProperty("wang700_disk_file_suffix");
		if (s == null || s.length() == 0) {
			++changes;
			setProperty("wang700_disk_file_suffix", "w7d");
		}

		// process (obsolete?) env vars...
		s = System.getenv("WANG700HOME");
		if (s != null) {
			setProperty("wang700_home", s);
		}
		s = System.getenv("WANG700_FONT");
		if (s != null) {
			setProperty("wang700_displayfont", s);
		}

		// special processing for any required...
		s = getProperty("wang700_home");
		if (s.startsWith("~/")) {
			s = System.getProperty("user.home") + s.substring(1);
			setProperty("wang700_home", s);
		}
		if (changes > 0) {
			_changed = true;
		}
	}

	private void getComboSelection(JComboBox<String> kb, String prop) {
		int x;
		String m = null;
		x = kb.getSelectedIndex();
		if (x >= 0) {
			m = _mdl_val[x];
		}
		setProperty(prop, m);
	}

	private void setComboSelection(JComboBox<String> kb, String prop) {
		int x;
		String m = getProperty(prop);
		if (m == null) return;
		for (x = 0; x < _mdl_val.length; ++x) {
			if (_mdl_val[x].equals(m)) {
				kb.setSelectedIndex(x);
				return;
			}
		}
	}

	public boolean editPreferences() {
		_sp1_cb.setSelected(getBoolean("wang700_special1"));
		boolean wfl = (getProperty("wang_function_labels") != null);
		_wfl_cb.setSelected(wfl);
		setComboSelection(_mdl_kb, "wang700_model");
		String f = getProperty("wang700_displayfont");
		if (f.equals(_f_rb1.getActionCommand())) {
			_f_rb1.setSelected(true);
		} else if (f.equals(_f_rb2.getActionCommand())) {
			_f_rb2.setSelected(true);
		} else {
			_f_rb3.setText(f);
			_f_rb3.setActionCommand(f);
			_f_rb3.setEnabled(true);
			_f_rb3.setSelected(true);
			// Need something user-editable...
		}
		_home_tx.setText(getProperty("wang700_home"));
		_707host_tx.setText(getProperty("wang700_707_host"));

		int ret = doDialog();
		if (ret != OPTION_APPLY && ret != OPTION_SAVE) return false;

		// TBD: change parameters and restart?
		// TBD: do validation?
		getComboSelection(_mdl_kb, "wang700_model");
		ButtonModel bm = _f_bg.getSelection();
		setProperty("wang700_displayfont", bm.getActionCommand());
		setProperty("wang700_special1", Boolean.toString(_sp1_cb.isSelected()));
		setProperty("wang700_home", _home_tx.getText());
		setProperty("wang700_707_host", _707host_tx.getText());
		if (_wfl_cb.isSelected()) {
			if (!wfl) {
				setProperty("wang_function_labels", "");
			}
		} else {
			remove("wang_function_labels");
		}
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
