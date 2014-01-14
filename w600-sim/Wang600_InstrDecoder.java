// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang600_InstrDecoder.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $

class Wang600_InstrDecoder
		implements Wang_InstructionDecoder {

	// extremely simple decoder based on built-in printer drum.
	// a friendlier decoding is possible, but users are already
	// familiar with program listings from the built-in printer.

	// todo: this should be shared with Wang600_Printer

	final String[] pr_16 = {
		"E", "T", "+", "-", "\u00D7", "\u00F7", "ST", "RE",
		"*", "*", "f", "F", "A", "B", "C", "D", ""
	};	      
	final String[] pr_17 = {
		"0", "1", "2", "3", "4", "5", "6", "7",
		"8", "9", "10", "11", "12", "13", "14", "15", ""
	};	      
	final String[] pr_18 = {
		"S", "RE", "W", "GO", "Jo", "J+", "SN", "CS",
		"TN", "RD", "LN", "e\u207F", "x\u00B2", "\u221AX", "LP", "1/x",
		"  ", ""
	};	      
	final String[] pr_19 = {
		"M", "ST", "\u03B1", "SP", "J\u00F8", "Je", "S\u00B9", "C\u00B9",
		"T\u00B9", "DR", "LG", "10\u207F", "I", "|x|", "EP", "RT",
		"", ""  
	};	      

	public String decode(int h, int l) {
		String t = pr_16[h];
		if (h == 8) {
			t += pr_18[l];
		} else if (h == 9) {
			t += pr_19[l];
		} else {
			t += pr_17[l];
		}
		return t;
	}

}
