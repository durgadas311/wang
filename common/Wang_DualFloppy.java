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

// 640/740 used 8" hard-sectored (16) floppies.
//
// 16 sec/trk, assuming 256 bytes/sec
// 77 tracks (assuming standard 8" drives)
// 1232 sectors/disk
// assuming SS, SD, ~5208 bytes raw per track.
// 4096 formatted bytes per track.
// 315392 bytes per disk, or 0x04D000. This means the left disk ends
// at 0x04CFFF and the right disk starts at 0x04D000.

public class Wang_DualFloppy extends JFrame
		implements Wang_BlockIODevice,
			WindowListener, ActionListener, MouseListener, Runnable {
	static final int ssz = 256;
	static final int spt = 16;
	static final int trkz = ssz * spt;
	static final int ntrk = 77;
	static final int cap = trkz * ntrk;

	static String Model = "40";
	static String Name = "Dual Floppy";
	private static JMenuItem pmi = null;
	private static Wang_DualFloppy thus = null;
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
	public static Wang_DualFloppy s_getInstance() {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s_",
			Wang_UI.getSeries(), s_getModel());
		thus = new Wang_DualFloppy(p);
		return thus;
	}

	static final Color norm = Wang_Colors.slate;
	static final Color actv = norm.brighter();
	static final Color normHi = actv.brighter();
	static final Color normLo = Color.black;

	File _dir;
	String _pfx; // properties prefix
	GridBagLayout gb;
	GridBagConstraints gc;
	String _prop;
	static final String[] driveName = new String[]{ "Left", "Right" };
	javax.swing.Timer timer;

	Object[] _btns;
	JPanel _dia_pn;
	JOptionPane _prefs;
	private static final int OPTION_APPLY = 0;
	private static final int OPTION_SAVE = 1;
	private static final int OPTION_CANCEL = 2;
	JTextField _addr;

	boolean devEna;
	boolean gkbd;
	int curIob;
	java.util.concurrent.LinkedBlockingDeque<Integer> giChr;
	int bi_hdr;
	boolean bi_write;
	int bi_size;
	int bi_addr; // raw addr of xfer (reflects drive select)
	int bi_sect; // seek point on curUnit
	int bi_cnt;
	int bi_sts;
	int bi_rsp; // what to send with next GISN (manual or auto)
	int bi_cache;
	byte[] bi_buf;
	int bi_idx;
	boolean bi_dirty;

	int addr; // TODO: configure on front panel
	int curUnit;
	JLabel fp_adr;
	JPanel[] fpys;
	LED[] leds; // on actual drives
	LED[] inds; // on front panel
	LED pwr; // on front panel
	LED err; // on front panel

	RandomAccessFile[] imgs;
	boolean[] prot;
	boolean visib;

	public Wang_DualFloppy(String pfx) {
		super("Wang " + s_getName());
		getContentPane().setName("Wang " + s_getName());
		setResizable(false);
		addWindowListener(this);
		getContentPane().setBackground(Wang_Colors.ivory);
		//Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED);
		//getContentPane().setBorder(lb);

		_dir = Wang_UI.getDir();
		_pfx = pfx;
		giChr = new java.util.concurrent.LinkedBlockingDeque<Integer>();

		timer = new javax.swing.Timer(1000, this);
		bi_buf = new byte[256]; // max transfer
		fpys = new JPanel[2];
		leds = new LED[2];
		inds = new LED[2];
		imgs = new RandomAccessFile[2];
		prot = new boolean[2];
		bi_cache = -1;
		curUnit = -1;
		for (int x = 0; x < 2; ++x) {
			leds[x] = new RoundLED(LED.Colors.RED);
			inds[x] = new RoundLED(LED.Colors.INCAND);
			fpys[x] = getFloppy(x);
			fpys[x].setToolTipText("(no disk)");
		}
		pwr = new RoundLED(LED.Colors.INCAND);
		pwr.set(true);
		err = new RoundLED(LED.Colors.RED);

//		JMenuBar mb = new JMenuBar();
//		//JMenu mu = new JMenu("Memory");
//		JMenuItem mi = new JMenuItem("Config Units", KeyEvent.VK_U);
//		mi.addActionListener(this);
//		mb.add(mi);
//		//mu.add(mi);
//		//mi = new JMenuItem("Save Core", KeyEvent.VK_S);
//		//mi.addActionListener(this);
//		//mu.add(mi);
//		//mi = new JMenuItem("Load Core", KeyEvent.VK_L);
//		//mi.addActionListener(this);
//		//mu.add(mi);
//		//mb.add(mu);
//		setJMenuBar(mb);

		_addr = new JTextField();
		_addr.setPreferredSize(new Dimension(30, 20));
		_addr.setHorizontalAlignment(SwingConstants.RIGHT);
		_addr.setEditable(true);

		fp_adr = new JLabel("00-00");
		Border lb = BorderFactory.createBevelBorder(BevelBorder.LOWERED);
		fp_adr.setBorder(lb);
		fp_adr.setBackground(Color.white);
		fp_adr.setOpaque(true);
		//fp_adr.setMargin(new Insets(5,5,5,5));
		fp_adr.addMouseListener(this);
		String s = Wang_UI.getProperties().getProperty(_pfx + "addr");
		if (s != null) {
			try {
				addr = Integer.valueOf(s);
			} catch (Exception ee) {}
			addr &= 0x0f; // TODO: proper validation?
		}
		fp_adr.setText(String.format("00-%02d", addr));

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
		pn.setPreferredSize(new Dimension(30, 30));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		add(pn);
		++gc.gridx;
		++gc.gridy;
		gb.setConstraints(fpys[0], gc); // "left"
		add(fpys[0]);
		++gc.gridx;
		{
			int y = gc.gridy;
			gc.gridy = 0;
			gc.gridheight = 3;
			pn = getFrontPanel();
			gb.setConstraints(pn, gc);
			add(pn);
			gc.gridy = y;
			gc.gridheight = 1;
		}
		++gc.gridx;
		gb.setConstraints(fpys[1], gc); // "right"
		add(fpys[1]);
		++gc.gridx;
		++gc.gridy;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(30, 30));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		add(pn);

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
		JLabel lab = new JLabel("Device Address: ");

		gb.setConstraints(lab, gc);
		_dia_pn.add(lab);
		++gc.gridx;
		gb.setConstraints(_addr, gc);
		_dia_pn.add(_addr);
		++gc.gridx;

		_btns = new Object[3];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_SAVE] = "Save";
		_btns[OPTION_CANCEL] = "Cancel";
		_prefs = new JOptionPane(_dia_pn, JOptionPane.QUESTION_MESSAGE,
			JOptionPane.YES_NO_CANCEL_OPTION, Wang_UI.getIcon(), _btns);
	}

	private JPanel getFrontPanel() {
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(200, 200));
		pan.setOpaque(false);
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
		gc.anchor = GridBagConstraints.CENTER;
		pan.setLayout(gb);
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 5));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		++gc.gridy;
		gb.setConstraints(pwr, gc);
		pan.add(pwr);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(50, 5)); // only need once
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		gb.setConstraints(err, gc);
		pan.add(err);
		gc.gridx = 1;
		++gc.gridy;

		JLabel lab = new JLabel("POWER");
		gb.setConstraints(lab, gc);
		pan.add(lab);
		++gc.gridx;
		++gc.gridx;
		lab = new JLabel("ERROR");
		gb.setConstraints(lab, gc);
		pan.add(lab);
		gc.gridx = 1;
		++gc.gridy;

		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(50, 50));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		gc.gridx = 1;
		++gc.gridy;

		gb.setConstraints(inds[0], gc);
		pan.add(inds[0]);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(inds[1], gc);
		pan.add(inds[1]);
		gc.gridx = 1;
		++gc.gridy;

		lab = new JLabel("LEFT DISC");
		gb.setConstraints(lab, gc);
		pan.add(lab);
		++gc.gridx;
		lab = new JLabel("ADDR");
		gb.setConstraints(lab, gc);
		pan.add(lab);
		++gc.gridx;
		lab = new JLabel("RIGHT DISC");
		gb.setConstraints(lab, gc);
		pan.add(lab);
		gc.gridx = 1;
		++gc.gridy;

		// TODO: how to handle address setup...
		++gc.gridx;
		gb.setConstraints(fp_adr, gc);
		pan.add(fp_adr);
		++gc.gridy;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 5));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);

		return pan;
	}

	private JPanel getFloppy(int x) {
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(350, 80));
		Border lb = BorderFactory.createBevelBorder(BevelBorder.RAISED,
				normHi, normHi, normLo, normLo);
		pan.setBorder(lb);
		pan.setBackground(norm);
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
		pn.setPreferredSize(new Dimension(155, 40));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		gb.setConstraints(leds[x], gc);
		pan.add(leds[x]);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(155, 40));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		pan.add(pn);
		return pan;
	}

	private void doGO() {
		bi_hdr = 0;
		giChr.add(GO);
	}

	// Don't bother trying to cache... selDsk() issues
	private void cacheSector() {
		if (bi_sts != 0) return;
		// if (bi_addr == bi_cache) return; // really?
		// flushSector(); // TODO: more selective?
		bi_cache = -1;
		try {
			imgs[curUnit].seek(bi_sect);
			imgs[curUnit].read(bi_buf);
			bi_cache = bi_addr;
			bi_dirty = false;
		} catch (Exception ee) {
			err.set(true);
			bi_sts = 1;
		}
	}

	private void flushSector() {
		if (bi_sts != 0) return;
		if (!bi_dirty) return;
		try {
			imgs[curUnit].seek(bi_sect);
			imgs[curUnit].write(bi_buf);
			bi_dirty = false;
		} catch (Exception ee) {
			err.set(true);
			bi_sts = 1;
		}
	}

// long w_min = 0; // first pass handled special
// long w_max = 0;
// long w_sum = 0;
// int w_cnt = 0;
// long r_min = 0; // first pass handled special
// long r_max = 0;
// long r_sum = 0;
// int r_cnt = 0;
// long t0 = 0;
// private synchronized void recordTime(long t) {
// 	if (bi_write) {
// 		if (w_cnt == 0) w_min = t;
// 		++w_cnt;
// 		w_sum += t;
// 		if (t > w_max) w_max = t;
// 		if (t < w_min) w_min = t;
// 	} else {
// 		if (r_cnt == 0) r_min = t;
// 		++r_cnt;
// 		r_sum += t;
// 		if (t > r_max) r_max = t;
// 		if (t < r_min) r_min = t;
// 	}
// }
// private synchronized void reportTime() {
// 	if (w_cnt == 0 && r_cnt == 0) return;
// 	double w_d = (double)w_sum;
// 	double r_d = (double)r_sum;
// 	System.err.format("I/O times write: %d %d %f (%d)\n" +
// 	                  "           read: %d %d %f (%d)\n",
// 		w_min, w_max, w_d / w_cnt, w_cnt,
// 		r_min, r_max, r_d / r_cnt, r_cnt);
// 	w_min = 0; // first pass handled special
// 	w_max = 0;
// 	w_sum = 0;
// 	w_cnt = 0;
// 	r_min = 0; // first pass handled special
// 	r_max = 0;
// 	r_sum = 0;
// 	r_cnt = 0;
// }

// timing for disk copy
// I/O times write: 11188   78545   22630.456169 (1232)
//            read:  8653 1142374 1032527.159091 (1232)



	private void bi_next(int _iob, int c) {
		bi_rsp = 0; // assume response will be ACK
		if (_iob == 2) { // header
			switch (bi_hdr) {
			case 0: // first byte, init
				bi_addr = c << 16;
				bi_write = false;
				bi_size = 0;
				bi_sts = 0; // assume success
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
				selDsk(bi_addr >= cap ? 1 : 0);
				leds[curUnit].set(true);
				timer.addActionListener(this);
				timer.restart();
				if (imgs[curUnit] == null) {
					err.set(true);
					bi_sts = 1;
				}
				bi_sect = bi_addr;
				if (bi_sect >= cap) {
					bi_sect -= cap;
					if (bi_sect >= cap) {
						err.set(true);
						bi_sts = 1;
					}
				}
				if (bi_sect + bi_size > cap) {
					err.set(true);
					bi_sts = 1;
				}
				if (bi_write && prot[curUnit]) {
					err.set(true); // TODO: when to shut off?
					bi_sts = 1;
				}
				cacheSector();
				bi_idx = 0;
//t0 = Wang_UI.getCore().getDebug().getCycles();
				break;
			}
			++bi_hdr;
		} else if (_iob == 3) { // data or status
			if (bi_cnt < bi_size) {
				// recv data, send ack OR
				// (recv ack) send data
				if (bi_write) {
					bi_buf[bi_idx++] = (byte)c;
					bi_dirty = true;
				} else {
					bi_rsp = bi_buf[bi_idx++] & 0xff;
				}
			} else if (bi_cnt == bi_size) { // only once
				// always status sent to calculator
				bi_rsp = bi_sts;
				bi_hdr = 0;
//recordTime(Wang_UI.getCore().getDebug().getCycles() - t0);
				flushSector();
				leds[curUnit].set(false);
			}
			++bi_cnt;
		}
	}

	private void setupAddr() {
		_addr.setText(String.format("%d", addr));
		Dialog dlg = _prefs.createDialog(this, "Set " + getModel() + " Address");
		dlg.setVisible(true);
		Object res = _prefs.getValue();
		if (_btns[OPTION_CANCEL].equals(res)) return;
		if (_btns[OPTION_APPLY].equals(res) ||
				_btns[OPTION_SAVE].equals(res)) {
try {
			// normalize...
			int a = 0;
			try {
				a = Integer.valueOf(_addr.getText());
			} catch (Exception ee) {}
			addr = a & 0x0f; // TODO: proper validation?
			fp_adr.setText(String.format("00-%02d", addr));
			Wang_Properties temp = Wang_UI.getProperties().getClass().
				getDeclaredConstructor().newInstance();
			String prop = _pfx + "addr";
			temp.setProperty(prop, String.format("%d", addr));
			if (_btns[OPTION_SAVE].equals(res)) {
				temp.save();
			}
} catch (Exception ee) {}
		}
	}

	private boolean _prot; // yuk...
	private File pickFile(String purpose) {
		File file;
		SuffFileChooser ch = new SuffFileChooser(purpose,
			"wfd", "Wang Floppy Disc", _dir);
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
			_dir = file; // start back in same place
			_prot = ch.isProtected(); // yuk...
		} else {
			file = null;
		}
		return file;
	}

	private void mountFloppy(int x) {
		String s = String.format("Mount " + driveName[x] + " Floppy");
		File f = pickFile(s);
		if (f == null) return; // make no changes...
		if (imgs[x] != null) {
			try {
				imgs[x].close();
			} catch (Exception ee) {}
			imgs[x] = null;
			fpys[x].setToolTipText("(no disk)");
		}
		try {
			if (f.exists()) {
				long l = f.length();
				if (l == 0 && (_prot || !f.canWrite())) {
					Wang_UI.warning(s, "File is empty");
					return;
				}
				if (l > 0 && l != cap) {
					Wang_UI.warning(s, "File is wrong size");
					return;
				}
			} else {
				f.createNewFile(); // length is set later
			}
		} catch (Exception ee) {
			Wang_UI.warning(s, ee.getMessage());
			return;
		}
		prot[x] = _prot;
		try {
			imgs[x] = new RandomAccessFile(f, _prot ? "r" : "rw");
			if (imgs[x].length() == 0) {
				imgs[x].setLength(cap);
			}
			fpys[x].setToolTipText(f.getName());
		} catch (Exception ee) {
			imgs[x] = null;
			Wang_UI.warning(s, ee.getMessage());
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == timer) {
			timer.removeActionListener(this);
			if (curUnit >= 0) {
				leds[curUnit].set(false);
			}
			return;
		}
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		int mn = mi.getMnemonic();
		if (mn == KeyEvent.VK_D) {
			onOff(true);
			return;
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

	private int getMousedUnit(Object src) {
		if (src == fpys[0]) return 0;
		else if (src == fpys[1]) return 1;
		else return -1;
	}

	public void mouseClicked(MouseEvent e) {
		int x;
		Object obj = e.getSource();
		if (obj == fp_adr) {
			setupAddr();
			return;
		}
		x = getMousedUnit(obj);
		if (x < 0) return;
		int b = e.getButton();
		if (b == MouseEvent.BUTTON1) {
			mountFloppy(x);
		} else if  (b == MouseEvent.BUTTON3) {
			System.err.format("Unmount floppy %d\n", x);
		}
	}
	public void mouseEntered(MouseEvent e) {
		int x = getMousedUnit(e.getSource());
		if (x < 0) return;
		fpys[x].setBackground(actv);
	}
	public void mouseExited(MouseEvent e) {
		int x = getMousedUnit(e.getSource());
		if (x < 0) return;
		fpys[x].setBackground(norm);
	}
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
	// Calculator is sending hdr/data
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
			// end of I/O... but 700 does not do this
			if (curUnit >= 0) leds[curUnit].set(false); // or leave on?
			return devEna;
		}
		if ((_iob & 0b101) == 0b100) { // GROUP-1, ignore
			return devEna;
		}
		if (c != addr) {
			// disable anything enabled...
			if (curUnit >= 0) leds[curUnit].set(false);
			curUnit = -1;
			devEna = false;
		} else {
			devEna = true; // just to be sure
			if (c != curUnit && curUnit >= 0) {
				leds[curUnit].set(false);
			}
			// curUnit only valid during I/O
		}
		if (!devEna) return devEna;
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

	private void selDsk(int u) {
		if (curUnit != u) {
			if (curUnit >= 0) {
				leds[curUnit].set(false);
				timer.removeActionListener(this);
				inds[curUnit].set(false);
			}
			curUnit = u;
			inds[curUnit].set(true);
			repaint(); // does nothing
		}
	}

	public void reset() { // hardware reset, a.k.a. PRIME
//reportTime();
		bi_hdr = 0;
		devEna = false;
		err.set(false);
		selDsk(0);
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
					Thread.sleep(0, 10000);
				}
			} catch (Exception ee) {
ee.printStackTrace();
}
			if (c < 0) continue; // or break?
			// TODO: verify IOB = 4,5,6,7 ?
			// do this early to avoid a race:
			//setKBD(false); // TODO: should we interfere?
			// replyIO() does that before returning.
			Wang_UI.getCore().replyIO(curIob, c);
		}
	}
}
