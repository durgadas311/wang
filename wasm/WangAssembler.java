// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.io.*;

public class WangAssembler implements WangMemory {
	Vector<File> paths;
	WangInstructions wi;
	boolean tape;
	boolean rom;
	boolean raw;
	int errs = 0;
	boolean stdout;
	int startPC;
	int maxPC;
	int regPC;

	byte[] mem;
	int adr;

	public WangAssembler(WangInstructions wi, boolean tape, boolean rom, boolean raw,
			Vector<File> paths) {
		this.paths = paths;
		this.wi = wi;
		this.tape = tape;
		this.rom = rom;
		this.raw = raw;
		startPC = 0;
		regPC = -1;
		if (rom) {
			maxPC = wi.maxRomPC() + 1;
		} else {
			maxPC = wi.maxPC() + 1;
		}
	}

	public int getMem(int adr) {
		if (adr < startPC || adr >= maxPC) {
			return -1;
		}
		adr -= startPC;
		if (mem == null) {
			return 0;
		}
		return mem[adr] & 0xff;
	}

	public boolean putMem(int adr, int val) {
		if (adr < startPC || adr >= maxPC) {
			return true;
		}
		adr -= startPC;
		if (mem == null) {
			mem = new byte[(maxPC - startPC) + 2];
			Arrays.fill(mem, (byte)0);
		}
		mem[adr] = (byte)val;
		return false;
	}

	private int low(int adr) {
		int b = getMem(adr);
		if (b < 0) return 0;
		return (b & 0x0f);
	}

	private int high(int adr) {
		int b = getMem(adr);
		if (b < 0) return 0;
		return ((b >> 4) & 0x0f);
	}

	private int objectOut(byte[] mem, int start, int len, OutputStream out) {
		try {
			out.write(mem, start, len);
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		return 0;
	}

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

	private int do_prog(String[] toks, int start) {
		int n = start + 1;	// skip .PROG
		int i;

		// must be before program starts,
		// and cannot specify <regs> in ROM.
		if ((!wi.finalPass() && mem != null) || (rom && n + 2 < toks.length)) {
			return 'X';
		}
		if (n >= toks.length) return 'S';	// syntax error
		try {
			i = Integer.valueOf(toks[n++]);
		} catch (Exception ee) { return 'S'; }
		if (i < 0 || i > wi.maxPC()) return 'S';
		startPC = i;
		if (n < toks.length) {
			try {
				i = Integer.valueOf(toks[n++]);
			} catch (Exception ee) { return 'S'; }
			if (i <= startPC || i > wi.maxPC()) return 'S';
			maxPC = i + 1;
		}
		if (n < toks.length) {
			try {
				i = Integer.valueOf(toks[n++]);
			} catch (Exception ee) { return 'S'; }
			if (i < 0 || i > wi.maxPC()) return 'S';
			regPC = wi.regPad(i);
		}

		adr = startPC; // reset to new start
		return 0;
	}

	private String slab(String sym) {
		if (sym.length() == 0 || sym.equals("-")) return null;
		if (sym.charAt(0) == '&') return sym.substring(1);
		return sym;
	}

	private void error(char c, String line, PrintStream ls) {
		errorList(String.format("%c               %s", c, line), -1, ls);
	}

	// returns 0 on success, else error letter.
	private int do_sreg(WangSLabel lab) {
		int e;

		if (rom) {
			return 'X';
		}
		if (!wi.finalPass()) {	// pass 1
			return (wi.getSymTab().addSLabel(lab) < 0 ? 'M' : 0);
		}
		// pass 2
		e = wi.getSymTab().chkSLabel(lab);
		if (e < 0) {
			if (e == -1) return 'M';
			return 'Z';
		}
		return 0;
	}

	private File search(String fn) {
		File f = new File(fn);
		File ff;

		if (f.isAbsolute() || f.isFile()) return f;
		for (File d : paths) {
			ff = new File(d, fn);
			if (ff.isFile()) return ff;
		}
		return f;
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
			if (toks[n].equalsIgnoreCase(".PROG")) {
				// .PROG <start> [<end> [<regs>]]
				// define program space.
				n = do_prog(toks, n);
				errorList(String.format("%c               %s",
							n == 0 ? ' ' : n, line),
						n == 0 ? 0 : -1, ls);
				continue;
			} else if (toks[n].equalsIgnoreCase(".REG")) {
				int m = wi.regPad(this, adr);
				int end = adr + m;
				while (adr < end) {
					errorList(String.format(" %04d  %02d-%02d     " +
						"; Register Pad",
						adr, high(adr), low(adr)),
						0, ls);
					++adr;
				}
				n = wi.dreg(toks, n, this, adr);
				line += wi.adrRegStr(adr);
			} else if (toks[n].equalsIgnoreCase(".SREG")) {
				WangSLabel lab = new WangSLabel();
				++n;	// skip ".SREG"
				if (n >= toks.length) {
					error('S', line, ls);
					continue;
				}
				String[] ss = toks[n++].split(",");
				if (ss.length > 2) {
					error('S', line, ls);
					continue;
				}
				lab.low.nam = slab(ss[0]);
				if (ss.length > 1) {
					lab.high.nam = slab(ss[1]);
				}
				if (n < toks.length) {
					lab.count = Integer.valueOf(toks[n++]);
				}
				n = do_sreg(lab);
				errorList(String.format("%c               %s (%d,%d)",
					(n == 0 ? ' ' : n), line,
					lab.low.val, lab.high.val),
					(n == 0 ? 0 : -1), ls);
				continue;
			} else if (toks[n].equalsIgnoreCase(".OUT")) {
				n = wi.setOutput(toks, n);
				errorList(String.format("%c               %s",
							wi.lastError(), line),
					n, ls);
				continue;
			} else if (toks[n].equalsIgnoreCase(".INCLUDE")) {
				++n;
				if (n >= toks.length) {
					error('S', line, ls);
					continue;
				}
				errorList(String.format(">               %s", line),
					0, ls);
				File f = search(toks[n]);
				n = asm(f, ls); // errs counted
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
			} else if (toks[n].equalsIgnoreCase(".DEF")) {
				// pre-defined symbols
				n = wi.def(toks, n);
				errorList(String.format("%c               %s",
							wi.lastError(), line),
					n, ls);
				continue;
			} else {
				n = wi.encode(toks, n, this, adr);
			}
			errorList(String.format("%c%04d  %02d-%02d    %s",
						wi.lastError(),
						adr, high(adr), low(adr), line),
					n, ls);
			if (ls == null) {
				adr += (n > 0 ? n : -n);
			} else {
				int end = adr + (n > 0 ? n : -n);
				++adr;
				while (adr < end) {
					errorList(String.format(" %04d  %02d-%02d",
						adr, high(adr), low(adr)),
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

		errs = 0;
		adr = startPC;
		// start first pass
		wi.finalPass(false);
		n = asm(file, ls);
		if (n < 0) return n;
		// prepare for second pass
		if (regPC < 0) {
			regPC = wi.regPad(adr);
		}
		wi.getSymTab().resolveMarks(wi.adrReg(regPC));
		errs = 0;
		adr = startPC;
		wi.getSymTab().reset();
		// start second pass
		wi.finalPass(true);
		n = asm(file, ls);
		if (n < 0) return errs;
		if (mem == null) return 1;

		if (rom) {
			if (adr < maxPC) {
				Arrays.fill(mem, adr - startPC, maxPC - startPC,
						(byte)wi.stop());
				adr = maxPC;
			}
		} else if (tape) {
			if ((mem[adr - 1] & 0xff) == wi.endProg()) {
				mem[adr++] = (byte)wi.endProg();
			} else {
				mem[adr++] = (byte)wi.endProg();
				mem[adr++] = (byte)0xff;
			}
		}
		n = objectOut(mem, adr - startPC, os);
		if (n < 0) return n;
		return errs;
	}

	// Convert ASCII list of numbers into a block of register data.
	// Note that register data is backwards: array[0] is the last.
	// 
	public int data(File file, File os) {
		BufferedReader in;
		String line = null;
		int reg;
		int num = 0;
		int err = 0;
		int i, n;

		if (rom) {
			System.err.format("ROM may not contain data\n");
			return -1;
		}
		// Wang 700 tape data is marginally useful, but will be allowed.
		// Note that Wang 700 requires an END PROG on tape, while
		// Wang 600 handles a simple end-of-block.
		try {
			in = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException fnf) {
			System.err.format("No file: %s", file.getName());
			return -1;
		} catch (Exception ee) {
			// TODO: print cleaner message
			ee.printStackTrace();
			return -1;
		}
		// Use full address space...
		maxPC = wi.regAdr(-1); // end of RAM, incl reserved registers
		reg = 0;
		// TODO: must reverse the order!
		do {
			for (i = 0; i < wi.regsPerBlk(); ++i) {
				try {
					line = in.readLine();
				} catch (Exception ee) {
					line = null;
				}
				if (line == null) break;
				++num;
				// TODO: comments?
				// TODO: allow end-of-block directives?
				adr = wi.regAdr(reg);
				n = wi.setReg(reg, line, this, adr);
				if (n < 0) {
					++err;
					System.err.format("%c %3d: %s\n",
						wi.lastError(), num, line);
				}
				++reg;
			}
			if (line == null) break;
		} while (reg <= wi.maxReg());
		if (reg > wi.maxReg()) {
			// TODO: only error if more data in file...
			// TODO: automatically break into chunks?
			System.err.format("Memory Overflow\n");
			return -1;
		}
		if (num == 0) {
			System.err.format("No Data\n");
			return -1;
		}
		startPC = adr;
		adr = maxPC;
		if (!raw) {
			mem[adr++] = (byte)wi.endProg();
			if (tape) {
				mem[adr++] = (byte)wi.endData();
			}
		}
		FileOutputStream fo;
		try {
			fo = new FileOutputStream(os);
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		if (!raw) {
			WangRegBlock blk = new WangRegBlock(wi);
			int a = 0;

			wi.setReg(0, String.format("%d", num), blk, a);
			a += wi.regBlkLen();
			blk.putMem(a++, wi.endProg());
			if (tape) {
				blk.putMem(a++, wi.endData());
			}
			n = objectOut(blk.mem, 0, a, fo);
			// TODO: errors
		}
		// TODO: write output file...
		n = objectOut(mem, startPC, adr - startPC, fo);
		// TODO: errors
		try {
			fo.close();
		} catch (Exception ee) {
			ee.printStackTrace();
			return -1;
		}
		// TODO: return error status
		return 0;
	}
}
