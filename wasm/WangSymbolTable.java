// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class WangSymbolTable {
	private Vector<WangSymbol> regs;
	private Vector<WangSymbol> syms;
	private Vector<WangSymbol> subs;
	private Map<Integer,Integer> labs;
	private Vector<WangSLabel> slbs;
	private boolean[] lset;
	private int subrLo, subrHi;

	public WangSymbolTable(int lo, int hi) {
		regs = new Vector<WangSymbol>();
		syms = new Vector<WangSymbol>();
		subs = new Vector<WangSymbol>();
		labs = new HashMap<Integer,Integer>();
		slbs = new Vector<WangSLabel>();
		lset = new boolean[256];
		subrLo = lo;
		subrHi = hi;
	}

	// reset anything between passes
	public void reset() {
		WangSLabel.reset();
	}

	// check all register sources to ensure 'sym' is globally unique.
	// only called in pass 2, where all instances should exist.
	private boolean isUniq(String name, int ref) {
		int n = 0;
		if (name == null) return true;
		for (WangSymbol sym : regs) {
			if (sym.nam.equalsIgnoreCase(name)) {
				++n;
			}
		}
		for (WangSLabel lab : slbs) {
			if (lab.low.ref == ref) continue;
			if (lab.low.nam.equalsIgnoreCase(name) ||
				lab.high.nam.equalsIgnoreCase(name)) {
				++n;
			}
		}
		return (n < 2);
	}

	private WangSymbol lookup(String lab) {
		for (WangSymbol sym : regs) {
			if (sym.nam.equalsIgnoreCase(lab)) {
				return sym;
			}
		}
		return null;
	}

	private WangSLabel slookup(WangSLabel lab) {
		for (WangSLabel lb : slbs) {
			if (lb.low.ref == lab.low.ref) {
				return lb;
			}
		}
		return null;
	}

	// Find symbol by key code, in either syms or subs
	private WangSymbol find(int code) {
		for (WangSymbol sym : syms) {
			if (sym.val == code) return sym;
		}
		for (WangSymbol sym : subs) {
			if (sym.val == code) return sym;
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

	private WangSymbol subr(String lab) {
		for (WangSymbol sym : subs) {
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
	public int setLabel(String lab, int val) {
		if (lab == null || lab.length() == 0 || lab.equals("-")) {
			return 0;
		}
		if (lab.charAt(0) == '&') lab = lab.substring(1);
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
		if (lab.charAt(0) == '&') lab = lab.substring(1);
		WangSymbol sym = lookup(lab);

		if (sym == null) {
			// undefined... error... (unless first pass)
			return -1;
		}
		return sym.val;
	}

	// pre-define symbolic program label
	// (caller avoids this on second pass?)
	public int defMark(String key, int val) {
		WangSymbol sym = find(key);

		if (sym != null) {
			if (sym.val != val) {
				return -1;	// multiple def
			}
		} else {
			sym = new WangSymbol(key, val);
			syms.add(sym);
			lset[val & 0xff] = true;	// label no longer free
		}
		return 0;
	}

	// define a symbolic program label - MARK &xxxx
	// returns label code, if resolved (second pass).
	public int setMark(String key, int ref) {
		WangSymbol sym = find(key);

		if (sym != null) {
			if (sym.ref < 0) {
				sym.ref = ref;
			} else if (sym.ref != ref) {
				return -1;
			}
		} else {
			sym = new WangSymbol(ref, key);
			syms.add(sym);
		}
		return sym.val;
	}

	// reference a symbolic program label - e.g. SEARCH &xxxx
	public int getMark(String key, int ref, int type) {
		WangSymbol sym = find(key);

		if (sym == null || sym.ref < 0) {
			// undefined... error... (unless first pass)
			return -1;
		}
		return sym.val; // on second pass, should be resolved
	}

	// pre-define symbolic subroutine label
	// (caller avoids this on second pass?)
	public int defSubr(String key, int val) {
		WangSymbol sym = subr(key);

		if (sym != null) {
			if (sym.val != val) {
				return -1;	// multiple def
			}
		} else {
			sym = new WangSymbol(key, val);
			subs.add(sym);
			lset[val & 0xff] = true;	// label no longer free
		}
		return 0;
	}

	// define a symbolic subroutine label - MARK $xxxx
	// returns label code, if resolved (second pass).
	public int setSubr(String key, int ref) {
		WangSymbol sym = subr(key);

		if (sym != null) {
			if (sym.ref < 0) {
				sym.ref = ref;
			} else if (sym.ref != ref) {
				return -1;
			}
		} else {
			sym = new WangSymbol(ref, key);
			subs.add(sym);
		}
		return sym.val;
	}

	// reference a symbolic subroutine label - e.g. $xxxx
	// (the label *is* the command/instruction)
	public int getSubr(String key, int ref, int type) {
		WangSymbol sym = subr(key);

		if (sym == null || sym.ref < 0) {
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

	// define an external program label
	public int setExt(int key, int ref, boolean rom) {
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
		WangSymbol sym = find(key & 0xff);
		if (sym == null) return 0;
		if (sym.ref >= 0 && sym.ref != ref) {
			return -1;	// multiple defs
		}
		sym.ref = ref;
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
		for (x = 0; x < lset.length && lset[x]; ++x) {
			if (x == subrLo) x = subrHi - 1;
		}
		if (x < lset.length) {
			lset[x] = true;
			return x;
		}
		return -2;
	}

	private int getFreeSubr() {
		int x;
		for (x = subrLo; x < subrHi && lset[x]; ++x);
		if (x < subrHi) {
			lset[x] = true;
			return x;
		}
		return -2;
	}

	// called during pass 1
	public int addSLabel(WangSLabel lab) {
		WangSymbol sym;

		slbs.add(lab);
		if (lab.low.nam != null) {
			sym = lookup(lab.low.nam);
			if (sym == null) {
				regs.add(lab.low);
			} else if (sym.ref != lab.low.ref) {
				lab.err = -1;	// multiple defs
			}
		}
		if (lab.high.nam != null) {
			sym = lookup(lab.high.nam);
			if (sym == null) {
				regs.add(lab.high);
			} else if (sym.ref != lab.high.ref) {
				lab.err = -1;	// multiple defs
			}
		}
		return 0;
	}

	// called during pass 2
	public int chkSLabel(WangSLabel lab) {
		// should already exist in 'slbs', plus
		// already has symbols in 'regs'...

		WangSLabel lb = slookup(lab); // lookup by ref
		if (lb == null) {	// should not be possible
			return -1;
		}
		lab.err = lb.err;
		lab.low.val = lb.low.val;
		lab.high.val = lb.high.val;
		return lb.err;
	}

	private void dump(String tag) {
		for (WangSymbol sym : this.regs) {
			System.err.format("%s REGS \"%s\" %02x/%d\n", tag,
				sym.nam, sym.val, sym.ref);
		}
		for (WangSLabel lab : slbs) {
			System.err.format("%s SLBS %d \"%s\" %d/%d : \"%s\" %d/%d\n", tag,
				lab.count, lab.low.nam, lab.low.val, lab.low.ref,
				lab.high.nam, lab.high.val, lab.high.ref);
		}
		for (WangSymbol sym : syms) {
			System.err.format("%s SYMS \"%s\" %02x/%d\n", tag,
				sym.nam, sym.val, sym.ref);
		}
		for (WangSymbol sym : subs) {
			System.err.format("%s SUBS \"%s\" %02x/%d\n", tag,
				sym.nam, sym.val, sym.ref);
		}
	}

	// 'regs' is the starting (highest) register number available.
	// allocations go downward towards "0".
	public int resolveMarks(int regs) {
		//dump("*");
		for (WangSLabel lab : slbs) {
			// if no space, leave high/low = -1
			if (regs >= lab.count - 1) {
				lab.high.val = regs;
				lab.low.val = regs - (lab.count - 1);
				// labels already added
			} else {
				lab.err = -2;	// overflow
			}
			regs -= lab.count;
		}
		//dump("-");
		for (WangSymbol sym : syms) {
			// might be pre-dedfined
			if (sym.val >= 0) continue;
			sym.val = getFreeMark();
		}
		for (WangSymbol sym : subs) {
			// might be pre-dedfined
			if (sym.val >= 0) continue;
			sym.val = getFreeSubr();
		}
		return 0;
	}
}
