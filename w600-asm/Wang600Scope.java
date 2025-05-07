// Copyright (c) 2025 Douglas Miller <durgadas311@gmail.com>

import java.io.*;
import java.util.Properties;
import java.util.Arrays;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class Wang600Scope extends JFrame
		implements ActionListener, WindowListener {

	Wang600_CPU cpu;

	GridBagLayout gb;
	GridBagConstraints gc;

	JTextField cycl;
	JTextField stk1;
	JTextField stk2;
	JTextField pc;
	JTextField next;
	JTextField z0;
	JTextField cc;
	JTextField sc;
	JTextField kbd;
	JTextField ov;
	JTextField err;
	JTextField d1;
	JTextField d2;
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
	JButton key;
	JTextField key_code;
	JCheckBox flt;
	JCheckBox prt;
	JCheckBox rad;
	JTextField dsp;
	byte[] disp;

	int mode0;
	int mode1;
	long last_dsp;
	boolean visib;

	public Wang600Scope(Wang600_CPU cpu) {
		super("Wang600 CPU Scope");
		this.cpu = cpu;
		getContentPane().setName("Wang600 Scope");
		Font font = new Font("Monospaced", Font.PLAIN, 18);

		mode0 = 0;
		mode1 = 0;

		disp = new byte[16];
		dsp = new JTextField();
		dsp.setFont(font);
		dsp.setPreferredSize(new Dimension(200, 30));
		dsp.setHorizontalAlignment(SwingConstants.RIGHT);
		dsp.setEditable(false);
		dsp.setFocusable(false);
		do_blanking();

		cycl = new JTextField();
		cycl.setPreferredSize(new Dimension(80, 20));
		cycl.setHorizontalAlignment(SwingConstants.RIGHT);
		cycl.setEditable(false);
		cycl.setFocusable(false);
		// ROM addrs
		stk1 = new JTextField();
		stk1.setPreferredSize(new Dimension(40, 20));
		stk1.setHorizontalAlignment(SwingConstants.RIGHT);
		stk1.setEditable(false);
		stk1.setFocusable(false);
		stk2 = new JTextField();
		stk2.setPreferredSize(new Dimension(40, 20));
		stk2.setHorizontalAlignment(SwingConstants.RIGHT);
		stk2.setEditable(false);
		stk2.setFocusable(false);
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
		d2 = new JTextField();
		d2.setPreferredSize(new Dimension(40, 20));
		d2.setHorizontalAlignment(SwingConstants.RIGHT);
		d2.setEditable(false);
		d2.setFocusable(false);
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
		prm.setFocusPainted(false);
		prm.addActionListener(this);
		v_p = new JButton("VER PG");
		v_p.setFocusPainted(false);
		v_p.addActionListener(this);
		spc = new JButton("SET PC");
		spc.setFocusPainted(false);
		spc.addActionListener(this);
		r_p = new JButton("REC PG");
		r_p.setFocusPainted(false);
		r_p.addActionListener(this);
		s_m = new JButton("S.M.");
		s_m.setFocusPainted(false);
		s_m.addActionListener(this);
		b_s = new JButton("B.S.");
		b_s.setFocusPainted(false);
		b_s.addActionListener(this);
		ins = new JButton("INS");
		ins.setFocusPainted(false);
		ins.addActionListener(this);
		del = new JButton("DEL");
		del.setFocusPainted(false);
		del.addActionListener(this);
		stp = new JButton("STEP");
		stp.setFocusPainted(false);
		stp.addActionListener(this);
		flt = new JCheckBox("Fl/Sc");
		flt.setFocusPainted(false);
		flt.addActionListener(this);
		prt = new JCheckBox("Prt On");
		prt.setFocusPainted(false);
		prt.addActionListener(this);
		mode1 |= 0b0010; // printer is OFF
		rad = new JCheckBox("Rad/Deg");
		rad.setFocusPainted(false);
		rad.addActionListener(this);
		key = new JButton("KEY");
		key.setFocusPainted(false);
		key.addActionListener(this);
		key_code = new JTextField();
		key_code.setPreferredSize(new Dimension(60, 20));
		key_code.setHorizontalAlignment(SwingConstants.RIGHT);
		key_code.setEditable(true);
		key_code.setFocusable(true);

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
		setLabel("Cycles");
		++gc.gridx;
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
		setLabel("STK1");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("STK2");
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
		setLabel("D2");
		++gc.gridx;
		setGap(5);
		++gc.gridx;

		int width = gc.gridx + 1;
		++gc.gridy;
		gc.gridx = 1;
		gb.setConstraints(cycl, gc);
		add(cycl);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(next, gc);
		add(next);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(pc, gc);
		add(pc);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(stk1, gc);
		add(stk1);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(stk2, gc);
		add(stk2);
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
		gb.setConstraints(d2, gc);
		add(d2);
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
		gc.gridwidth = 12;
		gb.setConstraints(dsp, gc);
		add(dsp);
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
		gb.setConstraints(key, gc);
		add(key);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(lrn, gc);
		add(lrn);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(flt, gc);
		add(flt);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(v_p, gc);
		add(v_p);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(b_s, gc);
		add(b_s);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(key_code, gc);
		add(key_code);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(l_p, gc);
		add(l_p);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(rad, gc);
		add(rad);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(spc, gc);
		add(spc);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(ins, gc);
		add(ins);
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		gc.gridx -= gc.gridwidth;
		++gc.gridy;
		gb.setConstraints(lst, gc);
		add(lst);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(prt, gc);
		add(prt);
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
		d2.setText(binary4(mode1 ^ 1)); // quirk in Wang600_CPU

		pack();
		visib = false;
		setVisible(false);

		// bug in openjdk? does not remember current position
		setLocationByPlatform(true);

		refresh();
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

	public void refresh() {
		cycl.setText(String.format("%d", cpu.cycles));
		next.setText(String.format("%03x", cpu.next));
		pc.setText(String.format("%03x", cpu.pc));
		stk1.setText(String.format("%03x", cpu.stk1));
		stk2.setText(String.format("%03x", cpu.stk2));

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
		rb.setText(String.format("%d", cpu.rb));
		gioa.setText(String.format("%d", cpu.gioa));
		giob.setText(String.format("%d", cpu.giob));
		iob.setText(String.format("%d", cpu.iob));
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
		Arrays.fill(disp, (byte)' ');
		dsp.setText(new String(disp));
	}

	// the CPU did a cycle that updates display refresh regs (RB, N)
	public void dsp_refresh() {
		// display updates must be N msecs (X cycles)
		// apart in order to be visible.
		// refresh loop pauses 272 cycles between updates.
		long last = last_dsp;
		last_dsp = cpu.cycles;
		if (last_dsp - last < 100) { // what's the magic value?
			return;
		}
		byte n = cpu.n;
		byte d = cpu.rb;
		if (n == 0 || n == 13) {
			disp[n] = (byte)((d & 1) != 0 ? '-' : '+');
		} else {
			if (d == 10) d = '.';
			else if (d == 15) d = ' ';
			else if (d > 10)
				d += ('A' - 10);
			else
				d += '0';
			disp[n] = d;
		}
		dsp.setText(new String(disp));
	}

	private int parse_key() {
		int code = 0;
		String k = key_code.getText();
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

	private void do_key() {
		cpu.setKaKb(parse_key());
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JRadioButton) {
			JRadioButton rb = (JRadioButton)e.getSource();
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
			return;
		}
		if (e.getSource() instanceof JButton) {
			JButton bt = (JButton)e.getSource();
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
			} else if (bt == key) {
				do_key();
			}
			return;
		}
		if (e.getSource() instanceof JCheckBox) {
			JCheckBox cb = (JCheckBox)e.getSource();
			if (cb == flt) {
				if (flt.isSelected()) {
					mode0 |= 0b0001;
				} else {
					mode0 &= ~0b0001;
				}
				d1.setText(binary4(mode0));
			} else if (cb == prt) {
				if (prt.isSelected()) {
					mode1 &= ~0b0010;
				} else {
					mode1 |= 0b0010;
				}
				d2.setText(binary4(mode1 ^ 1));
			} else if (cb == rad) {
				if (rad.isSelected()) {
					mode1 &= ~0b0001;
				} else {
					mode1 |= 0b0001;
				}
				d2.setText(binary4(mode1 ^ 1));
			}
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
