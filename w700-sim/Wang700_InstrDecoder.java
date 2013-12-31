class Wang700_InstrDecoder
		implements Wang_InstructionDecoder {

	// extremely simple decoder, no context is tracked.

	// todo: this should be shared with wpcc/w7lst.c

	final String[] de_4 = {
		"+D", "-D", "\u00D7D", "\u00F7D", "ST D", "RE D", "EX D",
		"SE",
		"MK",
		"GRP1",
		"GRP2",
		"WR",
		"WR A",
		"ENDA",
		"ST Y", "RE Y"
	};
	final String[] de_5 = {
		"+I", "-I", "\u00D7I", "\u00F7I", "ST I", "RE I", "EX I",
		"*Y\u2265X",
		"*Y<X",
		"*Y=X",
		"*ERR",
		"RET",
		"EP",
		"LP",
		"GO", "STOP"
	};
	final String[] de_6 = {
		"+", "-", "\u00D7", "\u00F7", "\u2191", "\u2193", "\u2191\u2193",
		"|X|",
		"INT",
		"\u03C0",
		"log10X",
		"logX",
		"\u221AX",
		"10\u207F",
		"e\u207F",
		"1/X"
	};
	final String[] de_7 = {
		"E0", "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8", "E9",
		"EXP",
		"SGN",
		".",
		"X\u00B2",
		"RRES",
		"CLR X"
	};

	public String decode(int h, int l) {
		String t = new String();
		if (h < 4) {
			t = String.format("s%02d%02d", h, l);
		} else if (h == 12) {
			if (l < 7 || l > 13) {
				t = String.format("%s+", de_4[l]);
			} else {
				t = String.format("%02d %02d", h, l);
			}
		} else if (h == 4) {
			t = de_4[l];
		} else if (h == 5) {
			t = de_5[l];
		} else if (h == 6) {
			t = de_6[l];
		} else if (h == 7) {
			t = de_7[l];
		} else {
			t = String.format("%02d %02d", h, l);
		}
		return t;
	}
}
