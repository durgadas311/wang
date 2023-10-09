// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class WangDisassembler {
	WangInstructions wi;

	public WangDisassembler(WangInstructions wi) {
		this.wi = wi;
	}

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
		adr = 0;
		while (adr < mem.length && adr < 1848) {
			e = wi.decode(mem, adr);
			// TODO: stop at END PROG?
			os.format("%04d  %s\n", adr, e.mnemonic);
			adr += e.length;
		}
		return 0;
	}
}
