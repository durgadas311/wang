// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.io.*;

public class WangAssembler {
	WangInstructions wi;
	boolean tape;
	boolean rom;
	int errs = 0;
	boolean stdout;

	byte[] mem;
	int adr;

	public WangAssembler(WangInstructions wi, boolean tape, boolean rom) {
		this.wi = wi;
		this.tape = tape;
		this.rom = rom;
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

	// Send output to listing, and if error also to stderr
	private void errorList(String line, int ret, PrintStream ls) {
		if (!wi.finalPass()) return;
		if (ret < 0) ++errs;
		if (ret < 0 && !stdout) {
			System.err.format("%s\n", line);
		}
		if (ls != null) {
			ls.format("%s\n", line);
		}
	}

	private int asm(File file, PrintStream ls) {
		int n;
		WangInstruction e;
		BufferedReader in;
		String line;
		String s;
		String[] toks;
		int ie = errs;

		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException fnf) {
			errorList(String.format("No file: %s", file.getName()),
				-1, ls);
			return -1;
		} catch (Exception ee) {
			// TODO: print cleaner message
			ee.printStackTrace();
			return -1;
		}

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
			// TODO: fix this for e.g. ALPHA "...;..."
			s = line.replaceFirst(";.*$", "");
			toks = s.split("\\s(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
			n = 0;
			while (n < toks.length && toks[n].length() == 0) ++n;
			if (n >= toks.length) {
				errorList(String.format("                %s", line),
					0, ls);
				continue;
			}
			if (toks[n].equalsIgnoreCase(".REG")) {
				int m = wi.regPad(mem, adr);
				int end = adr + m;
				while (adr < end) {
					errorList(String.format(" %04d  %02d-%02d\n",
						adr, high(mem[adr]), low(mem[adr])),
						0, ls);
					++adr;
				}
				n = wi.dreg(toks, n, mem, adr);
				line += wi.adrRegStr(adr);
			} else if (toks[n].equalsIgnoreCase(".OUT")) {
				n = wi.setOutput(toks, n);
				errorList(String.format("%c               %s",
							wi.lastError(), line),
					n, ls);
				continue;
			} else if (toks[n].equalsIgnoreCase(".INCLUDE")) {
				++n;
				if (n >= toks.length) {
					errorList(String.format("S               %s",
								wi.lastError(), line),
						-1, ls);
					continue;
				}
				errorList(String.format(">               %s", line),
					0, ls);
				n = asm(new File(toks[n]), ls); // errs counted
				errorList(String.format("<               %s", line),
					0, ls);
				continue;
			} else if (toks[n].equalsIgnoreCase(".EXT") ||
					toks[n].equalsIgnoreCase(".EXTROM")) {
				// external label(s)
				n = wi.xlab(toks, n);
				errorList(String.format("%c               %s",
							wi.lastError(), line),
					n, ls);
				continue;
			} else {
				n = wi.encode(toks, n, mem, adr);
			}
			errorList(String.format("%c%04d  %02d-%02d     %s",
						wi.lastError(),
						adr, high(mem[adr]), low(mem[adr]), line),
					n, ls);
			if (ls == null) {
				if (n > 0) adr += n;
			} else {
				int end = adr + n;
				++adr;
				while (adr < end) {
					errorList(String.format(" %04d  %02d-%02d\n",
						adr, high(mem[adr]), low(mem[adr])),
						0, ls);
					++adr;
				}
			}
			if (errs > 10) {
				System.err.format("Too many errors\n");
				break;
			}
		}
		return errs > ie ? -1 : 0;
	}

	public int asm(File file, File os, PrintStream ls) {
		int n;
		stdout = (ls == System.out);
		int max;

		if (rom) {
			max = wi.maxRomPC() + 1;
		} else {
			max = wi.maxPC() + 1;
		}
		mem = new byte[max];
		errs = 0;
		adr = 0;
		wi.finalPass(false);
		n = asm(file, ls);
		if (n < 0) return n;
		Arrays.fill(mem, (byte)0);
		errs = 0;
		adr = 0;
		wi.finalPass(true);
		n = asm(file, ls);
		if (n < 0) return errs;

		if (rom) {
			if (adr < max) {
				Arrays.fill(mem, adr, max, (byte)wi.stop());
			}
		} else if (tape) {
			if ((mem[adr - 1] & 0xff) == wi.endProg()) {
				mem[adr++] = (byte)wi.endProg();
			} else {
				mem[adr++] = (byte)wi.endProg();
				mem[adr++] = (byte)0xff;
			}
		}
		for (Map.Entry<Integer,Integer> sym : wi.getSymTab().getMarks()) {
			int key = sym.getKey();
			int val = sym.getValue();
			if (val < 0) {
				errorList(String.format("Undefined: %s %02d-%02d",
					(key & 0x100) != 0 ? "ROM" : "",
					(key >> 4) & 0x0f, key & 0x0f), -1, ls);
			}
		}
		n = objectOut(mem, adr, os);
		if (n < 0) return n;
		return errs;
	}
}
