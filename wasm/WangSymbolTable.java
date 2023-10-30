// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class WangSymbolTable {
	private Vector<WangSymbol> regs;
	private Vector<WangSymbol> syms;
	private Map<Integer,Integer> labs;
	private boolean[] lset;

	public WangSymbolTable() {
		regs = new Vector<WangSymbol>();
		syms = new Vector<WangSymbol>();
		labs = new HashMap<Integer,Integer>();
		lset = new boolean[256];
	}

	private WangSymbol lookup(String lab) {
		for (WangSymbol sym : regs) {
			if (sym.nam.equalsIgnoreCase(lab)) {
				return sym;
			}
		}
		return null;
	}

	private WangSymbol find(String lab) {
		for (WangSymbol sym : syms) {
			if (sym.nam.equalsIgnoreCase(lab)) {
				return sym;
			}
		}
		return null;
	}

	public Vector<WangSymbol> getSyms() {
		return regs;
	}

	// define register label
	public int setLabel(String lab, int val, byte[] mem) {
		if (lab == null || lab.length() == 0 || lab.equals("-")) {
			return 0;
		}
		WangSymbol sym = lookup(lab);
		if (sym != null) {
			if (sym.val != val) {
				// multiple definitions... error...
				return -1;
			}
			return 0;
		}
		sym = new WangSymbol(lab, val);
		regs.add(sym);
		return 0;
	}

	// reference register label
	public int getLabel(String lab, int ref) {
		WangSymbol sym = lookup(lab);

		if (sym == null) {
			// undefined... error... (unless first pass)
			return -1;
		}
		return sym.val;
	}

	// define a symbolic program label - MARK &xxxx
	// returns label code, if resolved (second pass).
	public int setMark(String key, int ref) {
		WangSymbol sym = find(key);

		if (sym != null) {
			if (sym.ref != ref) {
				return -1;
			}
		} else {
			sym = new WangSymbol(ref, key);
			syms.add(sym);
		}
		return sym.val;
	}

	// reference a symbolic program label
	public int getMark(String key, int ref, int type) {
		WangSymbol sym = find(key);

		if (sym == null) {
			// undefined... error... (unless first pass)
			return -1;
		}
		return sym.val; // on second pass, should be resolved
	}

	// define a program label - MARK xx
	public int setMark(int key, int ref, boolean rom) {
		if (rom) {
			key |= 0x100;
		}
		if (labs.containsKey(key)) {
			if (labs.get(key) != ref) {
				// multiple definitions... error...
				return -1;
			}
			return 0;
		}
		labs.put(key, ref);
		lset[key & 0xff] = true;	// label no longer free
		return 0;
	}

	// reference a program label
	public int getMark(int key, int ref, int type) {
		if (type == WangInstructions.ROMARK ||
				type == WangInstructions.FROM) {
			key |= 0x100;
		}
		if (!labs.containsKey(key)) {
			// undefined... error... (unless first pass)
			return -1;
		}
		return 0;
	}

	// do not freely assign these labels
	public void reserveMark(int code) {
		lset[code] = true;
	}
	public void reserveMarks(int low, int high) {
		for (int x = low; x < high && x < lset.length; ++x) {
			reserveMark(x);
		}
	}

	private int getFreeMark() {
		int x;
		for (x = 0; x < lset.length && lset[x]; ++x);
		if (x < lset.length) {
			lset[x] = true;
			return x;
		}
		return -1;
	}

	public int resolveMarks() {
		for (WangSymbol sym : syms) {
			// can any be already resolved?
			sym.val = getFreeMark();
		}
		return 0;
	}
}
