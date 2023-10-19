// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class WangSymbol {
	public Vector<Integer> refs;
	public String nam;
	public int val;
	public boolean def;

	public WangSymbol(String lab) {
		refs = new Vector<Integer>();
		val = 0xff;
		def = false;
		nam = lab;
	}

	public WangSymbol(String lab, int reg) {
		refs = new Vector<Integer>();
		val = reg;
		def = true;
		nam = lab;
	}

	public void define(int reg) {
		val = reg;
		def = true;
	}
}
