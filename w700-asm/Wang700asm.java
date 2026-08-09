// Copyright (c) 2025 Douglas Miller <durgadas311@gmail.com>

import java.util.Properties;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Wang700asm {
	private Wang700Assembler front_end;

	public Wang700asm(String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("-d")) {
			disas(args);
			System.exit(0);
		}
		front_end = new Wang700Assembler(args);
	}

	public static void main(String[] args) {
		new Wang700asm(args);
	}

	private void disas(String[] args) {
		boolean trim = false;
		boolean raw = false;
		File f = null;
		for (String arg : args) {
			if (arg.equalsIgnoreCase("-d")) continue;
			if (arg.equalsIgnoreCase("-z")) {
				trim = true;
				continue;
			}
			if (arg.equalsIgnoreCase("-r")) {
				raw = true;
				continue;
			}
			f = new File(arg);
		}
		if (f == null || !f.exists()) {
			System.err.format("Usage: Wang700asm -d [-z] [-r] <rom-file>\n");
			System.exit(1);
		}
		byte[] rom = new byte[8 * 2048];
		try {
			InputStream is = new FileInputStream(f);
			int n = is.read(rom);
			is.close();
			if (n != rom.length) {
				System.err.format("%s: wrong size\n", f.getName());
				System.exit(1);
			}
		} catch (Exception ee) {
			System.err.format("%s: %s\n", f.getName(), ee.getMessage());
			System.exit(1);
		}
		int end = rom.length / 8 - 1;
		if (trim) {
			while (end >= 0 && isZero(rom, end)) --end;
			if (end < 0) {
				System.err.format("%s: all zero\n", f.getName());
				System.exit(1);
			}
		}
		Wang700_CPU cpu = new Wang700_CPU(new Properties(), rom, null, null);
		for (int adr = 0; adr <= end; ++adr) {
			String dis = cpu.disas(adr, raw);
			System.out.format("%03x: %s\n", adr, dis);
		}
	}

	private boolean isZero(byte[] rom, int adr) {
		int idx = adr * 8;
		int i = rom[idx] | rom[idx + 1] | rom[idx + 2] | rom[idx + 3] |
			rom[idx + 4] | rom[idx + 5] | rom[idx + 6] | rom[idx + 7];
		return (i == 0);
	}
}
