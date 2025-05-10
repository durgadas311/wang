// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang600_Simulator.java,v 1.13 2014/01/26 14:52:56 drmiller Exp $

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

// Implements the Wang600 hardware. Does not provide any debug/trace support.

class Wang600_Simulator
	implements Wang_FrontPanel, Wang_Core
{
	final String ident = "$Id: Wang600_Simulator.java,v 1.13 2014/01/26 14:52:56 drmiller Exp $";

	// simulator (no direct h/w relation)
	long cylimit;
	boolean run_sim;

	boolean trace;
	boolean trc_cycles;
	boolean trc_raw;
	FileOutputStream trc_fp;

	int memsize;
	int memmask;
	public byte[] _ram;
	public byte[] _xrom;

	private File _xromFile;
	private Wang600_CPU cpu;

	public JMenuItem getXRomMenu(int key) {
		String status = "none installed";
		if (_xromFile != null) {
			status = _xromFile.getName();
		}
		return new JMenuItem("Expansion ROM - " + status, key);
	}

	public void setXRom(byte[] img) {
		int l = img.length;
		if (l > _xrom.length) {
			// just in case...
			l = _xrom.length;
		}
		int z = _xrom.length - l;
		for (int x = 0; x < l; ++x) {
			_xrom[z] = img[x];
		}
	}

	private void loadXRom(File img) {
		// must reverse order of bytes....
		_xrom = null;
		if (img != null) {
			FileInputStream f;
			try {
				f = new FileInputStream(img);
			} catch (FileNotFoundException ee) {
				Wang_UI.warning("Install ROM", ee.getMessage());
				return;
			}
			int n = 0;
			int len = 2048;
			byte[] buf = new byte[len];
			try {
				n = f.read(buf);
			} catch (IOException ee) {
				Wang_UI.warning("Install ROM", ee.getMessage());
				n = -1;
			}
			try {
				f.close();
			} catch (IOException ee) {
			}
			if (n > 0) {
				_xrom = new byte[len];
				for (int x = 0; x < n; ++x) {
					_xrom[len - x - 1] = buf[x];
				}
			}
		}
	}

	public void pickXRomFile(JMenuItem m) {
		SuffFileChooser ch = new SuffFileChooser("Install",
					Wang_UI.getProperties().getProperty("wang600_rom_file_suffix"),
					"Wang ROM image files", Wang_UI.getDir());
		File file = Wang_UI.getProperties().getFile("wang600_rom_image",
							true, Wang_UI.getDir());
		if (file != null) {
			ch.setSelectedFile(file);
		}
		int rv = ch.showDialog(Wang600.Kbd);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
			// are we being too optimistic? maybe wait until
			// download succeeds?
			m.setText("Expansion ROM - " + file.getName());
		} else {
			file = null;
			m.setText("Expansion ROM - none installed");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				new Wang600_Properties(),
				"wang600_rom_image",
				file == null ? "" : file.getName());
		} catch(Exception ee) {}
		// NOTE: on real hardware, you can't change ROMs without
		// risking severe damage to ROM cartridge or calculator!
		// We could just save the property and wait for restart?
		loadXRom(file);
	}

	public class Wang600_UcodeRom {
		public byte[] _ucode; // raw ucode from file, 64-bit words
		public boolean std;
		// right now, the only override is for mem size, so just hardcode
		// all that.

		public Wang600_UcodeRom(java.io.InputStream img, int memsize) {
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

		// If ROM is the standard one, enable sleeps and override memsize
		private void setupROM(int memsize) {
			// patch mem-size override instruction...
			// TODO: more sophisticated tests
			int idx = 0x008 * 8;
			std = (_ucode[idx + 0] == 0x1c &&
				_ucode[idx + 1] == 0x00 &&
				(_ucode[idx + 2] & 0xff) == 0xc0 &&
				_ucode[idx + 3] == 0x00 &&
				_ucode[idx + 4] == 0x70 &&
				_ucode[idx + 5] == 0x00);
			if (std) {
				// 'memsize' is in bytes, Wang uses nibbles...
				byte kk = (byte)((((memsize << 1) - 1) >> 8) & 0x0f);
				_ucode[idx + 2] |= ((kk & 0x03) << 6);
				_ucode[idx + 3] |= ((kk >> 2) & 0x03);
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

		public Wang600_Ucode fetchUcode(int adr) {
			return new Wang600_Ucode(fetchBytes(adr));
		}
	}

	class Wang600_Debugger
		implements Wang_Debugger
	{
		public Wang600_Debugger() {
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
			} else if (reg.equalsIgnoreCase("stk1")) {
				return cpu.stk1;
			} else if (reg.equalsIgnoreCase("stk2")) {
				return cpu.stk2;
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
			} else if (reg.equalsIgnoreCase("stk1")) {
				cpu.stk1 = val & 0x7ff;
			} else if (reg.equalsIgnoreCase("stk2")) {
				cpu.stk2 = val & 0x7ff;
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
			String str = String.format("STK1 = %03x STK2 = %03x\n",
				cpu.stk1, cpu.stk2);
			str += String.format("T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
				cpu.t, cpu.u, cpu.v, cpu.ca, cpu.cb);
			str += String.format("S = %01x Zo = %d CC = %d SC = %d\n",
				cpu.s, cpu.zo, cpu.cc, cpu.sc);
			str += String.format("KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
				cpu.ka, cpu.kb, cpu.gioa, cpu.giob, cpu.iob);
			return str;
		}

		public String getMachine() {
			String str = String.format("d1=%01x|d2=%01x", Wang600.Kbd.getMode0(false), Wang600.Kbd.getMode1(false));
			if (cpu.ov != 0) str += "|Prog Err";
			if (cpu.err != 0) str += "|Mach Err";
			if (cpu.kbd != 0) str += "|Key Pressed";
			return str;
		}

		public void dup() {
			int xx = 0x100;
			int yy = _ram.length - 0x0a0;
			setXRom(Arrays.copyOfRange(_ram, xx, yy));
		}

		public void putWarp(String tag, int nxt, int cyc) throws Exception {
			String str = String.format("TRACE: %03x: %s", cpu.pc, tag);
			if (cyc > 0) {
				cpu.next = nxt;
				cpu.cycles += cyc;
				str += " Warp";
			} else if (nxt >= 0) {
				str += String.format(" PC %03x", nxt);
			} else {
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
			str += String.format("%03x: [%03x %03x %03x] ",
				cpu.pc, cpu.next, cpu.stk1, cpu.stk2);
			str += String.format("%01x %01x %01x %01x [",
				cpu.t, cpu.u, cpu.v, cpu.ca);
			if (cpu.zo != 0) str += "Z"; else str += "z";
			if (cpu.cc != 0) str += "I"; else str += "i";
			if (cpu.sc != 0) str += "C"; else str += "c";
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
			int aa = adr >> 1; 
			int ln = (len + 1) & ~1;
			int xx, yy;
			for (xx = 0; xx < ln;) {
				if (aa >= _ram.length) {
					str += " end memory\n";
					break;
				}
				str += String.format("%03x:", aa << 1);
				for (yy = 0; xx + yy < ln && yy < 16; yy += 2) {
					byte bb = _ram[aa];
					str += String.format(" %01x-%01x", (bb & 0x0f), (bb >> 4) & 0x0f);
					++aa;
				}
				str += "\n";
				xx += yy;
			}
			return str;
		}

		public String romDump(int adr, int len) {
			String str = new String();
			if (_xrom == null) {
				str = "No ROM installed";
				return str;
			}
			int aa = adr >> 1; 
			int ln = (len + 1) & ~1;
			int xx, yy;
			for (xx = 0; xx < ln;) {
				if (aa >= _xrom.length) {
					str += " end ROM\n";
					break;
				}
				str += String.format("%03x:", aa << 1);
				for (yy = 0; xx + yy < ln && yy < 16; yy += 2) {
					byte bb = _xrom[aa];
					str += String.format(" %01x-%01x", (bb & 0x0f), (bb >> 4) & 0x0f);
					++aa;
				}
				str += "\n";
				xx += yy;
			}
			return str;
		}

		public void ramSet(int adr, byte val) {
			byte vv = (byte)(val & 0x0f);
			int aa = adr >> 1;
			byte bb = _ram[aa];
			if ((adr & 1) != 0) {
				bb &= 0x0f;
				vv <<= 4;
			} else {
				bb &= 0x0f0;
			}
			_ram[aa] = (byte)(bb | vv);
		}

		public int getUcodeSize() {
			return _rom._ucode.length / 8;
		}
	}

	public Wang600_UcodeRom _rom;

	public Wang600_Ucode getUcode(int adr) {
		return _rom.fetchUcode(adr);
	}


	public void chgMode0() {
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode1() {
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode2() {} // never called on 600

	// "special key" is pressed (excl. STEP)
	public void pressCmd(int cmd) {
		cpu.jam = 0x1000 | cmd;
		if (trace) { // can only be if _dbg != null
			_dbg.warp("Key Jam", cmd, 0);
		}
		// needs other side-effects... display? clear key buffer?
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	// I/O protocol ("I/O" and "Group 1/2"):
	// Calc -> Device
	//	Calc loads KA,KB.
	//	Calc does MOP15, pulses GISO or TKWS (depends on IOB).
	// Device -> Calc
	//	Calc does RESET, GKBD goes OFF (clear-to-send).
	//	setup GKA,GKB and pulse GISN.
	//	GKBD (KBD3) is set ON ("do not send").
	// IOC (I/O Command, not GROUP 1/2)
	// IOB=2 (command phase):
	// 	Calc sends cmd (Calc -> Device)
	//	Device acks by sending 0 (Device -> Calc)
	// IOB=3 (data phase), cmd write:
	// 	Calc sends data (Calc -> Device)
	//	Device acks by sending 0 (Device -> Calc)
	// IOB=3 (data phase), cmd read:
	//	Device sends data (Device -> Calc)
	//	Calculator acks by sending 0 (Calc -> Device)
	// end of command ('length' bytes sent/recvd):
	//	Device sends status/error (Device -> Calc)
	//	Calculator acks (IOB=0)
	// GROUP I/O:
	//	Calc sends next code with IOB=4 or IOB=5.
	//	Device sends 0 or more characters(?)...
	//	Device sends GO (or ...?)

	public void ackIO(int iob) {
		// do some validation on iob?
		setKaKb(0);
	}

	public void replyIO(int iob, int rep) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		if (rep == Wang_GroupIODevice.GO) {
			rep = 0x83; // GO
		} else if (rep == Wang_GroupIODevice.START) {
			rep = 0x92; // ALPHA
		} else if (rep == Wang_GroupIODevice.END) {
			rep = 0x22; // end alpha
		} else if (rep == Wang_GroupIODevice.EOT) {
			rep = 0xa0; // f(0)
		} else if (rep == Wang_GroupIODevice.DP) {
			rep = 0x0a; // Decimal Point
		} else if (rep == Wang_GroupIODevice.CHG_SIGN) {
			rep = 0x0c; // Change Sign (make negative)
		} else if (rep >= Wang_GroupIODevice.E0 && rep <= Wang_GroupIODevice.E9) {
			rep = 0x00 | (rep - Wang_GroupIODevice.E0); // Digit
		} else if (rep == Wang_GroupIODevice.SET_EXP) {
			rep = 0x0b;
		} else if (rep == Wang_GroupIODevice.CLR_DSP) {
			rep = 0x0f;
		} else if (rep >= Wang_GroupIODevice.SR0 && rep < Wang_GroupIODevice.SREND) {
			rep = 0xa0 | (rep - Wang_GroupIODevice.SR0);
		}
		setKaKb(rep);
		// this needs to be done differently, if at all
		// GKBD pin on connector should reflect KBD going off.
		// KBD3 (GKBD) active prevents device sending data,
		// and is implied off by do_ack() and set by GISN.
		// KBD3 is also on when STEP is pressed (but not in ioc).
		if ((iob & ~1) == 2) {
			Wang600.M630.do_ack(iob);
		} else if (_cn36 != null) {
			_cn36.do_ack(iob);
		}
	}

	java.util.concurrent.LinkedBlockingDeque<Integer> keyCodes;

	public void setKaKb(int key) {
		cpu.setKaKb(key);
		keyCodes.add(key);
	}

	// This is also used to wakeup the simulator (key < 0)
	public void pressKey(int key) {
		if (key < 0) {
			keyCodes.add(key);
			return;
		}
//fprintf(stderr,"%03x: key down %02x (%s)\n", pc, key, z2);
//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
		if (cpu.kbl || cpu.z2) return;
		setKaKb(key);
		// needs other side-effects... display?
		do_blanking(); // yes?
	}

	private Wang_DebugConsole _dbg;

	public Wang600_Simulator(boolean dbg, boolean stop) {
		if (dbg) {
			_dbg = new Wang_DebugConsole(new Wang600_Debugger());
		} else {
			_dbg = null;
		}
		memsize = 2048; // could be based on Model (2TP, 6TP, 14TP, ...)
		memmask = 0xfff; // nibble address, not byte addr
		String model = Wang_UI.getProperties().getProperty("wang600_model");
		if (model == null) {
			model = "600-14TP";
			Wang_UI.getProperties().setProperty("wang600_model", model);
		}
		if (model.equals("600-14TP")) {
			// memsize already set
		} else if (model.equals("600-6TP")) {
			memsize = 1024;
			memmask = 0x7ff;
		} else if (model.equals("600-2TP")) {
			memsize = 512;
			memmask = 0x3ff;
		}
		String romfile = Wang_UI.getProperties().getProperty("wang600_ucode");
		if (romfile == null) {
			romfile = "wang600.rom";
		}
		java.io.InputStream rom = this.getClass().getResourceAsStream(romfile);
		if (rom == null) {
			try {
				rom = new FileInputStream(romfile);
			} catch(Exception ee) {
				Wang_UI.fatal("Opening microcode", ee.getMessage());
			}
		}
		_rom = new Wang600_UcodeRom(rom, memsize);
		_ram = new byte[memsize];

		_xromFile = Wang_UI.getProperties().getFile("wang600_rom_image", true, Wang_UI.getDir());
		loadXRom(_xromFile);
		// For debugging, allow the RAM image to be set to something
		String ram = Wang_UI.getProperties().getProperty("wang600_ram_image");
		if (ram != null) try {
			FileInputStream f = new FileInputStream(ram);
			f.read(_ram);
			f.close();
		} catch (Exception ee) {}

		disp = new short[16];
		keyCodes = new java.util.concurrent.LinkedBlockingDeque<Integer>();
		run_sim = !stop;
		cylimit = Long.MAX_VALUE;
		trace = false;
		trc_cycles = false;
		trc_raw = false;
		trc_fp = null;

		cpu = new Wang600_CPU(_rom._ucode, _ram, _xrom, this);
		Thread t = new Thread(this);
		t.start();
	}

	public void debugIntr() {
		if (_dbg != null) {
			// might be sleeping, so need to wake up...
			run_sim = false;
			keyCodes.addFirst(-1);
		}
	}

	private short[] disp;
	int good;
	int lastx;

	// CN-36 "Input" devices (Group 1/2 I/O Protocol)
	private Wang_GroupIODevice _cn36;	// current active device

	private void refresh(boolean canSleep) {
		byte n = cpu.n;
		short x = (short)((n << 4) | cpu.rb);
		if (disp[n] != x) {
			disp[n] = x;
			good = 0;
		}
		if (++lastx >= 16) {
			lastx = 0;
			++good;
			Wang600.Disp.do_display(disp);
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
		Arrays.fill(disp, (short)-1);
		Wang600.Disp.do_blanking();
	}

	//////////////////////////////
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
	public void display_check(boolean mr) {
		if (!_rom.std) {
			if (mr) refresh(false);
		}
		// this might sleep until UI event...
		if (cpu.pc == 0x252) {
		}
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if (cpu.pc == 0x51c) {	// display refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Display Refresh", 0x51f, 272);
			} else {
				cpu.next = 0x51f;	// update some regs too?
				cpu.cycles += 272;
			}
			refresh(cylimit == Long.MAX_VALUE);
		// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
		} else if (cpu.pc == 0x5c0) {	// alpha-stop refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Alpha-Stop", 0x5c3, 272);
			} else {
				cpu.next = 0x5c3;
				cpu.cycles += 272;
			}
			refresh(false);
		} else if (cpu.pc == 0x5c6) {	// alpha-stop done... "return"...
			if (cpu.next == 0x27b) { // alpha-stop in running program...
				if (trace) { // can only be if _dbg != null
					_dbg.warp("Alpha-Stop", -1, -1);
				}
				// observed 211975 cycles or about 0.53 second
				try {
					Thread.sleep(530);
				} catch(Exception ee) {
				}
			}
			do_blanking();
		}
	}
	public void tape_record(byte to_byte) {
		Wang600.Tape.tape_record(to_byte);
	}
	public int tape_play() {
		return Wang600.Tape.tape_play();
	}
	public void tape_on(int wr) {
		Wang600.Tape.tape_on(wr);
	}
	public void tape_off(int wr) {
		Wang600.Tape.tape_off(0);
	}
	public void dev_out(byte iob, byte c) {
		if (iob == 0) {
			_cn36 = null;
			if (Wang600.CN24 != null) {
				Wang600.CN24.reset();
			}
			Wang600.M630.reset();
			Wang_CN36_Bus.resetCN36();
		} else if (iob == 1) { // CN24 output only, 6 bits
			c &= 0x3f;
			if (Wang600.CN24 != null) {
				Wang600.CN24.do_cn24(c);
			}
		} else if (iob == 2 || iob == 3) { // CN36 Model 630
			Wang600.M630.do_dev(iob, c);
		} else if (iob == 4 || iob == 5) { // CN36 Group 1/2 devices
			if (_cn36 != null) {
				// All known devices are ACK only
				_cn36.do_ack(iob);
			} else {
				_cn36 = Wang_CN36_Bus.startCN36(iob, (c & 0x0ff));
			}
		}
	}
	public int getMode0(boolean clear) {
		int g = Wang600.Kbd.getMode0(clear);
		// clear 0010 if glrn?
		if (_cn36 != null) {
			g |= (byte)((_cn36.getGLRN() & 1) << 2);
		}
		return g;
	}
	public int getMode1(boolean clear) {
		return Wang600.Kbd.getMode1(clear);
	}
	public void do_printer(int x, byte pr_drum) {
		Wang600.Prt.do_printer(x, pr_drum);
	}
	public void do_line() {
		Wang600.Prt.do_line();
	}
	public void setOv(byte on) {
		Wang600.Disp.setOv(on);
	}
	public void setErr(byte on) {
		Wang600.Disp.setErr(on);
	}
	//////////////////////////////

	public void run() {
		// Run the simulator...
		boolean debug = (_dbg != null);
		int rc = 0;
		do {
			if (debug && !run_sim) {
				System.out.format("break at %03x %d\n", cpu.pc, cpu.cycles);
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
		Wang_UI.fatal("Wang600 Core", "Simulation error");
	}
}
