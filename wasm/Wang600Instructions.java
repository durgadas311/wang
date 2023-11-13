// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class Wang600Instructions implements WangInstructions {
	private WangSymbolTable tbl;
	private static Vector<Instruction> instr = new Vector<Instruction>();
	private TiltRotate tr = new TiltRotate();
	private char error;
	private boolean rom;
	private boolean pass;

	static final String E = "0123456789.E-";
	static final String F = "XYZABCDEFGHIJKLM";

	class Instruction {
		public String mnemonic;
		public byte opcode;
		public int flags;

		public Instruction(String s, int o, int f) {
			mnemonic = s;
			opcode = (byte)o;
			flags = f;
		}

		public boolean equalsOp(int op) {
			return opcode == (byte)op;
		}

		public boolean equalsMn(String mn) {
			if (flags == FCALL || flags == FROM) {
				return mnemonic.equals(mn);
			} else {
				return mnemonic.equalsIgnoreCase(mn);
			}
		}
	}

	// number entry and functions are parsed separately.
	void initAll() {
		instr.add(new Instruction("SEARCH",	0x80, MARK));
		instr.add(new Instruction("RECALL",	0x81, REG));
		instr.add(new Instruction("PRINT",	0x82, FMT));
		instr.add(new Instruction("GO",		0x83, 0));
		instr.add(new Instruction("JIF0",	0x84, 0));
		instr.add(new Instruction("JIF+",	0x85, 0));
		instr.add(new Instruction("SIN",	0x86, 0));
		instr.add(new Instruction("COS",	0x87, 0));
		instr.add(new Instruction("TAN",	0x88, 0));
		instr.add(new Instruction("RAD-DEG",	0x89, 0));
		instr.add(new Instruction("LNX",	0x8a, 0));
		instr.add(new Instruction("E^X",	0x8b, 0));
		instr.add(new Instruction("X^2",	0x8c, 0));
		instr.add(new Instruction("SQRT",	0x8d, 0));
		instr.add(new Instruction("LOAD",	0x8e, 0));
		instr.add(new Instruction("1/X",	0x8f, 0));
		instr.add(new Instruction("MARK",	0x90, LABEL));
		instr.add(new Instruction("STORE",	0x91, REG));
		instr.add(new Instruction("ALPHA",	0x92, ALPHA));
		instr.add(new Instruction("STOP",	0x93, 0));
		instr.add(new Instruction("JIFNZ",	0x94, 0));
		instr.add(new Instruction("JIFERR",	0x95, 0));
		instr.add(new Instruction("SIN^",	0x96, 0));
		instr.add(new Instruction("COS^",	0x97, 0));
		instr.add(new Instruction("TAN^",	0x98, 0));
		instr.add(new Instruction("DEG-RAD",	0x99, 0));
		instr.add(new Instruction("LOGX",	0x9a, 0));
		instr.add(new Instruction("10^X",	0x9b, 0));
		instr.add(new Instruction("INT",	0x9c, 0));
		instr.add(new Instruction("ABS",	0x9d, 0));
		instr.add(new Instruction("END",	0x9e, 0));
		instr.add(new Instruction("RET",	0x9f, 0));
		instr.add(new Instruction("RECALL*",	0xf0, REG));
		instr.add(new Instruction("PRINT*",	0xf1, FMT));
		instr.add(new Instruction("I/O",	0xf2, IO));
		instr.add(new Instruction("SRCHROM",	0xf3, ROMARK));
		instr.add(new Instruction("SRCHROM*",	0xf4, ROMARK));
		instr.add(new Instruction("SRCHROM*",	0xf5, ROMARK));
		instr.add(new Instruction("SRCHROM*",	0xf6, ROMARK));
		instr.add(new Instruction("CALL",	0xf7, MARK));
		instr.add(new Instruction("MARK*",	0xf8, LABEL));
		instr.add(new Instruction("STORE*",	0xf9, REG));
		instr.add(new Instruction("ALPHA*",	0xfa, ALPHA));
		instr.add(new Instruction("INDIR",	0xfb, INDIR));
		instr.add(new Instruction("CALLROM",	0xfc, ROMARK));
		instr.add(new Instruction("GROUP1",	0xfd, IO));
		instr.add(new Instruction("GROUP2",	0xfe, IO));
		instr.add(new Instruction("SEARCH*",	0xff, MARK));
		instr.add(new Instruction("E13",	0x0d, 0));
		instr.add(new Instruction("CLRALL",	0x0e, 0));
		instr.add(new Instruction("CLEAR",	0x0f, 0));
		for (int x = 0; x < 16; ++x) {
			String n = String.format("%02d", x);
			if (x < 13) {
				instr.add(new Instruction("E" + E.charAt(x), x, 0));
			}
			instr.add(new Instruction("T" + n, 0x10 + x, 0));
			instr.add(new Instruction("+" + n, 0x20 + x, 0));
			instr.add(new Instruction("-" + n, 0x30 + x, 0));
			instr.add(new Instruction("*" + n, 0x40 + x, 0));
			instr.add(new Instruction("/" + n, 0x50 + x, 0));
			instr.add(new Instruction("ST" + n, 0x60 + x, 0));
			instr.add(new Instruction("RE" + n, 0x70 + x, 0));
			instr.add(new Instruction("f" + n, 0xa0 + x, FCALL));
			instr.add(new Instruction("F" + n, 0xb0 + x, FCALL));
			instr.add(new Instruction("g" + n, 0xc0 + x, FROM));
			instr.add(new Instruction("G" + n, 0xd0 + x, FROM));
			instr.add(new Instruction("EX" + n, 0xe0 + x, 0));
		}
		// Add some assembler aliases, not seen by disassembler
		instr.add(new Instruction("CHGSGN", 0x0c, 0));
		instr.add(new Instruction("SETEXP", 0x0b, 0));
	}

	public Wang600Instructions(boolean rom) {
		if (rom) {
			tbl = new WangSymbolTable(0xc0, 0xe0);
			tbl.reserveMarks(0xa0, 0xc0); // excl non-ROM f(x)
		} else {
			tbl = new WangSymbolTable(0xa0, 0xc0);
			tbl.reserveMarks(0xc0, 0xe0); // excl ROM f(x)
		}
		tbl.reserveMark(endProg()); // END PROG is problematic
		initAll();
		this.rom = rom;
	}

	public int maxPC() { return 1847; }
	public int maxRomPC() { return 2047; }
	public int maxReg() { return 246; }
	public int endProg() { return 0x9e; }
	public int stop() { return 0x93; }
	public char lastError() { return error; }
	public boolean finalPass() { return pass; }
	public void finalPass(boolean p) { pass = p; }

	// Assembly methods //

	Instruction asm(String opcode) {
		for (Instruction x : instr) {
			if (x.equalsMn(opcode)) {
				return x;
			}
		}
		return null;
	}

	private byte getFormat(String opr) {
		int cd = (F.indexOf(opr.charAt(0)) << 4);
		cd |= Integer.valueOf(opr.substring(2));
		return (byte)cd;
	}

	private byte getCode(String opr) {
		int cd = (Integer.valueOf(opr.substring(0,2)) << 4);
		cd |= Integer.valueOf(opr.substring(3));
		return (byte)cd;
	}

	public Instruction keyOrCode(String opr) {
		Instruction e;
		int op;

		if (opr.matches("^[0-1][0-9]-[0-1][0-9]$")) {
			op = getCode(opr) & 0xff;
			e = disas(op);
			if (e == null) {
				// all codes are valid here, but caller
				// may check (e.mnemonic == null).
				e = new Instruction(null, op, 0);
			}
		} else {
			e = asm(opr);
		}
		return e;
	}

	public int setOutput(String dev) {
		error = ' ';
		if (!dev.matches("[wW]6[01][012]") && !dev.matches("[Ww]60[67]")) {
			error = 'S';
			return -1;
		}
		tr.setDevice(Integer.valueOf(dev.substring(2)));
		return 0;
	}

	public int setOutput(String[] line, int first) {
		int x = first + 1; // skip ".OUT"
		if (x >= line.length) {
			error = 'S';
			return -1;
		}
		return setOutput(line[x]);
	}

	// check symbolic program label
	private int chkSym(String key, int ref, int type) {
		int ret;
		boolean sub = (key.charAt(0) == '$');
		String sym = key.substring(1);

		if (type == LABEL) {
			if (sub) {
				ret = tbl.setSubr(sym, ref);
			} else {
				ret = tbl.setMark(sym, ref);
			}
			if (ret < 0) {
				error = 'M';
				return -1;
			}
		} else {
			if (sub) {
				ret = tbl.getSubr(sym, ref, type);
			} else {
				ret = tbl.getMark(sym, ref, type);
			}
			if (pass && ret < 0) {
				error = 'U';
				return -1;
			}
		}
		return ret;
	}

	// check specified program label
	private int chkLab(int key, int ref, int type) {
		if (type == LABEL) {
			if (tbl.setMark(key, ref, rom) < 0) {
				error = 'M';
				return -1;
			}
		} else {
			int reg = tbl.getMark(key, ref, type);
			if (pass && reg < 0) {
				error = 'U';
				return -1;
			}
		}
		return 0;
	}

	// label, if any, already parsed. Else 'lab' is null.
	public int encode(String[] line, int first, WangMemory mem, int start) {
		int adr = start;
		int x = 0;
		int reg;
		Instruction e;
		int flag;

		error = ' ';
		x = first;
		if (line[x].equalsIgnoreCase("ENTER")) {
			String val = line[++x];
			if (val.charAt(0) == '&') {
				reg = tbl.getLabel(val, adr);
				if (reg < 0) {
					if (pass) {
						error = 'U';
						return -3;
					}
					reg = 0;
				}
				val = String.format("%03d", reg);
			}
			for (int i = 0; i < val.length(); ++i) {
				int q = E.indexOf(Character.toUpperCase(val.charAt(i)));
				if (q < 0) {
					error = 'V';
					return -(adr - start);
				}
				if (mem.putMem(adr++, q)) {
					error = 'Z';
					return -(adr - start);
				}
			}
			return adr - start;
		}
		if (line[x].charAt(0) == '$') {
			// symbolic subroutine call
			reg = tbl.getSubr(line[x].substring(1), adr, FCALL);
			if (pass && reg < 0) {
				error = 'U';
				return -1;
			}
			if (mem.putMem(adr++, reg)) {
				error = 'Z';
				return -1;
			}
			flag = 0;
			e = null; // not used since flag=0
		} else {
			e = asm(line[x++]);
			if (e == null) {
				error = 'O';
				return -1;
			}
			if (mem.putMem(adr++, e.opcode)) {
				error = 'Z';
				return -1;
			}
			flag = e.flags;
		}
		switch (flag) {
		case FCALL:
		case FROM:
			// reference - f(x) call (program or ROM)
			reg = tbl.getMark(e.opcode & 0xff, adr - 1, flag);
			if (pass && reg < 0) {
				error = 'U';
				return -1;
			}
			// FALLTHROUGH
		case 0:
			return adr - start;
		default:
			break;
		}
		if (x >= line.length) {
			error = 'S';
			return -1;
		}
		switch (flag) {
		case MARK:	// TODO: prevent/warn on END PROG?
		case ROMARK:
		case LABEL:
			if (line[x].charAt(0) == '&' || line[x].charAt(0) == '$') {
				reg = chkSym(line[x], adr - 1, flag);
				if (reg < 0) {
					return -2;
				}
				if (mem.putMem(adr++, reg)) {
					error = 'Z';
					return -2;
				}
				break;
			}
			e = keyOrCode(line[x]);
			if (e == null) {
				error = 'P';
				return -2;
			}
			if (chkLab(e.opcode & 0xff, adr - 1, flag) < 0) {
				return -2;
			}
			if (mem.putMem(adr++, e.opcode)) {
				error = 'Z';
				return -2;
			}
			break;
		case INDIR:	// TODO: validate operation code?
			if (line[x].matches("[Rr][01][0-9]")) {
				// "case" statement: step += (Rxx)
				reg = Integer.valueOf(line[x].substring(1));
				if (reg <= 15) {
					if (mem.putMem(adr++, reg)) {
						error = 'Z';
						return -2;
					}
					break;
				}
			}
			e = asm(line[x]);
			if (e == null) {
				error = 'P';
				return -2;
			}
			if (mem.putMem(adr++, e.opcode)) {
				error = 'Z';
				return -2;
			}
			break;
		case REG:
			if (line[x].charAt(0) == '&') {
				reg = tbl.getLabel(line[x], adr);
				if (pass && reg < 0) {
					error = 'U';
					return -2;
				}
			} else {
				reg = Integer.valueOf(line[x]);
				if (reg < 0 || reg > maxReg()) {
					error = 'R';
					return -2;
				}
			}
			if (mem.putMem(adr++, reg)) {
				error = 'Z';
				return -2;
			}
			break;
		case FMT:
			if (!line[x].matches("^[X-ZA-M]/[0-1][0-9]$")) {
				error = 'F';
				return -2;
			}
			if (mem.putMem(adr++, getFormat(line[x]))) {
				error = 'Z';
				return -1;
			}
			break;
		case ALPHA:
			// line[x] must have quotes, or else be a key.
			if (line[x].matches("^\".*\"$")) {
				reg = tr.a2tr(line[x].substring(1, line[x].length() - 1),
					false, mem, adr);
				if (reg < 0) {
					error = 'Z';
					return reg;
				}
				adr += reg;
			} else {
				// TODO: same as INDIR, etc.
				e = asm(line[x]);
				if (e == null) {
					error = 'P';
					return -2;
				}
				if (mem.putMem(adr++, e.opcode)) {
					error = 'Z';
					return -2;
				}
			}
			break;
		case IO:
			if (!line[x].matches("^[0-1][0-9]-[0-1][0-9]$")) {
				error = 'I';
				return -2;
			}
			if (mem.putMem(adr++, getCode(line[x]))) {
				error = 'Z';
				return -2;
			}
			break;
		}

		return adr - start;
	}

	public int regPad(int start) {
		return (start + 0x07) & ~0x07;
	}

	public int regPad(WangMemory mem, int start) {
		int adr = start;

		while ((adr & 0x07) != 0) {
			// overflow is caught later
			mem.putMem(adr++, stop());
		}
		return adr - start;
	}

	// adr already adjusted with regPad().
	public int adrReg(int adr) {
		return (maxPC() - adr) / 8 + 16;
	}

	public String adrRegStr(int adr) {
		return String.format(" (%d)", adrReg(adr));
	}

	public int xlab(String[] line, int first) {
		int key = 0;
		int x = first;
		boolean extrom = false;
		int ret = 0;

		error = ' ';
		extrom = line[x].equalsIgnoreCase(".EXTROM");
		while (++x < line.length) {
			Instruction e = keyOrCode(line[x]);
			if (e == null) {
				error = 'P';
				return -1;
			}
			key = (e.opcode & 0xff);
			if (tbl.setMark(key, -1, extrom) < 0) {
				error = 'M';
				ret = -1;
			}
		}
		return ret;
	}

	public int def(String[] line, int first) {
		int x = first + 1;
		boolean sub;
		String key;
		Instruction e;

		error = ' ';
		if (x + 1 >= line.length) {
			error = 'S';
			return -1;
		}
		if (line[x].charAt(0) != '&' &&
				line[x].charAt(0) != '$') {
			error = 'P';
			return -1;
		}
		sub = (line[x].charAt(0) == '$');
		key = line[x].substring(1);
		e = keyOrCode(line[x + 1]);
		if (e == null) {
			error = 'O';
			return -1;
		}
		if (sub) {
			x = tbl.defSubr(key, e.opcode & 0xff);
		} else {
			x = tbl.defMark(key, e.opcode & 0xff);
		}
		if (x < 0) {
			if (x == -1) error = 'M';
			else error = 'P';
			return -1;
		}
		return 0;
	}

	// .REG <label> {"string"|number}
	// assumes regPad() already called.
	public int dreg(String[] line, int first, WangMemory mem, int start) {
		int adr = start;
		int reg;
		String val = "";
		int x;
		int c = 0;
		double d;

		error = ' ';
		if (rom) {
			error = 'X';
			return -1;
		}
		x = first + 1;	// skip ".REG"
		reg = adrReg(adr);
		if (x < line.length) {
			if (tbl.setLabel(line[x], reg) < 0) {
				error = 'M';
				return -8;
			}
			++x;
		}
		if (x < line.length) {
			if (line[x].matches("\"[0-9a-fA-F]*\"")) {
				val = line[x].replaceAll("\"", "");
			} else {
				d = Double.valueOf(line[x]);
				val = String.format("%+18.11e", d);
				val = val.replace('+', '0');
				val = val.replace('-', '1');
				val = val.replaceAll("[.eE]", "");
			}
		}
		for (x = 15; x >= 0; --x) {
			c <<= 4; // add '0'
			if (x < val.length()) {
				c |= Character.digit(val.charAt(x), 16);
			}
			if ((x & 1) == 0) {
				if (mem.putMem(adr++, c)) {
					error = 'Z';
					return -8;
				}
			}
		}
		return adr - start;
	}

	public WangSymbolTable getSymTab() {
		return tbl;
	}

	// Disassembly methods //

	Instruction disas(int opcode) {
		for (Instruction x : instr) {
			if (x.equalsOp(opcode)) {
				return x;
			}
		}
		return null;
	}

	private String getKey(int code) {
		Instruction e;

		e = disas(code);
		if (e == null) {
			return String.format("%02d-%02d", (code >> 4), (code & 0x0f));
		}
		return e.mnemonic;
	}

	// Used for assembler help
	public WangInstruction decodeOp(int op) {
		WangInstruction ins = new WangInstruction();
		boolean multi = false;

		for (Instruction e : instr) {
			if (e.equalsOp(op)) {
				if (ins.mnemonic == null) {
					ins.mnemonic = e.mnemonic;
					ins.flags = e.flags; // all the same?
				} else {
					multi = true;
					ins.mnemonic += " | " + e.mnemonic;
				}
			}
		}
		if (ins.mnemonic == null) return null;
		if (multi) {
			ins.mnemonic = "{ " + ins.mnemonic + " }";
		}
		// ins.length is implied by flags
		return ins;
	}

	public WangInstruction decode(byte[] mem, int start) {
		String ret = "";
		int x = start;
		int o;
		WangInstruction ins = new WangInstruction();
		Instruction e;
		boolean shifted;

		if ((mem[x] & 0xff) < 0x0d) {
			ret = "ENTER ";
			while ((mem[x] & 0xff) < 0x0d) {
				o = mem[x++] & 0xff;
				ret += E.charAt(o);
			}
			ins.mnemonic = ret;
			ins.length = x - start;
			ins.flags = 0;
			return ins;
		}
		o = mem[x++] & 0xff;
		e = disas(o);
		if (e == null) {
			ins.mnemonic = "?";
			ins.length = 1;
			ins.flags = 0;
			return ins;
		}
		ret += e.mnemonic;
		// TODO: prevent overflow of mem[]...
		if (e.flags != 0) {
			o = mem[x++] & 0xff;
		}
		switch (e.flags) {
		case MARK:
		case LABEL:
		case INDIR:
			if (e.flags == INDIR && o < 0x10) { // "case" statement
				ret += String.format(" R%02d", o);
			} else {
				ret += " " + getKey(o);
			}
			break;
		case REG:
			ret += String.format(" %d", o);
			break;
		case FMT:
			ret += " " + F.charAt(o >> 4) + "/" +
				String.format("%02d", o & 0x0f);
			break;
		case ALPHA:
			if (o < 0x80) {
				shifted = false; // a.k.a. Shift Down
				ret += " \"";
				while (o < 0x80) {
					if (o == tr.term()) {
						// terminator is implied...
						// ret += "\\0";
						break;
					} else if (o == tr.shiftDown()) {
						shifted = false;
					} else if (o == tr.shiftUp()) {
						shifted = true;
					} else {
						ret += tr.tr2a(o, shifted);
					}
					o = mem[x++] & 0xff;
				}
				if (o >= 0x80) --x;
				ret += "\"";
			} else {
				ret += " " + getKey(o);
			}
			break;
		case IO:
			ret += String.format(" %02d-%02d", (o >> 4), (o & 0x0f));
			break;
		}
		ins.mnemonic = ret;
		ins.length = x - start;
		ins.flags = e.flags;
		return ins;
	}

	public String printHelp() {
		return "<letter>/<decimal>";
	}

	public String regHelp() {
		return "[label] [\"string\" | number]";
	}
}
