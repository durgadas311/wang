// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.concurrent.LinkedBlockingDeque;

// GROUP-1 00-00 and 00-07
// GROUP-1 00-08 and 00-15 (config sw)
// hardware may support other addrs
//
//     CO0 = x0110xxx = '0'..'7'
//           x011xxxx = '0'..'9', more...
//     CO1 = x0101110 = '.' DP
//     CO2 = x0101101 = '-' CHANGE SIGN
//     CO3 = x1111111 = RUBOUT (dup?)
//     CO4 = x011100x = '8', '9' (N.C.?)
//     CO5 = x0001101 = !CR
// RUB OUT = x1111111
//
//           xxxxx = 00 + RST           00-00 * read number (skip pre)
//                        COT           00-01 - continue??? resume?
//    JMP0 = x1xx1 = 09 + RST		00-02 - send and resume?
//    JMP1 = xx1x1 = 05 + RST		00-03 - send DP and resume?
//    JMP2 = xx111 = 07 + RST		00-04 - send CHG SGN and resume?
//    JMP3 = 1xxxx = 10 + RST		00-05 - skip and resume?
//    JMP4 = xxxxx = 00 + RST		00-06 - start over?
//	     1xxx1 = 11 + RST	(603)
//    JMP5 = 1x11x = 16 + RST		00-07 * skip to next CR
//	     1x111 = 17 + RST	(603)
//
// PCD (version differ)
// 00-00: read number
//  00	READ
//  01	COMPARE0 (0-7): RAC, JMP0 (09)
//  02  COMPARE1 (DP):	RAC, JMP1 (05)
//  03	COMPARE2 (SGN):	RAC, JMP2 (07)
//  04	RESTART - - skip non-num at start...
//  05	DP (send)
//  06	JUMP3 (10)
//  07	CHANGE SIGN (send)
//  08	JUMP3 (10)
//  09	TRANS - translate and send
//  10	READ
//  11	COMPARE0 (0-7): RAC, JMP0 (09)
//  12	COMPARE1 (DP):  RAC, JMP1 (05)
//  13	COMPARE2 (SGN): RAC, JMP2 (07)
//  14	COMPARE3 (RUB):	RAC, JMP3 (10)
//  15	GO (stop)
// 00-07: skip to next CR
//  16	READ
//  17	COMPARE5 (!CR):	RAC, JMP5 (16)
//  18	GO (stop)
//  19	(noop)
//
// COT -> XBP
// XBR -> XBP
// TRANS -> set, XBP... (until GKBD), GISN (DAS)
// RAC -> XBP, ++PCD
// RST -> load PCD
// RUB OUT -> count 4, end, S&R

class Wang_PaperTapeReader extends JFrame
		implements Wang_GroupIODevice,
		ActionListener, WindowListener, MouseListener, Runnable
{
	public static final String Model = "03";
	public static final String Description = "Paper Tape Reader";

	private static JMenuItem pmi = null;
	private static Wang_PaperTapeReader thus = null;
	public static String s_getModel() {
		return Wang_UI.getSeries() + Model;
	}
	public static String s_getName() {
		return s_getModel() + " " + Description;
	}
	public static JMenuItem s_getMenu(int key) { // plug-in menu
		if (pmi != null) return pmi;
		pmi = new JMenuItem(s_getName() + " (not installed)", key);
		return pmi;
	}
	public static Wang_PaperTapeReader s_getInstance(Component comp) {
		if (thus != null) return thus;
		String p = String.format("wang%s00_%s03_image",
			Wang_UI.getSeries(), Wang_UI.getSeries());
		thus = new Wang_PaperTapeReader(p, comp);
		return thus;
	}

	static JMenuItem dev_mi = null;
	private boolean plugged = false;

	public String getModel() { return s_getModel(); }
	public String getName() { return s_getName(); }
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
	public JMenuItem getMenu() {
		if (dev_mi != null) return dev_mi;
		dev_mi = new JMenuItem(s_getName(), KeyEvent.VK_D);
		dev_mi.addActionListener(this);
		return dev_mi;
	}

	// Group 1 00 00 = skip non-num, read numeric until non-numeric, GO
	// Group 1 00 07 = skip until CR, GO

	String _prop; // tape image file
	String _mountLabel;
	String[] _pickLabel;
	String[] _fileType;
	File _file;
	Component _comp;
	boolean visib;

	JRadioButton a00_00;
	JRadioButton a00_08;
	JPanel _dia_pn;
	Object[] _btns;
	JOptionPane _prefs;
	static final int OPTION_APPLY = 0;
	static final int OPTION_SAVE = 1;
	static final int OPTION_CANCEL = 2;
	String _pfx;

	PaperTapeViewer rdr_vu;
	JMenuItem rdr_mi;
	JMenuItem pos_mi;
	long rdr_idx;
	long rdr_tot;

	int addr;
	int _iob;
	boolean _input;	// send to Wang vs. skip (00-00 vs. 00-07)
	boolean _end;
	int _currByte;	// -1 for none (BOT or EOT)
	private static final String numerics = "0123456789.+-";
	RandomAccessFile _fin;
	LinkedBlockingDeque<Integer> giCmd;
	boolean gkbd; // actually, !GKBD

	private boolean isNumeric() {
		int x = numerics.indexOf((char)_currByte);
		return (x >= 0);
	}

	public void reset() {
		giCmd.clear(); // still could be one in the chamber...
		_input = false;
		_iob = 0;
	}

	private void tape_close() {
		if (_fin == null) return;
		try {
			_fin.close();
		} catch (Exception ee) {}
		_fin = null;
		rdr_vu.update(0, 0);
		rdr_vu.repaint();
	}

	private void tape_open() {
		_end = false;
		_currByte = -1;
		if (_file == null) return;
		try {
			_fin = new RandomAccessFile(_file, "r");
			rdr_idx = -1;
			rdr_tot = _fin.length();
			updateRdrView(0);
		} catch (Exception ee) {
		}
	}

	private void updateRdrView(long newIdx) {
		long pos = newIdx - rdr_vu.buf;
		int _beg = 0;
		int _end = rdr_vu.win;

		if (newIdx == rdr_idx) return;
		if (pos < 0) {
			_beg = (int)-pos;
			pos = 0;
		}
		if (pos + (_end - _beg) > rdr_tot) {
			_end = (int)(_beg + (rdr_tot - pos));
		}
		try {
			_fin.seek(pos);
			_fin.read(rdr_vu.tapeBuf, _beg, _end - _beg);
			rdr_idx = newIdx;
			rdr_vu.update(_beg, _end);
			rdr_vu.repaint();
		} catch (Exception ee) {}
	}

	private void getByte() {
		if (_fin == null || rdr_idx >= rdr_tot) {
			_end = true;
			_currByte = -1;
			return;
		}
		int b = rdr_vu.tapeBuf[rdr_vu.buf]; // 'buf' only valid for !top
		// advance tape...
		updateRdrView(rdr_idx + 1);
		_currByte = b;
	}

	public boolean start_cn36(int iob, int c) {
		_input = false;
		if (_file == null) {
			//unless we allow mounting a tape later...
			return false;
		}
		// currently, don't care if running program or not...
		_input = ((iob & 0x05) == 4 && (c & ~7) == addr);
		if (!_input) return _input;
		_iob = iob;
		giCmd.add(c & 0x07);
		return _input;
	}

	public void do_ack(int iob) {} // not used
	public void do_dev(int iob, int b) {} // not a block i/o device

	public int getGLRN() { return 0; }
	public void setGKBD(boolean state) { gkbd = !state; }
	public boolean isBlockIO() { return false; }
	public boolean isDevEnabled() { return _input; }
	public void setProperties(Wang_Properties p) {}
	public boolean onOff() { return visib; }
	public void onOff(boolean vis) {
		if (visib != vis) {
			visib = vis;
			setVisible(vis);
		}
	}
	public JFrame getFrame() { return null; }
	public Component getComponent() { return null; }

	public Wang_PaperTapeReader(String prop, Component comp) {
		super(s_getName());
		getContentPane().setName("Wang " + s_getName());
		setResizable(false);
		addWindowListener(this);

		giCmd = new LinkedBlockingDeque<Integer>();
		_pfx = String.format("wang%s00_%s03_",
			Wang_UI.getSeries(), Wang_UI.getSeries());

		JMenuBar mb = new JMenuBar();
		JMenu mu = new JMenu("Device");
		JMenuItem mi = new JMenuItem("Mount Tape", KeyEvent.VK_M);
		mi.addActionListener(this);
		mu.add(mi);
		rdr_mi = mi;
		mi = new JMenuItem("Rewind Tape", KeyEvent.VK_R);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Tape Positioner", KeyEvent.VK_P);
		mi.addActionListener(this);
		pos_mi = mi;
		mu.add(mi);
		mi = new JMenuItem("Config Addr", KeyEvent.VK_A);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);
		setJMenuBar(mb);

		a00_00 = new JRadioButton("00-00");
		a00_08 = new JRadioButton("00-08");
		String s = Wang_UI.getProperties().getProperty(_pfx + "addr");
		addr = -1;
		if (s != null) {
			try {
				int a = Integer.valueOf(s);
				if (a == 8) {
					addr = 8;
					a00_08.setSelected(true);
				}
			} catch (Exception ee) {}
		}
		if (addr < 0) {
			addr = 0;
			a00_00.setSelected(true);
		}
		ButtonGroup grp = new ButtonGroup();
		grp.add(a00_00);
		grp.add(a00_08);
		_dia_pn = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		_dia_pn.setLayout(gb);
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
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(10, 10));
		gb.setConstraints(pn, gc);
		_dia_pn.add(pn);
		++gc.gridx;
		gb.setConstraints(a00_00, gc);
		_dia_pn.add(a00_00);
		++gc.gridx;
		gb.setConstraints(a00_08, gc);
		_dia_pn.add(a00_08);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(10, 10));
		gb.setConstraints(pn, gc);
		_dia_pn.add(pn);
		_btns = new Object[3];
		_btns[OPTION_APPLY] = "Apply";
		_btns[OPTION_SAVE] = "Save";
		_btns[OPTION_CANCEL] = "Cancel";
		_prefs = new JOptionPane(_dia_pn, JOptionPane.QUESTION_MESSAGE,
			JOptionPane.YES_NO_CANCEL_OPTION, Wang_UI.getIcon(), _btns);

		// Now do the main window
		rdr_vu = new PaperTapeViewer(13, 60, false);
		rdr_vu.addMouseListener(this);

		gb = new GridBagLayout();
		setLayout(gb);
		//setBackground(Wang_Colors.ivory);
		getContentPane().setBackground(Wang_Colors.ivory);
		gc.gridx = 0;
		gc.gridy = 0;

		int tw = (rdr_vu.tapeh * 3) / 2;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(tw, rdr_vu.tapeh));
		pn.setOpaque(false);
		JLabel lab = new JLabel(" Wang " + s_getModel());
		lab.setFont(new Font("Serif", Font.BOLD, 18));
		lab.setHorizontalAlignment(SwingConstants.LEFT);
		lab.setPreferredSize(new Dimension(tw, 20));
		pn.add(lab);
		gb.setConstraints(pn, gc);
		add(pn);
		++gc.gridx;
		gb.setConstraints(rdr_vu, gc);
		add(rdr_vu);

		_input = false;
		_mountLabel = "Mount Tape";
		_pickLabel = new String[]{"Wang Data files","Text Files"};
		_fileType = new String[]{"wdf","txt"};
		_prop = prop;
		_comp = comp;
		_file = Wang_UI.getProperties().getFile(_prop, true, Wang_UI.getDir());
		if (_file != null) {
			rdr_mi.setText("Mount Tape - " + _file.getName());
			tape_open();
		}

		pack();
		onOff(false);
		setLocationByPlatform(true);

		Thread t = new Thread(this);
		t.start();
	}

	private void setupAddr() {
		Dialog dlg = _prefs.createDialog(null, "Set " + getModel() + " Address");
		dlg.setVisible(true);
		Object res = _prefs.getValue();
		if (_btns[OPTION_CANCEL].equals(res)) return;

		if (_btns[OPTION_APPLY].equals(res) ||
				_btns[OPTION_SAVE].equals(res)) {
try {
			if (a00_08.isSelected()) {
				addr = 8;
			} else {
				addr = 0;
			}
			if (_btns[OPTION_SAVE].equals(res)) {
				Wang_UI.getProperties().setAndSaveProperty(
					Wang_UI.getProperties().getClass().
						getDeclaredConstructor().newInstance(),
					_pfx + "addr",
					String.format("%d", addr));
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
		Object obj = e.getSource();
		if (obj == this) {
			onOff(false);
			return;
		}
		// must be tape positioner
		long rdr_bytes = 0;
		try {
			rdr_bytes = _fin.getFilePointer();
		} catch (Exception ee) {}
		updateRdrView(rdr_bytes);
	}

	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) { }
	public void mouseMoved(MouseEvent e) { }
	public void mouseClicked(MouseEvent e) {
		Object obj = e.getSource();
		if (obj != rdr_vu) return;
		pos_mi.doClick();
	}


	public void actionPerformed(ActionEvent e) {
		// There is only one, but decode it anyway...
		Object src = e.getSource();
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		if (mi.getMnemonic() == KeyEvent.VK_D) {
			onOff(true);
			return;
		}
		if (mi.getMnemonic() == KeyEvent.VK_R) {
			if (_fin == null) return;
			rdr_idx = -1;
			_end = false;
			updateRdrView(0);
			return;
		}
		if (mi.getMnemonic() == KeyEvent.VK_P) {
			if (_fin == null) return;
			// TODO: stop reading?
			// file pointer is out-of-position
			try {
				_fin.seek(rdr_idx);
			} catch (Exception ee) {}
			JFrame jf = new PaperTapePositioner(this, _fin, 8, this);
			return;
		}
		if (mi.getMnemonic() == KeyEvent.VK_A) {
			setupAddr();
			return;
		}
		if (mi.getMnemonic() != KeyEvent.VK_M) return;
		// XXX: assert mi == dev_mi
		// XXX: There is no window, so only pop-up file dialog
//		String status = "not mounted";
//		if (_file != null) {
//			status = _file.getName();
//		}
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(_comp);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			mi.setText("Mount Tape - " + _file.getName());
		} else {
			_file = null;
			mi.setText("Mount Tapee - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().
					getDeclaredConstructor().newInstance(),
				_prop,
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		tape_open();
	}

	private void sendChr(int c) {
		while (!gkbd) {
			try { Thread.sleep(10); } catch (Exception ee) {}
		}
		if (c == GO || c == SR0) {
			_input = false;
		}
		Wang_UI.getCore().replyIO(_iob, c);
	}

	// Modeled after the 703/603/733/633 hardware.
	byte pcd;
	// Diode array on 6110 at E/F-4. Indexed by GIoB0:2 and JMPx
	// driving "next" PCD.
	static final byte jmp[] = new byte[]{
		0, -1, 9, 5, 7, 10, 0, 16
	};
	static final int JMP0 = 2;
	static final int JMP1 = 3;
	static final int JMP2 = 4;
	static final int JMP3 = 5;
	static final int JMP4 = 6;
	static final int JMP5 = 7;
	// Patch area on 6110 at A..G-10.
	private void doPCD() {
		switch (pcd) {
		case 0:  // READ
		case 10: // READ
		case 16: // READ
			getByte();
			if (_end || _currByte < 0) {
				// hardware counts 4 contig. RUBOUTs and
				// then forces a stop. We know immediately
				// when data ends. This overrides whatever
				// "program step" we're on.
				//
				// This does not trigger a GO, but does
				// deactivate this device. If user's SR0-00
				// executes a GO then the GROUP command
				// is terminated.
				sendChr(SR0);
				pcd = -1;
			} else {
				++pcd;
			}
			break;
		case 1:  // COMPARE0: JMP0
		case 11: // COMPARE0: JMP0
			if (_currByte >= '0' && _currByte <= '9') {
				pcd = jmp[JMP0];
			} else ++pcd;
			break;
		case 2:  // COMPARE1: JMP1
		case 12: // COMPARE1: JMP1
			if (_currByte == '.') {
				pcd = jmp[JMP1];
			} else ++pcd;
			break;
		case 3:  // COMPARE2: JMP 2
		case 13: // COMPARE2: JMP 2
			if (_currByte == '-') {
				pcd = jmp[JMP2];
			} else ++pcd;
			break;
		case 4:	// RESTART
			pcd = 0;
			break;
		case 5:	// DP
			sendChr(DP);
			++pcd;
			break;
		case 6: // JUMP3: JMP3
		case 8: // JUMP3: JMP3
			pcd = jmp[JMP3];
			break;
		case 7:	// DP
			sendChr(DP);
			++pcd;
			break;
		case 9:	// TRANS
			if (_currByte >= '0' && _currByte <= '9') {
				sendChr(E0 + (_currByte & 0x0f));
			} else {
				sendChr(_currByte & 0x3f); // fudge
			}
			++pcd;
			break;
		case 14: // COMPARE3: JMP 3
			if (_currByte == '-') {
				pcd = jmp[JMP3];
			} else ++pcd;
		case 15: // GO - terminal
		case 18: // GO - terminal
			sendChr(GO);
			pcd = -1; // how to signal stop?
			break;
		case 17: // COMPARE5: JMP 5
			// TODO: tolerate \r\n ?
			if (_currByte != '\r') {
				pcd = jmp[JMP5];
			} else ++pcd;
		case 19: // undefined, NO-OP
			// what does hardware actually do?
			pcd = 0; // wrap around?
			break;
		}
	}

	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giCmd.take();
			} catch (Exception ee) {}
			if (c < 0) continue; // or break?
			if (!_input) continue; // PRIME, etc.
			byte pc = jmp[c];
			if (pc >= 0) pcd = pc;
			while (pcd >= 0) {
				doPCD();
			}
		}
	}
}
