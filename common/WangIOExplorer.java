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

public class WangIOExplorer extends JFrame
		implements Wang_OutputDevice, Wang_BlockIODevice,
			ActionListener, WindowListener, Runnable {
	Wang_Debugger dbg;
	boolean visib;
	GenericHelp help;

	GridBagLayout gb;
	GridBagConstraints gc;
	Font tfont;
	Vector<Integer> sepy;
	int width = 0;

	// General I/O status (RO)
	JTextField gioa;
	JTextField giob;
	JTextField iob;

	// TypeWriter status/control
	JTextField tyo;
	JCheckBox tws;
	JButton ty_clr;
	JCheckBox ty_auto;
	JCheckBox ty_raw;
	JCheckBox rbs;

	// Group 1/2 status/control
	LED don;
	JTextField adr;
	JRadioButton grp1;
	JRadioButton grp2;
	JCheckBox gi_num;
	JTextField gin;
	JButton gisn;
	JCheckBox gkbd;
	JButton go;
	JCheckBox glrn;
	JCheckBox gi_auto;

	// Block I/O status/control
	JCheckBox bi_auto;
	JButton bi_gisn;
	JCheckBox bi_giso;
	JCheckBox bi_supp;
	JTextField hdr;	// IOB=2
	JTextField hdr_d; // hdr interpretation
	JTextField bi_in; // IOB=3 and READ
	JTextField bi_out; // IOB=3 and WRITE
	JTextField sts; // IOB=3 last byte

	boolean ty_shift; // true == SHIFT
	boolean ty_newline;
	String tyo_txt;
	String tyo_raw;
	boolean devEna;
	int curIob;
	int curCode;
	int[] gin_dat;
	int gin_idx;
	java.util.concurrent.LinkedBlockingDeque<Integer> giChr;
	boolean gi_done;
	int bi_hdr;
	boolean bi_write;
	int bi_size;
	int bi_addr;
	int bi_cnt;
	int bi_rsp; // what to send with next GISN (manual or auto)
	byte[] bin_dat; // these are always bytes
	int bin_idx;

	public WangIOExplorer(Properties props) {
		super("Wang I/O Explorer");
		getContentPane().setName("Wang I/O Explorer");
		setResizable(false);
		addWindowListener(this);
		tfont = new Font("SansSerif", Font.BOLD, 18);
		sepy = new Vector<Integer>();
		giChr = new java.util.concurrent.LinkedBlockingDeque<Integer>();

		java.net.URL url = this.getClass().getResource("docs/ioexplorer.html");
		help = new GenericHelp("Wang I/O Explorer Help", url);

		JMenuBar mb = new JMenuBar();
		JMenu mu = new JMenu("Machine");
		JMenuItem mi;
		mi = new JMenuItem("BIO Out -> In", KeyEvent.VK_Z);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);

		mi = new JMenuItem("Help", KeyEvent.VK_H);
		mi.addActionListener(this);
		mb.add(mi);
		setJMenuBar(mb);

		// General I/O status (RO)
		gioa = new JTextField();
		gioa.setPreferredSize(new Dimension(30, 20));
		gioa.setHorizontalAlignment(SwingConstants.RIGHT);
		gioa.setEditable(false);
		gioa.setFocusable(false);
		giob = new JTextField();
		giob.setPreferredSize(new Dimension(30, 20));
		giob.setHorizontalAlignment(SwingConstants.RIGHT);
		giob.setEditable(false);
		giob.setFocusable(false);
		iob = new JTextField();
		iob.setPreferredSize(new Dimension(30, 20));
		iob.setHorizontalAlignment(SwingConstants.RIGHT);
		iob.setEditable(false);
		iob.setFocusable(false);

		// TypeWriter status/control
		tyo = new JTextField();
		tyo.setPreferredSize(new Dimension(500, 20));
		tyo.setHorizontalAlignment(SwingConstants.LEFT);
		tyo.setEditable(false);
		tyo.setFocusable(false);
		tyo_raw = "";
		tyo_txt = "";
		ty_clr = new JButton("clear");
		ty_clr.setPreferredSize(new Dimension(50, 20));
		ty_clr.setMargin(new Insets(2,2,2,2));
		ty_clr.setFocusPainted(false);
		ty_clr.addActionListener(this);
		tws = new JCheckBox("TWS");
		tws.addActionListener(this);
		tws.setEnabled(false);
		ty_auto = new JCheckBox("auto");
		ty_auto.addActionListener(this);
		ty_raw = new JCheckBox("raw");
		ty_raw.addActionListener(this);
		rbs = new JCheckBox("RBS");
		rbs.addActionListener(this);

		// Group 1/2 status/control
		don = new RoundLED(LED.Colors.NEON);
		don.set(false);
		adr = new JTextField();
		adr.setPreferredSize(new Dimension(50, 20));
		adr.setHorizontalAlignment(SwingConstants.LEFT);
		adr.setEditable(true);
		ButtonGroup grp = new ButtonGroup();
		grp1 = new JRadioButton("Grp1", true);
		grp1.setFocusPainted(false);
		grp1.addActionListener(this);
		grp.add(grp1);
		grp2 = new JRadioButton("Grp2");
		grp2.setFocusPainted(false);
		grp2.addActionListener(this);
		grp.add(grp2);
		gin = new JTextField();
		gin.setPreferredSize(new Dimension(500, 20));
		gin.setHorizontalAlignment(SwingConstants.LEFT);
		gi_num = new JCheckBox("num");
		gi_num.addActionListener(this);
		gisn = new JButton("GISN");
		gisn.setPreferredSize(new Dimension(50, 20));
		gisn.setMargin(new Insets(2,2,2,2));
		gisn.setFocusPainted(false);
		gisn.addActionListener(this);
		gkbd = new JCheckBox("!GKBD");
		gkbd.addActionListener(this);
		gkbd.setEnabled(false);
		go = new JButton("GO");
		go.setPreferredSize(new Dimension(50, 20));
		go.setMargin(new Insets(2,2,2,2));
		go.setFocusPainted(false);
		go.addActionListener(this);
		glrn = new JCheckBox("GLRN");
		glrn.addActionListener(this);
		gi_auto = new JCheckBox("auto");
		gi_auto.addActionListener(this);

		// Block I/O status/control
		bi_auto = new JCheckBox("auto");
		bi_auto.addActionListener(this);
		bi_gisn = new JButton("GISN");
		bi_gisn.setPreferredSize(new Dimension(50, 20));
		bi_gisn.setMargin(new Insets(2,2,2,2));
		bi_gisn.setFocusPainted(false);
		bi_gisn.addActionListener(this);
		bi_giso = new JCheckBox("GISO");
		bi_giso.addActionListener(this);
		bi_giso.setEnabled(false);
		bi_supp = new JCheckBox("Supported");
		bi_supp.addActionListener(this);
		hdr = new JTextField();
		hdr.setPreferredSize(new Dimension(150, 20));
		hdr.setHorizontalAlignment(SwingConstants.LEFT);
		hdr.setEditable(false);
		hdr.setFocusable(false);
		hdr_d = new JTextField();
		hdr_d.setPreferredSize(new Dimension(120, 20));
		hdr_d.setHorizontalAlignment(SwingConstants.LEFT);
		hdr_d.setEditable(false);
		hdr_d.setFocusable(false);
		bi_in = new JTextField();
		bi_in.setPreferredSize(new Dimension(500, 20));
		bi_in.setHorizontalAlignment(SwingConstants.LEFT);
		bi_out = new JTextField();
		bi_out.setPreferredSize(new Dimension(500, 20));
		bi_out.setHorizontalAlignment(SwingConstants.LEFT);
		bi_out.setEditable(false);
		bi_out.setFocusable(false);
		sts = new JTextField();
		sts.setPreferredSize(new Dimension(50, 20));
		sts.setHorizontalAlignment(SwingConstants.LEFT);
		sts.setEditable(true);

		gb = new GridBagLayout();
		setLayout(gb);
		gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.anchor = GridBagConstraints.CENTER;
		setGap(10);

		// General I/O status (RO)
		gc.gridx = 1;
		gc.gridy = 1;
		setLabel("GIOA");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("GIOB");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("IOB");
		++gc.gridx;

		if (gc.gridx - 1 > width) width = gc.gridx - 1;
		++gc.gridy;
		gc.gridx = 1;
		gb.setConstraints(gioa, gc);
		add(gioa);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(giob, gc);
		add(giob);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(iob, gc);
		add(iob);
		++gc.gridx;
		if (gc.gridx - 1 > width) width = gc.gridx - 1;
		++gc.gridy;

		addSep(); // ----------------------------------

		gc.gridx = 1;
		gc.gridwidth = 3;
		gc.anchor = GridBagConstraints.WEST;
		setHeading("TypeWriter");
		++gc.gridy;
		gc.gridwidth = 1;
		setLabel("Output");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(ty_raw, gc);
		add(ty_raw);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(ty_clr, gc);
		add(ty_clr);

		gc.gridx = 1;
		++gc.gridy;
		int tyo_y = gc.gridy;
		++gc.gridy;
		gb.setConstraints(ty_auto, gc);
		add(ty_auto);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(tws, gc);
		add(tws);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(rbs, gc);
		add(rbs);
		++gc.gridx;
		if (gc.gridx - 1 > width) width = gc.gridx - 1;
		++gc.gridy;

		addSep(); // ----------------------------------

		gc.gridx = 1;
		gc.gridwidth = 3;
		gc.anchor = GridBagConstraints.WEST;
		setHeading("Group 1/2");
		gc.gridx += gc.gridwidth;
		gc.gridwidth = 1;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(don, gc);
		add(don);
		gc.gridx = 1;

		++gc.gridy;
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridwidth = 1;
		setLabel("Addr");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(adr, gc);
		add(adr);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(grp1, gc);
		add(grp1);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(grp2, gc);
		add(grp2);
		++gc.gridx;

		gc.gridx = 1;
		++gc.gridy;
		setLabel("Input");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(gi_num, gc);
		add(gi_num);
		++gc.gridy;
		gc.gridx = 1;
		int gin_y = gc.gridy;
		++gc.gridy;
		gc.gridwidth = 1;
		gb.setConstraints(gi_auto, gc);
		add(gi_auto);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(gkbd, gc);
		add(gkbd);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(gisn, gc);
		add(gisn);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(glrn, gc);
		add(glrn);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(go, gc);
		add(go);
		++gc.gridx;
		//setGap(100); // fudge on widest row
		//++gc.gridx;
		if (gc.gridx - 1 > width) width = gc.gridx - 1;
		++gc.gridy;

		addSep(); // ----------------------------------

		gc.gridx = 1;
		gc.gridwidth = 3;
		gc.anchor = GridBagConstraints.WEST;
		setHeading("Block I/O");
		gc.gridx += 3;
		setGap(5);
		++gc.gridx;
		gc.gridwidth = 1;
		gb.setConstraints(bi_supp, gc);
		add(bi_supp);
		++gc.gridx;
		++gc.gridy;
		gc.gridx = 1;
		gb.setConstraints(bi_auto, gc);
		add(bi_auto);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(bi_giso, gc);
		add(bi_giso);
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(bi_gisn, gc);
		add(bi_gisn);
		++gc.gridx;
		if (gc.gridx - 1 > width) width = gc.gridx - 1;
		++gc.gridy;

		gc.gridx = 1;
		setLabel("Header");
		++gc.gridx;
		gc.gridwidth = 4;
		gb.setConstraints(hdr, gc);
		add(hdr);
		gc.gridx += gc.gridwidth;
		gc.gridwidth = 1;
		setGap(5);
		++gc.gridx;
		gc.gridwidth = 3;
		gb.setConstraints(hdr_d, gc);
		add(hdr_d);
		gc.gridx += gc.gridwidth;
		gc.gridwidth = 1;
		setGap(5);
		++gc.gridx;
		setLabel("Status");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		gb.setConstraints(sts, gc);
		add(sts);
		++gc.gridx;
		setGap(100);
		++gc.gridx;
		if (gc.gridx - 1 > width) width = gc.gridx - 1;

		gc.gridx = 1;
		++gc.gridy;
		int bii_y = gc.gridy;
		setLabel("Input");
		++gc.gridy;
		int bio_y = gc.gridy;
		setLabel("Output");
		++gc.gridy;

		gc.gridx = 1;
		gc.gridwidth = 1;

		gc.gridx = width + 1;
		gc.gridwidth = 1;
		setGap(10);

		// Now go back and add full-width items...
		gc.gridx = 1;
		gc.gridwidth = width;
		gc.anchor = GridBagConstraints.WEST;
		gc.gridy = tyo_y;
		gb.setConstraints(tyo, gc);
		add(tyo);
		gc.gridy = gin_y;
		gb.setConstraints(gin, gc);
		add(gin);
		gc.gridwidth = width - 1;
		gc.gridx = 2;
		gc.gridy = bii_y;
		gb.setConstraints(bi_in, gc);
		add(bi_in);
		gc.gridy = bio_y;
		gb.setConstraints(bi_out, gc);
		add(bi_out);
		for (int y : sepy) {
			gc.gridy = y;
			setSep();
		}

		setBioSupp(false);
		reset();

		pack();
		visib = false;
		setVisible(visib);

		// bug in openjdk? does not remember current position
		setLocationByPlatform(true);

		Thread t = new Thread(this);
		t.start();
	}

	private void addSep() {
		sepy.add(gc.gridy);
		gc.gridy += 2; // must match setSep()
	}

	private void setSep() {
		gc.gridx = 1;
		gc.gridwidth = 1;
		setGap(5);
		++gc.gridy;
		gc.gridwidth = width;
		JSeparator sp = new JSeparator(SwingConstants.HORIZONTAL);
		sp.setPreferredSize(new Dimension(500, 10));
		sp.setForeground(Color.black);
		gb.setConstraints(sp, gc);
		add(sp);
		++gc.gridy;
		// gc.gridwidth = 1;
		// setGap(5);
		// ++gc.gridy;
	}

	private void setBtnCode(JButton btn, JTextField code) {
		JPanel pn = new JPanel();
		pn.setLayout(new BoxLayout(pn, BoxLayout.X_AXIS));
		pn.add(code);
		pn.add(btn);
		gb.setConstraints(pn, gc);
		add(pn);
	}

	private void setGap(int wid) {
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(wid, 10));
		//pan.setBackground(Color.red);
		gb.setConstraints(pan, gc);
		add(pan);
	}

	private void setHeading(String str) {
		JLabel lab = new JLabel(str);
		lab.setHorizontalAlignment(SwingConstants.LEFT);
		lab.setFont(tfont);
		lab.setOpaque(true);
		gb.setConstraints(lab, gc);
		add(lab);
	}

	private void setLabel(String str) {
		JLabel lab = new JLabel(str);
		lab.setHorizontalAlignment(SwingConstants.LEFT);
		lab.setOpaque(true);
		gb.setConstraints(lab, gc);
		add(lab);
	}

	private String binary4(int v) {
		String s = Integer.toBinaryString(v & 0x0f);
		while (s.length() < 4) {
			s = "0" + s;
		}
		return s;
	}

	private void clear() { // soft reset, IOB=0
		tws.setEnabled(false);
		tws.setSelected(false);
		rbs.setSelected(true); // device idle state
		bi_giso.setEnabled(false);
		bi_giso.setSelected(false);
		// bi_gisn.setEnabled(false); ???
		gkbd.setEnabled(false);
		gkbd.setSelected(false);
		// gisn.setEnabled(false); ???
	}

	// calculator pulsed TWS
	private void trigTWS() {
		// TODO: auto mode
		tws.setEnabled(true);
		tws.setSelected(true);
		rbs.setSelected(false);
	}

	// calculator GKBD change - 'false' = now ready for input
	private void trigGKBD(int _iob, boolean state) {
		gkbd.setSelected(!state);
		gkbd.setEnabled(!state); // or false?
		// gisn.setEnabled(???);
		// caller does gi_next() as needed
	}

	// calculator pulsed GISO
	private void trigGISO(int iob) {
		if (iob == 0) { // end any I/O type
			clear();
		} else if ((iob & ~1) == 2) { // block I/O
			// TODO: auto mode
			bi_giso.setEnabled(true);
			bi_giso.setSelected(true);
			// bi_gisn.setEnabled(true); ???
		} else { // Group 1/2
		}
	}

	// device (this) pulsed GISN to calculator
	private void trigGISN(int iob) {
		if (iob == 0) { // should not happen
		} else if ((iob & ~1) == 2) { // block I/O
			bi_giso.setSelected(false);
			bi_giso.setEnabled(false);
			// bi_gisn.setEnabled(false); ???
		} else { // Group 1/2
			gkbd.setSelected(false);
			// gkbd.setEnabled(false); ???
			// gisn.setEnabled(false); ???
		}
	}

	// 'c' is tilt-rotate code for IBM Selectrics
	private void typeCode(byte c) {
		if ((c & 0x0f) == 0x08) { // control characters...
			switch((c & 0x30) >> 4) {
			case 0:	// TAB
				do_tab();
				break;
			case 1:	// RETURN+INDEX
				do_crlf();
				break;
			}
			return; // none are printable
		}
		if ((c & 0x06) == 0x02) {
			switch((c & 0x39)) {
			case 0x00:
				do_space();
				break;
			case 0x01:
				do_backspace();
				break;
			case 0x08:      // set tab 
				do_settab();
				return;
			case 0x09:      // clr tab 
				do_clrtab();
				return;
			case 0x10:
				do_shift_dn();
				return;
			case 0x11:
				do_shift_up();
				return;
			case 0x18:
				do_index();
				break;
			case 0x19:
				//do_revindex();
				break;
			}
			return; // none are printable
		}
		// printable
		tyo_txt += Wang_UI.getCharConv().tiltrotateToAscii(c, ty_shift);
	}

	private int parse_key(String k) {
		int code = 0;
		if (k.length() == 0) {
			return code;
		}
		try {
			if (k.indexOf("-") > 0) {
				String[] kk = k.split("-");
				int ka = Integer.valueOf(kk[0]) & 0x0f;
				int kb = Integer.valueOf(kk[1]) & 0x0f;
				code = (ka << 4) | kb;
			} else {
				code = Integer.valueOf(k, 16) & 0xff;
			}
		} catch (Exception ee) {}
		return code;
	}

	private void do_key(String k) {
	}

	private void updateGIO(int _iob, int _gioa, int _giob) {
		curIob = _iob;
		iob.setText(String.format("%d", curIob));
		gioa.setText(String.format("%d", _gioa));
		giob.setText(String.format("%d", _giob));
	}

	private void updateGIO(int _iob, int _b) {
		updateGIO(_iob, _b >> 4, _b & 0x0f);
	}

	private void updateGIO() {
		if (dbg == null) {
			dbg = Wang_UI.getCore().getDebug();
		}
		updateGIO(dbg.getReg("iob"), dbg.getReg("gioa"), dbg.getReg("giob"));
	}

	private void tyo_clear() {
		tyo_raw = "";
		tyo_txt = "";
		tyo.setText("");
	}

	private void parseNum(Vector<Integer> inp) {
		for (byte c : gin.getText().getBytes()) {
			if (c >= '0' && c <= '9') {
				inp.add((c & 0x0f) + E0);
			} else if (c == '.') {
				inp.add(DP);
			} else if (c == '-') {
				inp.add(CHG_SIGN);
			} else if (c == 'e' || c == 'E') {
				inp.add(SET_EXP);
			} // silently drop invalid characters
		}
	}

	private void parseTR(Vector<Integer> inp) {
		boolean shift = false; // TODO: force on first char?
		// what a mess, there must be a better way...
		for (byte c : gin.getText().getBytes()) {
			byte[] b = Wang_UI.getCharConv().asciiToTiltrotate(c);
			if (b == null) continue;
			boolean shft = (b[0] == 0x13);
			if (shft != shift) {
				inp.add(b[0] & 0xff);
				shift = shft;
			}
			inp.add(b[1] & 0xff);
		}
		if (shift) { // always end in shift down state
			inp.add(0x12);
			shift = false;
		}
	}

	// setup "input" from 'gin'
	private void startInput() {
		Vector<Integer> inp = new Vector<Integer>();
		if (gi_num.isSelected()) {
			parseNum(inp);
		} else {
			parseTR(inp);
		}
		gin_dat = new int[inp.size()];
		gin_idx = 0;
		for (int c : inp) {
			gin_dat[gin_idx++] = c;
		}
		gin_idx = 0;
		gi_done = false;
	}

	// -1 on end of string
	private int giNxtChr() {
		if (gin_idx < gin_dat.length) {
			return gin_dat[gin_idx++];
		} else {
			return -1;
		}
	}

	private void gi_next() {
		if (gi_done) return;
		int c = giNxtChr();
		if (c < 0) { // EOF, as it were
			// Wang_UI.getCore().replyIO(curIob, 0); ? if button?
			doGO();
		} else {
			giChr.add(c);
		}
	}

	private void doGO() {
		if (gi_done) return;
		gi_done = true;
		bi_hdr = 0;
		giChr.add(GO);
	}

	private void bioChar(JTextField tf, int c) {
		String s = tf.getText();
		s += String.format(" %02x", c);
		tf.setText(s);
	}

	private void startBI() {
		String[] ss = bi_in.getText().trim().split("\\s");
		bin_dat = new byte[bi_size];
		bin_idx = 0;
		for (String s : ss) {
			if (bin_idx >= bi_size) break;
			try {
				bin_dat[bin_idx] = (byte)(int)Integer.valueOf(s, 16);
			} catch (Exception ee) {
				bin_dat[bin_idx] = (byte)0;
			}
			++bin_idx;
		}
		bin_idx = 0;
	}

	private void bi_next(int _iob, int c) {
		bi_rsp = 0; // assume response will be ACK
		if (_iob == 2) { // header
			switch (bi_hdr) {
			case 0: // first byte, init
				hdr.setText("");
				hdr_d.setText("");
				bi_out.setText("");
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
				if (!bi_write) {
					startBI();
				}
				bi_cnt = 0;
				hdr_d.setText(String.format("%s %d @ %06x",
					bi_write ? "write" : "read", bi_size,
					bi_addr));
				break;
			}
			bioChar(hdr, c);
			++bi_hdr;
		} else if (_iob == 3) { // data or status
			if (bi_cnt < bi_size) {
				// recv data, send ack OR
				// (recv ack) send data
				if (bi_write) {
					bioChar(bi_out, c);
				} else {
					if (bin_idx < bin_dat.length) {
						bi_rsp = bin_dat[bin_idx++] & 0xff;
					} else {
						// should never happen
					}
				}
			} else if (bi_cnt == bi_size) { // only once
				// always status sent to calculator
				bi_rsp = parse_key(sts.getText());
				bi_hdr = 0;
			}
			++bi_cnt;
		}
	}

	private void do_radiobutton(JRadioButton rb) {
	}

	private void do_button(JButton bt) {
		if (bt == bi_gisn) {
			if ((curIob & ~1) == 2) { // block I/O
				bi_next(curIob, 0); // assume ACK (unless read)
				trigGISN(curIob);
				// fire back previously set up ACK or next
				Wang_UI.getCore().replyIO(curIob, bi_rsp);
			}
		} else if (bt == gisn) {
			if ((curIob & 4) == 4) { // Group I/O
				gi_next();
			}
		} else if (bt == go) {
			doGO();
		} else if (bt == ty_clr) {
			tyo_clear();
		}
	}

	private void setBioSupp(boolean supp) {
		bi_auto.setEnabled(supp);
		bi_giso.setEnabled(false); // just in case
		bi_gisn.setEnabled(supp);
		sts.setEnabled(supp);
		bi_in.setEnabled(supp);
	}

	private void doRBS() {
		rbs.setSelected(true);
		tws.setEnabled(false);
		tws.setSelected(false);
	}

	private void do_checkbox(JCheckBox cb) {
		//cb.setSelected(!cb.isSelected()); // already done
		if (cb == rbs) {
			if (rbs.isSelected()) {
				doRBS();
			}
		} else if (cb == ty_raw) {
			if (ty_raw.isSelected()) {
				tyo.setText(tyo_raw);
			} else {
				tyo.setText(tyo_txt);
			}
		} else if (cb == bi_supp) {
			setBioSupp(bi_supp.isSelected());
		}
	}

	private void do_menuitem(JMenuItem mi) {
		int mn = mi.getMnemonic();
		if (mn == KeyEvent.VK_Z) {
			// Copy BIO Output data to Input
			bi_in.setText(bi_out.getText());
			return;
		} else if (mn == KeyEvent.VK_H) {
			if (help != null) {
				help.setVisible(true);
			}
			return;
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof JRadioButton) {
			do_radiobutton((JRadioButton)src);
			return;
		}
		if (src instanceof JButton) {
			do_button((JButton)src);
			return;
		}
		if (src instanceof JCheckBox) {
			do_checkbox((JCheckBox)src);
			return;
		}
		if (src instanceof JMenuItem) {
			do_menuitem((JMenuItem)src);
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
		visib = false;
		setVisible(false);
	}

	// Wang_OutputDevice
	public void do_cn24(byte b) { // 'b' already stripped to 6 bits
		onOff(true);
		trigTWS();
		updateGIO();
		tyo_raw += String.format(" %02x", b);
		typeCode(b); // updates tyo_txt (maybe)
		if (ty_raw.isSelected()) {
			tyo.setText(tyo_raw);
		} else {
			tyo.setText(tyo_txt);
		}
		if (ty_auto.isSelected()) {
			doRBS();
		}
	}
	public void do_cn24_direct(char c) {}
	public int getRBS() { return rbs.isSelected() ? 1 : 0; }
	// carriage-control, etc.
	public void do_space() {
		tyo_txt += ' ';
	}
	public void do_backspace() {}
	public void do_crlf() { ty_newline = true; }
	public void do_index() { ty_newline = true; } // not exactly...
	public void do_tab() {}
	public void do_settab() {}
	public void do_clrtab() {}
	public void do_shift_up() { ty_shift = true; }
	public void do_shift_dn() { ty_shift = false; }
	public void do_lock(int lk) {}
	public void do_bell() {}

	private boolean mu_init = false;
	// Wang_BlockIODevice
	public JMenuItem getMenu(int key) {
		return new JMenuItem("I/O Explorer - not connected", key);
	}

	public void menuClick(JMenuItem m) {
		if (!mu_init) {
			mu_init = true;
			m.setText("I/O Explorer");
			Wang_CN24_dev.connect(this);
			Wang_CN36_Bus.registerCN36(this);
		}
		onOff(true);
	}

	// Wang_GroupIODevice
	public boolean isBlockIO() { return bi_supp.isSelected(); }
	public boolean isDevEnabled() { return devEna; }
	public boolean start_cn36(int _iob, int c) { // iob is 0,4,5,6,7
		onOff(true);
		updateGIO(_iob, c);
		int da = parse_key(adr.getText());
		if (_iob == 0) {
			if (!bi_supp.isSelected()) {
				devEna = false;
			}
		} else {
			devEna = (((_iob & 1) == 0 && grp1.isSelected() ||
				(_iob & 1) == 1 && grp2.isSelected()) &&
 				da == c);
		}
		don.set(devEna);
		if (!devEna || _iob == 0) return devEna;
		startInput();
		if (gi_auto.isSelected()) {
			if (bi_supp.isSelected()) { // this device is for block I/O
				doGO();
			} else {
				gi_next();
			}
		}
		return devEna;
	}

	// Calculator is sending hdr/data/ACK
	// IOB is 2 or 3
	public void do_dev(int _iob, int c) {
		onOff(true);
		updateGIO(_iob, c);
		trigGISO(_iob);
		bi_next(_iob, c); // process this byte, setup response
		if (bi_auto.isSelected()) {
			// fire back previously set up ACK or next
			giChr.add(bi_rsp); // must wait for GKBD...
		}
	}
	// Calculator is ACKing our prev GISN
	// IOB is 2 or 3?
	// passed iob is suspect
	// NEVER CALLED???
	public void do_ack(int _iob) {
System.err.format("do_ack\n");
		onOff(true);
		updateGIO();
		trigGISO(curIob);
		if (bi_auto.isSelected()) {
			// fire back next
		}
	}
	public int getGLRN() { return glrn.isSelected() ? 1 : 0; }
	// must not call back into simulator (directly)
	public void setGKBD(boolean state) { // 'true' == blocked
		// TODO: only for Group 1/2?
		trigGKBD(curIob, state);
		if (!state) { // calculator is now ready
			if ((curIob & 4) == 4) { // Group 1/2
				if (gi_auto.isSelected()) {
					gi_next();
				}
			} else if (curIob == 3 && !bi_write) { // Block I/O read
				// might be ACK of status - must ignore
				if (bi_auto.isSelected() && bi_cnt <= bi_size) {
					bi_next(curIob, 0);
					giChr.add(bi_rsp);
				}
			}
		}
	}

	// Wang_Peripheral
	public void reset() { // hardware reset, a.k.a. PRIME
		clear();
		bi_hdr = 0;
		gioa.setText("");
		giob.setText("");
		iob.setText("");
		tyo_clear(); // or not?
		//ty_auto.setSelected(false);
		glrn.setSelected(false);
		devEna = false;
		don.set(devEna);
		//gi_auto.setSelected(false);
		//bi_auto.setSelected(false);
		hdr.setText("");
		hdr_d.setText("");
		bi_out.setText("");
		
	}
	public JFrame getFrame() { return this; }
	public Component getComponent() { return this; }
	public void onOff(boolean vis) {
		if (visib != vis) {
			visib = vis;
			setVisible(vis);
		}
	}
	public boolean onOff() { return visib; }
	public void setProperties(Wang_Properties p) {}

	// Only used for Group 1/2 input
	public void run() {
		while (true) {
			int c = -1;
			try {
				c = giChr.take();
				while (!gkbd.isSelected()) {
					Thread.sleep(10);
				}
				// sleep at least once after
				// Thread.sleep(10);
			} catch (Exception ee) {}
			if (c < 0) continue; // or break?
			// TODO: verify IOB = 4,5,6,7 ?
			if (c == GO) { // TODO: others as well
				glrn.setSelected(false);
			}
			trigGISN(curIob);
			Wang_UI.getCore().replyIO(curIob, c);
		}
	}
}
