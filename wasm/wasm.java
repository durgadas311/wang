// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.io.*;

public class wasm {
	boolean dis = false;
	WangDisassembler disas = null;
	WangInstructions mach = null;
	File file;

	public wasm(String[] args) {
		for (String arg : args) {
			if (arg.startsWith("dis=")) {
				dis = true;
				file = new File(arg.substring(4));
			} else if (arg.equals("600")) {
				mach = new Wang600Instructions(null);
			}
		}
		if (mach == null) {
			mach = new Wang600Instructions(null);
		}
		if (dis) {
			disas = new WangDisassembler(mach);
			disas.disas(file, System.out);
			System.exit(0);
		}
		System.err.format("Unsupported mode\n");
		System.exit(1);
	}

	public static void main(String[] args) {
		new wasm(args);
	}
}
