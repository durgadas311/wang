// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class Wang600Instructions implements WangInstructions {
	private WangSymTable tbl;
	private static Vector<Instruction> instr = new Vector<Instruction>();
	private TiltRotate tr = new TiltRotate();
	private char error;

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
			return mnemonic.equalsIgnoreCase(mn);
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
		instr.add(new Instruction("SRCHROM",	0xf3, MARK));
		instr.add(new Instruction("SRCHROM*",	0xf4, MARK));
		instr.add(new Instruction("SRCHROM*",	0xf5, MARK));
		instr.add(new Instruction("SRCHROM*",	0xf6, MARK));
		instr.add(new Instruction("CALL",	0xf7, MARK));
		instr.add(new Instruction("MARK*",	0xf8, LABEL));
		instr.add(new Instruction("STORE*",	0xf9, REG));
		instr.add(new Instruction("ALPHA*",	0xfa, ALPHA));
		instr.add(new Instruction("INDIR",	0xfb, INDIR));
		instr.add(new Instruction("CALLROM",	0xfc, MARK));
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
			instr.add(new Instruction("f" + n, 0xa0 + x, 0));
			instr.add(new Instruction("F" + n, 0xb0 + x, 0));
			instr.add(new Instruction("g" + n, 0xc0 + x, 0));
			instr.add(new Instruction("G" + n, 0xd0 + x, 0));
			instr.add(new Instruction("EX" + n, 0xe0 + x, 0));
		}
		// Add some assembler aliases, not seen by disassembler
		instr.add(new Instruction("CHGSGN", 0x0c, 0));
		instr.add(new Instruction("SETEXP", 0x0b, 0));
	}

	public Wang600Instructions(WangSymTable tbl) {
		this.tbl = tbl;
		initAll();
	}

	public int maxPC() { return 1847; }
	public int maxReg() { return 246; }
	public int endProg() { return 0x9e; }
	public char lastError() { return error; }

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

	// label, if any, already parsed. Else 'lab' is null.
	public int encode(String[] line, byte[] mem, int start) {
		int adr = start;
		int x = 0;
		int reg;
		Instruction e;

		error = ' ';
		while (x < line.length && line[x].length() == 0) ++x;
		if (x >= line.length) return 0;
		if (line[x].equalsIgnoreCase("ENTER")) {
			++x;
			for (int i = 0; i < line[x].length(); ++i) {
				int q = E.indexOf(Character.toUpperCase(line[x].charAt(i)));
				if (q < 0) {
					error = 'V';
					return -1;
				}
				mem[adr++] = (byte)q;
			}
			return adr - start;
		}
		e = asm(line[x++]);
		if (e == null) {
			error = 'O';
			return -1;
		}
		mem[adr++] = e.opcode;
		if (e.flags == 0) return adr - start;
		if (x >= line.length) {
			error = 'S';
			return -1;
		}
		switch (e.flags) {
		case MARK:	// TODO: prevent/warn on END PROG?
		case LABEL:
			if (line[x].matches("^[0-1][0-9]-[0-1][0-9]$")) {
				mem[adr++] = getCode(line[x]);
				break;
			}
		case INDIR:	// TODO: validate operation code?
			e = asm(line[x]);
			if (e == null) {
				error = 'P';
				return -1;
			}
			mem[adr++] = e.opcode;
			break;
		case REG:
			reg = Integer.valueOf(line[x]);
			if (reg < 0 || reg > maxReg()) {
				error = 'R';
				return -1;
			}
			mem[adr++] = (byte)reg;
			break;
		case FMT:
			if (!line[x].matches("^[X-ZA-M]/[0-1][0-9]$")) {
				error = 'F';
				return -1;
			}
			mem[adr++] = getFormat(line[x]);
			break;
		case ALPHA:
			// line[x] must have quotes, or else be a key.
			if (line[x].matches("^\".*\"$")) {
				reg = tr.a2tr(line[x].substring(1, line[x].length() - 1),
					false, mem, adr);
				adr += reg;
			} else {
				// TODO: same as INDIR, etc.
				e = asm(line[x]);
				if (e == null) {
					error = 'P';
					return -1;
				}
				mem[adr++] = e.opcode;
			}
			break;
		case IO:
			if (!line[x].matches("^[0-1][0-9]-[0-1][0-9]$")) {
				error = 'I';
				return -1;
			}
			mem[adr++] = getCode(line[x]);
			break;
		}

		return adr - start;
	}

	public int dreg(String line, byte[] mem, int start) {
		return 0;
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
			ret += " " + getKey(o);
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
}
