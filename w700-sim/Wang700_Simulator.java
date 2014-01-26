// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang700_Simulator.java,v 1.12 2014/01/26 14:52:57 drmiller Exp $

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

// Implements the Wang700 hardware. Does not provide any debug/trace support.

class Wang700_Simulator
	implements Wang_Core
{
	final String ident = "$Id: Wang700_Simulator.java,v 1.12 2014/01/26 14:52:57 drmiller Exp $";
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
	byte ra;
	byte rb;
	byte gioa;
	byte giob;
	byte iob;

	// status flags (1 bit)
	byte zo;
	byte cc;
	byte sc;
	byte q;
	byte kbd;
	byte ov;
	byte err;

	// simulator (no direct h/w relation)
	int jam;
	int next;
	int last;
	int pc;
	long cycles;
	long cylimit;
	boolean run_sim;

	boolean trace;
	boolean trc_cycles;
	boolean trc_raw;
	FileOutputStream trc_fp;

	static final int D10_FP = 0x01;
	static final int D11_LST_L_P = 0x02;
	static final int D12_LRN_L_P = 0x04;
	static final int D13_STEP = 0x08;

	public byte[] _ram;

	public JMenuItem getXRomMenu(int key) {
		return new JMenuItem("Not Used", key);
	}

	public void setXRom(byte[] img) {
		if (img.length == 0) {}
	}

	public void pickXRomFile(JMenuItem m) {
		if (m == null) {}
	}

	public class Wang700_Ucode {
		public byte jl;
		public byte jh;
		public int jad;
		public byte st;
		public byte kk;
		public byte mop;
		public byte bd;
		public byte bc;
		public byte ac;
		public byte aop;
		public byte zo;
		public byte bi;
		public byte ai;
		public boolean brkpt;

		public Wang700_Ucode(byte[] instr) {
			// "LE", i.e. "jl" in byte[0]
			jl = (byte)((instr[0] >> 1) & 0x07);
			jh = (byte)((instr[0] >> 4) & 0x07);
			jad = (((instr[1] & 0x00ff) << 1) | ((instr[0] >> 7) & 1));
			st = (byte)(instr[2] & 0x0f);
			kk = (byte)((instr[2] >> 4) & 0x0f);
			mop = (byte)(instr[3] & 0x0f);
			bd = (byte)((instr[3] >> 4) & 1);
			bc = (byte)((instr[3] >> 5) & 0x03);
			ac = (byte)((instr[3] >> 7) & 1);
			aop = (byte)(instr[4] & 0x07);
			zo = (byte)((instr[4] >> 3) & 0x07);
			bi = (byte)(((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2));
			ai = (byte)((instr[5] >> 1) & 0x07);
			brkpt = ((instr[7] & 1) != 0);
		}
	}

	public class Wang700_UcodeRom {
		public byte[] _ucode; // raw ucode from file, 64-bit words
		// right now, the only override is for mem size, so just hardcode
		// all that.

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

					// patch instructions...
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
				iob = (byte)(val & 0x07);
			} else if (reg.equalsIgnoreCase("ov")) {
				ov = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("err")) {
				err = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("pc")) {
				pc = val & 0x7ff;
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
			Wang700_Simulator.Wang700_Ucode uu = getUcode(adr);
			String stack = new String();;
			int k = uu.kk;
			int nxt = uu.jad << 2;
			if (uu.jh < 2) {
				nxt |= (uu.jh << 1);
			}
			if (uu.jl < 2) {
				nxt |= (uu.jl << 0);
			}
			stack += "jump";
			stack += String.format(" %03x", nxt);
			if (uu.jh >= 2 || uu.jl >= 2) {
				stack += "[";
				switch(uu.jh) {
				case 2: stack += "S<1>"; break;
				case 3: stack += "S<3>"; break;
				case 4: stack += "OV"; break;
				case 5: stack += "CC"; break;
				case 6: stack += "KBD"; break;
				case 7: stack += "{7}"; break;
				}
				stack += ":";
				switch(uu.jl) {
				case 2: stack += "S<0>"; break;
				case 3: stack += "S<2>"; break;
				case 4: stack += "Zo"; break;
				case 5: stack += "Q"; break;
				case 6: stack += "SC"; break;
				case 7: stack += "{7}"; break;
				}
				stack += "]";
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
			case 3: g = "0?"; break;
			case 4: g = "KA"; break;
			case 5: g = "KB"; break;
			case 6: g = "CA"; break;
			case 7: g = "CB"; break;
			default: g = ""; break;
			}

			if (uu.ac == 0) h = "0"; // "15"? "0"? ???
			String gx = (uu.bd != 0 ? "9" : "15");
			switch(uu.bc) {
			case 0: g = "0"; break;
			case 1: break;
			case 2: g = gx; break;
			case 3: g = "(" + gx + "-" + g + ")"; break;
			}

			String ops = "+++++&^+";
			String alu = new String();
			if (uu.aop == 7) {
				alu = "SC >> ";
			}
			alu += h + " " + ops.substring(uu.aop, uu.aop + 1) + " " + g;
			switch (uu.aop) {
			case 1:
			case 4:
				alu += " " + ops.substring(uu.aop, uu.aop + 1) + " 1";
				break;
			case 3:
				alu += " " + ops.substring(uu.aop, uu.aop + 1) + " SC";
				break;
			}
			if (uu.bd != 0) {
				alu = "BCD(" + alu + ")";
			}
			alu += " ->[Zo";
			if (uu.aop == 7) {
				alu += ",CC] >> SC";
			} else {
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
			// P9 is where this result gets stored/computed.

			// P4
			String mp4 = null;
			if (uu.mop >= 2 && uu.mop <= 5) {
				if (uu.mop >= 4) {
					mp4 = String.format("L=15,M=%d,N=V", k);
				} else {
					mp4 = "L=T,M=U,N=V";
				}
			}

			// P4-5
			String mp45 = null;
			switch(uu.mop) {
			case 10: mp45 = "KB<0>=Dot"; break;
			case 11: mp45 = "Din=KB<0>"; break;
			case 12:
				mp45 = "TMR=1,";
				mp45 += ((uu.bi & 1) != 0 ? "WR" : "RD");
				break;
			case 13: mp45 = "TMR=0"; break;
			}

			// P10
			String stp10 = null;
			if (uu.st >=1 && uu.st <= 8) {
				stp10 = String.format("S<%d>=%d", (uu.st - 1) & 3, ((uu.st - 1) >> 2) ^ 1);
			} else {
				switch(uu.st) {
				case 0: /* sprintf(stp10, "NOP"); */ break;
				case 9: stp10 = "RESET"; break;
				case 10: stp10 = "S<0>=!Z"; break;
				case 11: stp10 = "S<1>=Z"; break;
				case 12: stp10 = "OV=1"; break;
				case 13: stp10 = "S=0"; break;
				case 14: stp10 = "ERR=1"; break;
				}
			}

			// P9
			String targ = new String();
			switch(uu.zo) {
			case 0:	targ += "S"; break;
			case 1:	targ += "T"; break;
			case 2:	targ += "U"; break;
			case 3:	targ += "V"; break;
			case 4:	targ += "KA"; break;
			case 5:	targ += "KB"; break;
			case 6:	targ += "CA"; break;
			case 7:	targ += "CB"; break;
			}
			if (targ.length() > 0) {
				targ += " = ";
			}

			// P5-6
			String mp56 = null;
			switch(uu.mop) {
			case 7: mp56 = "IOB=KB<0:2>"; break;
			case 14: mp56 = "GIOA,GIOB=KA,KB"; break;
			}

			// P9
			String opA = null;
			switch(uu.mop) {
			case 0:	opA = "mem(LMN) = RA=alu,RB"; break;
			case 1:	opA = "mem(LMN) = RA,RB=alu"; break;
			case 2:	opA = "CA,CB=RA,RB = mem(LMN)"; break;
			case 3:	opA = "RA,RB = mem(LMN)"; break;
			case 4:	opA = "CA,CB=RA,RB = mem(LMN)"; break;
			case 5:	opA = "RA,RB = mem(LMN)"; break;
			case 6:	opA = "KB<0>=RBS"; break;
			case 7:	break; // done at P5-6
			case 8:	break;
			case 9:
				if (uu.aop == 7) {
					opA = "Q=SC";
				} else {
					opA = "Q=CC";
				}
				break;
			case 10:
			case 11:
			case 12:
			case 13:
				break; // done at P4-5
			case 14: break; // done at P5-6
			case 15: opA = "{15}"; break;
			}

			String buf = new String();
			if (mp4 != null) {
				if (buf.length() > 0) buf += "; ";
				buf += mp4;
			}
			if (mp45 != null) {
				if (buf.length() > 0) buf += "; ";
				buf += mp45;
			}
			if (mp56 != null) {
				if (buf.length() > 0) buf += "; ";
				buf += mp56;
			}
			if (buf.length() > 0) buf += "; ";
			buf += targ + alu;
			if (opA != null) {
				buf += "; " + opA;
			}
			if (stp10 != null) {
				buf += "; " + stp10;
			}
			if (stack.length() > 0) {
				buf += "; " + stack;
			}
			if (raw) {
				buf = String.format("[%x%x%x%x%x%x%x%x%x%x%03x%x%x] ",
					uu.ai, uu.bi, uu.zo, uu.aop, uu.ac, uu.bc, uu.bd,
					uu.mop, uu.kk, uu.st,
					uu.jad << 2, uu.jh, uu.jl) + buf;
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
			String str = new String();
			str += String.format("T = %01x U = %01x V = %01x CA = %01x CB = %01x\n",
				t, u, v, ca, cb);
			str += String.format("S = %01x Zo = %d CC = %d SC = %d Q = %d\n",
				s, zo, cc, sc, q);
			str += String.format("KA = %01x KB = %01x GIOA = %01x GIOB = %01x IOB = %01x\n",
				ka, kb, gioa, giob, iob);
			str += String.format("L = %01x M = %01x N = %01x RA = %01x RB = %01x\n",
				l, m, n, ra, rb);
			return str;
		}

		public String getMachine() {
			String str = String.format("d1=%01x", Wang700.Kbd.getMode0(false));
			if (ov != 0) str += "|Prog Err";
			if (err != 0) str += "|Mach Err";
			if (keyCodes.size() > 0) str += "|Key Pressed";
			return str;
		}

		public void dup() {
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
			str += String.format("%03x: [%03x] ",
				pc, next);
			str += String.format("%01x %01x %01x %01x %01x [",
				t, u, v, ca, cb);
			if (zo != 0) str += "Z"; else str += "z";
			if (cc != 0) str += "I"; else str += "i";
			if (sc != 0) str += "C"; else str += "c";
			if (q != 0) str += "Q"; else str += "q";
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
		good = 0;
		do_blanking();
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode1() {} // never called on 700

	public void chgMode2() {} // never called on 700

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
		if (rep == Wang_InputDevice.GO) {
			rep = 0x5e; // GO
		} else if (rep == Wang_InputDevice.START) {
			rep = 0x4c; // WRITE ALPHA
		} else if (rep == Wang_InputDevice.END) {
			rep = 0x4d; // END ALPHA
		} else if (rep == Wang_InputDevice.EOT) {
			rep = 0x00; // SR 0000
		} else if (rep == Wang_InputDevice.DP) {
			rep = 0x7c; // Decimal Point
		} else if (rep == Wang_InputDevice.CHG_SIGN) {
			rep = 0x7b; // Decimal Point
		} else if (rep >= Wang_InputDevice.E0 && rep <= Wang_InputDevice.E9) {
			rep = 0x70 | (rep - Wang_InputDevice.E0); // Digit
		} else if (rep == Wang_InputDevice.SET_EXP) {
			rep = 0x7a;
		} else if (rep == Wang_InputDevice.CLR_DSP) {
			rep = 0x7f;	// CLEAR X
		} else if (rep >= Wang_InputDevice.SR0 && rep < Wang_InputDevice.SREND) {
			rep = 0x00 | (rep - Wang_InputDevice.SR0);
		}
		pressKey(rep);
	}

	java.util.concurrent.LinkedBlockingDeque<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
		// needs other side-effects... display?
	}

	private Wang_DebugConsole _dbg;

	public Wang700_Simulator(boolean dbg, boolean stop) {
		if (dbg) {
			_dbg = new Wang_DebugConsole(new Wang700_Debugger());
		} else {
			_dbg = null;
		}
		// at some point, get these from properties...
		int memsize = 2048;
		// might need to search for possible ucode versions???
		String romfile = "wang720c.rom";
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

		dispx = new short[16];
		dispy = new short[16];
		odd_parity = new byte[] { 1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1 };
		keyCodes = new java.util.concurrent.LinkedBlockingDeque<Integer>();
		run_sim = !stop;
		cylimit = Long.MAX_VALUE;
		trace = false;
		trc_cycles = false;
		trc_raw = false;
		trc_fp = null;
		// On real machines, did not always happen that power-on asserted PRIME...
		pc = 0x000;	// force PRIME on power-up...
		l = 0;
		m = 0;
		n = 0;

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

	private byte even_parity8(byte data) {
		byte p = odd_parity[data & 0x0f];
		p ^= odd_parity[(data >> 4) & 0x0f];
		return p;
	}

	byte to_last;
	byte to_data;
	int to_bitc;
	int to_sigc;

	private void tape_write(int dat) {
		to_last <<= 1;
		to_last |= dat;
		++to_sigc;
		if ((to_sigc & 0x03) != 0) return;
		byte bit = 0; 
		byte h = (byte)(to_last & 0x0f);
		if (h == 0x05) bit = 1;
		if (++to_bitc == 9) {
			// ignore parity bit...
			Wang700.Tape.tape_record(to_data);
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

	private boolean do_repc() {
		return (cycles < ti_repc);
	}
	private boolean do_sigc() {
		// can only arrive here if cycles >= ti_repc...
		while (ti_sigc > 0) {
			--ti_sigc;
			ti_bit = (byte)(ti_last & 1);
			ti_last >>= 1;
			// must maintain real-time aspect...
			ti_repc += 390;	// very sensitive? 390/391
			if (do_repc()) {
				return true;
			}
		}
		return false;
	}
	private boolean do_bitc() {
		if (ti_bitc > 0) {
			--ti_bitc;
			int mask = (1 << ti_bitc);
			if ((ti_data & mask) != 0) {
				ti_last = 0x07;	// lsb first out...
			} else {
				ti_last = 0x01;	// lsb first out...
			}
			ti_sigc = 4;
			if (do_sigc()) {
				return true;
			}
		}
		return false;
	}

	private int tape_read() {
		// wait for TD 0->1
		// delay 815 cycles
		// sample for 170 cycles (sample TD at end of loop)
		// delay 353 cycles
		// wait up to 800? cycles for TD 0->1
		//         __    __
		// "1" = _|  |__|  |_ (each "bit" is 390/391 cycles)
		//         __
		// "0" = _|  |_______
		//
		if (do_repc()) {
			return ti_bit;
		}
		// might have been a lot of elapsed time since last call,
		// so have to account for the extra, possibly skipping bits...
		// i.e. must keep "real time" representation of bit stream.
		// This is because the '700 uses delay loops that do not call
		// tape_read().
		if (do_sigc()) {
			return ti_bit;
		}
		if (do_bitc()) {
			return ti_bit;
		}
		int ti = Wang700.Tape.tape_play();
		if (ti < 0) { // EOF
			ti_bit = 0; // "dead air"...
			// relative or absolute?
			//ti_repc += 1000; // how long is needed?
			ti_repc = cycles + 1000; // how long is needed?
			return ti_bit;
		} else {
			if ((ti & 0x00ff00) != 0) {
				byte b1 = (byte)((ti >> 8) & 0x0ff);
				byte b2 = (byte)(ti & 0x0ff);
				ti_data = ((b1 << 1) | even_parity8(b1)) << 9;
				ti_data |= (b2 << 1) | even_parity8(b2);
				ti_bitc = 18;
			} else {
				ti_data = (ti << 1) | even_parity8((byte)ti);
				ti_bitc = 9;
			}
			if (do_bitc()) { // must always return true?
				return ti_bit;
			}
		}
		return 0; // run-out the clock...
	}

	private void tape_on(int wr) {
		Wang700.Tape.tape_on(wr);
		if (wr == 0) {
			ti_bit = 0;
			ti_last = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_repc = cycles + 10; // how much time before it starts looking?
		} else {
			to_last = 0;
			to_data = 0;
			to_bitc = 0;
			to_sigc = 0;
		}
	}

	private void tape_off() {
		Wang700.Tape.tape_off(0);
	}

	private void dev_reset() {
		_cn36 = null;
		if (Wang700.CN24 != null) {
			Wang700.CN24.reset();
		}
		Wang700.M730.reset();
		Wang_CN36_Bus.resetCN36();
	}

	private void dev_out() {
		byte c = (byte)((gioa << 4) | giob);
		if (iob == 1) { // CN24 output only, 6 bits
			c &= 0x3f;
			if (Wang700.CN24 != null) {
				Wang700.CN24.do_cn24(c);
			}
		} else if (iob == 2 || iob == 3) { // CN36 Model 630
try {
			Wang700.M730.do_dev(iob, c);
} catch (Exception ee) {
if (_dbg != null) {
	System.err.println(ee.getMessage());
	run_sim = false;
	return;
}
}
		} else if ((iob & 0x04) == 4) { // CN36 Group 1/2 devices
			if (_cn36 != null) {
				// All known devices are ACK only
				_cn36.do_ack(iob);
			} else {
				_cn36 = Wang_CN36_Bus.startCN36(iob, (c & 0x0ff));
			}
		}
	}

	private byte bin_add3_i(byte a, byte b, byte c) {
		byte _s = (byte)(a + b + c);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((_s & 0x10) != 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_add3_i(byte a, byte b, byte c) {
		byte _s = (byte)(a + b + c);
		byte _cc = 0;
		while (_s >= 10) {
			_s -= 10;
			_cc = 1;
		}
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		cc = _cc;
		return (byte)(_s & 0x0f);
	}

	private byte bin_add3_c(byte a, byte b, byte c) {
		byte _s = bin_add3_i(a, b, c);
		sc = cc;
		return (byte)(_s & 0x0f);
	}

	private byte bcd_add3_c(byte a, byte b, byte c) {
		byte _s = bcd_add3_i(a, b, c);
		sc = cc;
		return (byte)(_s & 0x0f);
	}

	private byte bin_shift3_c(byte a, byte b, byte c) {
		byte _s = bin_add3_i(a, b, (byte)0);
		_s |= (byte)(c << 4);
		sc = (byte)(_s & 1);
		_s >>= 1;
		return (byte)(_s & 0x0f);
	}

	private byte bcd_shift3_c(byte a, byte b, byte c) {
		byte _s = bcd_add3_i(a, b, (byte)0);
		s |= (byte)(c << 4);
		sc = (byte)(_s & 1);
		_s >>= 1;
		return (byte)(_s & 0x0f);
	}

	private byte bin_and2(byte a, byte b) {
		bin_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a & b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_and2(byte a, byte b) {
		bcd_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a & b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bin_xor2(byte a, byte b) {
		bin_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a ^ b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_xor2(byte a, byte b) {
		bcd_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a ^ b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private void rd_ram_i() {
		int adr = ((l & 0x0f) << 8) | ((m & 0x0f) << 4) | (n & 0x0f);
		adr &= 0x07ff;
		byte b = _ram[adr];
		_ram[adr] = 0; //core memory: destructive read
		ra = (byte)((b >> 4) & 0x0f);
		rb = (byte)(b & 0x0f);
	}

	private void wr_ram_i() {
		int adr = ((l & 0x0f) << 8) | ((m & 0x0f) << 4) | (n & 0x0f);
		adr &= 0x07ff;
		_ram[adr] = (byte)((ra << 4) | rb);
	}

	private short[] dispx;
	private short[] dispy;
	int good;
	int lastx;

	// CN-36 "Input" devices (Group 1/2 I/O Protocol)
	private Wang_InputDevice _cn36;	// current active device

	private void refresh(boolean canSleep) {
		short x = (short)(((s & 2) << 7) | (n << 4) | rb);
		short y = (short)((((s & 1) ^ 1) << 8) | (n << 4) | ra);
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
			if ((Wang700.Kbd.getMode0(false) & D12_LRN_L_P) == 0 &&
					(_cn36 == null || _cn36.getGLRN() == 0)) {
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
					if (k >= 0) {
						keyCodes.addFirst(k);
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

	private void display_check() {
		if (pc == 0x252) {
		}
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if ((pc & 0xffe) == 0x034) {	// display refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Display Refresh", 0x472, 431);
			} else {
				next = 0x472;	// update some regs too?
				cycles += 431;
			}
			refresh(cylimit == Long.MAX_VALUE);
		// 5ed: begin alpha-stop display-refresh delay loop... short-cut to 4ae...
		} else if (pc == 0x5ed) {	// alpha-stop refresh routine...
			if (trace) { // can only be if _dbg != null
				_dbg.warp("Alpha-Stop", 0x4ae, 513);
			} else {
				next = 0x4ae;
				cycles += 513;
			}
			refresh(false);
		} else if (pc == 0x4af) {	// alpha-stop done... "return"...
			if (next == 0x081) { // alpha-stop in running program...
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

	private int instr_exec() {
		Wang700_Ucode uu = _rom.fetchUcode(pc);
		int nxt;
		int ret = 0;

		if (uu.brkpt) {
			_rom.breakPoint(pc);
			run_sim = false;
			return 0;
		}

		// For conditional jump, these bits are latched early...
		byte br_s = s;
		byte br_sc = sc;
		byte br_q = q;
		byte br_k = uu.kk;
		nxt = uu.jad << 2;

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
			g = (byte)Wang700.Kbd.getMode0(true);
			if (_cn36 != null) {
				g |= (byte)((_cn36.getGLRN() & 1) << 2);
			}
			break;
		case 3: g = 0; break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		byte gx = (byte)(uu.bd != 0 ? 9 : 15);
		if (uu.ac == 0) h = 0;
		switch(uu.bc) {
		case 0: g = 0; break;
		case 1: break;
		case 2: g = gx; break;
		case 3: g = (byte)((gx - g) & 0x0f); break;
		}

		if (uu.bd != 0) {
			switch (uu.aop) {
			case 0:
				alu = bcd_add3_i(h, g, (byte)0);
				break;
			case 1:
				alu = bcd_add3_i(h, g, (byte)1);
				break;
			case 2:
				alu = bcd_add3_c(h, g, (byte)0);
				break;
			case 3:
				alu = bcd_add3_c(h, g, br_sc);
				break;
			case 4:
				alu = bcd_add3_c(h, g, (byte)1);
				break;
			case 5:
				alu = bcd_and2(h, g);
				break;
			case 6:
				alu = bcd_xor2(h, g);
				break;
			case 7:
				alu = bcd_shift3_c(h, g, br_sc);
				break;
			}
		} else {
			switch (uu.aop) {
			case 0:
				alu = bin_add3_i(h, g, (byte)0);
				break;
			case 1:
				alu = bin_add3_i(h, g, (byte)1);
				break;
			case 2:
				alu = bin_add3_c(h, g, (byte)0);
				break;
			case 3:
				alu = bin_add3_c(h, g, br_sc);
				break;
			case 4:
				alu = bin_add3_c(h, g, (byte)1);
				break;
			case 5:
				alu = bin_and2(h, g);
				break;
			case 6:
				alu = bin_xor2(h, g);
				break;
			case 7:
				alu = bin_shift3_c(h, g, br_sc);
				break;
			}
		}

		// Now we start changing machine state... must adhere
		// to designated P-clock cycles...

		// P4
		if (uu.mop >= 2 && uu.mop <= 5) {
			l = (uu.mop >= 4 ? (byte)15 : t);
			m = (uu.mop >= 4 ? br_k : u);
			n = v;
		}

		// P4-5
		switch(uu.mop) {
		case 10: kb = (byte)((kb & ~1) | tape_read()); break;
		case 11: tape_write(kb & 1); break;
		case 12: tape_on(uu.bi & 1); break;
		case 13: tape_off(); break;
		}

		// P5-6
		switch(uu.mop) {
		case 7:
			iob = (byte)(kb & 0x07);
			if (iob == 0) {
				dev_reset();
			}
			break;
		case 14: gioa = ka; giob = kb; dev_out(); break;
		}

		// P9
		switch(uu.zo) {
		case 0:	s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		case 4:	ka = alu; break;
		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		case 7:	cb = alu; break;
		}

		// P10
		switch(uu.st) {
		case 0: break;
		case 1: s |= 1; break;
		case 2: s |= 2; break;
		case 3: s |= 4; break;
		case 4: s |= 8; break;
		case 5: s &= ~1; break;
		case 6: s &= ~2; break;
		case 7: s &= ~4; break;
		case 8: s &= ~8; break;
		case 9: kbd = 0; break;
		case 10: s = (byte)((s & 0x0e) | (zo ^ 1)); break;
		case 11: s = (byte)((s & 0x0d) | (zo << 1)); break;
		case 12: ov = 1; Wang700.DispX.setOv(ov); break;
		case 13: s = 0; break;
		case 14: err = 1; Wang700.DispX.setErr(err); break;
		}

		// P9 (non-conflict with P10 ST ops?)
		switch(uu.mop) {
		case 0:	ra = alu; wr_ram_i(); break; // L,M,N setup at P5-6
		case 1:	rb = alu; wr_ram_i(); break; // L,M,N setup at P5-6
		case 2:	rd_ram_i(); ca = ra; cb = rb; break; // L,M,N setup at P5-6
		case 3:	rd_ram_i(); break; // L,M,N setup at P5-6
		case 4:	rd_ram_i(); ca = ra; cb = rb; break; // L,M,N setup at P5-6
		case 5:	rd_ram_i(); break; // L,M,N setup at P5-6
		case 6:	kb |= 1; break; // RBS (always "ready" for us)
		case 7:	break; // done at P5-6
		case 8:	break;
		case 9:	q = (uu.aop == 7 ? sc : cc); break;
		case 10:
		case 11:
		case 12:
		case 13:
		case 14:
			// done at P4-5 or P5-6
			break;
		case 15:
			ret = 2;
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		// P9
		switch(uu.jh) {
		case 0: nxt |= (0 << 1); break;
		case 1: nxt |= (1 << 1); break;
		case 2: nxt |= ((br_s & 2) >> 0); break;
		case 3: nxt |= ((br_s & 8) >> 2); break;
		case 4:
			nxt |= (ov << 1);
			ov = 0;
			Wang700.DispX.setOv(ov);
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
//System.err.format("Key pressed %02x\n", key);
				kbd = 1;
				ka = (byte)((key >> 4) & 0x0f);
				kb = (byte)(key & 0x0f);
				if ((iob & ~1) == 2) {
					Wang700.M730.do_ack(iob);
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
		case 7: ret = 3; break;
		}
		switch(uu.jl) {
		case 0: nxt |= (0 << 0); break;
		case 1: nxt |= (1 << 0); break;
		case 2: nxt |= ((br_s & 1) >> 0); break;
		case 3: nxt |= ((br_s & 4) >> 2); break;
		case 4: nxt |= (zo << 0); break;
		case 5: nxt |= (br_q << 0); break;
		case 6: nxt |= (br_sc << 0); break;
		case 7: ret = 5; break;
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
			Wang700.DispX.setOv(ov);
			if (next == 0) { // PRIME
				err = 0;
				Wang700.DispX.setErr(err);
			}
		}

		last = pc;
		pc = next;
		return ret;
	}

	public void run() {
		// Run the simulator...
		boolean debug = (_dbg != null);
		int rc = 0;
		do {
			if (debug && !run_sim) {
				System.out.format("break at %03x (from %03x) %d\n", pc, last, cycles);
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
		Wang_UI.fatal("Wang700 Core", "Simulation error");
	}
}
