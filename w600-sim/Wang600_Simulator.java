// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang600_Simulator.java,v 1.1 2013/11/20 16:22:37 drmiller Exp $

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

// Implements the Wang600 hardware. Does not provide any debug/trace support.

class Wang600_Simulator
	implements Wang_Core
{
	final String ident = "$Id: Wang600_Simulator.java,v 1.1 2013/11/20 16:22:37 drmiller Exp $";
	// CPU registers.
	// ucode accessible
	byte s;
	byte t;
	byte u;
	byte v;
	byte ca;
	byte cb;
	byte ka;
	byte kb;
	// internal hardware accessible
	byte l;
	byte m;
	byte n;
	byte rb;
	byte gioa;
	byte giob;
	byte iob;

	// status flags (1 bit)
	byte zo;
	byte cc;
	byte sc;
	byte kbd;
	byte ov;
	byte err;

	// ucode subroutine stack
	int stk1;
	int stk2;

	// simulator (no direct h/w relation)
	int jam;
	int next;
	int pc;
	long cycles;
	long cylimit;
	boolean run_sim;

	boolean trace;
	boolean trc_cycles;
	boolean trc_raw;
	FileOutputStream trc_fp;

	byte pr_drum;
	int pr_hammers;
	byte pr_tach;
	int pr_col;

	static final int D10_FP = 0x01;
	static final int D11_LST_L_P = 0x02;
	static final int D12_LRN_L_P = 0x04;
	static final int D13_STEP = 0x08;

	static final int D20_DEGREES = 0x01;
	static final int D21_PRT_ON = 0x02;

	public byte[] _ram;
	public byte[] _xrom;

	private File _xromFile;

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

	public class Wang600_Ucode {
		public byte jl;
		public byte jh;
		public int jad;
		public byte sub;
		public byte st;
		public byte kk;
		public byte mop;
		public byte bc;
		public byte ac;
		public byte aop;
		public byte zo;
		public byte bi;
		public byte ai;
		public boolean brkpt;

		public Wang600_Ucode(byte[] instr) {
			// "LE", i.e. "jl" in byte[0]
			jl = (byte)((instr[0] >> 2) & 0x07);
			jh = (byte)((instr[0] >> 5) & 0x07);
			jad = ((instr[1] & 0x00ff) | ((instr[2] & 1) << 8));
			sub = (byte)((instr[2] >> 1) & 1);
			st = (byte)((instr[2] >> 2) & 0x0f);
			kk = (byte)(((instr[2] >> 6) & 0x03) | ((instr[3] & 0x03) << 2));
			mop = (byte)((instr[3] >> 2) & 0x0f);
			bc = (byte)((instr[3] >> 6) & 1);
			ac = (byte)((instr[3] >> 7) & 1);
			aop = (byte)(instr[4] & 0x07);
			zo = (byte)((instr[4] >> 3) & 0x07);
			bi = (byte)(((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2));
			ai = (byte)((instr[5] >> 1) & 0x07);
			brkpt = ((instr[7] & 1) != 0);
		}
	}

	public class Wang600_UcodeRom {
		public byte[] _ucode; // raw ucode from file, 64-bit words
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

					// patch mem-size override instruction...
					int idx = 0x008 * 8;
					// 'memsize' is in bytes, Wang uses nibbles...
					byte kk = (byte)((((memsize << 1) - 1) >> 8) & 0x0f);
					_ucode[idx + 2] |= ((kk & 0x03) << 6);
					_ucode[idx + 3] |= ((kk >> 2) & 0x03);
				} else {
					Wang_UI.fatal("Loading microcode", "Wrong size");
				}
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
			return pc;
		}
		public int getReg(String reg) {
			if (reg.equalsIgnoreCase("s")) {
				return s;
			} else if (reg.equalsIgnoreCase("t")) {
				return t;
			} else if (reg.equalsIgnoreCase("u")) {
				return u;
			} else if (reg.equalsIgnoreCase("v")) {
				return v;
			} else if (reg.equalsIgnoreCase("ca")) {
				return ca;
			} else if (reg.equalsIgnoreCase("cb")) {
				return cb;
			} else if (reg.equalsIgnoreCase("ka")) {
				return ka;
			} else if (reg.equalsIgnoreCase("kb")) {
				return kb;
			} else if (reg.equalsIgnoreCase("gioa")) {
				return gioa;
			} else if (reg.equalsIgnoreCase("giob")) {
				return giob;
			} else if (reg.equalsIgnoreCase("iob")) {
				return iob;
			} else if (reg.equalsIgnoreCase("ov")) {
				return ov;
			} else if (reg.equalsIgnoreCase("err")) {
				return err;
			} else if (reg.equalsIgnoreCase("pc")) {
				return pc;
			} else if (reg.equalsIgnoreCase("stk1")) {
				return stk1;
			} else if (reg.equalsIgnoreCase("stk2")) {
				return stk2;
			} else {
				return -1;
			}
		}

		public int setReg(String reg, int val) {
			if (reg.equalsIgnoreCase("s")) {
				s = (byte)val;
			} else if (reg.equalsIgnoreCase("t")) {
				t = (byte)val;
			} else if (reg.equalsIgnoreCase("u")) {
				u = (byte)val;
			} else if (reg.equalsIgnoreCase("v")) {
				v = (byte)val;
			} else if (reg.equalsIgnoreCase("ca")) {
				ca = (byte)val;
			} else if (reg.equalsIgnoreCase("cb")) {
				cb = (byte)val;
			} else if (reg.equalsIgnoreCase("ka")) {
				ka = (byte)val;
			} else if (reg.equalsIgnoreCase("kb")) {
				kb = (byte)val;
			} else if (reg.equalsIgnoreCase("gioa")) {
				gioa = (byte)val;
			} else if (reg.equalsIgnoreCase("giob")) {
				giob = (byte)val;
			} else if (reg.equalsIgnoreCase("iob")) {
				iob = (byte)(val & 0x03);
			} else if (reg.equalsIgnoreCase("ov")) {
				ov = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("err")) {
				err = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("pc")) {
				pc = val & 0x7ff;
			} else if (reg.equalsIgnoreCase("stk1")) {
				stk1 = val & 0x7ff;
			} else if (reg.equalsIgnoreCase("stk2")) {
				stk2 = val & 0x7ff;
			} else {
				return -1;
			}
			return getReg(reg);
		}

		public long relCycleLimit(long num) {
			cylimit = cycles + num;
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
			return ((l & 0x0f) << 8) | ((m & 0x0f) << 4) | (n & 0x0f);
		}

		public long getCycles() {
			return cycles;
		}

		public String disas(int adr, boolean raw) {
			Wang600_Simulator.Wang600_Ucode uu = getUcode(adr);
			String stack = new String();;
			int k = uu.kk;
			int nxt = uu.jad << 2;
			if (uu.jh < 2) {
				nxt |= (uu.jh << 1);
			}
			if (uu.jl < 2) {
				nxt |= (uu.jl << 0);
			}
			if (uu.jl == 7) {
				stack += "return";
			} else {
				if (uu.sub != 0) {
					stack += "call";
				} else {
					stack += "jump";
				}
				stack += String.format(" %03x", nxt);
				if (uu.jh >= 2 || uu.jl >= 2) {
					stack += "[";
					switch(uu.jh) {
					case 2: stack += "S<1>"; break;
					case 3: stack += "S<3>"; break;
					case 4: stack += "OV"; break;
					case 5: stack += "CC"; break;
					case 6: stack += "KBD"; break;
					}
					stack += ":";
					switch(uu.jl) {
					case 2: stack += "S<0>"; break;
					case 3: stack += "S<2>"; break;
					case 4: stack += "Zo"; break;
					case 5: stack += "Q?"; break;
					case 6: stack += "SC"; break;
					case 7: stack += "1?"; break;
					}
					stack += "]";
				}
			}
			String h;
			switch(uu.ai) {
			case 0: h = "S"; break;
			case 1: h = "T"; break;
			case 2: h = "U"; break;
			case 3: h = "V"; break;
			case 4: h = "KA"; break;
			case 5: h = "KB"; break;
			case 6: h = "CA"; break;
			case 7: h = "CB"; break;
			default: h = ""; break;
			}
			String g;
			switch(uu.bi) {
			case 0: g = "0"; break;
			case 1: g = Integer.toString(k); break;
			case 2: g = "D1"; break;
			case 3: g = "D2"; break;
			case 4: g = "KA"; break;
			case 5: g = "KB"; break;
			case 6: g = "CA"; break;
			case 7: g = "CB"; break;
			default: g = ""; break;
			}

			if (uu.ac == 0) h = "0"; // "15"? "0"? ???
			String ops = "+++++&|$";
			if (uu.bc != 0) ops = "-----&^$";
			String alu;
			if (uu.aop == 7) {
				alu = "0";
			} else {
				alu = h + " " + ops.substring(uu.aop, uu.aop + 1) + " " + g;
				switch (uu.aop) {
				case 1:
				case 4:
					alu += " " + ops.substring(uu.aop, uu.aop + 1) + " 1";
					break;
				case 3:
					alu += " " + ops.substring(uu.aop, uu.aop + 1) + " SC";
					break;
				}
				alu += " ->[Zo";
				if (uu.aop < 5) {
					alu += ",CC";
					switch (uu.aop) {
					case 2:
					case 3:
					case 4:
						alu += ",SC";
						break;
					}
				}
				alu += "]";
			}
			String acc = null;
			String mach = null;
			if (uu.st >=1 && uu.st <= 8) {
				acc = String.format("S<%d>=%d", (uu.st - 1) & 3, ((uu.st - 1) >> 2) ^ 1);
			} else {
				switch(uu.st) {
				case 0: /* sprintf(mach, "NOP"); */ break;
				case 9: mach = "RESET"; break;
				case 10: acc = "S<0>=!Z"; break;
				case 11: acc = "S<1>=Z"; break;
				case 12: mach = "OV=1"; break;
				case 13: acc = "S=0"; break;
				case 14: mach = "ERR=1"; break;
				}
			}
			String targ = new String();
			if (targ.length() > 0 && uu.zo != 7) {	// always false???
				targ += " = ";
			}
			switch(uu.zo) {
			case 0:	if (uu.st == 15) targ += "S"; break;
			case 1:	targ += "T"; break;
			case 2:	targ += "U"; break;
			case 3:	targ += "V"; break;
			case 4:	targ += "KA"; break;
			case 5:	targ += "KB"; break;
			case 6:	targ += "CA"; break;
			}
			if (targ.length() > 0) {
				targ += " = ";
			}

			String opA = null;
			switch(uu.mop) {
			case 1:	opA = "mem(T,U,V) = CA"; break;
			case 2:	opA = String.format("mem(15,%d,V) = CA", k); break;
			case 3:	opA = String.format("mem(15,15,%d) = CA", k); break;
			case 4:	opA = "CA = mem(T,U,V), CB = rom(T,U,V)"; break;
			case 5:	opA = String.format("CA = mem(15,%d,V), CB = rom(15,%d,V)", k, k); break;
			case 6:	opA = String.format("CA = mem(15,15,%d), CB = rom(15,15,%d)", k, k); break;
			case 7:	opA = "KBP <<+ KB<0>"; break;
			case 8:	opA = "PPF=1"; break;
			case 9:	opA = "<A9>"; break;
			case 10:	opA = "KB<0> = MHG/MHO"; break;
			case 11:	opA = "WDT = KB<0>"; break;
			case 12:	opA = "KA=PC0-3, KB<3>=PC4, KB<1>=RBS"; break;
			case 13:	opA = "TMR=1(";
					if ((uu.bi & 1) != 0) opA += "WR";
					else opA += "RD";
					opA += ")";
					break;
			case 14:	opA = "TMR=0";
					if ((uu.bi & 1) != 0) opA += "(noreset)";
					break;
			case 15:	opA = "GIOA=KA, GIOB=KB, IOB=";
					opA += Integer.toString(k);
					break;
			}

			String buf = targ + alu;
			if (acc != null) {
				buf += "; " + acc;
			}
			if (mach != null) {
				buf += "; " + mach;
			}
			if (opA != null) {
				buf += "; " + opA;
			}
			if (stack.length() > 0) {
				buf += "; " + stack;
			}
			if (raw) {
				buf = String.format("[%x%x%x%x%x%x%x%x%x%x%03x%x%x] ",
					uu.ai, uu.bi, uu.zo, uu.aop, uu.ac, uu.bc, uu.mop, uu.kk, uu.st,
					uu.sub, uu.jad << 2, uu.jh, uu.jl) + buf;
			}
			return buf;
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
			String str = String.format("STK1 = %03x STK2 = %03x\n", stk1, stk2);
			str += String.format("T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
				t, u, v, ca, cb);
			str += String.format("S = %01x Zo = %d CC = %d SC = %d\n",
				s, zo, cc, sc);
			str += String.format("KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
				ka, kb, gioa, giob, iob);
			return str;
		}

		public String getMachine() {
			String str = String.format("d1=%01x|d2=%01x", Wang600.Kbd.getMode0(), Wang600.Kbd.getMode1());
			if (ov != 0) str += "|Prog Err";
			if (err != 0) str += "|Mach Err";
			if (keyCodes.size() > 0) str += "|Key Pressed";
			return str;
		}

		public void dup() {
			int xx = 0x100;
			int yy = _ram.length - 0x0a0;
			setXRom(Arrays.copyOfRange(_ram, xx, yy));
		}

		public void putWarp(String tag, int nxt, int cyc) throws Exception {
			String str = String.format("TRACE: %03x: %s", pc, tag);
			if (cyc > 0) {
				next = nxt;
				cycles += cyc;
				str += " Warp";
			} else if (nxt >= 0) {
				str += String.format(" PC %03x", nxt);
			} else {
				str += " Sleep";
			}
			str += String.format("... %d\n", cycles);
			if (trc_fp != null) {
				trc_fp.write(str.getBytes());
			} else {
				System.err.print(str);
			}
		}

		public void putTrace() throws Exception {
			String str = ": ";
			if (trc_cycles) {
				str += String.format("%9d ", cycles);
			}
			str += String.format("%03x: [%03x %03x %03x] ",
				pc, next, stk1, stk2);
			str += String.format("%01x %01x %01x %01x [",
				t, u, v, ca);
			if (zo != 0) str += "Z"; else str += "z";
			if (cc != 0) str += "I"; else str += "i";
			if (sc != 0) str += "C"; else str += "c";
			str += String.format("] %01x %01x %01x : ",
				s, ka, kb);
			str += disas(pc, trc_raw);
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

	public void pressCmd(int cmd) {
		jam = 0x1000 | cmd;
		if (trace) { // can only be if _dbg != null
			_dbg.warp("Key Jam", cmd, 0);
		}
		// needs other side-effects... display? clear key buffer?
		good = 0;
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void ackIO(int iob) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		pressKey(0);
	}

	public void replyIO(int iob, int rep) {
		// might need to separate from keyboard input, but hardware
		// doesn't (?)
		// do some validation on iob?
		pressKey(rep);
	}

	java.util.concurrent.LinkedBlockingDeque<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
		// needs other side-effects... display?
	}

	private Wang_DebugConsole _dbg;

	public Wang600_Simulator(boolean dbg, boolean stop) {
		if (dbg) {
			_dbg = new Wang_DebugConsole(new Wang600_Debugger());
		} else {
			_dbg = null;
		}
		// at some point, get these from properties...
		int memsize = 2048; // could be based on Model (2TP, 6TP, 14TP, ...)
		String romfile = "wang600.rom";
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

		pr_drum = 0;
		pr_hammers = 0;
		pr_tach = 0;
		pr_col = 0;
		disp = new short[16];
		odd_parity = new byte[] { 1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1 };
		keyCodes = new java.util.concurrent.LinkedBlockingDeque<Integer>();
		run_sim = !stop;
		cylimit = Long.MAX_VALUE;
		trace = false;
		trc_cycles = false;
		trc_raw = false;
		trc_fp = null;

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

	private byte[] odd_parity;

	byte to_last;
	byte to_data;
	byte to_byte;
	int to_nibc;
	int to_bitc;
	int to_sigc;

	private void tape_write(int dat) {
		to_last <<= 1;
		to_last |= dat;
		to_sigc ^= 1;
		if (to_sigc != 0) return;
		byte bit = 0; 
		byte h = (byte)(to_last & 0x03);
		if (h == 0x02 || h == 0x01) bit = 1;
		if (++to_bitc == 5) {
			to_nibc ^= 1;
			if (to_nibc != 0) {
				to_byte = (byte)((to_byte & 0x0f) | (to_data << 4));
			} else {
				to_byte = (byte)((to_byte & 0xf0) | to_data);
				Wang600.Tape.tape_record(to_byte);
			}
			to_data = 0;
			to_bitc = 0;
		} else {
			to_data <<= 1;
			to_data |= bit;
		}
	}

	byte ti_last;
	int ti_data;
	int ti_bitc;
	int ti_sigc;
	long ti_repc;
	byte ti_bit;

	private int do_repc() {
		return ti_bit;
	}
	private int do_sigc() {
		--ti_sigc;
		ti_bit = (byte)(ti_last & 1);
		ti_last >>= 1;
		ti_repc = cycles + 97;	// very sensitive...
		return do_repc();
	}
	private int do_bitc() {
		--ti_bitc;
		ti_data <<= 1;
		if ((ti_data & 0x400) != 0) {
			ti_last = 0x05;	// lsb first out...
		} else {
			ti_last = 0x01;	// lsb first out...
		}
		ti_sigc = 4;
		return do_sigc();
	}

	private int tape_read() {
		// wait for TD 0->1
		// delay 56 cycles
		// wait 220 cycles (sample TD for end of loop)
		// [15,15,6] ^= DL	; compute parity?
		// CY = 0 - DL		; CY = bit0
		// [15,15,5] <<= 1	; make space
		// [15,15,5] += CY	; insert new bit
		// ACC += 1		; count bits
		// wait up to 256 cycles for TD 0->1
		//         __    __
		// "1" = _|  |__|  |_
		//         __
		// "0" = _|  |_______
		//
		if (cycles < ti_repc) {
			return do_repc();
		}
		if (ti_sigc > 0) {
			return do_sigc();
		}
		if (ti_bitc > 0) {
			return do_bitc();
		}
		int ti = Wang600.Tape.tape_play();
		if (ti < 0) { // EOF
			ti_repc = cycles + 700;	// expects at least 650?
			ti_bit = 0;
			return do_repc();
		}
		int x = ((ti >> 4) & 0x0f);
		ti_data = (x << 1) | odd_parity[x];
		x = (ti & 0x0f);
		ti_data <<= 5;
		ti_data |= (x << 1) | odd_parity[x];
		ti_bitc = 10;
		return do_bitc();
	}

	private void tape_on(int wr) {
		Wang600.Tape.tape_on(wr);
		if (wr == 0) {
			ti_bit = 0;
			ti_last = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_repc = 0;
		}
	}

	private void tape_off() {
		Wang600.Tape.tape_off(0);
	}

	private void dev_out() {
		byte c = (byte)((gioa << 4) | giob);
		if (iob == 0) {
			_cn36 = null;
			if (Wang600.CN24 != null) {
				Wang600.CN24.reset();
			}
			Wang600.M630.reset();
			Wang_UI.resetCN36();
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
				_cn36 = Wang_UI.startCN36(iob, c);
			}
		}
	}

	private byte add3_i(byte a, byte b, byte c) {
		byte s = (byte)(a + b + c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte sub3_i(byte a, byte b, byte c) {
		byte s = (byte)(a - b - c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte and2(byte a, byte b) {
		byte s = (byte)(a & b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte or2(byte a, byte b) {
		byte s = (byte)(a | b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte xor2(byte a, byte b) {
		byte s = (byte)(a ^ b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte add3_c(byte a, byte b, byte c) {
		byte s = add3_i(a, b, c);
		sc = cc;
		return s;
	}

	private byte sub3_c(byte a, byte b, byte c) {
		byte s = sub3_i(a, b, c);
		sc = cc;
		return s;
	}

	private void printer_status() {
		// we don't want to do this unless it is really the
		// drum printer we're looking at... can't tell?
		if ((Wang600.Kbd.getMode1() & D21_PRT_ON) == 0) {
			// only if running program doesn't get here...
			// printer is off, tach will never pulse, so don't spin
			if (pc == 0x6db) {
				// sleep until key event... incl PRT ON...
			}
			return;
		}
		if (pr_tach != 0) {
			pr_col = 0;
			pr_drum = (byte)((pr_drum + 1) & 0x0f);
			pr_hammers = 0;
		}
		pr_tach ^= 0x08;
		ka = pr_drum;
		kb = pr_tach;
	}

	private void printer_hammers() {
		int x;
		int h;

		pr_hammers <<= 1;
		pr_hammers &= 0x0fffff;
		pr_hammers |= kb & 1;
		if (++pr_col >= 20) {
			h = pr_hammers;
			for (x = 0; h != 0; ++x) {
				if ((h & 1) != 0) {
					Wang600.Prt.do_printer(x, pr_drum);
				}
				h >>= 1;
			}
			pr_col = 0;
		}
	}

	private void printer_feed() {
		// now, actually print it...
		Wang600.Prt.do_line();
	}

	private void rd_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		//adr &= ram_mask;
		boolean odd = ((adr & 1) != 0);
		adr >>= 1;
		byte b = _ram[adr];
		if (odd) {
			b >>= 4;
		}
		rb = ca = (byte)(b & 0x0f);
		// ROM should always be >= RAM in size... always 2K...
		// (if present)
		if (_xrom != null && adr < _xrom.length) {
			b = _xrom[adr];
			if (odd) {
				b >>= 4;
			}
		} else {
			b = 0x0f;
		}
		cb = (byte)(b & 0x0f);
	}

	private void wr_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		//adr &= ram_mask;
		byte a = ca;
		byte b = _ram[adr >> 1];
		if ((adr & 1) != 0) {
			a <<= 4;
			b &= 0x0f;
		} else {
			b &= 0xf0;
		}
		_ram[adr >> 1] = (byte)(b | a);
	}

	private short[] disp;
	int good;
	int lastx;

	// CN-36 "Input" devices (Group 1/2 I/O Protocol)
	private Wang_InputDevice _cn36;	// current active device

	private void refresh(boolean canSleep) {
		short x = (short)((n << 4) | rb);
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
					if (k >= 0) {
						keyCodes.addFirst(k);
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

	private void display_check() {
		if (pc == 0x252) {
		}
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if (pc == 0x51c) {	// display refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Display Refresh", 0x51f, 272);
			} else {
				next = 0x51f;	// update some regs too?
				cycles += 272;
			}
			refresh(cylimit == Long.MAX_VALUE);
		// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
		} else if (pc == 0x5c0) {	// alpha-stop refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Alpha-Stop", 0x5c3, 272);
			} else {
				next = 0x5c3;
				cycles += 272;
			}
			refresh(false);
		} else if (pc == 0x5c6) {	// alpha-stop done... "return"...
			if (next == 0x27b) { // alpha-stop in running program...
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

	private int instr_exec() {
		Wang600_Ucode uu = _rom.fetchUcode(pc);
		int nxt;
		int rc = 0;

		if (uu.brkpt) {
			_rom.breakPoint(pc);
			run_sim = false;
			return 0;
		}

		// F==7 && J==0:
		//	PC <= STK1, STK1 <= PC, STK2 <= STK1
		//
		// F==7 && J==1:
		//	PC <= STK1, STK1 <= STK2, STK2 <= STK1
		//
		// F!=7 && J==0:
		//	PC <= NEXT**
		//
		// F!=7 && J==1:
		//	STK2 = STK1, STK1 <= PC, PC <= NEXT**
		//
		// For conditional jump/call, these bits are latched early...
		byte br_acc = s;
		byte br_c = sc;
		byte br_k = uu.kk;
		int opf7 = (uu.jl == 7 ? 1 : 0);
		if (opf7 != 0) {
			nxt = stk1 | 1;
			if (uu.sub != 0) {
				stk1 = stk2;
			} else {
				stk1 = stk2; // bugfix?
				//stk1 = pc;	// bad?
				// rc = 1;
			}
		} else {
			nxt = uu.jad << 2;
		}

		if (uu.mop >= 1 && uu.mop <= 6) {
			l = t;
			m = u;
			n = v;
		}

		byte g = 0;
		byte h = 0;
		switch(uu.ai) {
		case 0: h = s; break;
		case 1: h = t; break;
		case 2: h = u; break;
		case 3: h = v; break;
		case 4: h = ka; break;
		case 5: h = kb; break;
		case 6: h = ca; break;
		case 7: h = cb; break;
		}

		switch(uu.bi) {
		case 0: g = 0; break;
		case 1: g = br_k; break;
		case 2:
			g = (byte)Wang600.Kbd.getMode0();
			// clear 0010 if glrn?
			if (_cn36 != null) {
				g |= (byte)((_cn36.getGLRN() & 1) << 2);
			}
			break;
		case 3: g = (byte)(Wang600.Kbd.getMode1() ^ D20_DEGREES); break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		if (uu.ac == 0) h = 0; // "15"? "0"? ???
		switch (uu.aop) {
		case 0:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)0);
			else alu = add3_i(h, g, (byte)0);
			break;
		case 1:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)1);
			else alu = add3_i(h, g, (byte)1);
			break;
		case 2:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)0);
			else alu = add3_c(h, g, (byte)0);
			break;
		case 3:
			if (uu.bc != 0) alu = sub3_c(h, g, sc);
			else alu = add3_c(h, g, sc);
			break;
		case 4:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)1);
			else alu = add3_c(h, g, (byte)1);
			break;
		case 5:
			alu = and2(h, g);
			break;
		case 6:
			if (uu.bc != 0) alu = xor2(h, g);
			else alu = or2(h, g);
			break;
		case 7:
			// alu = 0;
			break;
		}

		switch(uu.zo) {
		case 0:	if (uu.st == 15) s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		case 4:	ka = alu; break;
		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		}

		switch(uu.st) {
		case 0:
			// nop
			break;
		case 1:
			s |= 1;
			break;
		case 2:
			s |= 2;
			break;
		case 3:
			s |= 4;
			break;
		case 4:
			s |= 8;
			break;
		case 5:
			s &= ~1;
			break;
		case 6:
			s &= ~2;
			break;
		case 7:
			s &= ~4;
			break;
		case 8:
			s &= ~8;
			break;
		case 9:
			// T.B.D. reset 6184...
	//fprintf(stderr, "%03x: res (%04x)\n", pc, key);
			kbd = 0;
			break;
		case 10:
			s = (byte)((s & 0x0e) | (zo ^ 1));
			break;
		case 11:
			s = (byte)((s & 0x0d) | (zo << 1));
			break;
		case 12:
			ov = 1;
			Wang600.Disp.setOv(ov);
			break;
		case 13:
			s = 0;
			break;
		case 14:
			err = 1;
			Wang600.Disp.setErr(err);
			break;
		}

		switch(uu.mop) {
		case 1:	wr_ram_i(l, m, n); break;
		case 2:	wr_ram_i((byte)15, br_k, n); break;
		case 3:	wr_ram_i((byte)15, (byte)15, br_k); break;
		case 4:	rd_ram_i(l, m, n); break;
		case 5:	rd_ram_i((byte)15, br_k, n); break;
		case 6:	rd_ram_i((byte)15, (byte)15, br_k); break;
		case 7:	printer_hammers(); break;
		case 8:	printer_feed(); break;
		case 9:	rc = 2; break;
		case 10:
			kb = (byte)((kb & ~1) | tape_read());
			break;
		case 11:
			tape_write(kb & 1);
			break;
		case 12:
			printer_status();
			// not just printer, but CN-24 as well...
			kb |= 2;
			break;
		case 13:
			tape_on(uu.bi & 1);
			break;
		case 14:
			tape_off(); // uu.bi & 1 affects this...
			break;
		case 15:
			gioa = ka;	// gioa = g;
			giob = kb;	// giob = h;
			iob = (byte)(br_k & 0x07);
			dev_out();
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		if (opf7 == 0) {
			if (uu.sub != 0) {
				stk2 = stk1;
				stk1 = pc;
			}
			switch(uu.jh) {
			case 0: nxt |= (0 << 1); break;
			case 1: nxt |= (1 << 1); break;
			case 2: nxt |= ((br_acc & 2) >> 0); break;
			case 3: nxt |= ((br_acc & 8) >> 2); break;
			case 4:
				nxt |= (ov << 1);
				ov = 0;
				Wang600.Disp.setOv(ov);
				break;
			case 5: nxt |= (cc << 1); break;
			case 6:
				int key = -1;
				if (keyCodes.size() > 0) {
					// might return -1 for wake-up only,
					// must ignore in that case.
					key = keyCodes.remove();
				}
				if (key >= 0) {
//fprintf(stderr,"%03x: chk pe\n", pc, key);
//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
					kbd = 1;
					ka = (byte)((key >> 4) & 0x0f);
					kb = (byte)(key & 0x0f);
					if ((iob & ~1) == 2) {
						Wang600.M630.do_ack(iob);
					} else if (_cn36 != null) {
						_cn36.do_ack(iob);
					}
				}
				nxt |= (kbd << 1);
				if (kbd != 0) {
					good = 0;
					kbd = 0;
					do_blanking();
				}
				break;
			case 7: rc = 3; break;
			}
			switch(uu.jl) {
			case 0: nxt |= (0 << 0); break;
			case 1: nxt |= (1 << 0); break;
			case 2: nxt |= ((br_acc & 1) >> 0); break;
			case 3: nxt |= ((br_acc & 4) >> 2); break;
			case 4: nxt |= (zo << 0); break;
			case 5: rc = 4; break;
			case 6: nxt |= (br_c << 0); break;
			case 7: rc = 5; break;
			}
		}

		++cycles;
		next = nxt;

		if (trace) { // can only be if _dbg != null
			_dbg.instr_trace();
		}

		// the following are called in specific order...
		// keyboard injection of next pc must override all, so is last.

		display_check();	// this might sleep until UI event...

		//sys->keyboard(sys, &key, 0);

		if (jam != 0) {
			next = jam & 0x0fff;
			jam = 0;
			ov = 0;
			Wang600.Disp.setOv(ov);
			if (next == 0) { // PRIME
				err = 0;
				Wang600.Disp.setErr(err);
			}
		}

		pc = next;
		return rc;
	}

	public void run() {
		// Run the simulator...
		boolean debug = (_dbg != null);
		int rc = 0;
		do {
			if (debug && !run_sim) {
				System.out.format("break at %03x %d\n", pc, cycles);
				while (debug && !run_sim) {
					rc = _dbg.command();
					if (rc != 0) {
						System.exit(0);
					}
				}
			}
			rc = instr_exec();
			if (rc != 0) {
				break;
			}
			if (debug && cycles >= cylimit) {
				// PC has NOT been executed...
				cylimit = Long.MAX_VALUE;
				run_sim = false;
			}
		} while (run_sim || debug);
		// not normally reached...
		Wang_UI.fatal("Wang600 Core", "Simulation error");
	}
}
