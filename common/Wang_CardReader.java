// Copyright (c) 2026 Douglas Miller

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.util.concurrent.LinkedBlockingDeque;

// GROUP-1 04-01 in RUN mode
// GROUP-2 04-01 in LEARN mode
//

class Wang_CardReader extends JFrame
		implements Wang_GroupIODevice,
		ActionListener, WindowListener, Runnable
{
	public static final String Model = "14";
	public static final String Description = "Card Reader";
	public static final int stepsPerCard = 40;
	static final Color buff1 = new Color(243, 226, 182);
	static final Color silver1 = new Color(200, 200, 200);
	static final Color silver2 = new Color(220, 220, 220);
	static final Color silver3 = new Color(180, 180, 180);

	private static JMenuItem pmi = null;
	private static Wang_CardReader thus = null;
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
	public static Wang_CardReader s_getInstance(Component comp) {
		if (thus != null) return thus;
		thus = new Wang_CardReader(comp);
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

	String _prop; // tape image file
	String _mountLabel;
	String[] _pickLabel;
	String[] _fileType;
	File _file;
	Component _comp;
	boolean visib;

	JTextField dk_nm;
	JButton load;
	JButton next;
	JButton rew;
	JCheckBox auto;
	JCheckBox no_go;
	CardDeck inp;
	CardDeck out;
	LED gon;

	int addr = 0x41;
	int _iob;
	boolean _input;
	int _glrn;
	boolean _end;
	int _currByte;	// -1 for none (BOT or EOT)
	LinkedBlockingDeque<Integer> giCmd;
	boolean gkbd; // actually, !GKBD

	byte[] deck;
	int deck_len;
	int deck_idx;
	int go;
	int ep;
	int ncard;

	public void reset() {
		giCmd.clear(); // still could be one in the chamber...
		setActive(false);
		_iob = 0;
	}

	private void initCards() {
		inp.initDeck(ncard, ncard);
		out.initDeck(0, ncard);
	}

	private void updateCards() {
		int n_in = (deck_len - deck_idx + stepsPerCard - 1) / stepsPerCard;
		int n_out = ncard - n_in;
		inp.updateCards(n_in);
		out.updateCards(n_out);
	}

	private void rewindDeck() {
		deck_idx = 0;
		updateCards();
	}

	private void getByte() {
		if (deck_idx >= deck_len) {
			_end = true;
			_currByte = -1;
			return;
		}
		int b = deck[deck_idx++] & 0xff;
		updateCards();
		_currByte = b;
	}

	public boolean start_cn36(int iob, int c) {
		// currently, don't care if running program or not...
		setActive(((iob & 0x04) == 4 && c == addr));
		if (!_input) return _input;
		_iob = iob;
		_glrn = (iob & 1);
		if (auto.isSelected() && ncard > 0) {
			giCmd.add((iob << 8) | c);
		}
		return _input;
	}

	public void do_ack(int iob) {} // not used
	public void do_dev(int iob, int b) {} // not a block i/o device

	public int getGLRN() { return _glrn; }
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
		if (vis) toFront();
	}
	public JFrame getFrame() { return null; }
	public Component getComponent() { return null; }

	public Wang_CardReader(Component comp) {
		super(s_getName());
		getContentPane().setName("Wang " + s_getName());
		setResizable(false);
		addWindowListener(this);
		getContentPane().setBackground(silver1);
		go = Wang_UI.getCore().getGo();
		ep = Wang_UI.getCore().getEndProg();
		gon = new RoundLED(LED.Colors.INCAND);

		deck = new byte[50 * stepsPerCard]; // more than fill 600/700 max memory
		deck_len = 0;
		load = new JButton("Load");
		load.setPreferredSize(new Dimension(80, 30));
		load.addActionListener(this);
		load.setMnemonic(KeyEvent.VK_L);
		rew = new JButton("Rewind");
		rew.setPreferredSize(new Dimension(80, 30));
		rew.addActionListener(this);
		rew.setMnemonic(KeyEvent.VK_R);
		next = new JButton("Next");
		next.setPreferredSize(new Dimension(80, 30));
		next.addActionListener(this);
		next.setMnemonic(KeyEvent.VK_N);
		auto = new JCheckBox("Auto Feed");
		auto.addActionListener(this);
		auto.setSelected(true);
		no_go = new JCheckBox("No GO");
		no_go.addActionListener(this);
		no_go.setSelected(true);
		next.setEnabled(false);
		inp = new CardDeck(50, 100, true);
		out = new CardDeck(50, 100, false);
		dk_nm = new JTextField();
		dk_nm.setPreferredSize(new Dimension(200, 20));
		dk_nm.setBackground(Color.white);
		dk_nm.setOpaque(true);

		giCmd = new LinkedBlockingDeque<Integer>();

		// Now do the main window
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

		setLayout(gb);
		setGap(10, gb, gc);
		++gc.gridx;
		++gc.gridy;

		int tw = 200;
		int th = 100;
		JLabel lab = new JLabel("Wang " + s_getModel());
		lab.setFont(new Font("Serif", Font.BOLD, 18));
		lab.setHorizontalAlignment(SwingConstants.LEFT);
		lab.setPreferredSize(new Dimension(tw / 2, 20));
		gc.gridwidth = 1;
		gb.setConstraints(lab, gc);
		add(lab);
		++gc.gridx;
		gb.setConstraints(gon, gc);
		add(gon);
		--gc.gridx;
		++gc.gridy;
		gc.gridwidth = 1;
		setGap(5, gb, gc);
		++gc.gridy;
		gb.setConstraints(load, gc);
		add(load);
		++gc.gridx;
		gb.setConstraints(no_go, gc);
		add(no_go);
		--gc.gridx;
		++gc.gridy;
		setGap(5, gb, gc);
		++gc.gridy;
		gc.gridwidth = 2;
		gb.setConstraints(dk_nm, gc);
		add(dk_nm);
		++gc.gridy;
		gc.gridwidth = 1;
		setGap(5, gb, gc);
		++gc.gridy;
		gc.anchor = GridBagConstraints.EAST;
		gc.gridwidth = 2;
		gb.setConstraints(auto, gc);
		add(auto);
		++gc.gridy;
		gc.gridwidth = 1;
		setGap(5, gb, gc);
		++gc.gridy;
		gb.setConstraints(rew, gc);
		add(rew);
		++gc.gridx;
		gb.setConstraints(next, gc);
		add(next);
		++gc.gridy;
		int e = gc.gridy;
		++gc.gridx;
		gc.gridy = 1;
		setGap(5, gb, gc);
		++gc.gridx;
		gc.gridheight = 4;
		gc.anchor = GridBagConstraints.SOUTH;
		gb.setConstraints(out, gc);
		add(out);
		gc.gridy += gc.gridheight;
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridheight = 1;
		// TODO: a raised bar... gridheight = 1
		++gc.gridy;
		gc.anchor = GridBagConstraints.NORTH;
		gc.gridheight = 4;
		gb.setConstraints(inp, gc);
		add(inp);
		gc.gridy += gc.gridheight;
		gc.gridheight = 1;
		if (e > gc.gridy) gc.gridy = e;
		++gc.gridx;
		setGap(10, gb, gc);

		setActive(false);
		_mountLabel = "Mount Cards";
		_pickLabel = new String[]{"Wang Program files"};
		_fileType = new String[]{"w" + Wang_UI.getSeries() + "p"};
		_comp = comp;

		pack();
		onOff(false);
		setLocationByPlatform(true);

		Thread t = new Thread(this);
		t.start();
	}

	private void setGap(int z, GridBagLayout gb, GridBagConstraints gc) {
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(z, z));
		pn.setOpaque(false);
		gb.setConstraints(pn, gc);
		add(pn);
	}

	private void setActive(boolean act) {
		_input = act;
		gon.set(act);
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
		// TODO: any other windows?
	}

	public void actionPerformed(ActionEvent e) {
		// There is only one, but decode it anyway...
		Object src = e.getSource();
		if (src == auto) {
			next.setEnabled(!auto.isSelected());
			if (auto.isSelected() && _input) { // let things fly...
				giCmd.add((_iob << 8) | addr);
			}
			return;
		}
		if (src == load) {
			loadCards();
			return;
		}
		if (src == next) {
			// TODO: limit overrun?
			if (_input) {
				giCmd.add((_iob << 8) | addr);
			}
			return;
		}
		if (src == rew) {
			rewindDeck();
			if (auto.isSelected() && _input) { // let things fly...
				giCmd.add((_iob << 8) | addr);
			}
			return;
		}
		if (!(src instanceof JMenuItem)) return;
		JMenuItem mi = (JMenuItem)src;
		if (mi.getMnemonic() == KeyEvent.VK_D) {
			onOff(true);
			return;
		}
		if (mi.getMnemonic() == KeyEvent.VK_L) {
			loadCards();
			return;
		}
	}

	private void loadCards() {
		// XXX: assert mi == dev_mi
		// XXX: There is no window, so only pop-up file dialog
//		String status = "not mounted";
//		if (_file != null) {
//			status = _file.getName();
//		}
		//tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(_comp);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
		} else {
			_file = null;
		}
		try {
			InputStream fi = new FileInputStream(_file);
			deck_len = fi.read(deck);
			fi.close();
			deck_idx = 0;
			if (deck_len >= 2) { // check for tape image
				if ((deck[deck_len - 2] & 0xff) == ep) {
					if ((deck[deck_len - 1] & 0xff) == ep) {
						--deck_len;
					} else {
						deck_len -= 2;
					}
				}
			}
			ncard = (deck_len + stepsPerCard - 1) / stepsPerCard;
			dk_nm.setText(_file.getName());
		} catch (Exception ee) { // also when _file == null
			deck_len = 0;
			deck_idx = 0;
			ncard = 0;
			dk_nm.setText("");
		}
		initCards();
		if (_file != null && auto.isSelected() && _input) { // let things fly...
			giCmd.add((_iob << 8) | addr);
		}
	}

	private void sendChr(int c) {
		while (!gkbd) {
			try { Thread.sleep(10); } catch (Exception ee) {}
		}
		if (c == GO) {
			_glrn = 0;
			setActive(false);
		}
		Wang_UI.getCore().replyIO(_iob, c);
	}

	// Do one card max.
	// Reader does entire card, excluding any SKIP rows.
	// The hardware seems to detect a GO and terminate the
	// input, but the is problematic for programs that use
	// GO as a no-op. Unclear just how this device was used.
	private void readCard() {
		boolean skip = false;
		int n = 0;
		while (n < stepsPerCard) {
			getByte();
			if (_currByte < 0) {
				// as if all remaining rows are SKIP
				if (auto.isSelected()) {
					sendChr(GO);
				}
				break;
			}
			++n;
			// TODO: hardware does this, but it's annoying if
			// program has any GOs...
			if (_currByte == go) {
				skip = !no_go.isSelected();
				sendChr(GO); // TODO: exact behavior needed
			}
			if (!skip) {
				sendChr(_currByte);
			}
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
			do {
				readCard();
			} while (_currByte >= 0 && auto.isSelected());
		}
	}
}
