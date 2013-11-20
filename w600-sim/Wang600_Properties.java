// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang600_Properties.java,v 1.1 2013/11/20 21:35:55 drmiller Exp $

import java.awt.*;
import javax.swing.*;

class Wang600_Properties extends Wang_Properties
		implements Wang_PropertyEditor
{
	static final long serialVersionUID = 311000000015L;
	JCheckBox _d12_cb;
	JCheckBox _cdp_cb;
	JCheckBox _sp1_cb;
	JCheckBox _rem_cb;
	JRadioButton _f_rb1;
	JRadioButton _f_rb2;
	JRadioButton _f_rb3;
	ButtonGroup _f_bg;
	JLabel _f_lb;
	JTextArea _home_tx;
	JTextArea _host_tx;
	JTextArea _port_tx;
	JPanel _home_pn;
	JPanel _host_pn;
	JPanel _port_pn;
	JPanel _dia_pn;

	public Wang600_Properties() {
		try {
			initProperties("Wang600", "~/.wang600.rc");
		} catch (Exception e) {
			Wang_UI.warning("Load Setup", e.getMessage());
		}
		processDefaults();

		// Edit Properties...
		_d12_cb = new JCheckBox("Enable Column 12");
		_cdp_cb = new JCheckBox("Center DP");
		_sp1_cb = new JCheckBox("Enable PanaPlex '1'");
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
		_rem_cb = new JCheckBox("Use remote server");
		_host_tx = new JTextArea();
		_host_tx.setPreferredSize(new Dimension(200, 20));
		_host_pn = new JPanel();
		_host_pn.add(new JLabel("Remote host:"));
		_host_pn.add(_host_tx);
		_port_tx = new JTextArea();
		_port_tx.setPreferredSize(new Dimension(50, 20));
		_port_pn = new JPanel();
		_port_pn.add(new JLabel("Remote port:"));
		_port_pn.add(_port_tx);
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
		gridbag.setConstraints(_d12_cb, s);
		_dia_pn.add(_d12_cb);
		s.gridy += 1;
		gridbag.setConstraints(_cdp_cb, s);
		_dia_pn.add(_cdp_cb);
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
		gridbag.setConstraints(_rem_cb, s);
		_dia_pn.add(_rem_cb);
		s.gridy += 1;
		gridbag.setConstraints(_host_pn, s);
		_dia_pn.add(_host_pn);
		s.gridy += 1;
		gridbag.setConstraints(_port_pn, s);
		_dia_pn.add(_port_pn);

		setupDialog(_dia_pn, Wang_UI.getIcon());
	}

	public void processDefaults() {
		// setup defaults for everything...
		String s;
		s = getProperty("wang600_digit12");
		if (s == null || s.length() == 0) {
			setProperty("wang600_digit12", "false");
		}
		s = getProperty("wang600_centerDP");
		if (s == null || s.length() == 0) {
			setProperty("wang600_centerDP", "false");
		}
		s = getProperty("wang600_special1");
		if (s == null || s.length() == 0) {
			setProperty("wang600_special1", "true");
		}
		s = getProperty("wang600_displayfont");
		if (s == null || s.length() == 0) {
			setProperty("wang600_displayfont", "Panaplex9seg.ttf");
		}
		s = getProperty("wang600_home");
		if (s == null || s.length() == 0) {
			setProperty("wang600_home", "~/Wang600Files");
		}
		s = getProperty("wang600_tape_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_tape_file_suffix", "w6t");
		}
		s = getProperty("wang600_rom_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_rom_file_suffix", "w6x");
		}
		s = getProperty("wang600_disk_file_suffix");
		if (s == null || s.length() == 0) {
			setProperty("wang600_disk_file_suffix", "w6d");
		}
		s = getProperty("wang600_remote");
		if (s == null || s.length() == 0) {
			setProperty("wang600_remote", "false");
		}
//	do we have defaults for these?
//		s = getProperty("wang600_host");
//		if (s == null || s.length() == 0) {
//			setProperty("wang600_host", "localhost");
//		}
//		s = getProperty("wang600_port");
//		if (s == null || s.length() == 0) {
//			setProperty("wang600_port", "10311");
//		}

		// process (obsolete?) env vars...
		s = System.getenv("WANG600HOME");
		if (s != null) {
			setProperty("wang600_home", s);
		}
		s = System.getenv("WANG600_FONT");
		if (s != null) {
			setProperty("wang600_displayfont", s);
		}
		s = System.getenv("WANG600_HOST");
		if (s != null) {
			setProperty("wang600_host", s);
		}
		s = System.getenv("WANG600_PORT");
		if (s != null) {
			setProperty("wang600_port", s);
		}

		// special processing for any required...
		s = getProperty("wang600_home");
		if (s.startsWith("~/")) {
			s = System.getProperty("user.home") + s.substring(1);
			setProperty("wang600_home", s);
		}
	}

	public boolean editPreferences() {
		_cdp_cb.setSelected(getBoolean("wang600_centerDP"));
		_d12_cb.setSelected(getBoolean("wang600_digit12"));
		_sp1_cb.setSelected(getBoolean("wang600_special1"));
		String f = getProperty("wang600_displayfont");
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
		_rem_cb.setSelected(getBoolean("wang600_remote"));
		_home_tx.setText(getProperty("wang600_home"));
		_host_tx.setText(getProperty("wang600_host"));
		_port_tx.setText(getProperty("wang600_port"));

		int ret = doDialog();
		if (ret != OPTION_APPLY && ret != OPTION_SAVE) return false;

		// TBD: change parameters and restart?
		// TBD: do validation?
		ButtonModel bm = _f_bg.getSelection();
		setProperty("wang600_displayfont", bm.getActionCommand());
		setProperty("wang600_digit12", Boolean.toString(_d12_cb.isSelected()));
		setProperty("wang600_centerDP", Boolean.toString(_cdp_cb.isSelected()));
		setProperty("wang600_special1", Boolean.toString(_sp1_cb.isSelected()));
		setProperty("wang600_home", _home_tx.getText());
		setProperty("wang600_remote", Boolean.toString(_rem_cb.isSelected()));
		setProperty("wang600_host", _host_tx.getText());
		setProperty("wang600_port", _port_tx.getText());
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
