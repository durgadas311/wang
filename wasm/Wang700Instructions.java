// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class Wang700Instructions implements WangInstructions {
	private WangSymbolTable tbl;
	private static Vector<Instruction> instr = new Vector<Instruction>();
	private TiltRotate tr;
	private char error;
	private boolean pass;

	static final String E = "0123456789E-.";

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
			return mnemonic.equalsIgnoreCase(mn);
		}
	}

	// number entry and functions are parsed separately.
	void initAll() {
		instr.add(new Instruction("+D",		0x40, REG));
		instr.add(new Instruction("-D",		0x41, REG));
		instr.add(new Instruction("*D",		0x42, REG));
		instr.add(new Instruction("/D",		0x43, REG));
		instr.add(new Instruction("STD",	0x44, REG));
		instr.add(new Instruction("RED",	0x45, REG));
		instr.add(new Instruction("EXD",	0x46, REG));
		instr.add(new Instruction("SEARCH",	0x47, MARK));
		instr.add(new Instruction("MARK",	0x48, LABEL));
		instr.add(new Instruction("GROUP1",	0x49, IO));
		instr.add(new Instruction("GROUP2",	0x4a, IO));
		instr.add(new Instruction("WRITE",	0x4b, FMT));
		instr.add(new Instruction("ALPHA",	0x4c, ALPHA));
		instr.add(new Instruction("ENDAL",	0x4d, 0));
		instr.add(new Instruction("STYD",	0x4e, REG));
		instr.add(new Instruction("REYD",	0x4f, REG));

		instr.add(new Instruction("+I",		0x50, 0));
		instr.add(new Instruction("-I",		0x51, 0));
		instr.add(new Instruction("*I",		0x52, 0));
		instr.add(new Instruction("/I",		0x53, 0));
		instr.add(new Instruction("STI",	0x54, 0));
		instr.add(new Instruction("REI",	0x55, 0));
		instr.add(new Instruction("EXI",	0x56, 0));
		instr.add(new Instruction("SKY>=X",	0x57, 0));
		instr.add(new Instruction("SKY<X",	0x58, 0));
		instr.add(new Instruction("SKY=X",	0x59, 0));
		instr.add(new Instruction("SKERR",	0x5a, 0));
		instr.add(new Instruction("RET",	0x5b, 0));
		instr.add(new Instruction("END",	0x5c, 0));
		instr.add(new Instruction("LOAD",	0x5d, 0));
		instr.add(new Instruction("GO",		0x5e, 0));
		instr.add(new Instruction("STOP",	0x5f, 0));

		instr.add(new Instruction("+Y",		0x60, 0));
		instr.add(new Instruction("-Y",		0x61, 0));
		instr.add(new Instruction("*Y",		0x62, 0));
		instr.add(new Instruction("/Y",		0x63, 0));
		instr.add(new Instruction("STY",	0x64, 0));
		instr.add(new Instruction("REY",	0x65, 0));
		instr.add(new Instruction("EXY",	0x66, 0));
		instr.add(new Instruction("ABS",	0x67, 0));
		instr.add(new Instruction("INT",	0x68, 0));
		instr.add(new Instruction("PI",		0x69, 0));
		instr.add(new Instruction("LOGX",	0x6a, 0));
		instr.add(new Instruction("LNX",	0x6b, 0));
		instr.add(new Instruction("SQRT",	0x6c, 0));
		instr.add(new Instruction("10^X",	0x6d, 0));
		instr.add(new Instruction("E^X",	0x6e, 0));
		instr.add(new Instruction("1/X",	0x6f, 0));

		instr.add(new Instruction("E0",		0x70, 0));
		instr.add(new Instruction("E1",		0x71, 0));
		instr.add(new Instruction("E2",		0x72, 0));
		instr.add(new Instruction("E3",		0x73, 0));
		instr.add(new Instruction("E4",		0x74, 0));
		instr.add(new Instruction("E5",		0x75, 0));
		instr.add(new Instruction("E6",		0x76, 0));
		instr.add(new Instruction("E7",		0x77, 0));
		instr.add(new Instruction("E8",		0x78, 0));
		instr.add(new Instruction("E9",		0x79, 0));
		instr.add(new Instruction("EE",		0x7a, 0));
		instr.add(new Instruction("E-",		0x7b, 0));
		instr.add(new Instruction("E.",		0x7c, 0));
		instr.add(new Instruction("X^2",	0x7d, 0));
		instr.add(new Instruction("RES",	0x7e, 0));
		instr.add(new Instruction("CLRX",	0x7f, 0));

		instr.add(new Instruction("+D",		0xc0, REG100));
		instr.add(new Instruction("-D",		0xc1, REG100));
		instr.add(new Instruction("*D",		0xc2, REG100));
		instr.add(new Instruction("/D",		0xc3, REG100));
		instr.add(new Instruction("STD",	0xc4, REG100));
		instr.add(new Instruction("RED",	0xc5, REG100));
		instr.add(new Instruction("EXD",	0xc6, REG100));
		instr.add(new Instruction("STYD",	0xce, REG100));
		instr.add(new Instruction("REYD",	0xcf, REG100));

		for (int x = 0; x < 16; ++x) {
			String n = String.format("%02d", x);
			instr.add(new Instruction("SR0-" + n, 0x00 + x, FCALL));
			instr.add(new Instruction("SR1-" + n, 0x10 + x, FCALL));
			instr.add(new Instruction("SR2-" + n, 0x20 + x, FCALL));
			instr.add(new Instruction("SR3-" + n, 0x30 + x, FCALL));
			instr.add(new Instruction("IO" + n, 0x80 + x, 0));
		}
		// Add some assembler aliases, not seen by disassembler
		instr.add(new Instruction("CHGSGN", 0x7b, 0));
		instr.add(new Instruction("SETEXP", 0x7a, 0));
	}

	public Wang700Instructions() {
		tbl = new WangSymbolTable(0x00, 0x40);
		tbl.reserveMark(endProg()); // END PROG is problematic
		initAll();
		tr = new TiltRotate(0x4d);
	}

	public int maxPC() { return 1983; }
	public int maxRomPC() { return 0; }
	public int maxReg() { return 248; }
	public int endProg() { return 0x5c; }
	public int stop() { return 0x5f; }
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

	private boolean fixReg(WangMemory mem, int adr, int reg) {
		if (reg > 99) {
			mem.putMem(adr - 1, mem.getMem(adr - 1) | 0x80);
			reg -= 100;
		}
		reg = ((reg / 10) << 4) | (reg % 10);
		return mem.putMem(adr, reg);
	}

	public int setOutput(String dev) {
		error = ' ';
		if (!dev.matches("[wW]7[01][012]") && !dev.matches("[Ww]70[67]")) {
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
			if (tbl.setMark(key, ref, false) < 0) {
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
				reg = tbl.getLabel(val.substring(1), adr);
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
				if (mem.putMem(adr++, q + 0x70)) {
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
		case REG:
			if (line[x].charAt(0) == '&') {
				reg = tbl.getLabel(line[x].substring(1), adr);
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
			if (fixReg(mem, adr++, reg)) {
				error = 'Z';
				return -2;
			}
			break;
		case FMT:	// WRITE instruction
			if (!line[x].matches("^[0-1][0-9]-[0-1][0-9]$")) {
				error = 'F';
				return -2;
			}
			if (mem.putMem(adr++, getCode(line[x]))) {
				error = 'Z';
				return -2;
			}
			break;
		case ALPHA:	// WRITE ALPHA instruction
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
		return (start + 0x0f) & ~0x0f;
	}

	public int regPad(WangMemory mem, int start) {
		int adr = start;

		while ((adr & 0x0f) != 0) {
			// overflow is caught later
			mem.putMem(adr++, stop());
		}
		return adr - start;
	}

	// R (high nibble) and R+1 (low nibble)
	// adr already adjusted with regPad().
	public int adrReg(int adr) {
		return ((maxPC() - adr) / 16) * 2;
	}

	public String adrRegStr(int adr) {
		int reg = adrReg(adr);
		return String.format(" (%d,%d)", reg, reg + 1);
	}

	private String getRegVal(String s) {
		String val = "";

		if (s.matches("\"[0-9a-fA-F]*\"")) {
			val = s.replaceAll("\"", "");
		} else if (s.length() > 0) {
			double d = Double.valueOf(s);
			val = String.format("%+18.11e", d);
			val = val.replace('+', '0');
			val = val.replace('-', '1');
			val = val.replaceAll("[.eE]", "");
		}
		return val;
	}

	public int xlab(String[] line, int first) {
		int key = 0;
		int x = first;
		int ret = 0;

		error = ' ';
		while (++x < line.length) {
			Instruction e = keyOrCode(line[x]);
			if (e == null) {
				error = 'P';
				return -1;
			}
			key = (e.opcode & 0xff);
			if (tbl.setMark(key, -1, false) < 0) {
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
		String val0 = "";
		String val1 = "";
		int x;
		int c = 0;
		double d;
		String[] ss;

		error = ' ';
		x = first + 1;	// skip ".REG"
		reg = adrReg(adr);
		if (x < line.length) {
			ss = line[x].split(",");
			if (tbl.setLabel(ss[0], reg) < 0) {
				error = 'M';
				return -16;
			}
			if (ss.length > 1) {
				if (tbl.setLabel(ss[1], reg + 1) < 0) {
					error = 'M';
					return -16;
				}
			}
			++x;
		}
		if (x < line.length) {
			ss = line[x].split(",");
			val0 = getRegVal(ss[0]);
			if (ss.length > 1) {
				val1 = getRegVal(ss[1]);
			}
		}
		for (x = 15; x >= 0; --x) {
			c = 0;
			if (x < val0.length()) {
				c |= (Character.digit(val0.charAt(x), 16) << 4);
			}
			if (x < val1.length()) {
				c |= Character.digit(val1.charAt(x), 16);
			}
			if (mem.putMem(adr++, c)) {
				error = 'Z';
				return -16;
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

		o = mem[x++] & 0xff;
		if (o >= 0x70 && o < 0x7d) {
			ret = "ENTER ";
			while (o >= 0x70 && o < 0x7d) {
				ret += E.charAt(o & 0x0f);
				o = mem[x++] & 0xff;
			}
			ins.mnemonic = ret;
			ins.length = x - start - 1;
			ins.flags = 0;
			return ins;
		}
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
			ret += " " + getKey(o);
			break;
		case REG100:
		case REG:
			o = ((o >> 4) * 10) + (o & 0x0f);
			if (e.flags == REG100) o += 100;
			ret += String.format(" %d", o);
			break;
		case FMT:
			ret += String.format(" %02d-%02d", (o >> 4), (o & 0x0f));
			break;
		case ALPHA:
			if ((o & 0x40) == 0) {
				shifted = false; // a.k.a. Shift Down
				ret += " \"";
				while ((o & 0x40) == 0) {
					if (o == tr.term()) {
						// terminator is implied
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
				if (o != tr.term()) --x;
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
		return "<padding>-<decimal>";
	}

	public String regHelp() {
		return "[label[,label]] [\"string\" | number][,...]";
	}
}
