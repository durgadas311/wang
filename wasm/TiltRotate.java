// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

import java.util.Vector;

public class TiltRotate {
	static final String[] a = {	// unshifted
		"-","y", " ","\\b","q","p","=", "j","\\t","/","\\x","\\y",",",";","f","g",
		"w","s","\\u","\\s","i","'",".","\\h","\\r","o","\\i","\\v","a","r","v","m",
		"b","h","\\+","\\-","k","e","n", "t","\\p","l","\\+","\\-","c","d","u","x",
		"9","0","\\+","\\-","6","5","2", "z","\\p","4","\\+","\\-","8","7","3","1",
	};
	static final String[] A = {	// shifted
		"_","Y", " ","\\b", "Q","P", "+", "J","\\t","?","\\x","\\y",",",":","F","G",
		"W","S","\\u","\\s", "I","\"",".","\\q","\\r","O","\\i","\\v","A","R","V","M",
		"B","H","\\+","\\-", "K","E", "N", "T","\\p","L","\\+","\\-","C","D","U","X",
		"(",")","\\+","\\-","\\c","%", "@", "Z","\\p","$","\\+","\\-","*","&","#","!",
	};

	int aterm = 0x22;

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

	// one at a time, caller keeps track of SHIFT
	// TODO: handle plotting.
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
	public int a2tr(String s, boolean shifted, byte[] mem, int start) {
		int adr = start;
		boolean shift;
		int x;
		int i;

		for (x = 0; x < s.length(); ++x) {
			// TODO: escapes. Also non-shifted (blank, etc).
			if (s.charAt(x) == '\\' && x + 1 < s.length()) {
				++x;
				if (s.charAt(x) == '0') {
					break; // we're done
				}
				mem[adr++] = doEsc(s.charAt(x));
				continue;
			}
			shift = true;
			i = isIn(s.charAt(x), A);
			if (i < 0) {
				shift = false;
				i = isIn(s.charAt(x), a);
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
				mem[adr++] = (byte)(shift ? shiftUp() : shiftDown());
				shifted = shift;
			}
			mem[adr++] = (byte)i;
		}
		if (shifted) {
			mem[adr++] = (byte)shiftDown();
		}
		mem[adr++] = (byte)term();
		return adr - start;
	}
}
