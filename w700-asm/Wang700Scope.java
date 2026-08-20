// Copyright (c) 2026 Douglas Miller <durgadas311@gmail.com>

import java.io.*;
import java.util.Properties;
import java.util.Arrays;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class Wang700Scope extends JFrame
		implements ActionListener, WindowListener {

	static final int btn_w = 80;
	static final int btn_h = 30;

	Wang700_CPU cpu;

	GridBagLayout gb;
	GridBagConstraints gc;

	JTextField cycl;
	JTextField pc;
	JTextField next;
	JTextField z0;
	JTextField cc;
	JTextField sc;
	JTextField kbd;
	JTextField ov;
	JTextField err;
	JTextField d1;
	JTextField s;
	JTextField t;
	JTextField u;
	JTextField v;
	JTextField ca;
	JTextField cb;
	JTextField ka;
	JTextField kb;
	JTextField l;
	JTextField m;
	JTextField n;
	JTextField ra;
	JTextField rb;
	JTextField gioa;
	JTextField giob;
	JTextField iob;

	JRadioButton run;
	JRadioButton lrn;
	JRadioButton l_p;
	JRadioButton lst;
	JButton prm;
	JButton v_p;
	JButton spc;
	JButton r_p;
	JButton s_m;
	JButton b_s;
	JButton ins;
	JButton del;
	JButton stp;
	JButton key1;
	JButton key2;
	JButton gisn;
	JTextField key1_code;
	JTextField key2_code;
	JTextField gisn_code;
	RefreshedDisplay dspX;
	RefreshedDisplay dspY;
	Wang700RamPortal ram;

	int mode0;
	int mode1;
	long last_dsp;
	boolean visib;

	public Wang700Scope(Wang700_CPU cpu) {
		super("Wang700 CPU Scope");
		this.cpu = cpu;
		getContentPane().setName("Wang700 Scope");
		setResizable(false);
		Font font = new Font("Monospaced", Font.PLAIN, 18);

		mode0 = 0;
		mode1 = 0;

		JMenuBar mb = new JMenuBar();
		JMenu mu = new JMenu("Machine");
		JMenuItem mi;
		mi = new JMenuItem("Reset Zero", KeyEvent.VK_Z);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Reset Random", KeyEvent.VK_R);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Blank Display", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);
		setJMenuBar(mb);

		ram = new Wang700RamPortal(cpu, 4);

		dspX = new RefreshedDisplay();
		dspX.setFont(font);
		dspX.setPreferredSize(new Dimension(200, 30));
		dspX.setHorizontalAlignment(SwingConstants.RIGHT);
		dspX.setEditable(false);
		dspX.setFocusable(false);
		dspY = new RefreshedDisplay();
		dspY.setFont(font);
		dspY.setPreferredSize(new Dimension(200, 30));
		dspY.setHorizontalAlignment(SwingConstants.RIGHT);
		dspY.setEditable(false);
		dspY.setFocusable(false);
		do_blanking();

		cycl = new JTextField();
		cycl.setPreferredSize(new Dimension(80, 20));
		cycl.setHorizontalAlignment(SwingConstants.RIGHT);
		cycl.setEditable(false);
		cycl.setFocusable(false);
		// ROM addrs
		pc = new JTextField();
		pc.setPreferredSize(new Dimension(40, 20));
		pc.setHorizontalAlignment(SwingConstants.RIGHT);
		pc.setEditable(false);
		pc.setFocusable(false);
		next = new JTextField();
		next.setPreferredSize(new Dimension(40, 20));
		next.setHorizontalAlignment(SwingConstants.RIGHT);
		next.setEditable(false);
		next.setFocusable(false);
		// CPU flags
		z0 = new JTextField();
		z0.setPreferredSize(new Dimension(20, 20));
		z0.setHorizontalAlignment(SwingConstants.RIGHT);
		z0.setEditable(false);
		z0.setFocusable(false);
		cc = new JTextField();
		cc.setPreferredSize(new Dimension(20, 20));
		cc.setHorizontalAlignment(SwingConstants.RIGHT);
		cc.setEditable(false);
		cc.setFocusable(false);
		sc = new JTextField();
		sc.setPreferredSize(new Dimension(20, 20));
		sc.setHorizontalAlignment(SwingConstants.RIGHT);
		sc.setEditable(false);
		sc.setFocusable(false);
		kbd = new JTextField();
		kbd.setPreferredSize(new Dimension(20, 20));
		kbd.setHorizontalAlignment(SwingConstants.RIGHT);
		kbd.setEditable(false);
		kbd.setFocusable(false);
		ov = new JTextField();
		ov.setPreferredSize(new Dimension(20, 20));
		ov.setHorizontalAlignment(SwingConstants.RIGHT);
		ov.setEditable(false);
		ov.setFocusable(false);
		err = new JTextField();
		err.setPreferredSize(new Dimension(20, 20));
		err.setHorizontalAlignment(SwingConstants.RIGHT);
		err.setEditable(false);
		err.setFocusable(false);
		d1 = new JTextField();
		d1.setPreferredSize(new Dimension(40, 20));
		d1.setHorizontalAlignment(SwingConstants.RIGHT);
		d1.setEditable(false);
		d1.setFocusable(false);
		// GP regs
		s = new JTextField();
		s.setPreferredSize(new Dimension(30, 20));
		s.setHorizontalAlignment(SwingConstants.RIGHT);
		s.setEditable(false);
		s.setFocusable(false);
		t = new JTextField();
		t.setPreferredSize(new Dimension(30, 20));
		t.setHorizontalAlignment(SwingConstants.RIGHT);
		t.setEditable(false);
		t.setFocusable(false);
		u = new JTextField();
		u.setPreferredSize(new Dimension(30, 20));
		u.setHorizontalAlignment(SwingConstants.RIGHT);
		u.setEditable(false);
		u.setFocusable(false);
		v = new JTextField();
		v.setPreferredSize(new Dimension(30, 20));
		v.setHorizontalAlignment(SwingConstants.RIGHT);
		v.setEditable(false);
		v.setFocusable(false);
		ca = new JTextField();
		ca.setPreferredSize(new Dimension(30, 20));
		ca.setHorizontalAlignment(SwingConstants.RIGHT);
		ca.setEditable(false);
		ca.setFocusable(false);
		cb = new JTextField();
		cb.setPreferredSize(new Dimension(30, 20));
		cb.setHorizontalAlignment(SwingConstants.RIGHT);
		cb.setEditable(false);
		cb.setFocusable(false);
		ka = new JTextField();
		ka.setPreferredSize(new Dimension(30, 20));
		ka.setHorizontalAlignment(SwingConstants.RIGHT);
		ka.setEditable(false);
		ka.setFocusable(false);
		kb = new JTextField();
		kb.setPreferredSize(new Dimension(30, 20));
		kb.setHorizontalAlignment(SwingConstants.RIGHT);
		kb.setEditable(false);
		kb.setFocusable(false);
		// internal regs
		l = new JTextField();
		l.setPreferredSize(new Dimension(30, 20));
		l.setHorizontalAlignment(SwingConstants.RIGHT);
		l.setEditable(false);
		l.setFocusable(false);
		m = new JTextField();
		m.setPreferredSize(new Dimension(30, 20));
		m.setHorizontalAlignment(SwingConstants.RIGHT);
		m.setEditable(false);
		m.setFocusable(false);
		n = new JTextField();
		n.setPreferredSize(new Dimension(30, 20));
		n.setHorizontalAlignment(SwingConstants.RIGHT);
		n.setEditable(false);
		n.setFocusable(false);
		ra = new JTextField();
		ra.setPreferredSize(new Dimension(30, 20));
		ra.setHorizontalAlignment(SwingConstants.RIGHT);
		ra.setEditable(false);
		ra.setFocusable(false);
		rb = new JTextField();
		rb.setPreferredSize(new Dimension(30, 20));
		rb.setHorizontalAlignment(SwingConstants.RIGHT);
		rb.setEditable(false);
		rb.setFocusable(false);
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

		ButtonGroup grp;
		grp = new ButtonGroup();
		run = new JRadioButton("RUN", true);
		run.setFocusPainted(false);
		run.setActionCommand("run");
		run.addActionListener(this);
		grp.add(run);
		lrn = new JRadioButton("LEARN");
		lrn.setFocusPainted(false);
		lrn.setActionCommand("lrn");
		lrn.addActionListener(this);
		grp.add(lrn);
		l_p = new JRadioButton("LEARN+PRINT");
		l_p.setFocusPainted(false);
		l_p.setActionCommand("l_p");
		l_p.addActionListener(this);
		grp.add(l_p);
		lst = new JRadioButton("LIST");
		lst.setFocusPainted(false);
		lst.setActionCommand("lst");
		lst.addActionListener(this);
		grp.add(lst);
		prm = new JButton("PRIME");
		prm.setPreferredSize(new Dimension(btn_w, btn_h));
		prm.setFocusPainted(false);
		prm.addActionListener(this);
		v_p = new JButton("VER PG");
		v_p.setPreferredSize(new Dimension(btn_w, btn_h));
		v_p.setFocusPainted(false);
		v_p.addActionListener(this);
		spc = new JButton("SET PC");
		spc.setPreferredSize(new Dimension(btn_w, btn_h));
		spc.setFocusPainted(false);
		spc.addActionListener(this);
		r_p = new JButton("REC PG");
		r_p.setPreferredSize(new Dimension(btn_w, btn_h));
		r_p.setFocusPainted(false);
		r_p.addActionListener(this);
		s_m = new JButton("S.M.");
		s_m.setPreferredSize(new Dimension(btn_w, btn_h));
		s_m.setFocusPainted(false);
		s_m.addActionListener(this);
		b_s = new JButton("B.S.");
		b_s.setPreferredSize(new Dimension(btn_w, btn_h));
		b_s.setFocusPainted(false);
		b_s.addActionListener(this);
		ins = new JButton("INS");
		ins.setPreferredSize(new Dimension(btn_w, btn_h));
		ins.setFocusPainted(false);
		ins.addActionListener(this);
		del = new JButton("DEL");
		del.setPreferredSize(new Dimension(btn_w, btn_h));
		del.setFocusPainted(false);
		del.addActionListener(this);
		stp = new JButton("STEP");
		stp.setPreferredSize(new Dimension(btn_w, btn_h));
		stp.setFocusPainted(false);
		stp.addActionListener(this);
		key1 = new JButton("KEY");
		key1.setPreferredSize(new Dimension(50, btn_h));
		key1.setMargin(new Insets(2,2,2,2));
		key1.setFocusPainted(false);
		key1.addActionListener(this);
		key1_code = new JTextField();
		key1_code.setPreferredSize(new Dimension(50, btn_h));
		key1_code.setHorizontalAlignment(SwingConstants.RIGHT);
		key1_code.setEditable(true);
		key2 = new JButton("KEY");
		key2.setPreferredSize(new Dimension(50, btn_h));
		key2.setMargin(new Insets(2,2,2,2));
		key2.setFocusPainted(false);
		key2.addActionListener(this);
		key2_code = new JTextField();
		key2_code.setPreferredSize(new Dimension(50, btn_h));
		key2_code.setHorizontalAlignment(SwingConstants.RIGHT);
		key2_code.setEditable(true);
		gisn = new JButton("GISN");
		gisn.setPreferredSize(new Dimension(50, btn_h));
		gisn.setMargin(new Insets(2,2,2,2));
		gisn.setFocusPainted(false);
		gisn.addActionListener(this);
		gisn_code = new JTextField();
		gisn_code.setPreferredSize(new Dimension(50, btn_h));
		gisn_code.setHorizontalAlignment(SwingConstants.RIGHT);
		gisn_code.setEditable(true);
		gisn_code.setFocusable(true);

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
		gc.gridx = 1;
		gc.gridy = 1;
		gc.gridwidth = 3;
		setLabel("Cycles");
		gc.gridx += gc.gridwidth;
		gc.gridwidth = 1;
		setGap(5);
		++gc.gridx;
		setLabel("NEXT");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("PC");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Z0");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("CC");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("SC");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("KBD");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("OV");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("ERR");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("D1");
		++gc.gridx;
		setGap(5);
		++gc.gridx;

		int width = gc.gridx + 1;
		++gc.gridy;
		gc.gridx = 1;
		gc.gridwidth = 3;
		gb.setConstraints(cycl, gc);
		add(cycl);
		gc.gridx += gc.gridwidth;
		gc.gridwidth = 1;
		++gc.gridx;
		gb.setConstraints(next, gc);
		add(next);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(pc, gc);
		add(pc);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(z0, gc);
		add(z0);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(cc, gc);
		add(cc);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(sc, gc);
		add(sc);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(kbd, gc);
		add(kbd);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(ov, gc);
		add(ov);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(err, gc);
		add(err);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(d1, gc);
		add(d1);
		++gc.gridx;
		++gc.gridx;

		++gc.gridy;
		gc.gridx = 1;
		setGap(10);
		++gc.gridy;
		setLabel("S");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("T");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("U");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("V");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("CA");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("CB");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("KA");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("KB");
		++gc.gridx;
		setGap(5);
		++gc.gridx;

		setLabel("L");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("M");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("N");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("RA");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("RB");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
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
		setGap(5);
		++gc.gridx;
		++gc.gridy;
		if (width < gc.gridx + 1) {
			width = gc.gridx + 1;
		}

		gc.gridx = 1;
		gb.setConstraints(s, gc);
		add(s);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(t, gc);
		add(t);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(u, gc);
		add(u);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(v, gc);
		add(v);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(ca, gc);
		add(ca);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(cb, gc);
		add(cb);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(ka, gc);
		add(ka);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(kb, gc);
		add(kb);
		++gc.gridx;
		++gc.gridx;

		gb.setConstraints(l, gc);
		add(l);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(m, gc);
		add(m);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(n, gc);
		add(n);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(ra, gc);
		add(ra);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(rb, gc);
		add(rb);
		++gc.gridx;
		++gc.gridx;
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
		++gc.gridx;
		++gc.gridy;

		if (ram != null) {
			gc.gridx = 0;
			setGap(10);
			++gc.gridy;
			gc.gridx = 1;
			gc.gridwidth = width - 2;
			gb.setConstraints(ram, gc);
			add(ram);
			gc.gridwidth = 1;
			++gc.gridy;
		}
		gc.gridx = 0;
		setGap(10);
		++gc.gridy;
		JSeparator sp = new JSeparator(SwingConstants.HORIZONTAL);
		sp.setPreferredSize(new Dimension(500, 10));
		sp.setForeground(Color.black);
		gc.gridwidth = width;
		gb.setConstraints(sp, gc);
		add(sp);
		++gc.gridy;
		gc.gridwidth = 1;
		setGap(10);
		++gc.gridy;

		gc.anchor = GridBagConstraints.WEST;
		gc.gridx = 1;
		setLabel("Y");
		++gc.gridy;
		setLabel("X");
		--gc.gridy;
		gc.gridx = 2;
		gc.gridwidth = 11;
		gb.setConstraints(dspY, gc);
		add(dspY);
		++gc.gridy;
		gb.setConstraints(dspX, gc);
		add(dspX);
		gc.gridx = 1;

		++gc.gridy;
		gc.gridwidth = 1;
		setGap(10);
		++gc.gridy;
		gc.gridx = 1;
		gc.gridwidth = 6;
		gb.setConstraints(run, gc);
		add(run);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(stp, gc);
		add(stp);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(prm, gc);
		add(prm);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(s_m, gc);
		add(s_m);
		gc.gridx += gc.gridwidth;
		setBtnCode(key1, key1_code);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(lrn, gc);
		add(lrn);
		gc.gridx += gc.gridwidth;
		// gb.setConstraints(flt, gc);
		// add(flt);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(v_p, gc);
		add(v_p);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(b_s, gc);
		add(b_s);
		gc.gridx += gc.gridwidth;
		setBtnCode(key2, key2_code);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(l_p, gc);
		add(l_p);
		gc.gridx += gc.gridwidth;
		// gb.setConstraints(rad, gc);
		// add(rad);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(spc, gc);
		add(spc);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(ins, gc);
		add(ins);
		gc.gridx += gc.gridwidth;
		setBtnCode(gisn, gisn_code);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(lst, gc);
		add(lst);
		gc.gridx += gc.gridwidth;
		// gb.setConstraints(prt, gc);
		// add(prt);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(r_p, gc);
		add(r_p);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(del, gc);
		add(del);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gc.gridwidth = 1;
		gc.anchor = GridBagConstraints.CENTER;
		
		int height = gc.gridy;
		gc.gridx = 0;
		gc.gridwidth = width;
		//
		gc.gridwidth = 1;

		gc.gridx = 0;
		gc.gridy = 0;
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		gb.setConstraints(pan, gc);
		add(pan);
		gc.gridx = width;
		gc.gridy = height;
		pan = new JPanel();
		pan.setPreferredSize(new Dimension(10, 10));
		gb.setConstraints(pan, gc);
		add(pan);

		d1.setText(binary4(mode0));

		pack();
		visib = false;
		setVisible(false);

		// bug in openjdk? does not remember current position
		setLocationByPlatform(true);

		refresh();
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
		gb.setConstraints(pan, gc);
		add(pan);
	}

	private void setLabel(String str) {
		JLabel lab = new JLabel(str);
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

	private void reset_clear() {
		cpu.cycles = 0;
		cpu.next = 0;
		cpu.pc = 0;

		cpu.zo = 0;
		cpu.cc = 0;
		cpu.sc = 0;
		cpu.kbd = 0;
		cpu.ov = 0;
		cpu.err = 0;

		cpu.s = 0;
		cpu.t = 0;
		cpu.u = 0;
		cpu.v = 0;
		cpu.ca = 0;
		cpu.cb = 0;
		cpu.ka = 0;
		cpu.kb = 0;

		cpu.l = 0;
		cpu.m = 0;
		cpu.n = 0;
		cpu.ra = 0;
		cpu.rb = 0;
		cpu.gioa = 0;
		cpu.giob = 0;
		cpu.iob = 0;
		Arrays.fill(cpu._ram, (byte)0);
		do_blanking();
	}

	private void reset_random() {
		Random r = new Random(System.nanoTime());
		cpu.cycles = 0;
		cpu.next = 0;
		cpu.pc = 0;

		cpu.zo = 0;
		cpu.cc = 0;
		cpu.sc = 0;
		cpu.kbd = 0;
		cpu.ov = 0;
		cpu.err = 0;

		int i = r.nextInt();
		cpu.s = (byte)(i & 0x0f);
		cpu.t = (byte)((i >> 4) & 0x0f);
		cpu.u = (byte)((i >> 8) & 0x0f);
		cpu.v = (byte)((i >> 12) & 0x0f);
		cpu.ca = (byte)((i >> 16) & 0x0f);
		cpu.cb = (byte)((i >> 20) & 0x0f);
		cpu.ka = (byte)((i >> 24) & 0x0f);
		cpu.kb = (byte)((i >> 28) & 0x0f);

		cpu.l = 0;
		cpu.m = 0;
		cpu.n = 0;
		cpu.ra = 0;
		cpu.rb = 0;
		i = r.nextInt();
		cpu.gioa = (byte)(i & 0x0f);
		cpu.giob = (byte)((i >> 4) & 0x0f);
		cpu.new_iob(i >> 8);
		r.nextBytes(cpu._ram);
		do_blanking();
	}

	public void refresh() {
		cycl.setText(String.format("%d", cpu.cycles));
		next.setText(String.format("%03x", cpu.next));
		pc.setText(String.format("%03x", cpu.pc));

		z0.setText(String.format("%d", cpu.zo));
		cc.setText(String.format("%d", cpu.cc));
		sc.setText(String.format("%d", cpu.sc));
		kbd.setText(String.format("%d", cpu.kbd));
		ov.setText(String.format("%d", cpu.ov));
		err.setText(String.format("%d", cpu.err));

		s.setText(String.format("%d", cpu.s));
		t.setText(String.format("%d", cpu.t));
		u.setText(String.format("%d", cpu.u));
		v.setText(String.format("%d", cpu.v));
		ca.setText(String.format("%d", cpu.ca));
		cb.setText(String.format("%d", cpu.cb));
		ka.setText(String.format("%d", cpu.ka));
		kb.setText(String.format("%d", cpu.kb));

		l.setText(String.format("%d", cpu.l));
		m.setText(String.format("%d", cpu.m));
		n.setText(String.format("%d", cpu.n));
		ra.setText(String.format("%d", cpu.ra));
		rb.setText(String.format("%d", cpu.rb));
		gioa.setText(String.format("%d", cpu.gioa));
		giob.setText(String.format("%d", cpu.giob));
		iob.setText(String.format("%d", cpu.iob));
		if (ram != null) {
			ram.refresh();
		}
		// TODO: other hardware state?
		repaint();
	}

	public int getMode0(boolean clear) {
		int m = mode0;
		if (clear) {
			mode0 &= 0b0111;	// clear STEP
			d1.setText(binary4(mode0));
		}
		return m;
	}

	public int getMode1(boolean clear) {
		int m = mode1;
		// nothing to clear
		return m;
	}

	public void view(boolean vis) {
		if (visib != vis) {
			visib = vis;
			setVisible(vis);
		}
	}

	private void do_blanking() {
		dspX.do_blanking();
		dspY.do_blanking();
	}

	private String dsp_string(byte[] disp, byte n, byte d, boolean fxd) {
		int dp;
		String ret = "";
		if (n == 0 || n == 13) {
			if (d == 0) {
				d = '+';
			} else if (d == 1) {
				d = '-';
			} else {
				d = ' ';
			}
		} else {
			if (d < 10)
				d += '0';
			else
				d = ' ';
		}
		disp[n] = d;
		// (re)compute decimal point location
		if (fxd) {
			dp = 0;
		} else {
			dp = disp[15] - '0'; // invalid nums tolerated
		}
		for (int x = 0; x < 16; ++x) {
			ret += (char)disp[x];
			if (x < 13 && x == dp) ret += '.';
		}
		return ret;
	}

	// the CPU did a cycle that updates display refresh regs (RB, N)
	public void dsp_refresh() {
		// display updates must be N msecs (X cycles)
		// apart in order to be visible.
		// refresh loop pauses 272 cycles between updates.
		long last = last_dsp;
		last_dsp = cpu.cycles;
		// It is the delay *after* that matters, not before.
		// If anything, need to cancel the previous "refresh"
		// when the timing is too short.
		//if (last_dsp - last < 100) { // what's the magic value?
		boolean ok = (last_dsp - last > 100); // what's the magic value?
//System.err.format("skip %x %x %x (%d)\n", cpu.n, cpu.ra, cpu.rb, last_dsp - last);
		//	return;
		//}
		dspX.do_refresh(cpu.n, cpu.rb, (cpu.s & 2) == 0, ok);
		if ((mode0 & 0b0100) != 0) {
			dspY.do_blanking();
		} else {
			dspY.do_refresh(cpu.n, cpu.ra, (cpu.s & 1) != 0, ok);
		}
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
		if (cpu.kbl || cpu.z2) return;
		cpu.setKaKb(parse_key(k));
	}

	private void do_radiobutton(JRadioButton rb) {
		String k = rb.getActionCommand();
		mode0 &= 0b1001;
		if (k.equals("run")) {
			//
		} else if (k.equals("lrn")) {
			mode0 |= 0b0100;
		} else if (k.equals("l_p")) {
			mode0 |= 0b0110;
		} else if (k.equals("lst")) {
			mode0 |= 0b0010;
		}
		d1.setText(binary4(mode0));
	}

	private void do_button(JButton bt) {
		if (bt == stp) {
			mode0 |= 0b1000;
			d1.setText(binary4(mode0));
		} else if (bt == prm) {
			cpu.jam = 0x1000;
		} else if (bt == v_p) {
			cpu.jam = 0x1001;
		} else if (bt == spc) {
			cpu.jam = 0x1002;
		} else if (bt == r_p) {
			cpu.jam = 0x1003;
		} else if (bt == s_m) {
			cpu.jam = 0x1004;
		} else if (bt == b_s) {
			cpu.jam = 0x1005;
		} else if (bt == ins) {
			cpu.jam = 0x1006;
		} else if (bt == del) {
			cpu.jam = 0x1007;
		} else if (bt == key1) {
			do_key(key1_code.getText());
		} else if (bt == key2) {
			do_key(key2_code.getText());
		} else if (bt == gisn) {
			// TODO: prevent when not in I/O?
			cpu.setKaKb(parse_key(gisn_code.getText()));
		}
	}

	private void do_checkbox(JCheckBox cb) {
	}

	private void do_menuitem(JMenuItem mi) {
		int mn = mi.getMnemonic();
		if (mn == KeyEvent.VK_Z) {
			reset_clear();
			refresh();
			return;
		} else if (mn == KeyEvent.VK_R) {
			reset_random();
			refresh();
			return;
		} else if (mn == KeyEvent.VK_B) {
			do_blanking();
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
}
