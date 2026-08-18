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
import javax.swing.event.*;
import java.awt.datatransfer.StringSelection;

public class Wang700Assembler extends JFrame
		implements Wang_FrontPanel,
			KeyListener, MouseListener, ActionListener, MouseWheelListener,
			WindowListener, FocusListener, CaretListener, Runnable {
	static final String[] txtx = { "txt" };
	static final String[] txtd = { "Text" };
	static final String[] wucx = { "wuc" };
	static final String[] wucd = { "Wang uCode" };
	static final String[] cx = { "c" };
	static final String[] cd = { "C uCode Macros" };

	static final int OPTION_CANCEL = 0;
	static final int OPTION_YES = 1;

	JTextArea text;
	FontMetrics fm;
	int fh;
	int fw;
	int ww;
	int wh;
	JScrollPane scroll;
	File _last = null;
	GenericHelp _help;
	Wang700_CPU cpu;
	Wang700Scope scope;
	private java.util.concurrent.LinkedBlockingDeque<Long> fifo;
	Properties props;
	Object[] dis_btns;

	JComboBox zo;
	JComboBox ai;	// AI,AC
	JComboBox bi;
	JComboBox bc;
	JComboBox bd;
	JComboBox aop;
	JComboBox mop;
	JTextField kk;
	JComboBox st;
	JTextField jad;	// incl constant JH or JL
	JComboBox jh;
	JComboBox jl;

	String[] zo_cb = new String[] {
		"S", "T", "U", "V", "KA", "KB", "CA", "CB" };
	String[] ai_cb = new String[] { // [0] is AC=0
		"0", "S", "T", "U", "V", "KA", "KB", "CA", "CB" };
	String[] bi_cb = new String[] { // BC overrides
		"0", "KK", "D1", "?", "KA", "KB", "CA", "CB" };
	String[] bc_cb = new String[] { // overrides BI
		"0", "BI", "max", "~BI" };
	String[] bd_cb = new String[] { // alters AOP
		"bin", "bcd" };
	String[] aop_cb = new String[] {
		"A+B",		// 000
		"A+B+1",	// 001
		"SC;A+B",	// 010
		"SC;A+B+SC",	// 011
		"SC;A+B+1",	// 100
		"A&B",		// 101
		"A^B",		// 110
		"SC;(A+B)>>1",	// 111
	};
	String[] mop_cb = new String[] {
		"wr/Z_RB",	// ram(L,M,N) = Z,RA
		"wr/RA_Z",	// ram(L,M,N) = RA,Z
		"rdTUV/CACB",	// CA,CB = ram(T,U,V)
		"rdTUV",	// RA,RB = ram(T,U,V)
		"rdFKV/CACB",	// CA,CB = ram(15,KK,V)
		"rdFKV",	// RA,RB = ram(15,KK,V)
		"KB0=RBS",	// KB<0> <- RBS
		"IOB=KB",	// IOB<2:0> <- KB<2:0>
		"noop",		// no operation
		"Q=CC",		//
		"rdTAPE",	// KB<0> = Dot
		"wrTAPE",	// Din = KB<0>
		"TMR=1",	// tape motor ON
		"TMR=0",	// tape motor OFF
		"GIO",		// GIOA/B = KA/B
		"undef"
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
		"undef"
	};
	String[] jh_cb = new String[] {
		"0", "1", "S1", "S3", "OV", "CC", "KBD", "undef" };
	String[] jl_cb = new String[] {
		"0", "1", "S0", "S2", "ZR", "Q", "SC", "undef" };

	JTextField cmacro;
	JButton cpmac;
	JButton store;
	JButton revert;
	JButton exec;
	JButton run;
	JTextField cyc;
	JTextField bpt;
	int brkpt;
	JMenuItem save;
	long run_rate;
	boolean running;

	GridBagLayout gb;
	GridBagConstraints gc;

	byte[] rom;	// microcode ROM
	byte[] ram;	// program RAM
	Wang700_Ucode uu;
	int curr;	// current microcode ROM address
	int max;	// last instr rendered
	int carr;	// where we want the caret to stay
	boolean dirty;	// whether display word differs from ROM image
	boolean saved;	// whether ROM image has been saved to a file.
	boolean update;
	boolean foobar;
	File romFile;

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
	};

	public Wang700Assembler(String[] args) {
		super("Wang700 Microcode Assembler");
		String s;
		props = new Properties();
		// TODO: possibly load properties from file
		java.net.URL url;
		url = this.getClass().getResource("docs/help.html");
		if (url != null) {
			_help = new GenericHelp("Wang700 ucode asm Help", url);
		}

		dis_btns = new Object[2];
		dis_btns[OPTION_YES] = "Discard";
		dis_btns[OPTION_CANCEL] = "Cancel";

		getContentPane().setName("Wang700 UCode Asm");
		setResizable(false);
		//getContentPane().setBackground(new Color(100, 100, 100));
		_last = new File(System.getProperty("user.dir"));
		max = -1;
		fifo = new java.util.concurrent.LinkedBlockingDeque<Long>();

		File f = null;
		for (String arg : args) {
			if (arg.indexOf("=") >= 0) {
				String[] ss = arg.split("=", 2);
				props.setProperty(ss[0], ss[1]);
			} else if (f == null) {
				f = new File(arg);
				if (!f.exists()) {
					f = null;
				}
			}
		}

		ww = 120;
		wh = 24;
		s = props.getProperty("lines");
		if (s != null) {
			int nl = Integer.valueOf(s);
			if (nl >= 5 && nl <= 200) {
				wh = nl;
			}
		}
		int fz = 12;
		s = props.getProperty("font_size");
		if (s != null) {
			fz = Integer.valueOf(s);
		}

		text = new JTextArea(wh, ww);
		text.setEditable(false); // this prevents caret... grrr.
		text.setBackground(Color.white);
		Font font = new Font("Monospaced", Font.PLAIN, fz);
		setupFont(font);
		text.setCaret(new BlockCaret());
		text.addCaretListener(this);
		text.addKeyListener(this);
		text.addMouseListener(this);
		text.addMouseWheelListener(this);
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
		mi = new JMenuItem("Save as Text", KeyEvent.VK_T);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save as C Macros", KeyEvent.VK_M);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Show Scope", KeyEvent.VK_W);
		mi.addActionListener(this);
		mu.add(mi);
		mb.add(mu);

		mu = new JMenu("Edit");
		mi = new JMenuItem("Copy C Macro", KeyEvent.VK_C);
		// Accelerators don't actually work yet, but is informative
		mi.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.ALT_DOWN_MASK));
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
		store.setPreferredSize(new Dimension(70, 30));
		store.addActionListener(this);
		store.setActionCommand("store");
		revert = new JButton("revert");
		revert.setPreferredSize(new Dimension(80, 30));
		revert.addActionListener(this);
		revert.setActionCommand("revert");
		exec = new JButton("execute");
		exec.setPreferredSize(new Dimension(90, 30));
		exec.addActionListener(this);
		exec.setActionCommand("exec");
		// components of the "run" panel...
		run = new JButton("run");
		run.setPreferredSize(new Dimension(70, 30));
		run.addActionListener(this);
		run.setActionCommand("run");
		cyc = new JTextField();
		cyc.setPreferredSize(new Dimension(60, 20));
		cyc.setHorizontalAlignment(SwingConstants.RIGHT);
		//cyc.addFocusListener(this); // needed?
		//cyc.addActionListener(this); // needed?
		brkpt = -1;
		bpt = new JTextField();
		bpt.setPreferredSize(new Dimension(30, 20));
		bpt.setHorizontalAlignment(SwingConstants.RIGHT);
		bpt.addFocusListener(this);
		bpt.addActionListener(this);

		zo = new JComboBox<String>(zo_cb);
		ai = new JComboBox<String>(ai_cb);
		bi = new JComboBox<String>(bi_cb);
		bc = new JComboBox<String>(bc_cb);
		bd = new JComboBox<String>(bd_cb);
		aop = new JComboBox<String>(aop_cb);
		mop = new JComboBox<String>(mop_cb);
		kk = new JTextField();
		kk.setPreferredSize(new Dimension(20, 20));
		kk.setHorizontalAlignment(SwingConstants.RIGHT);
		st = new JComboBox<String>(st_cb);
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
		bc.addActionListener(this);
		bd.addActionListener(this);
		aop.addActionListener(this);
		mop.addActionListener(this);
		st.addActionListener(this);
		jh.addActionListener(this);
		jl.addActionListener(this);

		zo.setActionCommand("zo");
		ai.setActionCommand("ai");
		bi.setActionCommand("bi");
		aop.setActionCommand("aop");
		mop.setActionCommand("mop");
		st.setActionCommand("st");
		jh.setActionCommand("jh");
		jl.setActionCommand("jl");

		cmacro = new JTextField();
		cmacro.setFont(font);
		cmacro.setPreferredSize(new Dimension(500, fz * 3 / 2));
		cmacro.setEditable(false);
		cmacro.setFocusable(false);
		cpmac = new JButton("copy");
		cpmac.setPreferredSize(new Dimension(70, fz * 3 / 2));
		cpmac.addActionListener(this);
		cpmac.setActionCommand("copy");

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
		setLabel("AC,Ai");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("Bi");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("BC");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("BD");
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
		setGap(10);
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
		gb.setConstraints(bc, gc);
		add(bc);
		++gc.gridx;
		++gc.gridx;
		gb.setConstraints(bd, gc);
		add(bd);
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

		gc.gridwidth = 15;
		gb.setConstraints(cmacro, gc);
		add(cmacro);
		gc.gridx += 15;
		gc.gridwidth = 4;
		gb.setConstraints(cpmac, gc);
		add(cpmac);
		++gc.gridy;
		gc.gridwidth = 1;
		gc.gridx = 1;
		setGap(10);
		++gc.gridy;

		gc.gridwidth = 8;
		JPanel pan = storePanel();
		gc.anchor = GridBagConstraints.WEST;
		gb.setConstraints(pan, gc);
		add(pan);
		gc.anchor = GridBagConstraints.CENTER;
		gc.gridx += gc.gridwidth;

		pan = runPanel();
		gc.gridwidth = 11; // width - 2 - gc.gridwidth;
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

		run_rate = 10;
		s = props.getProperty("run_rate");
		if (s != null) {
			run_rate = Integer.valueOf(s);
		}
		int sz = 2048;
		s = props.getProperty("ram_size");
		if (s != null) {
			if (s.equalsIgnoreCase("2K")) {
				// already set
			} else if (s.equalsIgnoreCase("1K")) {
				sz = 1024;
			}
		}

		rom = new byte[8 * 2048];
		ram = new byte[sz];
		// TODO: property prefix...
		cpu = new Wang700_CPU(props, rom, ram, this);
		scope = new Wang700Scope(cpu);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		pack();
		setVisible(true);
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

	private JPanel storePanel() {
		JPanel pan = new JPanel();
		pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
		pan.add(store);
		JPanel pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 10));
		pan.add(pn);
		pan.add(revert);
		return pan;
	}

	private JPanel runPanel() {
		JPanel pn;
		GridBagLayout gb = new GridBagLayout();
		GridBagConstraints gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.anchor = GridBagConstraints.CENTER;
		JPanel pan = new JPanel();
		pan.setLayout(gb);
		//pan.setLayout(new BoxLayout(pan, BoxLayout.X_AXIS));
		gb.setConstraints(exec, gc);
		pan.add(exec);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 10));
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		gb.setConstraints(run, gc);
		pan.add(run);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 10));
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		JLabel lb = new JLabel("cycles:");
		gb.setConstraints(lb, gc);
		pan.add(lb);
		++gc.gridx;
		gb.setConstraints(cyc, gc);
		pan.add(cyc);
		++gc.gridx;
		pn = new JPanel();
		pn.setPreferredSize(new Dimension(5, 10));
		gb.setConstraints(pn, gc);
		pan.add(pn);
		++gc.gridx;
		lb = new JLabel("breakpt:");
		gb.setConstraints(lb, gc);
		pan.add(lb);
		++gc.gridx;
		gb.setConstraints(bpt, gc);
		pan.add(bpt);
		++gc.gridx;
		return pan;
	}

	private void setDirty(boolean drt) {
		dirty = drt;
		store.setEnabled(dirty);
		revert.setEnabled(dirty);
	}

	private void setSaved(boolean svd) {
		saved = svd;
		save.setEnabled(!saved);
		String ttl;
		if (saved) {
			ttl = "Wang700 Microcode Assembler";
		} else {
			ttl = "* Wang700 Microcode Assembler";
		}
		if (romFile != null) {
			ttl += " - " + romFile.getName();
		}
		setTitle(ttl);
	}

	private void newRom() {
		Arrays.fill(rom, (byte)0);
		romFile = null;
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
		romFile = f;
		setDirty(false);
		setSaved(true);
	}

	private synchronized void setRunning(boolean r) {
		// TODO: only if changed?
		running = r;
		if (r) {
			if (brkpt >= 0) {
				setBreakpoint(brkpt);
			}
			// TODO: disable microcode modifiers?
			run.setText("stop");
			run.setActionCommand("stop");
		} else {
			if (brkpt >= 0) {
				clrBreakpoint(brkpt);
			}
			run.setText("run");
			run.setActionCommand("run");
			text.requestFocus();
		}
	}

	private synchronized boolean getRunning() {
		return running;
	}

	// called from run() thread for any/all instructions
	private void singleStep(boolean noBrkpt) {
		int lastPC = curr;
		cpu.pc = curr;
		boolean bp = false;
		if (noBrkpt) {
			bp = clrBreakpoint(lastPC);
		}
		int x = cpu.instr_exec();
		if (bp) {
			setBreakpoint(lastPC);
		}
		scope.refresh();
		scope.view(true); // TODO: if not running?
		setLoc(cpu.pc);
	}

	private String getCMacro(Wang700_Ucode uu, int adr) {
		return String.format(
			"[0x%03x]=UCODE(%d,%d,%d,%d,%d,%d,%d,%2d,%2d,%2d,0x%03x,%d,%d),",
			adr, uu.ai, uu.bi, uu.zo, uu.aop, uu.ac, uu.bc, uu.bd,
			uu.mop, uu.kk, uu.st, uu.jad << 2, uu.jh, uu.jl);
	}
	private void setCMacro(Wang700_Ucode uu) {
		cmacro.setText(getCMacro(uu, curr));
	}

	private void cpCMacro() {
		String str = cmacro.getText() + '\n';
		StringSelection ss = new StringSelection(str);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
	}

	private boolean isZero(byte[] rom, int adr) {
		int idx = adr * 8;
		int i = rom[idx] | rom[idx + 1] | rom[idx + 2] | rom[idx + 3] |
			rom[idx + 4] | rom[idx + 5] | rom[idx + 6] | rom[idx + 7];
		return (i == 0);
	}

	private void savCMacro(File f) {
		Wang700_Ucode uu;
		String cm;
		// Trim trailing zeros
		int end = rom.length / 8 - 1;
		while (end >= 0 && isZero(rom, end)) --end;
		if (end < 0) end = 0;
		try {
			FileOutputStream fo = new FileOutputStream(f);
			PrintStream ps = new PrintStream(fo, true);
			if (romFile != null) {
				ps.format("/* %s */\n", romFile.getName());
			} else {
				ps.format("/* %s */\n", f.getName());
			}
			ps.println("#include \"ucode.h\"\n");
			ps.println("ucword ucode[2048] = {");
			ps.println("//                  a        m");
			ps.println("//            a b z o a b b  o  k  s       j j");
			ps.println("//            i i o p c c d  p  k  t   jad h l");
			for (int adr = 0; adr <= end; ++adr) {
				uu = cpu.fetchUcode(adr);
				cm = getCMacro(uu, adr);
				ps.println(cm);
			}
			ps.println("};");
			ps.close();
			// TODO: tear off?
		} catch (Exception ee) {
			// ...
		}
	}

	private void padDisas(int adr) {
		String dis;
		int nl = max;
		while (nl < adr) {
			++nl;
			dis = String.format("%03x: %s\n", nl, cpu.disas(nl, false));
			text.append(dis);
		}
		if (max < adr) max = adr;
	}

	// move caret to address
	private void gotoAdr(int adr) {
		try {
			carr = text.getLineStartOffset(adr); // each adr is a line
		} catch (Exception ee) {
			System.err.println(ee.getMessage());
			return;
		}
		foobar = true;
		text.setCaretPosition(carr);
		foobar = false;
		text.repaint();
	}

	// render disassembly and set caret to location
	private void setDisas(Wang700_Ucode uu) {
		String dis = String.format("%03x: %s\n", curr, cpu.disas(uu, false));

		foobar = true;
		try {
			carr = text.getLineStartOffset(curr); // each adr is a line
			int end = text.getLineEndOffset(curr);
			text.replaceRange(dis, carr, end);
			text.setCaretPosition(carr);
		} catch (Exception ee) {
			System.err.println(ee.getMessage());
		}
		foobar = false;
		setCMacro(uu);
		// TODO: any other actions to skip?
		if (!getRunning()) {
			text.requestFocus();
		}
	}

	// instruction changed, update disassembly
	private void updDisas(Wang700_Ucode uu) {
		setDirty(true);
		setSaved(false);
		setDisas(uu);
	}

	// setup fields based on instruction
	private void setLoc(Wang700_Ucode uu) {
		int ja = uu.jad << 2;
		if (uu.jh == 1) {
			ja |= 2;
		}
		if (uu.jl == 1) {
			ja |= 1;
		}
		update = true;
		zo.setSelectedIndex(uu.zo);
		int a;
		if (uu.ac == 0) {
			a = 0;
		} else {
			a = uu.ai + 1;
		}
		ai.setSelectedIndex(a);
		bi.setSelectedIndex(uu.bi);
		bc.setSelectedIndex(uu.bc);
		bd.setSelectedIndex(uu.bd);
		aop.setSelectedIndex(uu.aop);
		mop.setSelectedIndex(uu.mop);
		kk.setText(String.format("%d", uu.kk));
		st.setSelectedIndex(uu.st);
		jad.setText(String.format("%03x", ja));
		jh.setSelectedIndex(uu.jh);
		jl.setSelectedIndex(uu.jl);
		setCMacro(uu);
		update = false;
	}

	private void setLoc(int adr) {
		if (dirty) {
			storeUcode(uu);
		}
		foobar = true;
		padDisas(adr);
		gotoAdr(adr);
		foobar = false;
		curr = adr;
		uu = cpu.fetchUcode(adr);
		setLoc(uu);
		setDirty(false);
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
			int ans = JOptionPane.showOptionDialog((Component)this,
					"Discard Unsaved ROM?", purpose,
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null, dis_btns, dis_btns[OPTION_YES]);
			if (ans != OPTION_YES) {
				return false;
			}
		}
		return true;
	}

	// only the 'text' component gets key events
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {
		int c = (int)e.getKeyChar();
		int k = e.getKeyCode();
		int m = e.getModifiersEx();
		boolean shift = ((m & InputEvent.SHIFT_DOWN_MASK) != 0);

		if (k == KeyEvent.VK_DOWN) {
			if (curr < 0x7ff) {
				setLoc(curr + 1);
			}
		} else if (k == KeyEvent.VK_UP) {
			if (curr > 0x000) {
				setLoc(curr - 1);
			}
		} else if (k == KeyEvent.VK_PAGE_DOWN) {
				int n = curr + (wh - 1);
				if (n > 0x7ff) n = 0x7ff;
				setLoc(n);
		} else if (k == KeyEvent.VK_PAGE_UP) {
				int n = curr - (wh - 1);
				if (n < 0) n = 0;
				setLoc(n);
		} else if (k == KeyEvent.VK_HOME) {
				setLoc(0);
		} else if (k == KeyEvent.VK_END) {
				setLoc(0x7ff);
		} else if (k == KeyEvent.VK_F1) {
				bpt.setText(String.format("%03x", curr));
				brkpt = curr;
		} else if (shift && k == KeyEvent.VK_DELETE) {
				uu = new Wang700_Ucode(new byte[8]);
				setLoc(uu);
				updDisas(uu);
				setDirty(true);
		} else if (((m & InputEvent.ALT_DOWN_MASK) != 0) && k == KeyEvent.VK_C) {
			// Because menu accelerators don't actually work yet,
			// this is required.
			cpCMacro();
		}
		e.consume();
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
		int j0 = jl.getSelectedIndex();
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
		uu.zo = (byte)z;
		updDisas(uu);
	}

	private void updateST() {
		int s = st.getSelectedIndex();
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

	// TODO: also BC, show conflicts...
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

	private void updateBC() {
		int op = bc.getSelectedIndex();
		// TODO: any special handling?
		uu.bc = (byte)op;
		updDisas(uu);
	}

	private void updateBD() {
		int op = bd.getSelectedIndex();
		// TODO: any special handling?
		uu.bd = (byte)op;
		updDisas(uu);
	}

	// store at curr...
	private void storeUcode(Wang700_Ucode uu) {
		int idx = curr * 8;
		System.arraycopy(uu.asBytes(), 0, rom, idx, 8);
		setDirty(false);
		text.repaint();
	}

	// called just before running...
	private void setBreakpoint(int pc) {
		int idx = pc * 8;
		rom[idx + 7] |= 1;
	}

	// called to cancel breakpoint
	private boolean clrBreakpoint(int pc) {
		int idx = pc * 8 + 7;
		boolean was = ((rom[idx] & 1) != 0);
		rom[idx] &= ~1;
		return was;
	}

	private void doBreakpoint() {
		String a = bpt.getText();
		int ad = -1;
		brkpt = -1;
		try {
			ad = Integer.valueOf(a, 16);
		} catch (Exception ee) { // includes a.length() == 0
			ad = -1;
		}
		if (ad < 0 || ad > 0x7ff) {
			bpt.setText("");
			return;
		}
		brkpt = ad;
		bpt.setText(String.format("%03x", brkpt));
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
		setRunning(true); // sets breakpoint in ROM, if any
		fifo.add(limit);
	}

	private void stopRun() {
		fifo.add((long)-1);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) {
			JButton bt = (JButton)e.getSource();
			if (bt == store) {
				storeUcode(uu);
			} else if (bt == revert) {
				setDirty(false); // prevent setLoc() from storing
				setLoc(curr);
				updDisas(uu);
				setDirty(false); // reverse what updDisas() did
			} else if (bt == exec) {
				startOne();
			} else if (bt == run) {
				String act = run.getActionCommand();
				if (act.equals("run")) {
					startRun();
				} else {
					stopRun();
				}
			} else if (bt == cpmac) {
				cpCMacro();
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
			else if (cb == bc) updateBC();
			else if (cb == bd) updateBD();
			else if (cb == aop) updateAOP();
			else if (cb == mop) updateMOP();
			else if (cb == st) updateST();
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
			} else if (tf == bpt) {
				doBreakpoint();
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
			max = -1;
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
				max = -1;
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
		if (m.getMnemonic() == KeyEvent.VK_M) {
			File sav = pickFile("Save C Macros", cx, cd);
			if (sav == null) return;
			savCMacro(sav);
			_last = sav;
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_W) {
			scope.view(true);
			return;
		}
		if (m.getMnemonic() == KeyEvent.VK_C) {
			cpCMacro();
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
			} else if (tf == bpt) {
				doBreakpoint();
			}
		}
	}

	public void breakpoint(int pc) {
		stopRun();
	}
	public void debug_check() {}
	public void tape_record(byte to_byte) {}
	public int tape_play() { return 0; }
	public void tape_on(int wr) {}
	public void tape_off(int wr) {}
	public void dev_reset() {}
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
	public int getMemMask() { return ram.length - 1; }

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

	public void caretUpdate(CaretEvent e) {
		if (foobar) return;
		int dot = e.getDot();
		if (dot == carr) return;
		int ca = -1;
		int nl = -1;
		try {
			ca = text.getLineOfOffset(dot);
		} catch (Exception ee) {}
		if (ca >= 0 && ca <= 0x7ff) {
			setLoc(ca);
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		int clicks = e.getWheelRotation();
		int nc = curr + clicks;
		if (nc < 0) nc = 0;
		if (nc > 0x7ff) nc = 0x7ff;
		setLoc(nc);
	}

	public void run() {
		long limit;
		while (true) {
			try {
				limit = fifo.take();
			} catch (Exception ee) {
				continue; // TODO: prevent runaway?
			}
			// TODO: must skip breakpoint on first instruction...
			boolean first = true;
			while (limit > 0 && fifo.size() == 0) {
				singleStep(first);
				first = false;
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
