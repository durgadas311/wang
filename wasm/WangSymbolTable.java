// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class WangSymbolTable {
	private Vector<WangSymbol> syms;
	private Map<Integer,Integer> labs;

	public WangSymbolTable() {
		syms = new Vector<WangSymbol>();
		labs = new HashMap<Integer,Integer>();
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
		if (sym != null) {
			if (sym.val != val) {
				// multiple definitions... error...
				return -1;
			}
			return 0;
		}
		sym = new WangSymbol(lab, val);
		syms.add(sym);
		return 0;
	}

	public int getLabel(String lab, int ref) {
		WangSymbol sym = lookup(lab);

		if (sym == null) {
			// undefined... error...
			return -1;
		}
		return sym.val;
	}

	// define or reference a label
	public void setMark(int key, int ref, int type, boolean rom) {
		switch (type) {
		case WangInstructions.LABEL:
			if (rom) {
				key |= 0x100;
			}
			if (!labs.containsKey(key)) {
				labs.put(key, ref);
			} else {
				labs.replace(key, ref);
			}
			break;
		case WangInstructions.ROMARK:	// SEARCH, CALL ROM
		case WangInstructions.FROM:	// f(x) ROM
			key |= 0x100;
			// FALLTHROUGH
		case WangInstructions.MARK:	// SEARCH, CALL, ...
		case WangInstructions.FCALL:	// f(x)
			if (labs.containsKey(key)) break;
			labs.put(key, -ref);
			break;
		default:
			break;
		}
	}

	public Set<Map.Entry<Integer,Integer>> getMarks() {
		return labs.entrySet();
	}
}
