// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class WangAssembler {
	WangInstructions wi;
	boolean tape;
	int errs = 0;

	public WangAssembler(WangInstructions wi, boolean tape) {
		this.wi = wi;
		this.tape = tape;
	}

	private int low(byte b) { return (b & 0x0f); }
	private int high(byte b) { return ((b >> 4) & 0x0f); }

	private int objectOut(byte[] mem, int len, File out) {
		try {
			FileOutputStream fo;
			fo = new FileOutputStream(out);
			fo.write(mem, 0, len);
			fo.close();
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		return 0;
	}

	public int asm(File file, File os, PrintStream ls) {
		byte[] mem;
		int adr;
		int n;
		WangInstruction e;
		BufferedReader in;
		String line;
		String s;
		String[] toks;
		boolean stdout = (ls == System.out);

		try {
			in = new BufferedReader(new FileReader(file));
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		mem = new byte[wi.maxPC() + 1];
		adr = 0;
		// TODO: line numbers...
		while (true) {
			try {
				line = in.readLine();
			} catch (Exception ee) {
				line = null;
			}
			if (line == null) {
				break;
			}
			s = line.replaceFirst(";.*$", "");
			toks = s.split("\\s(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			n = 0;
			while (n < toks.length && toks[n].length() == 0) ++n;
			if (n >= toks.length) {
				if (ls != null) {
					ls.format("                %s\n", line);
				}
				continue;
			}
			if (toks[n].equalsIgnoreCase(".REG")) {
				int m = wi.regPad(mem, adr);
				int end = adr + m;
				while (adr < end) {
					ls.format(" %04d  %02d-%02d\n", adr, high(mem[adr]), low(mem[adr]));
					++adr;
				}
				n = wi.dreg(toks, n, mem, adr);
				line += wi.adrRegStr(adr);
			} else {
				n = wi.encode(toks, n, mem, adr);
			}
			if (n <= 0) {
				++errs;
				if (!stdout) {
					System.err.format("%c%04d  %02d-%02d     %s\n",
						wi.lastError(),
						adr, high(mem[adr]), low(mem[adr]), line);
				}
			}
			if (ls == null) {
				if (n > 0) adr += n;
			} else {
				int end = adr + n;
				ls.format("%c%04d  %02d-%02d     %s\n",
					wi.lastError(),
					adr, high(mem[adr]), low(mem[adr]), line);
				++adr;
				while (adr < end) {
					ls.format(" %04d  %02d-%02d\n", adr, high(mem[adr]), low(mem[adr]));
					++adr;
				}
			}
			if (errs > 10) {
				System.err.format("Too many errors\n");
				break;
			}
		}
		if (tape) {
			if ((mem[adr - 1] & 0xff) == wi.endProg()) {
				mem[adr++] = (byte)wi.endProg();
			} else {
				mem[adr++] = (byte)wi.endProg();
				mem[adr++] = (byte)0xff;
			}
		}
		for (WangSymbol sym : wi.getSymTab().getSyms()) {
			if (!sym.def) {
				++errs;
				ls.format("Undefined: %s\n", sym.nam);
				if (!stdout) {
					System.err.format("Undefined: %s\n", sym.nam);
				}
			}
			// TODO: check for unused symbols?
		}
		n = objectOut(mem, adr, os);
		if (n < 0) {
			return n;
		}
		return errs;
	}
}
