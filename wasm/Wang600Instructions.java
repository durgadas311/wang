// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class Wang600Instructions implements WangInstructions {
	private WangSymTable tbl;
	private static Vector<Instruction> instr = new Vector<Instruction>();
	private 

	// operand type, if any.
	static final int NONE = 0;	// one-step instructions
	static final int MARK = 1;
	static final int REG = 2;
	static final int FMT = 3;
	static final int LABEL = 4;
	static final int ALPHA = 5;
	static final int IO = 6;
	static final int INDIR = 7;

	static final int ATERM = 0x22;	// terminates ALPHA string command

	static final String E = "0123456789.E-";
	static final String F = "XYZABCDEFGHIJKLM";
	static final String[] a = {	// unshifted
		"-","y", " ","\\b","q","p","=", "j","\\t","/","\\x","\\y",",",";","f","g",
		"w","s","\\s","\\u","i","'",".","\\h","\\r","o","\\i","\\v","a","r","v","m",
		"b","h","\\+","\\-","k","e","n", "t","\\p","l","\\+","\\-","c","d","u","x",
		"9","0","\\+","\\-","6","5","2", "z","\\p","4","\\+","\\-","8","7","3","1",
	};
	static final String[] A = {	// shifted
		"_","Y", " ","\\b", "Q","P", "+", "J","\\t","?","\\x","\\y",",",":","F","G",
		"W","S","\\s","\\u", "I","\"",".","\\q","\\r","O","\\i","\\v","A","R","V","M",
		"B","H","\\+","\\-", "K","E", "N", "T","\\p","L","\\+","\\-","C","D","U","X",
		"(",")","\\+","\\-","\\c","%", "@", "Z","\\p","$","\\+","\\-","*","&","#","!",
	};

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
		instr.add(new Instruction("SRCHROM2",	0xf4, MARK));
		instr.add(new Instruction("SRCHROM3",	0xf5, MARK));
		instr.add(new Instruction("SRCHROM4",	0xf6, MARK));
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
		instr.add(new Instruction("E14",	0x0e, 0));
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
	}

	public Wang600Instructions(WangSymTable tbl) {
		this.tbl = tbl;
		initAll();
	}

	// Assembly methods //

	// label, if any, already parsed. Else 'lab' is null.
	public int encode(String line, byte[] mem, int start) {
		return 0;
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

	public WangInstruction decode(byte[] mem, int start) {
		String ret = "";
		int x = start;
		int o;
		WangInstruction ins = new WangInstruction();
		Instruction e;
		boolean shifted;

		// TODO: handle ALPHA text sequences...

		if ((mem[x] & 0xff) < 0x0d) {
			ret = "ENTER ";
			while ((mem[x] & 0xff) < 0x0d) {
				ret += E.charAt(mem[x++] & 0x0f);
			}
			ins.length = x - start;
			ins.mnemonic = ret;
			ins.flags = 0;
			return ins;
		}
		o = mem[x++] & 0xff;
		e = disas(o);
		if (e == null) {
			ins.mnemonic = String.format("?%02d %02d", 
					(o >> 4), (o & 0x0f));
			ins.length = 1;
			ins.flags = 0;
			return ins;
		}
		ret = e.mnemonic;
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
			ret += F.charAt(o >> 4) + "/" + String.format("%02d", o & 0x0f);
			break;
		case ALPHA:
			if (o < 0x80) {
				shifted = false;
				ret += "\"";
				while (o < 0x80) {
					if (shifted) {
						ret += A[o];
					} else {
						ret += a[o];
					}
					if (o == ATERM) break;
					else if (o == 0x12) shifted = false;
					else if (o == 0x13) shifted = true;
					o = mem[x++] & 0xff;
				}
				// TODO: need to backup if no ATERM...
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
}
