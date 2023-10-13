// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class WangDisassembler {
	boolean lst;
	boolean tape;
	WangInstructions wi;

	public WangDisassembler(WangInstructions wi, boolean lst, boolean tape) {
		this.wi = wi;
		this.lst = lst;
		this.tape = tape;
		// TODO: allow for definition of a "register point" after which
		// all data is for registers. Note that END PROG may/will follow
		// register data.
	}

	private int low(byte b) { return (b & 0x0f); }
	private int high(byte b) { return ((b >> 4) & 0x0f); }

	public int disas(File file, PrintStream os) {
		byte[] mem;
		int adr;
		WangInstruction e;

		try {
			FileInputStream fi = new FileInputStream(file);
			mem = new byte[fi.available()];
			fi.read(mem);
			fi.close();
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		os.format("; Disassembled from %s\n", file.getName());
		adr = 0;
		while (adr < mem.length && adr <= wi.maxPC()) {
			if (tape && (mem[adr] & 0xff) == wi.endProg() &&
					adr + 1 < mem.length) {
				++adr;
				if ((mem[adr] & 0xff) != wi.endProg()) {
					os.format("EOD\n");
				}
				continue;
			}
			e = wi.decode(mem, adr);
			if (!lst) {
				os.format("\t%s\n", e.mnemonic);
				adr += e.length;
				continue;
			}
			int end = adr + e.length;
			// TODO: stop at END PROG?
			os.format("%04d  %02d-%02d\t%s\n", adr, high(mem[adr]), low(mem[adr]), e.mnemonic);
			++adr;
			while (adr < end) {
				os.format("%04d  %02d-%02d\n", adr, high(mem[adr]), low(mem[adr]));
				++adr;
			}
		}
		return 0;
	}
}
