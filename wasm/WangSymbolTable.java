// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class WangSymbolTable {
	private Vector<WangSymbol> syms;
	private WangRegFixer fix;

	public WangSymbolTable(WangRegFixer fix) {
		this.fix = fix;
		syms = new Vector<WangSymbol>();
	}

	private WangSymbol lookup(String lab) {
		for (WangSymbol sym : syms) {
			if (sym.nam.equalsIgnoreCase(lab)) {
				return sym;
			}
		}
		return null;
	}

	public Vector<WangSymbol> getSyms() {
		return syms;
	}

	public int setLabel(String lab, int val, byte[] mem) {
		if (lab == null || lab.length() == 0 || lab.equals("-")) {
			return 0;
		}
		WangSymbol sym = lookup(lab);
		if (sym == null) {
			sym = new WangSymbol(lab, val);
			syms.add(sym);
		} else {
			sym.define(val);
		}
		for (Integer ref : sym.refs) {
			fix.fixReg(mem, ref, val);
		}
		sym.refs.clear();
		return 0;
	}

	public int getLabel(String lab, int ref) {
		WangSymbol sym = lookup(lab);

		if (sym == null) {
			sym = new WangSymbol(lab);
			syms.add(sym);
		}
		if (!sym.def) {
			sym.refs.add(ref);
		}
		return sym.val;
	}
}
