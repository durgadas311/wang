// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class TiltRotate {
	static final String[] wx01 = {	// unshifted
	"-","y",  " ","\\b","q","p","=",  "j","\\t","/","\\x","\\y",",",";","f","g",
	"w","s","\\u","\\s","i","'",".","\\h","\\r","o","\\i","\\v","a","r","v","m",
	"b","h",   "",   "","k","e","n",  "t",   "","l","\\+","\\-","c","d","u","x",
	"9","0",   "",   "","6","5","2",  "z",   "","4","\\+","\\-","8","7","3","1",
	};
	static final String[] WX01 = {	// shifted
	"_","Y",  " ","\\b",  "Q", "P","+",  "J","\\t","?","\\x","\\y",",",":","F","G",
	"W","S","\\u","\\s",  "I","\"",".","\\q","\\r","O","\\i","\\v","A","R","V","M",
	"B","H",   "",   "",  "K", "E","N",  "T",   "","L",   "",   "","C","D","U","X",
	"(",")",   "",   "","\\c", "%","@",  "Z",   "","$",   "",   "","*","&","#","!",
	};

	static final String[] wx02 = {	// unshifted
	"-","y",  " ","\\b","q","p","=",  "j","\\t","/","\\x","\\y",",",";","f","g",
	"w","s","\\u","\\s","i","'",".","\\h","\\r","o","\\i","\\v","a","r","v","m",
	"b","h","\\+","\\-","k","e","n",  "t","\\p","l","\\+","\\-","c","d","u","x",
	"9","0","\\+","\\-","6","5","2",  "z","\\p","4","\\+","\\-","8","7","3","1",
	};
	static final String[] WX02 = {	// shifted
	"_","Y",  " ","\\b",  "Q", "P","+",  "J","\\t","?","\\x","\\y",",",":","F","G",
	"W","S","\\u","\\s",  "I","\"",".","\\q","\\r","O","\\i","\\v","A","R","V","M",
	"B","H","\\+","\\-",  "K", "E","N",  "T","\\p","L","\\+","\\-","C","D","U","X",
	"(",")","\\+","\\-","\\c", "%","@",  "Z","\\p","$","\\+","\\-","*","&","#","!",
	};

	static final String[] wx12 = {	// unshifted
	"-","Y",  " ",  "/","Q","P","+","J",  "}","?",  "=",  "{",",",":","F","G",
	"W","S","\\u","\\d","I","'",".","!","\\r","O","\\i","\\v","A","R","V","M",
	"B","H","\\+","\\-","K","E","N","T","\\x","1","\\+","\\-","C","D","U","X",
	"9","0","\\+","\\-","6","5","2","Z","\\y","4","\\+","\\-","8","7","3","L",
	 "","Y",  " ",  "/","Q","P","+","J",  "}","?",  "=",  "{",",",":","F","G",
	"W","S","\\p","\\m","I","'",".", "","\\z","O","\\s","\\h","A","R","V","M",
	"B","H","\\+","\\-","K","E","N","T","\\p","1","\\+","\\-","C","D","U","X",
	"9","0","\\+","\\-","6","5","2","Z","\\p","4","\\+","\\-","8","7","3","L",
	};
	static final String[] WX12 = {	// shifted - no shift
	};

	static final String[] tty = {	// unshifted
	"-","Y",  " ",   "","Q","P","=","J",   "","/","\\n","\\f",",",";","F","G",
	"W","S","\\u","\\s","I","'",".","!","\\r","O","\\i",   "","A","R","V","M",
	"B","H",   "",   "","K","E","N","T",   "","1",   "",   "","C","D","U","X",
	"9","0",   "",   "","6","5","2","Z",   "","4",   "",   "","8","7","3","L",
	};
	static final String[] TTY = {	// shifted
	 "","Y",  " ",   "", "Q","P", "+", "J",   "","?","\\n","\\f",",",":","F","G",
	"W","S","\\u","\\s", "I","\"",".",  "","\\r","O","\\i",   "","A","R","V","M",
	"B","H",   "",   "", "K","E", "N", "T",   "","1",   "",   "","C","D","U","X",
	"(",")",   "",   "",  "","%", "@", "Z",   "","$",   "",   "","*","&","#","L",
	};

	String[] a = wx01;
	String[] A = WX01;

	int aterm = 0x22;
	boolean upper = false;

	public TiltRotate() {
	}

	public TiltRotate(int trm) {
		aterm = trm;
	}

	private int isIn(char c, String[] s) {
		int x;

		for (x = 0; x < s.length; ++x) {
			if (s[x].length() == 1 && c == s[x].charAt(0)) {
				return x;
			}
		}
		return -1;
	}

	private byte doEsc(char c) {
		for (int x = 0; x < a.length; ++x) {
			if (a[x].length() == 2 && c == a[x].charAt(1)) {
				return (byte)x;
			}
		}
		return (byte)0; // TODO:
	}

	public int term() { return aterm; }
	public int shiftDown() { return 0x12; }
	public int shiftUp() { return 0x13; }

	public void setDevice(int dev) {
		upper = false;
		switch (dev) {
		case 2:		// Plotting Output Writer
			a = wx02;
			A = WX02;
			break;
		case 12:	// Flatbed Plotter
			upper = true;
			a = wx12;
			A = WX12;
			break;
		case 6:		// x06/x07 Teletype
		case 7:
			upper = true;
			a = tty;
			A = TTY;
			break;
		default:	// x01 standard Output Writer
				// x11 Input/Output Writer
				// x00 (default to x01)
			a = wx01;
			A = WX01;
			break;
		}
	}

	// one at a time, caller keeps track of SHIFT
	// might return empty string (invalid char)
	public String tr2a(int tr, boolean shifted) {
		if (shifted) {
			return A[tr];
		} else {
			return a[tr];
		}
	}

	// all at once, caller supplies initial SHIFT
	// TODO: how to return new SHIFT state?
	// Returns length of string in memory.
	public int a2tr(String s, boolean shifted, WangMemory mem, int start) {
		int adr = start;
		boolean shift;
		boolean err = false;
		boolean e;
		int x;
		int i;
		char c;

		for (x = 0; x < s.length(); ++x) {
			// TODO: escapes. Also non-shifted (blank, etc).
			if (s.charAt(x) == '\\' && x + 1 < s.length()) {
				++x;
				if (s.charAt(x) == '0') {
					break; // we're done
				}
				e = mem.putMem(adr++, doEsc(s.charAt(x)));
				err = err || e;
				continue;
			}
			shift = false;
			c = s.charAt(x);
			if (upper) c = Character.toUpperCase(c);
			i = isIn(c, a);
			if (i < 0) {
				shift = true;
				i = isIn(c, A);
			}
			if (i < 0) {
				// TODO: invalid character placeholder...
				continue;
			}
			if ((i & 0x06) == 0x02 || (i & 0x0f) == 0x08) {
				// do not shift for these
				shift = shifted;
			}
			if (x == 0 || shift != shifted) {
				e = mem.putMem(adr++, (shift ? shiftUp() : shiftDown()));
				err = err || e;
				shifted = shift;
			}
			e = mem.putMem(adr++, i);
			err = err || e;
		}
		if (shifted) {
			e = mem.putMem(adr++, shiftDown());
			err = err || e;
		}
		e = mem.putMem(adr++, term());
		err = err || e;
		if (err) {
			return -(adr - start);
		} else {
			return adr - start;
		}
	}
}
