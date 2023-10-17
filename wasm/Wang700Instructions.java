// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class Wang700Instructions implements WangInstructions {
	private WangSymTable tbl;
	private static Vector<Instruction> instr = new Vector<Instruction>();
	private TiltRotate tr;
	private char error;

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
			instr.add(new Instruction("SR0-" + n, 0x00 + x, 0));
			instr.add(new Instruction("SR1-" + n, 0x10 + x, 0));
			instr.add(new Instruction("SR2-" + n, 0x20 + x, 0));
			instr.add(new Instruction("SR3-" + n, 0x30 + x, 0));
			instr.add(new Instruction("IO" + n, 0x80 + x, 0));
		}
	}

	public Wang700Instructions(WangSymTable tbl) {
		this.tbl = tbl;
		initAll();
		tr = new TiltRotate(0x4d);
	}

	public int maxPC() { return 1983; }
	public int maxReg() { return 248; }
	public int endProg() { return 0x5c; }
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
				mem[adr++] = (byte)(q + 0x70);
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
			if (reg > 99) {
				mem[adr - 1] |= 0x80;
				reg -= 100;
			}
			reg = ((reg / 10) << 4) | (reg % 10);
			mem[adr++] = (byte)reg;
			break;
		case FMT:	// WRITE instruction
			if (!line[x].matches("^[0-1][0-9]-[0-1][0-9]$")) {
				error = 'F';
				return -1;
			}
			mem[adr++] = getCode(line[x]);
			break;
		case ALPHA:	// WRITE ALPHA instruction
			// line[x] must have quotes, or else be a key.
			if (line[x].matches("^\".*\"$")) {
				reg = tr.a2tr(line[x].substring(1, line[x].length() - 1),
					false, mem, adr);
				adr += reg;
			} else {
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

	public WangInstruction decodeOp(int op) {
		WangInstruction ins = new WangInstruction();
		Instruction e;

		e = disas(op);
		if (e == null) return null;
		ins.mnemonic = e.mnemonic;
		ins.flags = e.flags;
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
}
