// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class WangAssembler {
	boolean lst;
	WangInstructions wi;
	int err = 0;

	public WangAssembler(WangInstructions wi, boolean lst) {
		this.wi = wi;
		this.lst = lst;
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
				if (lst) {
					ls.format("                %s\n", line);
				}
				continue;
			}
			n = wi.encode(toks, mem, adr);
			if (n <= 0) {
System.err.format("ERROR: %s\n", line);
				// TODO: error? what to print?
				break;
			}
			if (!lst) {
				adr += n;
			} else {
				int end = adr + n;
				ls.format("%04d  %02d-%02d     %s\n", adr, high(mem[adr]), low(mem[adr]), line);
				++adr;
				while (adr < end) {
					ls.format("%04d  %02d-%02d\n", adr, high(mem[adr]), low(mem[adr]));
					++adr;
				}
			}
		}
		return objectOut(mem, adr, os);
	}
}
