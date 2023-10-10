// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class wasm {
	boolean dis = false;
	boolean raw = false;
	boolean w600 = true;
	WangDisassembler disas = null;
	WangAssembler asm = null;
	WangInstructions mach = null;
	File file;

	public wasm(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("dis=")) {
				dis = true;
				file = new File(arg.substring(4));
			} else if (arg.startsWith("asm=")) {
				dis = false;
				file = new File(arg.substring(4));
			} else if (arg.equals("600")) {
				w600 = true;
			} else if (arg.equals("raw")) {
				raw = true;
			}
		}
		if (w600) {
			mach = new Wang600Instructions(null);
		} else {
			System.err.format("No machine specified\n");
			System.exit(1);
		}
		if (dis) {
			disas = new WangDisassembler(mach, !raw);
			disas.disas(file, System.out);
			System.exit(0);
		}
		asm = new WangAssembler(mach, true);
		int foo = asm.asm(file, new File("a.out"), System.out);
		System.exit(foo == 0 ? 0 : 1);
	}

	public static void main(String[] args) {
		new wasm(args);
	}
}
