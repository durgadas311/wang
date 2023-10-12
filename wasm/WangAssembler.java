// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class WangAssembler {
	WangInstructions wi;
	int errs = 0;

	public WangAssembler(WangInstructions wi) {
		this.wi = wi;
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
			if (toks.length == 0) { // is this the way?
				if (ls != null) {
					ls.format("                %s\n", line);
				}
				continue;
			}
			n = wi.encode(toks, mem, adr);
			if (n <= 0) {
				++errs;
			}
			if (ls == null) {
				adr += n;
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
		n = objectOut(mem, adr, os);
		if (n < 0) {
			return n;
		}
		return errs;
	}
}
