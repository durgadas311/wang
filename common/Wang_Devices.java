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
// x08     Wang_ExtendedMemory
// x11     Wang_InputOutputWriter
// x12     Wang_Plotter
// x14     Wang_CardReader
// x40     Wang_DualFloppy
// x99     WangIOExplorer
//
// Future/Other:
// x09     Dual Cassette
// x10     Fixed/Removable Harddisc
// x30     Fixed/Removable Harddisc

class Wang_Devices implements ActionListener {
	private JMenu _mu; // active Devices menu
	private Component _comp;
	private String prop;

	// "Devices" main menu as 'amu'.
	public Wang_Devices(JMenu amu, Component comp) {
		_mu = amu;
		_comp = comp;
		String series = Wang_UI.getSeries();
		prop = "wang" + series + "00_conn_devs";
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

		mi = Wang_CardReader.s_getMenu(714);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_ExtendedMemory.s_getMenu(708);
		mi.addActionListener(this);
		mu.add(mi);

		mi = Wang_DualFloppy.s_getMenu(740);
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
		case 714:
			p = Wang_CardReader.s_getInstance(_comp);
			break;
		case 708:
			p = Wang_ExtendedMemory.s_getInstance();
			break;
		case 740:
			p = Wang_DualFloppy.s_getInstance();
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

	private void updateProperty() {
		String list = null;
		Wang_Peripheral p = Wang_CN24_dev.get();
		if (p != null) list = p.getModel();
		list = Wang_CN36_Bus.listAll(list);

		// TODO: try to remove if empty?
		// Doesn't really matter, empty list means nothing connected.
		// if (list.length() == 0) {

		// This property is only used at startup, so no need
		// to re-evaluate anything now. Just save it to properties file.
		try {
			Wang_Properties temp = Wang_UI.getProperties().getNew();
			Wang_UI.getProperties().setAndSaveProperty(
				temp, prop, list);
		} catch(Exception ee) {}
	}

	// unplug whatever is on CN24
	private void unPlugCN24() {
		Wang_Peripheral p = Wang_CN24_dev.get();
		if (p == null) return;
		p.unPlug(_mu);
		Wang_CN24_dev.connect(null);
	}

	// connect device to CN24 and/or CN36 as appropriate
	private void connectDev(Wang_Peripheral p) {
		if (p instanceof Wang_OutputDevice) { // CN24 needed
			// if something already on CN24, must unplug it
			unPlugCN24(); // nulls Wang_CN24_dev
		}
		p.plugIn(_mu);
		if (p instanceof Wang_OutputDevice) {
			Wang_CN24_dev.connect((Wang_OutputDevice)p);
		}
		if (p instanceof Wang_GroupIODevice) {
			Wang_CN36_Bus.registerCN36((Wang_GroupIODevice)p);
		}
	}

	// disconnect device from CN24 and/or CN36 as appropriate
	private void disconnectDev(Wang_Peripheral p) {
		if (p instanceof Wang_OutputDevice && Wang_CN24_dev.get() == p) {
			Wang_CN24_dev.connect(null);
		}
		if (p instanceof Wang_GroupIODevice) {
			Wang_CN36_Bus.deregisterCN36((Wang_GroupIODevice)p);
		}
		p.unPlug(_mu);
	}

	// This is called at startup, before anything connected
	// TODO: assert that? disconnect any?
	public void initDevs() {
		String p = Wang_UI.getProperties().getProperty(prop);
		if (p == null || p.length() == 0) return;
		// a list of device models
		for (String m : p.split("\\s")) {
			try {
				int n = Integer.valueOf(m);
				// we only use 700 series model numbers
				n = 700 + (n % 100);
				Wang_Peripheral wp = getDevice(n);
				if (wp == null) continue;
				connectDev(wp);
			} catch (Exception ee) {
				// silently ignore invalid terms
				continue;
			}
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
			disconnectDev(p);
		} else {
			connectDev(p);
		}
		updateProperty();
	}
}
