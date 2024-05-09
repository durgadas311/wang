// Copyright (c) 2011,2026 Douglas Miller

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

// Implements the Wang700 hardware. Does not provide any debug/trace support.

class Wang700_Simulator
	implements Wang_FrontPanel, Wang_Core
{

	static final int D10_NC = 0x01;
	static final int D11_LST_L_P = 0x02;
	static final int D12_LRN_L_P = 0x04;
	static final int D13_STEP = 0x08;

	// simulator (no direct h/w relation)
	long cylimit;
	boolean run_sim;

	boolean trace;
	boolean trc_cycles;
	boolean trc_raw;
	FileOutputStream trc_fp;

	public byte[] _ram;
	int memsize;
	int memmask;
	boolean modelC;
	private Wang700_CPU cpu;

	private int mode0 = 0;

	public JMenuItem getXRomMenu(int key) {
		return new JMenuItem("Not Used", key);
	}

	public void setXRom(byte[] img) {
		if (img.length == 0) {}
	}

	public void pickXRomFile(JMenuItem m) {
		if (m == null) {}
	}

	public class Wang700_UcodeRom {
		public byte[] _ucode; // raw ucode from file, 64-bit words
		public boolean std;

		public Wang700_UcodeRom(java.io.InputStream img) {
			// Can't change _ucode after initial setup (i.e. while running).
			// Can't run if _ucode is null... need to check
			// (right now, will throw NULL pointer exception when fetching)
			// Enforce fixed-size 2048-word x 64-bit ucode.
			if (_ucode == null && img != null) {
				int n = 0;
				byte[] buf = new byte[16384];
				try {
					// if 'img' came from a resource stream,
					// the read() may not return all bytes...
					while (n < 16384) {
						n += img.read(buf, n, 16384 - n);
					}
				} catch (IOException ee) {
					Wang_UI.fatal("Loading microcode", ee.getMessage());
				}
				try {
					img.close();
				} catch (IOException ee) {
				}
				if (n == 16384) {
					_ucode = buf;
					setupROM(memsize);
				} else {
					Wang_UI.fatal("Loading microcode", "Wrong size");
				}
			}
		}

		// If ROM is the standard one, enable sleeps
		private void setupROM(int memsize) {
			// TODO: more sophisticated tests.
			// we don't yet know how all the different
			// microcode versions differ.
			// This tests for ROMs derived from 720C...
			int idx = 0x034 * 8;
			std = ((_ucode[idx + 0] & 0xff) == 0x02 &&
				(_ucode[idx + 1] & 0xff) == 0xb3 &&
				(_ucode[idx + 2] & 0xff) == 0x00 &&
				(_ucode[idx + 3] & 0xff) == 0x88 &&
				(_ucode[idx + 4] & 0xff) == 0x14 &&
				(_ucode[idx + 5] & 0xff) == 0x04);
			if (std) {
				// anything to patch?
//System.err.format("Standard ROM detected\n");
			}
		}

		public boolean getBreakPoint(int adr) {
			int idx = adr * 8;
			return ((_ucode[idx + 7] & 1) != 0);
		}

		public boolean breakPoint(int adr) {
			int idx = adr * 8;
			_ucode[idx + 7] ^= 1;
			return ((_ucode[idx + 7] & 1) != 0);
		}

		public byte[] fetchBytes(int adr) {
			int idx = adr * 8;
			return Arrays.copyOfRange(_ucode, idx, idx + 8);
		}

		public long fetchLong(int adr) {
			byte[] b = fetchBytes(adr);
			long u = (b[0] & 0x00ff) | ((long)(b[1] & 0x00ff) << 8) |
				((long)(b[2] & 0x00ff) << 16) | ((long)(b[3] & 0x00ff) << 24) |
				((long)(b[4] & 0x00ff) << 32) | ((long)(b[5] & 0x00ff) << 40) |
				((long)(b[6] & 0x00ff) << 48) | ((long)(b[7] & 0x00ff) << 56);
			return u;
		}

		public Wang700_Ucode fetchUcode(int adr) {
			return new Wang700_Ucode(fetchBytes(adr));
		}
	}

	class Wang700_Debugger
		implements Wang_Debugger
	{
		public Wang700_Debugger() {
		}

		public int getPC() {
			return cpu.pc;
		}
		public int getReg(String reg) {
			if (reg.equalsIgnoreCase("s")) {
				return cpu.s;
			} else if (reg.equalsIgnoreCase("t")) {
				return cpu.t;
			} else if (reg.equalsIgnoreCase("u")) {
				return cpu.u;
			} else if (reg.equalsIgnoreCase("v")) {
				return cpu.v;
			} else if (reg.equalsIgnoreCase("ca")) {
				return cpu.ca;
			} else if (reg.equalsIgnoreCase("cb")) {
				return cpu.cb;
			} else if (reg.equalsIgnoreCase("ka")) {
				return cpu.ka;
			} else if (reg.equalsIgnoreCase("kb")) {
				return cpu.kb;
			} else if (reg.equalsIgnoreCase("gioa")) {
				return cpu.gioa;
			} else if (reg.equalsIgnoreCase("giob")) {
				return cpu.giob;
			} else if (reg.equalsIgnoreCase("iob")) {
				return cpu.iob;
			} else if (reg.equalsIgnoreCase("ov")) {
				return cpu.ov;
			} else if (reg.equalsIgnoreCase("err")) {
				return cpu.err;
			} else if (reg.equalsIgnoreCase("pc")) {
				return cpu.pc;
			} else {
				return -1;
			}
		}

		public int setReg(String reg, int val) {
			if (reg.equalsIgnoreCase("s")) {
				cpu.s = (byte)val;
			} else if (reg.equalsIgnoreCase("t")) {
				cpu.t = (byte)val;
			} else if (reg.equalsIgnoreCase("u")) {
				cpu.u = (byte)val;
			} else if (reg.equalsIgnoreCase("v")) {
				cpu.v = (byte)val;
			} else if (reg.equalsIgnoreCase("ca")) {
				cpu.ca = (byte)val;
			} else if (reg.equalsIgnoreCase("cb")) {
				cpu.cb = (byte)val;
			} else if (reg.equalsIgnoreCase("ka")) {
				cpu.ka = (byte)val;
			} else if (reg.equalsIgnoreCase("kb")) {
				cpu.kb = (byte)val;
			} else if (reg.equalsIgnoreCase("gioa")) {
				cpu.gioa = (byte)val;
			} else if (reg.equalsIgnoreCase("giob")) {
				cpu.giob = (byte)val;
			} else if (reg.equalsIgnoreCase("iob")) {
				cpu.iob = (byte)(val & 0x07);
			} else if (reg.equalsIgnoreCase("ov")) {
				cpu.ov = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("err")) {
				cpu.err = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("pc")) {
				cpu.pc = val & 0x7ff;
			} else {
				return -1;
			}
			return getReg(reg);
		}

		public String disas(int adr, boolean raw) {
			return cpu.disas(adr, raw);
		}

		public long relCycleLimit(long num) {
			cylimit = cpu.cycles + num;
			return cylimit;
		}

		public void setRun(boolean run) {
			run_sim = run;
		}

		public boolean getBreakPoint(int adr) {
			return _rom.getBreakPoint(adr);
		}

		public boolean breakPoint(int adr) {
			return _rom.breakPoint(adr);
		}

		public int getRamAdr() {
			return ((cpu.l & 0x0f) << 8) |
				((cpu.m & 0x0f) << 4) |
				(cpu.n & 0x0f);
		}

		public long getCycles() {
			return cpu.cycles;
		}

		public void core(FileOutputStream file) throws Exception {
			file.write(_ram);
		}

		public void setTrace(boolean on) throws Exception {
			if (!on) {
				if (trc_fp != null) {
					trc_fp.close();
					trc_fp = null;
				}
				trc_cycles = false;
				trc_raw = false;
			}
			trace = on;
		}

		public void setTraceFile(FileOutputStream file) throws Exception {
			setTrace(false);	// ensure previous file gets closed
			trc_fp = file;
			setTrace(true);
		}

		public void setTraceCycles(boolean on) {
			trc_cycles = on;
		}

		public void setTraceRaw(boolean on) {
			trc_raw = on;
		}

		public String getTrace() {
			String str = "Tracing is now ";
			if (trace) {
				if (trc_fp != null) {
					str += "<file>";
				} else {
					str += "on";
				}
				if (trc_cycles) {
					str += " cycles";
				}
				if (trc_raw) {
					str += " raw";
				}
			} else {
				str += "off";
			}
			return str;
		}

		public String getRegisters() {
			String str = new String();
			str += String.format("T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
				cpu.t, cpu.u, cpu.v, cpu.ca, cpu.cb);
			str += String.format("S = %01x Zo = %d CC = %d SC = %d Q = %d\n",
				cpu.s, cpu.zo, cpu.cc, cpu.sc, cpu.q);
			str += String.format("KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
				cpu.ka, cpu.kb, cpu.gioa, cpu.giob, cpu.iob);
			str += String.format("L = %01x M = %01x N = %01x RA = %01x RB = %01x\n",
				cpu.l, cpu.m, cpu.n, cpu.ra, cpu.rb);
			return str;
		}

		public String getMachine() {
			String str = String.format("d1=%01x", Wang700.Kbd.getMode0(false));
			if (cpu.ov != 0) str += "|Prog Err";
			if (cpu.err != 0) str += "|Mach Err";
			if (cpu.kbd != 0) str += "|Key Pressed";
			return str;
		}

		public void dup() {
		}

		public void putWarp(String tag, int nxt, int cyc) throws Exception {
			String str = String.format("TRACE: %03x: %s", cpu.pc, tag);
			if (cyc > 0) {
				cpu.next = nxt;
				cpu.cycles += cyc;
				str += " Warp";
			} else if (nxt >= 0) {
				str += String.format(" PC %03x", nxt);
			} else if (cyc < 0) {
				str += " Sleep";
			}
			str += String.format("... %d\n", cpu.cycles);
			if (trc_fp != null) {
				trc_fp.write(str.getBytes());
			} else {
				System.err.print(str);
			}
		}

		public void putTrace() throws Exception {
			String str = ": ";
			if (trc_cycles) {
				str += String.format("%9d ", cpu.cycles);
			}
			str += String.format("%03x: [%03x] ",
				cpu.pc, cpu.next);
			str += String.format("%01x %01x %01x %01x %01x [",
				cpu.t, cpu.u, cpu.v, cpu.ca, cpu.cb);
			if (cpu.zo != 0) str += "Z"; else str += "z";
			if (cpu.cc != 0) str += "I"; else str += "i";
			if (cpu.sc != 0) str += "C"; else str += "c";
			if (cpu.q != 0) str += "Q"; else str += "q";
			str += String.format("] %01x %01x %01x : ",
				cpu.s, cpu.ka, cpu.kb);
			str += cpu.disas(cpu.pc, trc_raw);
			str += "\n";
			if (trc_fp != null) {
				trc_fp.write(str.getBytes());
			} else {
				System.err.print(str);
			}
		}

		public String ramDump(int adr, int len) {
			String str = new String();
			int aa = adr;
			int ln = len;
			int xx, yy;
			for (xx = 0; xx < ln;) {
				if (aa >= _ram.length) {
					str += " end memory\n";
					break;
				}
				str += String.format("%03x:", aa);
				for (yy = 0; xx + yy < ln && yy < 16; ++yy) {
					byte bb = _ram[aa + yy];
					str += String.format(" %01x", (bb >> 4) & 0x0f);
				}
				str += "\n    ";
				for (yy = 0; xx + yy < ln && yy < 16; ++yy) {
					byte bb = _ram[aa + yy];
					str += String.format(" %01x", bb & 0x0f);
				}
				str += "\n";
				aa += yy;
				xx += yy;
			}
			return str;
		}

		public String romDump(int adr, int len) {
			String str = new String("Not Supported");
			return str;
		}

		public void ramSet(int adr, byte val) {
			_ram[adr] = val;
		}

		public int getUcodeSize() {
			return _rom._ucode.length / 8;
		}
	}

	public Wang700_UcodeRom _rom;

	public Wang700_Ucode getUcode(int adr) {
		return _rom.fetchUcode(adr);
	}


	public void chgMode0() {
		int m0 = getMode0(false);
		if (((m0 ^ mode0) & 8) != 0) cpu.setStep();
		mode0 = m0;
		good = 0;
		if (trace) { // can only be if _dbg != null
			_dbg.warp("MODE0 Jam", -1, 0);
		}
		do_blanking();
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode1() {} // never called on 700

	public void chgMode2() {} // never called on 700

	public void pressCmd(int cmd) {
		cpu.setJam(cmd);
		if (trace) { // can only be if _dbg != null
			_dbg.warp("Key Jam", cmd, 0);
		}
		// needs other side-effects... display? clear key buffer?
		good = 0;
		if (cmd == 0) { // PRIME is visible to all devices
			dev_reset();
		}
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void ackIO(int iob) {
		// do some validation on iob?
		setGi(0);
	}

	// device sends byte to calculator. might need translation from
	// generic to Wang 700 command codes.
	public void replyIO(int iob, int rep) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		if (rep == Wang_GroupIODevice.GO) {
			rep = 0x5e; // GO
		} else if (rep == Wang_GroupIODevice.START) {
			rep = 0x4c; // WRITE ALPHA
		} else if (rep == Wang_GroupIODevice.END) {
			rep = 0x4d; // END ALPHA
		} else if (rep == Wang_GroupIODevice.EOT) {
			rep = 0x00; // SR 0000
		} else if (rep == Wang_GroupIODevice.DP) {
			rep = 0x7c; // Decimal Point
		} else if (rep == Wang_GroupIODevice.CHG_SIGN) {
			rep = 0x7b; // Decimal Point
		} else if (rep >= Wang_GroupIODevice.E0 && rep <= Wang_GroupIODevice.E9) {
			rep = 0x70 | (rep - Wang_GroupIODevice.E0); // Digit
		} else if (rep == Wang_GroupIODevice.SET_EXP) {
			rep = 0x7a;
		} else if (rep == Wang_GroupIODevice.CLR_DSP) {
			rep = 0x7f;	// CLEAR X
		} else if (rep >= Wang_GroupIODevice.SR0 && rep < Wang_GroupIODevice.SREND) {
			rep = 0x00 | (rep - Wang_GroupIODevice.SR0);
		}
		setGi(rep);
// TODO: review how this should be done.
// seems like this should not be ACKing but
// rather the microcode does that later.
//		if ((iob & ~1) == 2) { // block I/O
//			Wang700.M730.do_ack(iob);
//		} else if (_cn36 != null) {
//			_cn36.do_ack(iob);
//		}
	}

	java.util.concurrent.LinkedBlockingDeque<Integer> keyCodes;

	public void setKaKb(int key) {
		cpu.setKaKb(key);
		keyCodes.add(key); // only used for the wakeup
	}

	public void setGi(int key) {
		cpu.setGi(key);
		keyCodes.add(key); // only used for the wakeup
	}

	public void pressKey(int key) {
		if (trace) { // can only be if _dbg != null
			_dbg.warp(String.format("Key Press %02x", key), -1, 0);
		}
		// TODO: any I/O conditions?
		if (cpu.kbl || cpu.z2) return;
		setKaKb(key);
		// needs other side-effects... display?
		do_blanking(); // yes?
	}

	private Wang_DebugConsole _dbg;

	public Wang700_Simulator(boolean dbg, boolean stop) {
		if (dbg) {
			_dbg = new Wang_DebugConsole(new Wang700_Debugger());
		} else {
			_dbg = null;
		}
		// might need to search for possible ucode versions???
		String romfile;
		// wang700_model is never null here
		String model = Wang_UI.getProperties().getProperty("wang700_model");
		if (model.matches(".2..")) {
			memsize = 2048;
			memmask = 0x07ff;
		} else {
			memsize = 1024;
			memmask = 0x03ff;
		}
		modelC = model.endsWith("C");
		romfile = Wang_UI.getProperties().getProperty("wang700_ucode");
		if (romfile == null) {
			romfile = "wang" + model.toLowerCase() + ".rom";
		}
		java.io.InputStream rom = this.getClass().getResourceAsStream(romfile);
		if (rom == null) {
			try {
				rom = new FileInputStream(romfile);
			} catch(Exception ee) {
				Wang_UI.fatal("Opening microcode", ee.getMessage());
			}
		}
		_rom = new Wang700_UcodeRom(rom);
		_ram = new byte[memsize];
		residualCore(_ram); // if any

		dispx = new short[16];
		dispy = new short[16];
		keyCodes = new java.util.concurrent.LinkedBlockingDeque<Integer>();
		run_sim = !stop;
		cylimit = Long.MAX_VALUE;
		trace = false;
		trc_cycles = false;
		trc_raw = false;
		trc_fp = null;
		cpu = new Wang700_CPU(Wang_UI.getProperties(), _rom._ucode, _ram, this);
		Thread t = new Thread(this);
		t.start();
	}

	private void residualCore(byte[] core) {
		InputStream f;
		String p = Wang_UI.getProperties().getProperty("wang700_core");
		if (p == null) return;
		try {
			f = new FileInputStream(p);
			f.read(core);
		} catch (Exception ee) {}
	}

	public void debugIntr() {
		if (_dbg != null) {
			// might be sleeping, so need to wake up...
			run_sim = false;
			keyCodes.addFirst(-1);
		}
	}

	public Wang_Debugger getDebug() {
		return _dbg.getDebug();
	}

	private short[] dispx;
	private short[] dispy;
	int good;
	int lastx;

	private void refresh(boolean canSleep) {
		byte n = cpu.n;
		short x = (short)(((cpu.s & 2) << 7) | (n << 4) | cpu.rb);
		short y = (short)((((cpu.s & 1) ^ 1) << 8) | (n << 4) | cpu.ra);
		if (dispx[n] != x) {
			dispx[n] = x;
			good = 0;
		}
		if (dispy[n] != y) {
			dispy[n] = y;
			good = 0;
		}
		if (++lastx >= 16) {
			lastx = 0;
			++good;
			Wang700.DispX.do_display(dispx);
			// do not refresh Y when LEARN (or LEARN AND PRINT)
			// (assumes display got blanked previously)
			if ((getMode0(false) & D12_LRN_L_P) == 0) {
				Wang700.DispY.do_display(dispy);
			}
		} else {
			if (good > 4) {
				if (canSleep) {
					int k = -1;
					try {
						k = keyCodes.take();
					} catch(Exception ee) {
						k = -1;
					}
					good = 0;
				}
			}
		}
	}

	private void do_blanking() {
		Arrays.fill(dispx, (short)-1);
		Arrays.fill(dispy, (short)-1);
		Wang700.DispX.do_blanking();
		Wang700.DispY.do_blanking();
	}

	///////////////////////////////////
	// Wang_FrontPanel interface
	public int getMemSize() { return memsize; }
	public int getMemMask() { return memmask; }
	public void breakpoint(int pc) {
		_rom.breakPoint(pc);
		run_sim = false;
	}
	public void debug_check() {
		if (trace) { // can only be if _dbg != null
			_dbg.instr_trace();
		}
	}

// Traps for standard "720C" ROM - must change for "REAL" A/B (and 700) ROMs
//	0x034, 0x472, 431, "Display Refresh"
//	0x5ed, 0x4ae, 513, "Alpha-Stop"
//	0x4af, -1, -1, "Alpha-Stop" - debug information only

	public void display_check(boolean mr) {
		if (!_rom.std) {
			if (mr) refresh(false);
			return;
		}
		//
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if ((cpu.pc & 0xffe) == 0x034) {	// display refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Display Refresh", 0x472, 431);
			} else {
				cpu.next = 0x472;	// update some regs too?
				cpu.cycles += 431;
			}
			refresh(cylimit == Long.MAX_VALUE);
		// 5ed: begin alpha-stop display-refresh delay loop... short-cut to 4ae...
		} else if (cpu.pc == 0x5ed) {	// alpha-stop refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Alpha-Stop", 0x4ae, 513);
			} else {
				cpu.next = 0x4ae;
				cpu.cycles += 513;
			}
			refresh(false);
		} else if (cpu.pc == 0x4af) {	// alpha-stop done... "return"...
			if (cpu.next == 0x081) { // alpha-stop in running program...
				if (trace) { // can only be if _dbg != null
					_dbg.warp("Alpha-Stop", -1, -1);
				}
				// observed 528386 cycles or about 0.727 second
				try {
					Thread.sleep(727);
				} catch(Exception ee) {
				}
			}
			do_blanking();
		}
	}

	public void tape_record(byte to_byte) {
		Wang700.Tape.tape_record(to_byte);
	}
	public int tape_play() {
		return Wang700.Tape.tape_play();
	}
	public void tape_on(int wr) {
		Wang700.Tape.tape_on(wr);
	}
	public void tape_off(int wr) {
		Wang700.Tape.tape_off(0);
	}
	public void dev_reset() {
		// PRIME pressed, everything is reset
		Wang_CN24_dev.reset(); // handles null
		Wang_CN36_Bus.resetCN36();
	}
	public void dev_out(byte iob, byte c) {
		if (iob == 1) { // "Typewriter", CN24 output only, 6 bits
			if ((c & 0x40) != 0) {
				// TODO: local control codes - not needed?
				return;
			}
			c &= 0x3f;
			if (Wang_CN24_dev.get() != null) {
				Wang_CN24_dev.get().do_cn24(c);
			}
		} else { // includes IOB=0 (end of command)
			Wang_CN36_Bus.doCN36(iob, (c & 0x0ff));
		}
	}
	// Get mode0 switches state, possibly modified by peripherals
	public int getMode0(boolean clear) {
		int g = Wang700.Kbd.getMode0(clear);
		if (clear) mode0 = Wang700.Kbd.getMode0(false);
		// clear 0010 if glrn?
		if (Wang_CN36_Bus.getGLRN() != 0) {
			g |= (byte)D12_LRN_L_P;
		}
		return g;
	}
	public int getMode1(boolean clear) {
		return 0; // No MODE1 switches
	}
	public int getRBS() { 
		if (Wang_CN24_dev.get() != null) {
			return Wang_CN24_dev.get().getRBS();
		} else {
			return 1; // always ready
		}
	}
	public void setGKBD(boolean state) {
		Wang_CN36_Bus.setGKBD(state);
	}
	public void do_printer(int x, byte pr_drum) {
		// No drum printer
	}
	public void do_line() {
		// No drum printer
	}
	public void setOv(byte on) {
		Wang700.DispX.setOv(on);
	}
	public void setErr(byte on) {
		Wang700.DispX.setErr(on);
	}
	//////////////////////////////

	public void run() {
		// Run the simulator...
		boolean debug = (_dbg != null);
		int rc = 0;
		do {
			if (debug && !run_sim) {
				System.out.format("break at %03x (from %03x) %d\n",
						cpu.pc, cpu.last, cpu.cycles);
				while (debug && !run_sim) {
					rc = _dbg.command();
					if (rc != 0) {
						System.exit(0);
					}
				}
			}
			rc = cpu.instr_exec();
			if (rc != 0) {
				break;
			}
			if (debug && cpu.cycles >= cylimit) {
				// PC has NOT been executed...
				cylimit = Long.MAX_VALUE;
				run_sim = false;
			}
		} while (run_sim || debug);
		// not normally reached...
		Wang_UI.fatal("Wang700 Core", "Simulation error");
	}
}
