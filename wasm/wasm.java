// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class wasm {
	boolean dis = false;
	boolean raw = false;
	boolean w600 = false;
	boolean w700 = false;
	boolean docs = false;
	boolean tape = false;
	boolean rom = false;
	WangDisassembler disas = null;
	WangAssembler asm = null;
	WangInstructions mach = null;
	File file = null;
	File aout = null;
	PrintStream list = System.out;
	String dev = null;

	private void help() {
		System.err.format("Usage: wasm [options] dis=<file>\n" +
				  "       wasm [options] asm=<file>\n" +
				  "       wasm { 600 | 700 } docs\n");
		System.err.format("Options:\n");
		System.err.format("600        Use Wang 600 machine codes\n");
		System.err.format("700        Use Wang 700 machine codes\n");
		System.err.format("tape       Treat as tape image\n");
		System.err.format("nolst      Do not produce assembly listing\n");
		System.err.format("lst=<file> Produce assembly listing to file (stdout)\n");
		System.err.format("out=<file> Produce output to file (a.out/stdout)\n");
		System.err.format("raw        Disassemble as source, not listing\n");
		System.err.format("docs       Dump instruction codes and mnemonics\n");
	}

	public wasm(String[] args) {
		if (args.length == 0) {
			help();
			System.exit(0);
		}
		for (String arg : args) {
			if (arg.startsWith("dis=")) {
				dis = true;
				file = new File(arg.substring(4));
			} else if (arg.startsWith("asm=")) {
				dis = false;
				file = new File(arg.substring(4));
			} else if (arg.matches("6[01][012]")) {
				w600 = true;
				dev = "W" + arg;
			} else if (arg.matches("7[01][012]")) {
				w700 = true;
				dev = "W" + arg;
			} else if (arg.equals("raw")) {
				raw = true;
			} else if (arg.equals("tape")) {
				tape = true;
			} else if (arg.equals("rom")) {
				rom = true;
			} else if (arg.equals("nolst")) {
				list = null;
			} else if (arg.startsWith("lst=")) {
				try {
					list = new PrintStream(new
						FileOutputStream(arg.substring(4)));
				} catch (Exception ee) {
					ee.printStackTrace();
					System.exit(1);
				}
			} else if (arg.startsWith("out=")) {
				aout = new File(arg.substring(4));
			} else if (arg.equals("docs")) {
				docs = true;
			}
		}
		if (!w600 && !w700 && file != null) {
			String s = file.getName();
			if (s.matches(".*\\.w7[a-z]")) w700 = true;
			if (s.matches(".*\\.w6[a-z]")) w600 = true;
		}
		if (!w600 && !w700 && aout != null) {
			String s = aout.getName();
			if (s.matches(".*\\.w7[a-z]")) w700 = true;
			if (s.matches(".*\\.w6[a-z]")) w600 = true;
		}
		if (!w600 && !w700 && rom) {
			w600 = true;
		}
		if (rom && w700) {
			System.err.format("ROM only supported for 600\n");
			System.exit(1);
		}
		if (w700) {
			mach = new Wang700Instructions();
		} else if (w600) {
			mach = new Wang600Instructions(rom);
		} else {
			System.err.format("No machine specified\n");
			System.exit(1);
		}
		if (docs) {
			for (int op = 0; op < 256; ++op) {
				WangInstruction i = mach.decodeOp(op);
				if (i == null) continue;
				if (i.flags == WangInstructions.REG100) continue;
				if (i.mnemonic.endsWith("*")) continue; // dups
				System.out.format("%02d-%02d %s",
					(op >> 4), (op & 0x0f), i.mnemonic);
				switch (i.flags) {
				case WangInstructions.MARK:
				case WangInstructions.LABEL:
					System.out.format(" <label>\n");
					break;
				case WangInstructions.REG:
					System.out.format(" <reg: 0..%d>\n", mach.maxReg());
					break;
				case WangInstructions.FMT:
					System.out.format(" %s\n", mach.printHelp());
					break;
				case WangInstructions.ALPHA:
					System.out.format(" { \"text...\" | <keycode> }\n");
					break;
				case WangInstructions.IO:
					System.out.format(" <iocode>\n");
					break;
				case WangInstructions.INDIR:
					System.out.format(" <keycode>\n");
					break;
				case WangInstructions.NONE:
				default:
					System.out.format("\n");
					break;
				}
			}
			System.out.format("      .REG %s\n", mach.regHelp());
			System.out.format("      .OUT <device>\n");
			System.out.format("      .INCLUDE <file>\n");
			System.exit(0);
		}
		if (dev != null) {
			if (mach.setOutput(dev) < 0) {
				System.err.format("Invalid device \"%s\"\n", dev);
				System.exit(1);
			}
		}
		if (dis) {
			if (aout != null) {
				try {
					list = new PrintStream(new FileOutputStream(aout));
				} catch (Exception ee) {
					ee.printStackTrace();
					System.exit(1);
				}
			} else {
				list = System.out;
			}
			tape = tape || file.getName().matches(".*\\.w[67]t");
			rom = rom || file.getName().matches(".*\\.w6x");
			disas = new WangDisassembler(mach, !raw, tape, rom);
			disas.disas(file, list);
			System.exit(0);
		}
		if (aout == null) {
			aout = new File("a.out");
		}
		tape = tape || (aout != null && aout.getName().matches(".*\\.w[67]t"));
		asm = new WangAssembler(mach, tape, rom);
		int foo = asm.asm(file, aout, list);
		if (foo != 0) {
			System.err.format("%d Errors in assembly\n", foo);
		}
		System.exit(foo == 0 ? 0 : 1);
	}

	public static void main(String[] args) {
		new wasm(args);
	}
}
