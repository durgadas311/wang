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

public class Wang600Assembler extends JFrame
		implements Wang_FrontPanel,
			KeyListener, MouseListener, ActionListener,
			WindowListener, FocusListener, Runnable {
	static final String[] txtx = { "txt" };
	static final String[] txtd = { "Text" };
	static final String[] wucx = { "wuc" };
	static final String[] wucd = { "Wang uCode" };

	JTextArea text;
	FontMetrics fm;
	int fh;
	int fw;
	int ww;
	int wh;
	JScrollPane scroll;
	File _last = null;
	GenericHelp _help;
	Wang600_CPU cpu;
	Wang600Scope scope;
	private java.util.concurrent.LinkedBlockingDeque<Long> fifo;
	Properties props;

	JComboBox zo;
	JComboBox ai;	// AI,AC
	JComboBox bi;
	JComboBox aop;	// AOP,BC
	JComboBox mop;
	JTextField kk;
	JComboBox st;
	JComboBox sub;
	JTextField jad;	// incl constant JH or JL
	JComboBox jh;
	JComboBox jl;

	String[] zo_cb = new String[] {	// special-case [0] - add ST=1111
		"", "T", "U", "V", "KA", "KB", "CA", "-", "S" };
	String[] ai_cb = new String[] {
		"0", "S", "T", "U", "V", "KA", "KB", "CA", "CB" };
	String[] bi_cb = new String[] {
		"0", "KK", "D1", "D2", "KA", "KB", "CA", "CB" };
	String[] aop_cb = new String[] { // idx hi bit is BC
		"A+B",		// 0,000
		"A+B+1",	// 0,001
		"SC;A+B",	// 0,010
		"SC;A+B+SC",	// 0,011
		"SC;A+B+1",	// 0,100
		"A&B",		// 0,101
		"A|B",		// 0,110
		"0",		// 0,111
		"A-B",		// 1,000
		"A-B-1",	// 1,001
		"SC;A-B",	// 1,010
		"SC;A-B-SC",	// 1,011
		"SC;A-B-1",	// 1,100
		"A&B",		// 1,101
		"A^B",		// 1,110
		"0"		// 1,111
	};
	String[] mop_cb = new String[] {
		"-",	// nothing
		"wrTUV",	// ram(T,U,V) = CA
		"wrFKV",	// ram(15,KK,V) = CA
		"wrFFK",	// ram(15,15,KK) = CA
		"rdTUV",	// CA = ram(T,U,V)/rom
		"rdFKV",	// CA = ram(15,KK,V)/rom
		"rdFFK",	// CA = ram(15,15,KK)/rom
		"PH<<KB0",	// PH <<= 1; PH |= KB<0>
		"PPF=1",	// print/advance
		"undef",
		"rdTAPE",	// KB<0> = MHG/MHO
		"wrTAPE",	// WDT = KB<0>
		"rdSTAT",	// read printer, cn24 status
		"TMR=1",	// tape motor ON, BI<0>
		"TMR=0",	// tape motor OFF, BI<0>
		"GIO"		// GIOA/B and IOB=KK
	};
	String[] st_cb = new String[] {
		"-",	// nothing
		"S0=1",	// S<0>=1
		"S1=1",	// S<1>=1
		"S2=1",	// S<2>=1
		"S3=1",	// S<3>=1
		"S0=0",	// S<0>=0
		"S1=0",	// S<1>=0
		"S2=0",	// S<2>=0
		"S3=0",	// S<3>=0
		"RESET",	// various h/w resets
		"S0=NZ",	// S<0> = not Z
		"S1=ZR",	// S<1> = Z (zero out ALU)
		"OV=1",		// overflow lamp ON
		"S=0",		// clear S reg
		"ERR=1",	// error lamp ON
		"zo:S"
	};
	// TODO: add "return" here and force jad,jh,jl accordingly?
	String[] sub_cb = new String[] { "jump", "call" };
	String[] jh_cb = new String[] {
		"0", "1", "S1", "S3", "OV", "CC", "KBD", "1" };
	String[] jl_cb = new String[] {
		"0", "1", "S0", "S2", "ZR", "-", "SC", "return" };

	JButton store;
	JButton exec;
	JButton run;
	JTextField cyc;
	JMenuItem save;
	long run_rate;
	boolean running;

	GridBagLayout gb;
	GridBagConstraints gc;

	byte[] rom;	// microcode ROM
	byte[] ram;	// program RAM
	byte[] xrom;	// program ext ROM
	Wang600_Ucode uu;
	int curr;	// current microcode ROM address
	int carr;	// where we want the caret to stay
	boolean dirty;	// whether display word differs from ROM image
	boolean saved;	// whether ROM image has been saved to a file.
	boolean update;

	class BlockCaret extends DefaultCaret {
		static final Color shadow = new Color(50, 50, 50, 100);
		static final Color rose = new Color(150, 50, 50, 100);
		public BlockCaret() {}

		public void paint(Graphics g) {
			JTextComponent comp = getComponent();
			Rectangle2D r = null;
			try {
				r = comp.modelToView2D(getDot());
			} catch(Exception ee) { }
			if (r == null) return;
			int x = (int)r.getX();
			int y = (int)r.getY();
			if (dirty) {
				g.setColor(rose);
			} else {
				g.setColor(shadow);
			}
			g.fillRect(x, y, ww * fw - 1, fh);
		}

		@Override
		public void setDot(int dot, Position.Bias dotBias) {
			// prevent cursor keys from changing caret
			if (dot != carr) return;
			super.setDot(dot, dotBias);
		}

		// prevent mouse from changing caret
		@Override
		protected void positionCaret(MouseEvent e) { }
	};

	public Wang600Assembler(String[] args) {
		super("Wang600 Microcode Assembler");
		props = new Properties();
		// TODO: possibly load properties from file
		java.net.URL url;
		url = this.getClass().getResource("docs/help.html");
		if (url != null) {
			_help = new GenericHelp("Wang600 ucode asm Help", url);
		}

		getContentPane().setName("Wang600 UCode Asm");
		//getContentPane().setBackground(new Color(100, 100, 100));
		_last = new File(System.getProperty("user.dir"));

		fifo = new java.util.concurrent.LinkedBlockingDeque<Long>();
		ww = 120;
		wh = 24;
		text = new JTextArea(wh, ww);
		text.setEditable(false); // this prevents caret... grrr.
		text.setBackground(Color.white);
		Font font = new Font("Monospaced", Font.PLAIN, 12);
		setupFont(font);
		text.setCaret(new BlockCaret());
		text.addKeyListener(this);
		text.addMouseListener(this);
		scroll = new JScrollPane(text);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		JMenuBar mb = new JMenuBar();
		JMenu mu = new JMenu("File");
		JMenu main = mu;
		JMenuItem mi;
		mi = new JMenuItem("New", KeyEvent.VK_N);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Load ROM", KeyEvent.VK_L);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save ROM", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		save = mi;
		mi = new JMenuItem("Save Text", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Show Scope", KeyEvent.VK_W);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);

		mu = new JMenu("Help");
		mi = new JMenuItem("Show Help", KeyEvent.VK_H);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);

		setJMenuBar(mb);

		store = new JButton("store");
		store.addActionListener(this);
		store.setActionCommand("store");
		exec = new JButton("execute");
		exec.addActionListener(this);
		exec.setActionCommand("exec");
		run = new JButton("run");
		run.addActionListener(this);
		run.setActionCommand("run");
		cyc = new JTextField();
		cyc.setPreferredSize(new Dimension(80, 20));
		cyc.setHorizontalAlignment(SwingConstants.RIGHT);
		cyc.addFocusListener(this); // needed?
		cyc.addActionListener(this); // needed?

		zo = new JComboBox<String>(zo_cb);
		ai = new JComboBox<String>(ai_cb);
		bi = new JComboBox<String>(bi_cb);
		aop = new JComboBox<String>(aop_cb);
		mop = new JComboBox<String>(mop_cb);
		kk = new JTextField();
		kk.setPreferredSize(new Dimension(20, 20));
		kk.setHorizontalAlignment(SwingConstants.RIGHT);
		st = new JComboBox<String>(st_cb);
		sub = new JComboBox<String>(sub_cb);
		jad = new JTextField();
		jad.setHorizontalAlignment(SwingConstants.RIGHT);
		jad.setPreferredSize(new Dimension(40, 20));
		jh = new JComboBox<String>(jh_cb);
		jl = new JComboBox<String>(jl_cb);

		kk.addFocusListener(this);
		kk.addActionListener(this);
		jad.addFocusListener(this);
		jad.addActionListener(this);

		zo.addActionListener(this);
		ai.addActionListener(this);
		bi.addActionListener(this);
		aop.addActionListener(this);
		mop.addActionListener(this);
		st.addActionListener(this);
		sub.addActionListener(this);
		jh.addActionListener(this);
		jl.addActionListener(this);

		zo.setActionCommand("zo");
		ai.setActionCommand("ai");
		bi.setActionCommand("bi");
		aop.setActionCommand("aop");
		mop.setActionCommand("mop");
		st.setActionCommand("st");
		sub.setActionCommand("sub");
		jh.setActionCommand("jh");
		jl.setActionCommand("jl");

		//setLayout(new BorderLayout()); // allow resizing
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

		gc.gridx = 1;
		gc.gridy = 1;
		setLabel("Zo");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Ai");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Bi");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Aop");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Mop");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("KK");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("ST");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("SUB");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("JAD");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("JH");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("JL");
		++gc.gridx;
		int width = gc.gridx + 1;
		++gc.gridy;

		gc.gridx = 1;
		gb.setConstraints(zo, gc);
		add(zo);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(ai, gc);
		add(ai);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(bi, gc);
		add(bi);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(aop, gc);
		add(aop);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(mop, gc);
		add(mop);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(kk, gc);
		add(kk);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(st, gc);
		add(st);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(sub, gc);
		add(sub);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(jad, gc);
		add(jad);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(jh, gc);
		add(jh);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(jl, gc);
		add(jl);
		++gc.gridx;
		++gc.gridy;
		gc.gridx = 1;
		setGap(10);
		++gc.gridy;
		gc.gridwidth = 4;
		gb.setConstraints(store, gc);
		add(store);
		gc.gridx += gc.gridwidth;
		gb.setConstraints(exec, gc);
		add(exec);
		gc.gridx += gc.gridwidth;

		gc.gridwidth = 6;
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
		pan.add(run);
		pan.add(cyc);
		pan.add(new JLabel("cycles"));
		gb.setConstraints(pan, gc);
		add(pan);

		gc.gridwidth = 1;
		++gc.gridy;
		gc.gridx = 1;
		setGap(10);

		++gc.gridy;
		gc.gridx = 0;
		gc.gridwidth = width;
		gb.setConstraints(scroll, gc);
		add(scroll);
		gc.gridwidth = 1;

		// bug in openjdk? does not remember current position
		setLocationByPlatform(true);

		rom = new byte[8 * 2048];
		ram = new byte[2048];
		xrom = new byte[2048];
		cpu = new Wang600_CPU(rom, ram, xrom, this);
		scope = new Wang600Scope(cpu);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		pack();
		setVisible(true);

		File f = null;
		for (String arg : args) {
			if (arg.indexOf("=") >= 0) {
				String[] ss = arg.split("=", 2);
				props.setProperty(ss[0], ss[1]);
			} else if (f == null) {
				f = new File(args[0]);
				if (!f.exists()) {
					f = null;
				}
			}
		}
		run_rate = 10;
		String s = props.getProperty("run_rate");
		if (s != null) {
			run_rate = Integer.valueOf(s);
		}
		if (f != null) {
			loadRom(f);
		} else {
			newRom();
		}
		setLoc(0);
		text.requestFocus();

		Thread t = new Thread(this);
		t.start();
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

	private void setupFont(Font f) {
		text.setFont(f);
		fm = text.getFontMetrics(f);
		//fa = fm.getAscent();
		fw = fm.charWidth('M');
		fh = fm.getHeight();
	}

	private void setDirty(boolean drt) {
		dirty = drt;
		store.setEnabled(dirty);
	}

	private void setSaved(boolean svd) {
		saved = svd;
		save.setEnabled(!saved);
		if (saved) {
			setTitle("Wang600 Microcode Assembler");
		} else {
			setTitle("* Wang600 Microcode Assembler");
		}
	}

	private void newRom() {
		Arrays.fill(rom, (byte)0);
		setDirty(false);
		setSaved(true);
	}

	// we know file exists...
	private void loadRom(File f) {
		long len = 0;
		try { len = f.length(); } catch (Exception ee) {}
		if (len != (8 * 2048)) {
			int ans = PopupFactory.confirm(this, "Load ROM",
						"File is Wrong Size - Load anyway?");
			if (ans != JOptionPane.YES_OPTION) {
				return;
			}
		}
		try {
			InputStream is = new FileInputStream(f);
			is.read(rom);
			is.close();
			// TODO: locate "end" of ROM and display up to that
		} catch (Exception ee) {}
		setDirty(false);
		setSaved(true);
	}

	private synchronized void setRunning(boolean r) {
		// TODO: only if changed?
		running = r;
		if (r) {
			// TODO: disable microcode modifiers?
			run.setText("stop");
			run.setActionCommand("stop");
		} else {
			run.setText("run");
			run.setActionCommand("run");
			text.requestFocus();
		}
	}

	private synchronized boolean getRunning() {
		return running;
	}

	private void singleStep() {
		cpu.pc = curr;
		int x = cpu.instr_exec();
		scope.refresh();
		scope.view(true); // TODO: if not running?
		setLoc(cpu.pc);
	}

	private void padDisas(int adr) {
		String dis;
		int nl = text.getLineCount() - 1;
		while (nl < adr) {
			dis = String.format("%03x: %s\n", nl, cpu.disas(nl, false));
			text.append(dis);
			++nl;
		}
	}

	private void setDisas(Wang600_Ucode uu) {
		padDisas(curr);
		String dis = String.format("%03x: %s\n", curr, cpu.disas(uu, false));

		try {
			carr = text.getLineStartOffset(curr); // each adr is a line
			int end = text.getLineEndOffset(curr);
			text.replaceRange(dis, carr, end);
			text.setCaretPosition(carr);
		} catch (Exception ee) {
			System.err.println(ee.getMessage());
		}
		// TODO: any other actions to skip?
		if (!getRunning()) {
			text.requestFocus();
		}
	}

	private void updDisas(Wang600_Ucode uu) {
		setDirty(true);
		setSaved(false);
		setDisas(uu);
	}

	private void setLoc(Wang600_Ucode uu) {
		int ja = uu.jad << 2;
		if (uu.jh == 1) {
			ja |= 2;
		}
		if (uu.jl == 1) {
			ja |= 1;
		}
		update = true;
		if (uu.zo == 0 && uu.st == 15) {
			zo.setSelectedIndex(8);
		} else {
			zo.setSelectedIndex(uu.zo);
		}
		int a;
		if (uu.ac == 0) {
			a = 0;
		} else {
			a = uu.ai + 1;
		}
		ai.setSelectedIndex(a);
		bi.setSelectedIndex(uu.bi);
		aop.setSelectedIndex(uu.aop);
		mop.setSelectedIndex(uu.mop);
		kk.setText(String.format("%d", uu.kk));
		st.setSelectedIndex(uu.st);
		sub.setSelectedIndex(uu.sub);
		jad.setText(String.format("%03x", ja));
		jh.setSelectedIndex(uu.jh);
		jl.setSelectedIndex(uu.jl);
		update = false;
		setDirty(false);
		setDisas(uu);
	}

	private void setLoc(int adr) {
		curr = adr;
		uu = cpu.fetchUcode(adr);
		setLoc(uu);
	}

	private File pickFile(String purpose, String[] sufx, String[] sufd) {
		File file = null;
		SuffFileChooser ch = new SuffFileChooser(purpose, sufx, sufd, _last, null);
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
		}
		return file;
	}

	private boolean saveCheck(String purpose) {
		if (dirty || !saved) {
			int ans = PopupFactory.confirm((Component)this, purpose,
						"Unsaved ROM - Discard?");
			if (ans != JOptionPane.YES_OPTION) {
				return false;
			}
		}
		return true;
	}

	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
		int c = (int)e.getKeyChar();
		int k = e.getKeyCode();
		int m = e.getModifiersEx();

		if (k == KeyEvent.VK_DOWN) {
			if (dirty) {
				storeUcode(uu);
			}
			if (curr < 0x7ff) {
				setLoc(curr + 1);
			}
			return;
		} else if (k == KeyEvent.VK_UP) {
			if (dirty) {
				storeUcode(uu);
			}
			if (curr > 0x000) {
				setLoc(curr - 1);
			}
			return;
		}

		//System.err.format("keyPressed %02x %04x %04x\n", c, k, m);
	}
	public void keyReleased(KeyEvent e) {}

	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }
	public void mouseDragged(MouseEvent e) { }
	public void mouseMoved(MouseEvent e) { }

	private void updateKK() {
		uu.kk = (byte)(Integer.valueOf(kk.getText()) & 0x0f);
		// normalize the value
		update = true;
		kk.setText(String.format("%d", uu.kk));
		update = false;
		updDisas(uu);
	}

	private void updateJAD() {
		int ja = Integer.valueOf(jad.getText(), 16) & 0x07ff;
		int j0 = jl.getSelectedIndex();
		int j1 = jh.getSelectedIndex();
		// normalize the value
		update = true;
		jad.setText(String.format("%03x", ja));
		update = false;
		uu.jad = ja >> 2;
		update = true;
		if ((ja & 2) != 0) {
			jh.setSelectedIndex(1);
			uu.jh = (byte)1;
		} else if (j1 == 1) {
			jh.setSelectedIndex(0);
			uu.jh = (byte)0;
		}
		if ((ja & 1) != 0) {
			jl.setSelectedIndex(1);
			uu.jl = (byte)1;
		} else if (j0 == 1) {
			jl.setSelectedIndex(0);
			uu.jl = (byte)0;
		}
		update = false;
		updDisas(uu);
	}

	private void updateJH() {
		int ja = uu.jad << 2;
		int j0 = uu.jl;
		int j1 = jh.getSelectedIndex();
		if (j0 == 1) {
			ja |= 1;
		}
		if (j1 == 1) {
			ja |= 2;
		}
		uu.jh = (byte)j1;
		updDisas(uu);
	}

	private void updateJL() {
		// TODO: force other fields to 0 for RETURN?
		int ja = uu.jad << 2;
		int j0 = jh.getSelectedIndex();
		int j1 = uu.jh;
		if (j0 == 1) {
			ja |= 1;
		}
		if (j1 == 1) {
			ja |= 2;
		}
		uu.jl = (byte)j0;
		updDisas(uu);
	}

	private void updateZO() {
		int z = zo.getSelectedIndex();
		int s = st.getSelectedIndex();
		if (z == 8 && s != 15) { // "S" requires ST==15...
			update = true;
			uu.zo = 0;
			uu.st = 15;
			st.setSelectedIndex(15);
			update = false;
			updDisas(uu);
			return;
		}
		if (z == 0 && s == 15) { // "" requires ST!=15...
			update = true;
			uu.st = 0;
			st.setSelectedIndex(0);
			update = false;
		}
		uu.zo = (byte)z;
		updDisas(uu);
	}

	private void updateST() {
		int s = st.getSelectedIndex();
		int z = zo.getSelectedIndex();
		if (s != 15 && z == 8) {
			update = true;
			uu.zo = 0; // should already be this
			zo.setSelectedIndex(0);
			update = false;
		}
		if (s == 15 && z == 0) {
			update = true;
			uu.zo = 0; // should already be this
			zo.setSelectedIndex(8);
			update = false;
		}
		uu.st = (byte)s;
		updDisas(uu);
	}

	private void updateAI() {
		int a = ai.getSelectedIndex();
		if (a > 0) {
			a = (a - 1) | 8;
		}
		uu.ac = (byte)((a >> 3) & 1);
		uu.ai = (byte)(a & 0x07);
		updDisas(uu);
	}

	private void updateBI() {
		int b = bi.getSelectedIndex();
		uu.bi = (byte)(b & 0x07);
		updDisas(uu);
	}

	private void updateAOP() {
		int op = aop.getSelectedIndex();
		uu.bc = (byte)((op >> 3) & 1);
		uu.aop = (byte)(op & 0x07);
		updDisas(uu);
	}

	private void updateMOP() {
		int op = mop.getSelectedIndex();
		// TODO: any special handling?
		uu.mop = (byte)op;
		updDisas(uu);
	}

	private void updateSUB() {
		int s = sub.getSelectedIndex();
		// TODO: any special handling? RETURN?
		uu.sub = (byte)s;
		updDisas(uu);
	}

	// store at curr...
	private void storeUcode(Wang600_Ucode uu) {
		int idx = curr * 8;
		System.arraycopy(uu.asBytes(), 0, rom, idx, 8);
		setDirty(false);
		text.repaint();
	}

	private void startOne() {
		if (dirty) {
			storeUcode(uu);
		}
		long c = 1;
		fifo.add(c);
	}

	private void startRun() {
		long limit = 0;
		if (dirty) {
			storeUcode(uu);
		}
		try {
			String c = cyc.getText();
			if (c.length() > 0) {
				limit = Long.valueOf(c);
			}
		} catch (Exception ee) {
			return;
		}
		if (limit <= 0) { // means infinity...
			limit = 1000000; // practical infinity
		}
		setRunning(true);
		fifo.add(limit);
	}

	private void stopRun() {
		long c = -1;
		fifo.add(c);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton bt = (JButton)e.getSource();
			if (bt == store) {
				storeUcode(uu);
			} else if (bt == exec) {
				startOne();
			} else if (bt == run) {
				String act = run.getActionCommand();
				if (act.equals("run")) {
					startRun();
				} else {
					stopRun();
				}
			} else {
				//
			}
			return;
		}
		if (e.getSource() instanceof JComboBox) {
			if (update) return;
			JComboBox cb = (JComboBox)e.getSource();
			if (cb == zo) updateZO();
			else if (cb == ai) updateAI();
			else if (cb == bi) updateBI();
			else if (cb == aop) updateAOP();
			else if (cb == mop) updateMOP();
			else if (cb == st) updateST();
			else if (cb == sub) updateSUB();
			else if (cb == jh) updateJH();
			else if (cb == jl) updateJL();
			return;
		}
		if (e.getSource() instanceof JTextField) {
			if (update) return;
			JTextField tf = (JTextField)e.getSource();
			if (tf == kk) {
				updateKK();
			} else if (tf == jad) {
				updateJAD();
			}
			return;
		}
		if (!(e.getSource() instanceof JMenuItem)) {
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_N) {
			if (!saveCheck("New ROM")) {
				return;
			}
			newRom();
			text.setText("");
			setLoc(0);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_L) {
			if (!saveCheck("Load ROM")) {
				return;
			}
			File lod = pickFile("Load ROM", wucx, wucd);
			if (lod != null) {
				loadRom(lod);
				text.setText("");
				setLoc(0);
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_S) {
			if (dirty) {
				storeUcode(uu);
			}
			File sav = pickFile("Save ROM", wucx, wucd);
			if (sav != null) {
				try {
					FileOutputStream fo = new FileOutputStream(sav);
					fo.write(rom);
					fo.close();
					setSaved(true);
					_last = sav;
					// TODO: tear off?
				} catch (Exception ee) {
					// ...
				}
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_T) {
			File sav = pickFile("Save Text", txtx, txtd);
			if (sav != null) {
				try {
					FileOutputStream fo = new FileOutputStream(sav);
					fo.write(text.getText().getBytes());
					fo.close();
					_last = sav;
					// TODO: tear off?
				} catch (Exception ee) {
					// ...
				}
			}
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_W) {
			scope.view(true);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_H) {
			if (_help != null) {
				_help.setVisible(true);
			}
			return;
		}
	}

	public void focusGained(FocusEvent e) {}
	public void focusLost(FocusEvent e) {
		if (e.getSource() instanceof JTextField) {
			JTextField tf = (JTextField)e.getSource();
			if (tf == kk) {
				updateKK();
			} else if (tf == jad) {
				updateJAD();
			}
		}
	}

	public void breakpoint(int pc) {}
	public void debug_check() {}
	public void tape_record(byte to_byte) {}
	public int tape_play() { return 0; }
	public void tape_on(int wr) {}
	public void tape_off(int wr) {}
	public void dev_out(byte iob, byte c) {}
	public int getMode0(boolean clear) { return scope.getMode0(clear); }
	public int getMode1(boolean clear) { return scope.getMode1(clear); }
	public void do_printer(int x, byte pr_drum) {}
	public void do_line() {}
	public void setOv(byte on) {}
	public void setErr(byte on) {}
	public void display_check(boolean mr) {
		if (mr) scope.dsp_refresh();
	}
	public int getMemSize() { return ram.length; }
	public int getMemMask() { return ram.length * 2 - 1; }

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }
	public void windowClosing(WindowEvent e) {
		if (!saveCheck("Exit")) {
			return;
		}
		System.exit(0);
	}

	public void run() {
		long limit;
		while (true) {
			try {
				limit = fifo.take();
			} catch (Exception ee) {
				continue; // TODO: prevent runaway?
			}
			while (limit > 0 && fifo.size() == 0) {
				singleStep();
				if (run_rate > 0) try {
					Thread.sleep(run_rate);
				} catch (Exception ee) {}
				--limit;
			}
			setRunning(false);
		}
		//System.err.format("Run thread died!\n");
	}
}
