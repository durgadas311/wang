// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang1200_Simulator.java,v 1.4 2013/12/01 20:57:47 drmiller Exp $

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

// Implements the Wang1200 hardware. Does not provide any debug/trace support.

class Wang1200_Simulator
	implements Wang_Core
{
	final String ident = "$Id: Wang1200_Simulator.java,v 1.4 2013/12/01 20:57:47 drmiller Exp $";
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
	byte to;	// TILT
	byte ro;	// ROTATE

	// status flags (1 bit)
	byte zo;
	byte cc;
	byte sc;
	byte kbd;

	byte ls;	// L/S - Lock/Shift on keyboard

	// indicators
	byte ern;	// RECORD
	byte tmr;	// TAPE MOVING (right)
	byte tml;	// TAPE MOVING (left)
	byte eln;	// END OF DOCUMENT
	byte nan;	// NO ADJUST
	byte csl;	// CHAR / STOP
	byte shl;	// SEARCH
	byte skl;	// SKIP

	byte right;	// tape device select (left/right)
	byte tm;	// tape motor control
	byte rv;	// tape movement direction (reverse/forward)
	byte rc;	// tape record (enable)
	byte hl;	// tape seek control? (engage head while FF/RWD)
	byte din0;	// left/right tape write clock bit
	byte din1;	// left/right tape write data bit
	byte tck;	// left/right tape read clock bit
	byte dk;	// left/right tape read data bit
	byte lhs;	// tape head engage, left
	byte rhs;	// tape head engage, right

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

	static final int D10_RIGHT = 0x01;
	static final int D11_DOUBLE = 0x02;
	static final int D12_RECORD = 0x04;
	static final int D13_TRANSFER = 0x08;

	static final int D20_UNK = 0x01;

	public byte[] _ram;


	public JMenuItem getXRomMenu(int key) {
		if (key == 0) {}
		return null;
	}

	public void setXRom(byte[] img) {
		if (img == null) {}
	}

	public void pickXRomFile(JMenuItem m) {
		if (m == null) {}
	}

	public class Wang1200_Ucode {
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

		public Wang1200_Ucode(byte[] instr) {
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
		public byte[] ovr(int ai, int bi, int zo, int aop, int ac,
				int bc, int mop, int kk, int st, int sub,
				int jad, int jh, int jl) {
			byte[] b = new byte[8];
			b[0] = (byte)((jl << 2) | (jh << 5));
			b[1] = (byte)((jad >> 2) & 0x0ff);
			b[2] = (byte)(((jad >> 10) & 1) | (sub << 1) | (st << 2) | ((kk & 0x03) << 6));
			b[3] = (byte)(((kk >> 2) & 0x03) | (mop << 2) | (bc << 6) | (ac << 7));
			b[4] = (byte)((aop) | (zo << 3) | ((bi & 0x03) << 6));
			b[5] = (byte)(((bi >> 2) & 1) | (ai << 1));
			b[6] = 0;
			b[7] = 0;
			return b;
		}
	}

	public class Wang1200_UcodeRom {
		public byte[] _ucode; // raw ucode from file, 64-bit words
		// right now, the only override is for mem size, so just hardcode
		// all that.

		private void ovr(int adr, byte[] ovr) {
			int idx = adr * 8;
			for (int x = 0; x < 8; ++x) {
				_ucode[idx + x] = ovr[x];
			}
		}

		public Wang1200_UcodeRom(java.io.InputStream img) {
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

					// TODO: apply patches... or patch file?
// ugh, for stupid...
Wang1200_Ucode uu = fetchUcode(0x000);
//////////////////////////// ai bi zo aop ac bc mop  kk  st sub    jad jh jl
ovr(0x3d0, uu.ovr(0, 0, 0,  0, 0, 0,  0,  0,  0,  1, 0x5fc, 1, 0));
ovr(0x052, uu.ovr(0, 0, 0,  0, 0, 0,  0,  0,  0,  0, 0x058, 0, 3));
ovr(0x423, uu.ovr(6, 1, 0,  0, 1, 1,  0, 11,  0,  0, 0x424, 5, 4));
ovr(0x42f, uu.ovr(6, 1, 0,  0, 0, 0,  6,  4,  3,  0, 0x424, 7, 4));
ovr(0x506, uu.ovr(0, 0, 7,  1, 0, 0,  3, 12, 13,  1, 0x7fc, 1, 0));
ovr(0x558, uu.ovr(1, 6, 1,  0, 1, 0,  0,  0, 13,  1, 0x7fc, 1, 0));
ovr(0x5ec, uu.ovr(5, 1, 0,  6, 1, 1,  0, 11,  0,  0, 0x5ec, 1, 4));
ovr(0x6ee, uu.ovr(0, 1, 7,  0, 0, 0,  0,  2,  0,  0, 0x6c8, 0, 0));
ovr(0x7fe, uu.ovr(0, 1, 7,  6, 1, 0,  0,  3,  0,  0, 0x6c8, 0, 0));
ovr(0x44f, uu.ovr(6, 0, 6,  3, 1, 1,  0,  0,  0,  0, 0x450, 0, 1));
ovr(0x33c, uu.ovr(7, 7, 7,  6, 1, 0, 10, 15, 15,  1, 0x414, 0, 0));
ovr(0x33d, uu.ovr(7, 7, 7,  6, 1, 0, 10, 15, 15,  0, 0x034, 0, 0));

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

		public Wang1200_Ucode fetchUcode(int adr) {
			return new Wang1200_Ucode(fetchBytes(adr));
		}
	}

	class Wang1200_Debugger
		implements Wang_Debugger
	{
		public Wang1200_Debugger() {
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
			} else if (reg.equalsIgnoreCase("to")) {
				return to;
			} else if (reg.equalsIgnoreCase("ro")) {
				return ro;
			} else if (reg.equalsIgnoreCase("ern")) {
				return ern;
			} else if (reg.equalsIgnoreCase("tmr")) {
				return tmr;
			} else if (reg.equalsIgnoreCase("tml")) {
				return tml;
			} else if (reg.equalsIgnoreCase("eln")) {
				return eln;
			} else if (reg.equalsIgnoreCase("nan")) {
				return nan;
			} else if (reg.equalsIgnoreCase("csl")) {
				return csl;
			} else if (reg.equalsIgnoreCase("shl")) {
				return shl;
			} else if (reg.equalsIgnoreCase("skl")) {
				return skl;
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
			} else if (reg.equalsIgnoreCase("to")) {
				to = (byte)val;
			} else if (reg.equalsIgnoreCase("ro")) {
				ro = (byte)val;
			} else if (reg.equalsIgnoreCase("ern")) {
				ern = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("tmr")) {
				tmr = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("tml")) {
				tml = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("eln")) {
				eln = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("nan")) {
				nan = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("csl")) {
				csl = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("shl")) {
				shl = (byte)(val != 0 ? 1 : 0);
			} else if (reg.equalsIgnoreCase("skl")) {
				skl = (byte)(val != 0 ? 1 : 0);
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
			Wang1200_Simulator.Wang1200_Ucode uu = getUcode(adr);
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
			str += String.format("KA = %01x KB = %01x TO = %01x RO = %01x\n",
				ka, kb, to, ro);
			return str;
		}

		public String getMachine() {
			String str = String.format("d1=%01x|d2=%01x|d3=%01x",
				Wang1200.Kbd.getMode0(false),
				Wang1200.Kbd.getMode1(false),
				Wang1200.Kbd.getMode2(false));
			// indicators?
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
					byte bb = _ram[aa];
					str += String.format(" %02x", bb);
					++aa;
				}
				str += "\n";
				xx += yy;
			}
			return str;
		}

		public String romDump(int adr, int len) {
			if (adr == 0 || len == 0) {}
			return null;
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

	public Wang1200_UcodeRom _rom;

	public Wang1200_Ucode getUcode(int adr) {
		return _rom.fetchUcode(adr);
	}

	public void chgMode0() {
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode1() {
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void chgMode2() {
		keyCodes.addFirst(-1); // don't press a key - just wake up sleeper
	}

	public void pressCmd(int cmd) {
		jam = 0x1000 | cmd;
		if (trace) { // can only be if _dbg != null
			_dbg.warp("Key Jam", cmd, 0);
		}
		// needs other side-effects... display? clear key buffer?
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
		// might be a key with special side-effects (latched, illuminated),
		// might NOT be queued to microcode...
		if (key == Wang_Keys.ALT_KEY(1)) { // SKIP
			skl ^= 1;
			Wang1200.Kbd.setSKIP(skl != 0);
			return;
		} else if (key == Wang_Keys.ALT_KEY(2)) { // SEARCH
			shl ^= 1;
			Wang1200.Kbd.setSEARCH(shl != 0);
			key = 0x100 | (shl != 0 ? 0x42 : 0x52);
		}
		keyCodes.add(key);
	}

	private Wang_DebugConsole _dbg;

	public Wang1200_Simulator(boolean dbg, boolean stop) {
		if (dbg) {
			_dbg = new Wang_DebugConsole(new Wang1200_Debugger());
		} else {
			_dbg = null;
		}
		// at some point, get these from properties...
		int memsize = 256;
		String romfile = "wang1200.rom";
		java.io.InputStream rom = this.getClass().getResourceAsStream(romfile);
		if (rom == null) {
			try {
				rom = new FileInputStream(romfile);
			} catch(Exception ee) {
				Wang_UI.fatal("Opening microcode", ee.getMessage());
			}
		}
		_rom = new Wang1200_UcodeRom(rom);
		_ram = new byte[memsize];

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

	byte to_last;
	byte to_data;
	int to_bitc;

	private void tape_write() {
		byte curr = (byte)((din1 << 1) | din0);
		byte chg = (byte)(curr ^ to_last);
		to_last = curr;
		if (chg != 0) {
			--chg;
			to_data <<= 1;
			to_data |= chg;
			if (++to_bitc >= 8) {
				if (right != 0) {
					Wang1200.TapeR.tape_record(to_data);
				} else {
					Wang1200.TapeL.tape_record(to_data);
				}
				to_data = 0;
			}
		}
	}

	byte ti_lastc;
	byte ti_lastd;
	int ti_data;
	int ti_bitc;
	int ti_sigc;
	long ti_repc;
	byte ti_init;
	int ti_curr;
	byte ti_chunk;
	int[] ti_chunks = new int[5];

	private int do_repc() {
		return 0;
	}
	private int do_sigc() {
		--ti_sigc;
		tck = (byte)(ti_lastc & 1);
		dk = (byte)(ti_lastd & 1);
		ti_lastc >>= 1;
		ti_lastd >>= 1;
		ti_repc = cycles + 10;	// sensitive?
System.err.format("tape signal %d %d\n", tck, dk);
		return do_repc();
	}
	private int do_bitc() {
		--ti_bitc;
		ti_data <<= 1;
		if ((ti_data & 0x100) != 0) {
			ti_lastc = 0x00;
			ti_lastd = 0x01;
		} else {
			ti_lastc = 0x01;
			ti_lastd = 0x00;
		}
		ti_sigc = 5;
		return do_sigc();
	}
	private int do_byte() {
		if (rv != 0) { // no data, just fake signals, in reverse...
			// TODO: when to stop (BOT)?
			ti_data = 0;
		} else {
			if (right != 0) {
				ti_data = Wang1200.TapeR.tape_play();
			} else {
				ti_data = Wang1200.TapeL.tape_play();
			}
System.err.format("tape data is %02x (%d)\n", ti_data, ti_curr);
		}
		if (ti_data < 0) { // EOF
			ti_repc = cycles + 900;	// 27,928cy... ?
			return do_repc();
		}
		ti_bitc = 8;
		ti_curr -= ti_bitc;
		if (ti_curr < 8) {
			// hack for 66-bits in 8-bytes storage...
			ti_bitc += ti_curr;
			ti_curr = 0;
		}
		return do_bitc();
	}

	private int tape_read() {
		if (ti_init == 0) {
			if (rv != 0) {
				ti_chunks[0] = -900;	// gap
				ti_chunks[1] = 800;	// bits of data
				ti_chunks[2] = -1600;	// gap
				ti_chunks[3] = 66;	// bits of header
				ti_chunks[4] = -16000;	// gap
			} else {
				ti_chunks[0] = -900;	// gap
				ti_chunks[1] = 66;	// bits of header
				ti_chunks[2] = -1600;	// gap
				ti_chunks[3] = 800;	// bits of data
				ti_chunks[4] = -16000;	// gap
			}
			ti_chunk = 0;
			ti_curr = 0;
			ti_init = 1;
		}
		if (right != 0 && rhs == 0) {
			return 0; // do not read tape unless read-head is engaged...
		}
		if (right == 0 && lhs == 0) {
			return 0; // do not read tape unless read-head is engaged...
		}

		if (cycles < ti_repc) {
			return do_repc();
		}
		if (ti_sigc > 0) {
			return do_sigc();
		}
		if (ti_bitc > 0) {
			return do_bitc();
		}
		if (ti_curr != 0) {
			return do_byte();
		}
		ti_curr = ti_chunks[ti_chunk];
		if (++ti_chunk > 4) ti_chunk = 0;
		if (ti_curr < 0) { // gap
			// leave signal unchanged...
			ti_repc = cycles + -(ti_curr);
			ti_curr = 0;
			return do_repc();
		} else {
			return do_byte();
		}
	}

	private void tape_on() {
		tck = 0;
		dk = 0;
		din0 = 0;
		din1 = 0;
		to_last = 0;
		to_data = 0;
		to_bitc = 0;
		if (right != 0) {
System.err.println("Tape On R");
			tmr = tm;
			Wang1200.Kbd.setTAPE_MOV_R(tmr != 0);
			Wang1200.TapeR.tape_on(rc, tm, rhs, rv, hl);
		} else {
System.err.println("Tape On L");
			tml = tm;
			Wang1200.Kbd.setTAPE_MOV_L(tml != 0);
			Wang1200.TapeL.tape_on(rc, tm, lhs, rv, hl);
		}
		if (rc == 0) {
			ti_lastc = 0;
			ti_lastd = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_init = 0;
		}
	}

	private void tape_off() {
		tck = 0;
		dk = 0;
		din0 = 0;
		din1 = 0;
		to_last = 0;
		to_data = 0;
		to_bitc = 0;
		if (right != 0) {
			tmr = tm;
			Wang1200.Kbd.setTAPE_MOV_R(tmr != 0);
			Wang1200.TapeR.tape_off(rc, tm, rhs, rv, hl);
		} else {
			tml = tm;
			Wang1200.Kbd.setTAPE_MOV_L(tml != 0);
			Wang1200.TapeL.tape_off(rc, tm, lhs, rv, hl);
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

	private void rd_ram_i() {
		int adr = ((m & 0x0f) << 4) | (n & 0x0f);
		byte b = _ram[adr];
		ca = (byte)((b >> 4) & 0x0f);
		cb = (byte)(b & 0x0f);
	}

	private void wr_ram_i() {
		int adr = ((m & 0x0f) << 4) | (n & 0x0f);
		_ram[adr] = (byte)((ca << 4) | cb);
	}

	private void refresh(boolean canSleep) {
		if (canSleep) {
			int k = -1;
			try {
				k = keyCodes.pollFirst(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
			} catch(Exception ee) {
				k = -1;
			}
			if (k >= 0) {
				keyCodes.addFirst(k);
			}
		}
	}

	private void display_check() {
		if (pc == 0x03e) {	// bottom of main loop?
			if ((_ram[0xfc] & 0x10) == 0) {
				if (trace) { // can only be if _dbg != null
					_dbg.warp("Idle Loop", -1, -1);
				}
				// timeout 1000 wait for key...
				refresh(cylimit == Long.MAX_VALUE);
			}
		}
	}

	private int instr_exec() {
		Wang1200_Ucode uu = _rom.fetchUcode(pc);
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

		switch(uu.mop) {
		case 1:
		case 4:
			m = u;
			n = v;
			break;
		case 2:
		case 5:
			m = br_k;
			n = v;
			break;
		case 3:
		case 6:
			m = 15;
			n = br_k;
			break;
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
		case 2: g = (byte)Wang1200.Kbd.getMode0(true); break;
		case 3:
			g = (byte)Wang1200.Kbd.getMode1(true); // clears it...
			g = (byte)((g & 0x07) | (skl << 3));
			break;
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
		case 7:	cb = alu; break;
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
			// TODO: should this flush the queue?
			kbd = 0;
			break;
		case 10:
			s = (byte)((s & 0x0e) | (zo ^ 1));
			break;
		case 11:
			s = (byte)((s & 0x0d) | (zo << 1));
			break;
		case 12:
			break;
		case 13:
			s = 0;
			break;
		case 14:
			break;
		}

		switch(uu.mop) {
		case 1:	wr_ram_i(); break;
		case 2:	wr_ram_i(); break;
		case 3:	wr_ram_i(); break;
		case 4:	rd_ram_i(); break;
		case 5:	rd_ram_i(); break;
		case 6:	rd_ram_i(); break;
		case 7:
			if ((br_k & 1) != 0) {
				csl = (byte)(uu.bi & 1);
				Wang1200.Kbd.setCHAR_STOP(csl != 0);
			}
			if ((br_k & 2) != 0) {
				eln = (byte)(uu.bi & 1);
				Wang1200.Kbd.setEND_DOC(eln != 0);
			}
			if ((br_k & 4) != 0) {
				ern = (byte)(uu.bi & 1);
				Wang1200.Kbd.setRECORD(ern != 0);
			}
			if ((br_k & 8) != 0) {
				nan = (byte)(uu.bi & 1);
				Wang1200.Kbd.setNO_ADJUST(nan != 0);
			}
			break;
		case 8:
			if ((br_k & 1) != 0) {
				// [un]lock keyboard...
				Wang1200.CN24.do_lock(uu.bi & 1);
			}
			if ((br_k & 2) != 0) {
				// sound alarm/bell
				Wang1200.CN24.do_bell();
			}
			if ((br_k & 4) != 0) {
				to = ka;
				ro = kb;
				if ((uu.bi & 1) != 0) {
					// special function codes
					switch(to) {
					case 0: Wang1200.CN24.do_space(); break;
					case 1: Wang1200.CN24.do_backspace(); break;
					case 2: Wang1200.CN24.do_tab(); break;
					case 3: Wang1200.CN24.do_crlf(); break;
					case 4: Wang1200.CN24.do_shift_up(); break;
					case 5: Wang1200.CN24.do_shift_dn(); break;
					case 8: Wang1200.CN24.do_index(); break;
					case 9: Wang1200.CN24.do_settab(); break;
					case 10: Wang1200.CN24.do_clrtab(); break;
					case 13: Wang1200.CN24.do_lock(0); break;
					case 14: Wang1200.CN24.do_lock(1); break;
					case 15: Wang1200.CN24.do_bell(); break;
					default:
						// assert or print error?
					}
				} else {
					byte c = (byte)(((to << 4) | ro) & 0x3f);
					Wang1200.CN24.do_cn24(c);
				}
			}
			break;
		case 9:
			switch(br_k & 7) {
			case 0:
				ka = (byte)Wang1200.Kbd.getMode2(true);
				break;
			case 1:
				ka = (byte)4; // temp workaround for UART
				break;
			case 4:
				ka = (byte) // TRE, SHC, PRINT, ATTN...
					(ls << 2);
				break;
			}
			break;
		case 10:
			if (rc == 0) {
				tape_read();
			}
			// TCK : DK : LOP : ROP
			kb = (byte)((tck << 1) |
				(dk << 0) |
				(Wang1200.TapeL.tape_prot() << 2) |
				(Wang1200.TapeR.tape_prot() << 3));
			break;
		case 11:
			din0 = (byte)(kb & 1);
			din1 = (byte)(ka & 1);
			tape_write();
			break;
		case 12:
			// RHS : LHS : R/B : L/S
			kb = (byte)((rhs << 3) |
				(lhs << 2) |
				(1 << 1) |	// R/B (always ready)
				(ls << 0));	// L/S
			break;
		case 13:
		case 14:
			right = (byte)((br_k >> 0) & 1);
			if (right != 0) {
				rhs = (byte)(((br_k >> 2) & 1) ^ 1);
			} else {
				lhs = (byte)(((br_k >> 2) & 1) ^ 1);
			}
			rc = (byte)(uu.bi & 1);
			hl = (byte)((br_k >> 3) & 1);
			rv = (byte)((br_k >> 1) & 1);
			tm = (byte)(uu.mop == 13 ? 1 : 0);
			if (tm == 0) {
				tape_off();
			} else {
				tape_on();
			}
			break;
		case 15:
			switch(br_k & 7) {
			case 0:
				// all UART functions?
				break;
			}
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
			case 4: nxt |= (0 << 1); break;
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
				}
				nxt |= (kbd << 1);
				if (kbd != 0) {
					kbd = 0;
				}
				break;
			case 7: nxt |= (zo << 1); break;
			}
			switch(uu.jl) {
			case 0: nxt |= (0 << 0); break;
			case 1: nxt |= (1 << 0); break;
			case 2: nxt |= ((br_acc & 1) >> 0); break;
			case 3: nxt |= ((br_acc & 4) >> 2); break;
			case 4: nxt |= (zo << 0); break;
			case 5: nxt |= (cc << 0); break;
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

		if (jam != 0) {
			next = jam & 0x0fff;
			jam = 0;
			if (next == 0) { // RESET
				skl = 0;
				shl = 0;
				Wang1200.Kbd.setSKIP(skl != 0);
				Wang1200.Kbd.setSEARCH(shl != 0);
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
		Wang_UI.fatal("Wang1200 Core", "Simulation error");
	}
}
