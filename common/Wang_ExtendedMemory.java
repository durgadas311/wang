// Copyright (c) 2026 Douglas Miller <durgadas311@gmail.com>

import java.io.*;
import java.util.Properties;
import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class Wang_ExtendedMemory extends JFrame
		implements Wang_BlockIODevice,
			WindowListener, ActionListener, MouseListener, Runnable {
	static final int ramSize = 4096; // all units are the same
	static final Color unitGone = Wang_Colors.gray;
	static final Color unitInst = Wang_Colors.green1;

	static String Model = "08";
	static String Name = "Extended Memory";
	private static JMenuItem pmi = null;
	private static Wang_ExtendedMemory thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Name;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_ExtendedMemory s_getInstance() {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s08_",
			Wang_UI.getSeries(), Wang_UI.getSeries());
		thus = new Wang_ExtendedMemory(p);
		return thus;
	}

	String _pfx; // properties prefix
	GridBagLayout gb;
	GridBagConstraints gc;
	Object[] _btns;
	JPanel _dia_pn;
	JOptionPane _prefs;
	private static final int OPTION_APPLY = 0;
	private static final int OPTION_SAVE = 1;
	private static final int OPTION_CANCEL = 2;
	String _prop;
	JCheckBox[] _devs;
	JPanel[] _units;
	LED[] _leds;

	boolean devEna;
	boolean gkbd;
	int curIob;
	java.util.concurrent.LinkedBlockingDeque<Integer> giChr;
	int bi_hdr;
	boolean bi_write;
	int bi_size;
	int bi_addr;
	int bi_cnt;
	int bi_rsp; // what to send with next GISN (manual or auto)

	int curUnit;
	byte[][] _ram;
	int _ramMask;
	boolean visib;

	public Wang_ExtendedMemory(String pfx) {
		super("Wang " + s_getName());
		getContentPane().setName("Wang " + s_getName());
		setResizable(false);
		addWindowListener(this);

		_pfx = pfx;
		giChr = new java.util.concurrent.LinkedBlockingDeque<Integer>();

		_ramMask = ramSize - 1;

		// GBM switches - address config dialog
		_ram = new byte[16][];
		_devs = new JCheckBox[16];
		_units = new JPanel[16];
		_leds = new LED[16];
		curUnit = -1;
		int n = 0;
		for (int x = 0; x < 16; ++x) {
			_devs[x] = new JCheckBox(String.format("00-%02d %dK", x, ramSize / 1024));
			_leds[x] = new RoundLED(LED.Colors.NEON);
			_units[x] = getMemoryUnit(x); // after _leds[] set
			_prop = String.format("%sdev%02d", _pfx, x);
			// TODO: somehow allow for none configuired? (why?)
			if (Wang_UI.getProperties().getProperty(_prop) != null) {
				cfgUnit(x);
				++n;
			}
		}
		if (n == 0) { // always at least one, at 00
			cfgUnit(0);
		}

		JMenuBar mb = new JMenuBar();
		//JMenu mu = new JMenu("Memory");
		JMenuItem mi = new JMenuItem("Config Units", KeyEvent.VK_U);
		mi.addActionListener(this);
		mb.add(mi);
		//mu.add(mi);
		//mi = new JMenuItem("Save Core", KeyEvent.VK_S);
		//mi.addActionListener(this);
		//mu.add(mi);
		//mi = new JMenuItem("Load Core", KeyEvent.VK_L);
		//mi.addActionListener(this);
		//mu.add(mi);
		//mb.add(mu);
		setJMenuBar(mb);

		gb = new GridBagLayout();
		gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets.left = 0;
		gc.insets.right = 0;
		gc.anchor = GridBagConstraints.WEST;
		setLayout(gb);
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(10, 10));
		gb.setConstraints(pn, gc);
		add(pn);
		++gc.gridx;
		++gc.gridy;
		for (int x = 0; x < 16; ++x) {
			gc.gridx = (x >> 2) * 2 + 1;
			gc.gridy = (x & 3) * 2 + 1;
			gb.setConstraints(_units[x], gc);
			add(_units[x]);
			++gc.gridx;
			++gc.gridy;
			pn = new JPanel();
			pn.setPreferredSize(new Dimension(10, 10));
			gb.setConstraints(pn, gc);
			add(pn);
		}
		setupDialog();
		pack();
		onOff(false);
		setLocationByPlatform(true);

		reset();

		Thread t = new Thread(this);
		t.start();
	}

	private void setupDialog() {
		_dia_pn = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets.left = 0;
		gc.insets.right = 0;
		gc.anchor = GridBagConstraints.WEST;
		_dia_pn.setLayout(gb);
		for (int x = 0; x < 16; ++x) {
			gb.setConstraints(_devs[x], gc);
			_dia_pn.add(_devs[x]);
			++gc.gridy;
		}

		_btns = new Object[3];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_SAVE] = "Save";
		_btns[OPTION_CANCEL] = "Cancel";
		_prefs = new JOptionPane(_dia_pn, JOptionPane.QUESTION_MESSAGE,
			JOptionPane.YES_NO_CANCEL_OPTION, Wang_UI.getIcon(), _btns);
	}

	private JPanel getMemoryUnit(int x) {
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(100, 40));
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		pan.setBorder(lb);
		pan.addMouseListener(this);
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 1;
		gc.weighty = 1;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.insets.left = 0;
		gc.insets.right = 0;
		gc.anchor = GridBagConstraints.WEST;
		pan.setLayout(gb);
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 5));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		gb.setConstraints(_leds[x], gc);
		pan.add(_leds[x]);
		++gc.gridx;
		JLabel lab = new JLabel(String.format("00-%02d (%dK)", x, ramSize / 1024));
		gb.setConstraints(lab, gc);
		pan.add(lab);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 5));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		pan.setBackground(unitGone);
		return pan;
	}

	private int getUnit(Object src) {
		int x;
		for (x = 0; x < 16; ++x) {
			if (_units[x] == src) return x;
		}
		return -1;
	}

	private void cfgUnit(int x) {
		//_units[x].setVisible(true);
		_units[x].setBackground(unitInst);
		_devs[x].setSelected(true);
		_ram[x] = new byte[ramSize];
	}

	private void decfgUnit(int x) {
		//_units[x].setVisible(false);
		_units[x].setBackground(unitGone);
		_devs[x].setSelected(false);
		_leds[x].set(false); // just in case
		if (curUnit == x) {
			curUnit = -1;
			devEna = false;
		}
		_ram[x] = null;
	}

	private int readRAM(int adr) {
		return _ram[curUnit][adr & _ramMask] & 0xff;
	}

	private void writeRAM(int adr, int b) {
		_ram[curUnit][adr & _ramMask] = (byte)b;
	}

	private void doGO() {
		bi_hdr = 0;
		giChr.add(GO);
	}

	private void bi_next(int _iob, int c) {
		bi_rsp = 0; // assume response will be ACK
		if (_iob == 2) { // header
			switch (bi_hdr) {
			case 0: // first byte, init
				bi_addr = c << 16;
				bi_write = false;
				bi_size = 0;
				break;
			case 1:
				bi_addr |= c << 8;
				break;
			case 2:
				bi_addr |= c;
				break;
			case 3:
				bi_write = ((c & 0x80) != 0);
				// should only have one bit set in 0x7f field...
				bi_size = ((c & 0x7e) << 2) | (c & 1);
				bi_cnt = 0;
				bi_addr &= _ramMask;
				break;
			}
			++bi_hdr;
		} else if (_iob == 3) { // data or status
			if (bi_cnt < bi_size) {
				// recv data, send ack OR
				// (recv ack) send data
				if (bi_write) {
					writeRAM(bi_addr++, c);
				} else {
					bi_rsp = readRAM(bi_addr++);
				}
			} else if (bi_cnt == bi_size) { // only once
				// always status sent to calculator
				bi_rsp = 0; // we never fail
				bi_hdr = 0;
			}
			++bi_cnt;
		}
	}

	private File pickFile(String purpose) {
		File file;
		SuffFileChooser ch = new SuffFileChooser(purpose,
			Wang_UI.getDir());
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
		} else {
			file = null;
		}
		return file;
	}

	private void saveCore(int x) {
		String s = String.format("Save 00-%02d Core", x);
		File core = pickFile(s);
		if (core == null) return;
		try {
			OutputStream os = new FileOutputStream(core);
			os.write(_ram[x]);
			os.close();
		} catch (Exception ee) {
			Wang_UI.warning(s, ee.getMessage());
		}
	}

	private void loadCore(int x) {
		String s = String.format("Load 00-%02d Core", x);
		File core = pickFile(s);
		if (core == null) return;
		try {
			InputStream is = new FileInputStream(core);
			is.read(_ram[x]);
			is.close();
		} catch (Exception ee) {
			Wang_UI.warning(s, ee.getMessage());
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		int mn = mi.getMnemonic();
		if (mn == KeyEvent.VK_D) {
			onOff(true);
			return;
		}
		if (mn != KeyEvent.VK_U) return;
		Dialog dlg = _prefs.createDialog(this, "Set " + getModel() + " Units");
		dlg.setVisible(true);
		Object res = _prefs.getValue();
		if (_btns[OPTION_CANCEL].equals(res)) return;
		if (_btns[OPTION_APPLY].equals(res) ||
				_btns[OPTION_SAVE].equals(res)) {
try {
			Wang_Properties temp = Wang_UI.getProperties().getClass().
				getDeclaredConstructor().newInstance();
			for (int x = 0; x < 16; ++x) {
				_prop = String.format("%sdev%02d", _pfx, x);
				if (_devs[x].isSelected()) {
					temp.setProperty(_prop, "yes");
					if (_ram[x] == null) {
						cfgUnit(x);
					}
				} else {
					temp.remove(_prop);
					decfgUnit(x);
				}
			}
			if (_btns[OPTION_SAVE].equals(res)) {
				temp.save();
			}
} catch (Exception ee) {}
		}
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
	public void windowClosing(WindowEvent e) {
		onOff(false);
	}

	public void mouseClicked(MouseEvent e) {
		int x = getUnit(e.getSource());
		if (x < 0 || !_devs[x].isSelected()) return;
		//int m = e.getModifiersEx();
		int b = e.getButton();
		if (b == MouseEvent.BUTTON1) {
			saveCore(x);
		} else if  (b == MouseEvent.BUTTON3) {
			loadCore(x);
		}
	}
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }

	private synchronized void setKBD(boolean kbd) {
		gkbd = kbd;
	}

	private synchronized boolean getKBD() {
		return gkbd;
	}

	static JMenuItem dev_mi = null;
	private boolean plugged = false;
	// Wang_BlockIODevice
	// Calculator is sending hdr/data/ACK
	// IOB is 2 or 3
	public void do_dev(int _iob, int c) {
		if (!devEna) return;
		curIob = _iob;
		bi_next(_iob, c); // process this byte, setup response
		// fire back previously set up ACK or next
		giChr.add(bi_rsp); // must wait for GKBD...
	}

	// Wang_GroupIODevice
	public boolean isBlockIO() { return true; }
	public boolean isDevEnabled() { return devEna; }
	public boolean start_cn36(int _iob, int c) { // iob is 0,4,5,6,7
		curIob = _iob;
		// must remain enabled unless IOB=0 or GROUP-2 addr mismatch
		if (_iob == 0) {
			return devEna;
		}
		if ((_iob & 0b101) == 0b100) { // GROUP-1, ignore
			return devEna;
		}
		if ((c & ~15) != 0 || !_devs[c].isSelected()) {
			// disable anything enabled...
			if (curUnit >= 0) _leds[curUnit].set(false);
			curUnit = -1;
			devEna = false;
		} else {
			devEna = true; // just to be sure
			if (c != curUnit && curUnit >= 0) {
				_leds[curUnit].set(false);
			}
			curUnit = c;
		}
		if (!devEna) return devEna;
		_leds[curUnit].set(true);
		doGO();
		return devEna;
	}
	// Calculator is ACKing our prev GISN
	// IOB is 2 or 3?
	// passed iob is suspect
	// NEVER CALLED???
	public void do_ack(int _iob) {
System.err.format("do_ack\n");
	}
	public int getGLRN() { return 0; }
	// must not call back into simulator (directly)
	public void setGKBD(boolean state) { // 'true' == blocked
		setKBD(!state);
		// can run() race-in and change this?
		if (!state) { // calculator is now ready
			if (curIob == 3 && !bi_write) { // Block I/O read
				// might be ACK of status - must ignore
				if (bi_cnt <= bi_size) {
					bi_next(curIob, 0);
					giChr.add(bi_rsp);
				}
			}
		}
	}

	// Wang_Peripheral
	public String getModel() {
		return s_getModel();
	}
	public String getName() {
		return s_getName();
	}
	public void plugIn(JMenu mu) {
		if (plugged) return;
		plugged = true;
		if (pmi != null) {
			pmi.setText(s_getName() + " (installed)");
		}
		if (mu != null) {
			mu.add(getMenu());
		}
		Wang_CN36_Bus.registerCN36(this);
		onOff(true);
	}
	public void unPlug(JMenu mu) {
		if (!plugged) return;
		reset();
		Wang_CN36_Bus.deregisterCN36(this);
		if (pmi != null) {
			pmi.setText(s_getName() + " (not installed)");
		}
		if (mu != null) {
			mu.remove(getMenu());
		}
		plugged = false;
		onOff(false);
	}
	public boolean isPlugged() { return plugged; }

	public void reset() { // hardware reset, a.k.a. PRIME
		bi_hdr = 0;
		devEna = false;
		if (curUnit >= 0) _leds[curUnit].set(false);
		curUnit = -1;
	}
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName(), KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}

	public JFrame getFrame() { return null; }
	public Component getComponent() { return null; }
	public void onOff(boolean vis) {
		if (visib != vis) {
			visib = vis;
			setVisible(vis);
		}
	}
	public boolean onOff() { return visib; }
	public void setProperties(Wang_Properties p) {}

	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giChr.take();
				while (!getKBD()) {
					Thread.sleep(1);
				}
			} catch (Exception ee) {
ee.printStackTrace();
}
			if (c < 0) continue; // or break?
			// TODO: verify IOB = 4,5,6,7 ?
			// do this early to avoid a race:
			setKBD(false); // TODO: should we interfere?
			Wang_UI.getCore().replyIO(curIob, c);
		}
	}
}
