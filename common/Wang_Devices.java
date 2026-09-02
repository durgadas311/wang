// Copyright (c) 2026 Douglas Miller <durgadas311@gmail.com>

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

// All known devices
//
// model   class
// x01     Wang_OutputWriter
// x02     Wang_PlottingOutputWriter
// x03     Wang_PaperTapeReader
// x05     Wang_MicroFace
// x07     Wang_Teletype
// x11     Wang_InputOutputWriter
// x12     Wang_Plotter
// x99     WangIOExplorer

class Wang_Devices implements ActionListener {
	private JMenu _mu; // active Devices menu
	private Component _comp;

	// "Devices" main menu as 'amu'.
	public Wang_Devices(JMenu amu, Component comp) {
		_mu = amu;
		_comp = comp;
	}

	// Caller passes in their "Device Connection" sub-menu 'mu'
	public void setPluginMenu(JMenu mu) {
		JMenuItem mi;

		// exact series doesn't matter, just need unique value key
		mi = Wang_OutputWriter.s_getMenu(701);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_PlottingOutputWriter.s_getMenu(702);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_PaperTapeReader.s_getMenu(703);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_MicroFace.s_getMenu(705);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_Teletype.s_getMenu(707);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_InputOutputWriter.s_getMenu(711);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_Plotter.s_getMenu(712);
		mi.addActionListener(this);
		mu.add(mi);

		mi = WangIOExplorer.s_getMenu(799);
		mi.addActionListener(this);
		mu.add(mi);

		// An item to unplug current device
		mi = new JMenuItem("None", 999);
		mi.addActionListener(this);
		mu.add(mi);
	}

	private Wang_Peripheral getDevice(int model) {
		Wang_Peripheral p = null;
		switch (model) {
		case 701:
			p = Wang_OutputWriter.s_getInstance();
			break;
		case 702:
			p = Wang_PlottingOutputWriter.s_getInstance();
			break;
		case 703:
			p = Wang_PaperTapeReader.s_getInstance(_comp);
			break;
		case 705:
			p = Wang_MicroFace.s_getInstance(_comp);
			break;
		case 707:
			p = Wang_Teletype.s_getInstance();
			break;
		case 711:
			p = Wang_InputOutputWriter.s_getInstance();
			break;
		case 712:
			p = Wang_Plotter.s_getInstance();
			break;
		case 799:
			p = WangIOExplorer.s_getInstance();
			break;
		}
		return p;
	}

	// only called at startup, nothing should be connected yet
	public void setActDev(String model) {
		int m = -1;
		try {
			m = Integer.valueOf(model);
		} catch (Exception ee) {}
		if (m < 0) return;
		// we only use 700 series model numbers
		m = 700 + (m % 100);
		Wang_Peripheral p = getDevice(m);
		if (p != null) {
			p.plugIn(_mu);
		}
	}

	private void unPlugCN24() {
		if (Wang_CN24_dev.get() != null) {
			Wang_CN24_dev.get().unPlug(_mu);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (!(src instanceof JMenuItem)) return; // for now
		JMenuItem mi = (JMenuItem)src;
		int mn = mi.getMnemonic();
		if (mn == 999) { // un-plug all
			unPlugCN24();
			Wang_CN36_Bus.unPlugAll(_mu);
			return;
		}
		Wang_Peripheral p = getDevice(mn);
		if (p == null) return; // should never happen
		if (p.isPlugged()) {
			p.unPlug(_mu);
			return;
		}
		if (p instanceof Wang_OutputDevice) { // CN24 needed
			// if something already on CN24, must unplug it
			unPlugCN24();
		}
		p.plugIn(_mu);
	}
}
