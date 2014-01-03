// Copyright (c) 2010, 2013 Douglas Miller
// $Id: Wang_CharConverter.java,v 1.6 2014/01/03 23:48:40 drmiller Exp $

import java.util.Arrays;

// Handles Wang 1200 oddities...
// And supports Teletype variations.
class Wang_CharConverter {

	private byte[] xlate_ibm;
	private byte[] revxlate_ibm;
	private byte[] xlate_tty;
	private byte[] revxlate_tty;
	private byte[] code_xlate;
	private String[] spcl_ibm;

	private void setup_ibm_xlate() {
		xlate_ibm = new byte[256];
		Arrays.fill(xlate_ibm, (byte)0);
		xlate_ibm[0x00] = '-';
		xlate_ibm[0x01] = 'y';
		xlate_ibm[0x02] = ' ';		// space - carriage control
		xlate_ibm[0x03] = '\b';	// bkspace - carriage control
		xlate_ibm[0x04] = 'q';
		xlate_ibm[0x05] = 'p';
		xlate_ibm[0x06] = '=';
		xlate_ibm[0x07] = 'j';
		xlate_ibm[0x08] = '\t';	// tab - carriage control
		xlate_ibm[0x09] = '/';
		xlate_ibm[0x0a] = '|';		// set-tab - carriage control
		xlate_ibm[0x0b] = '\\';	// clr-tab - carriage control
		xlate_ibm[0x0c] = ',';
		xlate_ibm[0x0d] = ';';
		xlate_ibm[0x0e] = 'f';
		xlate_ibm[0x0f] = 'g';

		xlate_ibm[0x10] = 'w';
		xlate_ibm[0x11] = 's';
		xlate_ibm[0x12] = '`';		// shift dn - carriage control
		xlate_ibm[0x13] = '~';		// shift up - carriage control
		xlate_ibm[0x14] = 'i';
		xlate_ibm[0x15] = '\'';
		xlate_ibm[0x16] = '.';
		xlate_ibm[0x17] = '\001';	// 1/2...
		xlate_ibm[0x18] = '\r';	// return/index - carriage control
		xlate_ibm[0x19] = 'o';
		xlate_ibm[0x1a] = '\n';	// index - carriage control
		xlate_ibm[0x1b] = '\014';	// rev index - carriage control (FF/VT)
		xlate_ibm[0x1c] = 'a';
		xlate_ibm[0x1d] = 'r';
		xlate_ibm[0x1e] = 'v';
		xlate_ibm[0x1f] = 'm';

		xlate_ibm[0x20] = 'b';
		xlate_ibm[0x21] = 'h';
		xlate_ibm[0x22] = (byte)'\201';	// step x+
		xlate_ibm[0x23] = (byte)'\202';	// step x-
		xlate_ibm[0x24] = 'k';
		xlate_ibm[0x25] = 'e';
		xlate_ibm[0x26] = 'n';
		xlate_ibm[0x27] = 't';
		xlate_ibm[0x28] = (byte)'\300';	// print mode
		xlate_ibm[0x29] = 'l';
		xlate_ibm[0x2a] = (byte)'\210';	// step y+
		xlate_ibm[0x2b] = (byte)'\220';	// step y-
		xlate_ibm[0x2c] = 'c';
		xlate_ibm[0x2d] = 'd';
		xlate_ibm[0x2e] = 'u';
		xlate_ibm[0x2f] = 'x';

		xlate_ibm[0x30] = '9';
		xlate_ibm[0x31] = '0';
		xlate_ibm[0x32] = (byte)'\211';	// step x+y+
		xlate_ibm[0x33] = (byte)'\212';	// step x-y+
		xlate_ibm[0x34] = '6';
		xlate_ibm[0x35] = '5';
		xlate_ibm[0x36] = '2';
		xlate_ibm[0x37] = 'z';
		xlate_ibm[0x38] = (byte)'\200';	// plot mode
		xlate_ibm[0x39] = '4';
		xlate_ibm[0x3a] = (byte)'\221';	// step x+y-
		xlate_ibm[0x3b] = (byte)'\222';	// step x-y-
		xlate_ibm[0x3c] = '8';
		xlate_ibm[0x3d] = '7';
		xlate_ibm[0x3e] = '3';
		xlate_ibm[0x3f] = '1';

		// shifted versions...
		xlate_ibm[0x40] = '_';
		xlate_ibm[0x41] = 'Y';
		xlate_ibm[0x42] = ' ';		// space - carriage control
		xlate_ibm[0x43] = '\b';	// bkspace - carriage control
		xlate_ibm[0x44] = 'Q';
		xlate_ibm[0x45] = 'P';
		xlate_ibm[0x46] = '+';
		xlate_ibm[0x47] = 'J';
		xlate_ibm[0x48] = '\t';	// tab - carriage control
		xlate_ibm[0x49] = '?';
		xlate_ibm[0x4a] = '|';		// set-tab - carriage control
		xlate_ibm[0x4b] = '\\';	// clr-tab - carriage control
		xlate_ibm[0x4c] = ',';
		xlate_ibm[0x4d] = ':';
		xlate_ibm[0x4e] = 'F';
		xlate_ibm[0x4f] = 'G';

		xlate_ibm[0x50] = 'W';
		xlate_ibm[0x51] = 'S';
		xlate_ibm[0x52] = '`';		// shift dn - carriage control
		xlate_ibm[0x53] = '~';		// shift up - carriage control
		xlate_ibm[0x54] = 'I';
		xlate_ibm[0x55] = '"';
		xlate_ibm[0x56] = '.';
		xlate_ibm[0x57] = '\002';	// 1/4
		xlate_ibm[0x58] = '\r';	// return/index - carriage control
		xlate_ibm[0x59] = 'O';
		xlate_ibm[0x5a] = '\n';	// index - carriage control
		xlate_ibm[0x5b] = '\014';	// rev index - carriage control (FF/VT)
		xlate_ibm[0x5c] = 'A';
		xlate_ibm[0x5d] = 'R';
		xlate_ibm[0x5e] = 'V';
		xlate_ibm[0x5f] = 'M';

		xlate_ibm[0x60] = 'B';
		xlate_ibm[0x61] = 'H';
		xlate_ibm[0x62] = (byte)'\201';	// step x+
		xlate_ibm[0x63] = (byte)'\202';	// step x-
		xlate_ibm[0x64] = 'K';
		xlate_ibm[0x65] = 'E';
		xlate_ibm[0x66] = 'N';
		xlate_ibm[0x67] = 'T';
		xlate_ibm[0x68] = (byte)'\300';	// print mode
		xlate_ibm[0x69] = 'L';
		xlate_ibm[0x6a] = (byte)'\210';	// step y+
		xlate_ibm[0x6b] = (byte)'\220';	// step y-
		xlate_ibm[0x6c] = 'C';
		xlate_ibm[0x6d] = 'D';
		xlate_ibm[0x6e] = 'U';
		xlate_ibm[0x6f] = 'X';

		xlate_ibm[0x70] = '(';
		xlate_ibm[0x71] = ')';
		xlate_ibm[0x72] = (byte)'\211';	// step x+y+
		xlate_ibm[0x73] = (byte)'\212';	// step x-y+
		xlate_ibm[0x74] = '\003';	// cent
		xlate_ibm[0x75] = '%';
		xlate_ibm[0x76] = '@';
		xlate_ibm[0x77] = 'Z';
		xlate_ibm[0x78] = (byte)'\200';	// plot mode
		xlate_ibm[0x79] = '$';
		xlate_ibm[0x7a] = (byte)'\221';	// step x+y-
		xlate_ibm[0x7b] = (byte)'\222';	// step x-y-
		xlate_ibm[0x7c] = '*';
		xlate_ibm[0x7d] = '&';
		xlate_ibm[0x7e] = '#';
		xlate_ibm[0x7f] = '!';

		spcl_ibm = new String[8];
		spcl_ibm[0x01] = "\u00BD";	// 1/2
		spcl_ibm[0x02] = "\u00BC";	// 1/4
		spcl_ibm[0x03] = "\u00A2";	// cent
	}

	private void setup_ibm_revxlate() {
		revxlate_ibm = new byte[256];
		Arrays.fill(revxlate_ibm, (byte)0xff);
		revxlate_ibm['-'] = 0x00;
		revxlate_ibm['y'] = 0x01;
		revxlate_ibm[' '] = 0x02;
		revxlate_ibm['\b'] = 0x03;
		revxlate_ibm['q'] = 0x04;
		revxlate_ibm['p'] = 0x05;
		revxlate_ibm['='] = 0x06;
		revxlate_ibm['j'] = 0x07;
		//revxlate_ibm[''] = (byte)0x08; // tab
		revxlate_ibm['/'] = 0x09;
		//revxlate_ibm[''] = (byte)0x0a; // set tab
		//revxlate_ibm[''] = (byte)0x0b; // clr tab
		revxlate_ibm[','] = 0x0c;
		revxlate_ibm[';'] = 0x0d;
		revxlate_ibm['f'] = 0x0e;
		revxlate_ibm['g'] = 0x0f;

		revxlate_ibm['w'] = 0x10;
		revxlate_ibm['s'] = 0x11;
		//revxlate_ibm[''] = (byte)0x12; // shift down
		//revxlate_ibm[''] = (byte)0x13; // shift up
		revxlate_ibm['i'] = 0x14;
		revxlate_ibm['\''] = 0x15;
		revxlate_ibm['.'] = 0x16;
		revxlate_ibm['['] = 0x17;	// 1/2...
		revxlate_ibm['\r'] = 0x18;	// ret+index - can this happen?
		revxlate_ibm['o'] = 0x19;
		revxlate_ibm['\n'] = 0x1a;	// index
		revxlate_ibm['\f'] = 0x1b;	// rev-index
		revxlate_ibm['a'] = 0x1c;
		revxlate_ibm['r'] = 0x1d;
		revxlate_ibm['v'] = 0x1e;
		revxlate_ibm['m'] = 0x1f;

		revxlate_ibm['b'] = 0x20;
		revxlate_ibm['h'] = 0x21;
		//revxlate_ibm[''] = (byte)0x22; // plotter
		//revxlate_ibm[''] = (byte)0x23; // plotter
		revxlate_ibm['k'] = 0x24;
		revxlate_ibm['e'] = 0x25;
		revxlate_ibm['n'] = 0x26;
		revxlate_ibm['t'] = 0x27;
		//revxlate_ibm[''] = (byte)0x28; // plotter
		revxlate_ibm['l'] = 0x29;
		//revxlate_ibm[''] = (byte)0x2a; // plotter
		//revxlate_ibm[''] = (byte)0x2b; // plotter
		revxlate_ibm['c'] = 0x2c;
		revxlate_ibm['d'] = 0x2d;
		revxlate_ibm['u'] = 0x2e;
		revxlate_ibm['x'] = 0x2f;

		revxlate_ibm['9'] = 0x30;
		revxlate_ibm['0'] = 0x31;
		//revxlate_ibm[''] = (byte)0x32; // plotter
		//revxlate_ibm[''] = (byte)0x33; // plotter
		revxlate_ibm['6'] = 0x34;
		revxlate_ibm['5'] = 0x35;
		revxlate_ibm['2'] = 0x36;
		revxlate_ibm['z'] = 0x37;
		//revxlate_ibm[''] = (byte)0x38; // plotter
		revxlate_ibm['4'] = 0x39;
		//revxlate_ibm[''] = (byte)0x3a; // plotter
		//revxlate_ibm[''] = (byte)0x3b; // plotter
		revxlate_ibm['8'] = 0x3c;
		revxlate_ibm['7'] = 0x3d;
		revxlate_ibm['3'] = 0x3e;
		revxlate_ibm['1'] = 0x3f;

		// shifted versions...
		revxlate_ibm['_'] = (byte)0x80;
		revxlate_ibm['Y'] = (byte)0x81;
		//revxlate_ibm[''] = (byte)0x82;
		//revxlate_ibm[''] = (byte)0x83;
		revxlate_ibm['Q'] = (byte)0x84;
		revxlate_ibm['P'] = (byte)0x85;
		revxlate_ibm['+'] = (byte)0x86;
		revxlate_ibm['J'] = (byte)0x87;
		//revxlate_ibm[''] = (byte)0x88;
		revxlate_ibm['?'] = (byte)0x89;
		//revxlate_ibm[''] = (byte)0x8a;
		//revxlate_ibm[''] = (byte)0x8b;
		revxlate_ibm[','] = (byte)0x8c;
		revxlate_ibm[':'] = (byte)0x8d;
		revxlate_ibm['F'] = (byte)0x8e;
		revxlate_ibm['G'] = (byte)0x8f;

		revxlate_ibm['W'] = (byte)0x90;
		revxlate_ibm['S'] = (byte)0x91;
		//revxlate_ibm[''] = (byte)0x92;
		//revxlate_ibm[''] = (byte)0x93;
		revxlate_ibm['I'] = (byte)0x94;
		revxlate_ibm['"'] = (byte)0x95;
		revxlate_ibm['.'] = (byte)0x96;
		revxlate_ibm['{'] = (byte)0x97;	// 1/4
		//revxlate_ibm[''] = (byte)0x98;
		revxlate_ibm['O'] = (byte)0x99;
		//revxlate_ibm[''] = (byte)0x9a;
		//revxlate_ibm[''] = (byte)0x9b;
		revxlate_ibm['A'] = (byte)0x9c;
		revxlate_ibm['R'] = (byte)0x9d;
		revxlate_ibm['V'] = (byte)0x9e;
		revxlate_ibm['M'] = (byte)0x9f;

		revxlate_ibm['B'] = (byte)0xa0;
		revxlate_ibm['H'] = (byte)0xa1;
		//revxlate_ibm[''] = (byte)0xa2;
		//revxlate_ibm[''] = (byte)0xa3;
		revxlate_ibm['K'] = (byte)0xa4;
		revxlate_ibm['E'] = (byte)0xa5;
		revxlate_ibm['N'] = (byte)0xa6;
		revxlate_ibm['T'] = (byte)0xa7;
		//revxlate_ibm[''] = (byte)0xa8;
		revxlate_ibm['L'] = (byte)0xa9;
		//revxlate_ibm[''] = (byte)0xaa;
		//revxlate_ibm[''] = (byte)0xab;
		revxlate_ibm['C'] = (byte)0xac;
		revxlate_ibm['D'] = (byte)0xad;
		revxlate_ibm['U'] = (byte)0xae;
		revxlate_ibm['X'] = (byte)0xaf;

		revxlate_ibm['('] = (byte)0xb0;
		revxlate_ibm[')'] = (byte)0xb1;
		//revxlate_ibm[''] = (byte)0xb2;
		//revxlate_ibm[''] = (byte)0xb3;
		revxlate_ibm['^'] = (byte)0xb4;	// cent
		revxlate_ibm['%'] = (byte)0xb5;
		revxlate_ibm['@'] = (byte)0xb6;
		revxlate_ibm['Z'] = (byte)0xb7;
		//revxlate_ibm[''] = (byte)0xb8;
		revxlate_ibm['$'] = (byte)0xb9;
		//revxlate_ibm[''] = (byte)0xba;
		//revxlate_ibm[''] = (byte)0xbb;
		revxlate_ibm['*'] = (byte)0xbc;
		revxlate_ibm['&'] = (byte)0xbd;
		revxlate_ibm['#'] = (byte)0xbe;
		revxlate_ibm['!'] = (byte)0xbf;
	}

	private void setup_tty_xlate() {
		xlate_tty = new byte[256];
		Arrays.fill(xlate_tty, (byte)0);
		xlate_tty[0x00] = '-';
		xlate_tty[0x01] = 'y';
		xlate_tty[0x02] = ' ';		// space - carriage control
		//xlate_tty[0x03] = '\b';	// bkspace - carriage control
		xlate_tty[0x04] = 'q';
		xlate_tty[0x05] = 'p';
		xlate_tty[0x06] = '=';
		xlate_tty[0x07] = 'j';
		//xlate_tty[0x08] = '\t';	// tab - carriage control
		xlate_tty[0x09] = '/';
		xlate_tty[0x0a] = '\022'; // set-tab == punch on == ^R
		xlate_tty[0x0b] = '\024'; // clr-tab == punch off == ^T
		xlate_tty[0x0c] = ',';
		xlate_tty[0x0d] = ';';
		xlate_tty[0x0e] = 'f';
		xlate_tty[0x0f] = 'g';

		xlate_tty[0x10] = 'w';
		xlate_tty[0x11] = 's';
		xlate_tty[0x12] = '\014'; // shift dn == ^L (already handled)
		xlate_tty[0x13] = '\010'; // shift up == ^H (already handled)
		xlate_tty[0x14] = 'i';
		xlate_tty[0x15] = '\'';
		xlate_tty[0x16] = '.';
		xlate_tty[0x17] = '!';
		xlate_tty[0x18] = '\r';	// return/index - carriage control
		xlate_tty[0x19] = 'o';
		xlate_tty[0x1a] = '\n';	// index - carriage control
		//xlate_tty[0x1b] = '\014';	// rev index - carriage control (FF/VT)
		xlate_tty[0x1c] = 'a';
		xlate_tty[0x1d] = 'r';
		xlate_tty[0x1e] = 'v';
		xlate_tty[0x1f] = 'm';

		xlate_tty[0x20] = 'b';
		xlate_tty[0x21] = 'h';
		//xlate_tty[0x22] = (byte)'\201';	// step x+
		//xlate_tty[0x23] = (byte)'\202';	// step x-
		xlate_tty[0x24] = 'k';
		xlate_tty[0x25] = 'e';
		xlate_tty[0x26] = 'n';
		xlate_tty[0x27] = 't';
		//xlate_tty[0x28] = (byte)'\300';	// print mode
		xlate_tty[0x29] = '1';
		//xlate_tty[0x2a] = (byte)'\210';	// step y+
		//xlate_tty[0x2b] = (byte)'\220';	// step y-
		xlate_tty[0x2c] = 'c';
		xlate_tty[0x2d] = 'd';
		xlate_tty[0x2e] = 'u';
		xlate_tty[0x2f] = 'x';

		xlate_tty[0x30] = '9';
		xlate_tty[0x31] = '0';
		//xlate_tty[0x32] = (byte)'\211';	// step x+y+
		//xlate_tty[0x33] = (byte)'\212';	// step x-y+
		xlate_tty[0x34] = '6';
		xlate_tty[0x35] = '5';
		xlate_tty[0x36] = '2';
		xlate_tty[0x37] = 'z';
		//xlate_tty[0x38] = (byte)'\200';	// plot mode
		xlate_tty[0x39] = '4';
		//xlate_tty[0x3a] = (byte)'\221';	// step x+y-
		//xlate_tty[0x3b] = (byte)'\222';	// step x-y-
		xlate_tty[0x3c] = '8';
		xlate_tty[0x3d] = '7';
		xlate_tty[0x3e] = '3';
		xlate_tty[0x3f] = 'l';

		// shifted versions...
		//xlate_tty[0x40] = '_';
		xlate_tty[0x41] = 'Y';
		xlate_tty[0x42] = ' ';		// space - carriage control
		//xlate_tty[0x43] = '\b';	// bkspace - carriage control
		xlate_tty[0x44] = 'Q';
		xlate_tty[0x45] = 'P';
		xlate_tty[0x46] = '+';
		xlate_tty[0x47] = 'J';
		//xlate_tty[0x48] = '\t';	// tab - carriage control
		xlate_tty[0x49] = '?';
		xlate_tty[0x4a] = '\022';
		xlate_tty[0x4b] = '\024';
		xlate_tty[0x4c] = ',';
		xlate_tty[0x4d] = ':';
		xlate_tty[0x4e] = 'F';
		xlate_tty[0x4f] = 'G';

		xlate_tty[0x50] = 'W';
		xlate_tty[0x51] = 'S';
		xlate_tty[0x52] = '\014';
		xlate_tty[0x53] = '\010';
		xlate_tty[0x54] = 'I';
		xlate_tty[0x55] = '"';
		xlate_tty[0x56] = '.';
		//xlate_tty[0x57] = '\002';	// 1/4
		xlate_tty[0x58] = '\r';	// return/index - carriage control
		xlate_tty[0x59] = 'O';
		xlate_tty[0x5a] = '\n';	// index - carriage control
		//xlate_tty[0x5b] = '\014';	// rev index - carriage control (FF/VT)
		xlate_tty[0x5c] = 'A';
		xlate_tty[0x5d] = 'R';
		xlate_tty[0x5e] = 'V';
		xlate_tty[0x5f] = 'M';

		xlate_tty[0x60] = 'B';
		xlate_tty[0x61] = 'H';
		//xlate_tty[0x62] = (byte)'\201';	// step x+
		//xlate_tty[0x63] = (byte)'\202';	// step x-
		xlate_tty[0x64] = 'K';
		xlate_tty[0x65] = 'E';
		xlate_tty[0x66] = 'N';
		xlate_tty[0x67] = 'T';
		//xlate_tty[0x68] = (byte)'\300';	// print mode
		xlate_tty[0x69] = '1';
		//xlate_tty[0x6a] = (byte)'\210';	// step y+
		//xlate_tty[0x6b] = (byte)'\220';	// step y-
		xlate_tty[0x6c] = 'C';
		xlate_tty[0x6d] = 'D';
		xlate_tty[0x6e] = 'U';
		xlate_tty[0x6f] = 'X';

		xlate_tty[0x70] = '(';
		xlate_tty[0x71] = ')';
		//xlate_tty[0x72] = (byte)'\211';	// step x+y+
		//xlate_tty[0x73] = (byte)'\212';	// step x-y+
		//xlate_tty[0x74] = '\003';	// cent
		xlate_tty[0x75] = '%';
		xlate_tty[0x76] = '@';
		xlate_tty[0x77] = 'Z';
		//xlate_tty[0x78] = (byte)'\200';	// plot mode
		xlate_tty[0x79] = '$';
		//xlate_tty[0x7a] = (byte)'\221';	// step x+y-
		//xlate_tty[0x7b] = (byte)'\222';	// step x-y-
		xlate_tty[0x7c] = '*';
		xlate_tty[0x7d] = '&';
		xlate_tty[0x7e] = '#';
		xlate_tty[0x7f] = 'L';
	}

	private void setup_tty_revxlate() {
		revxlate_tty = new byte[256];
		Arrays.fill(revxlate_tty, (byte)0xff);
		revxlate_tty['-'] = 0x00;
		revxlate_tty['y'] = 0x01;
		revxlate_tty[' '] = 0x02; // keep space here, for now
		//revxlate_tty['\b'] = 0x03; // backspace
		revxlate_tty['q'] = 0x04;
		revxlate_tty['p'] = 0x05;
		revxlate_tty['='] = 0x06;
		revxlate_tty['j'] = 0x07;
		//revxlate_tty[''] = (byte)0x08; // tab
		revxlate_tty['/'] = 0x09;
		revxlate_tty['\022'] = (byte)0x0a; // ^R == set tab == punch on
		revxlate_tty['\024'] = (byte)0x0b; // ^T == clr tab == punch off
		revxlate_tty[','] = 0x0c;
		revxlate_tty[';'] = 0x0d;
		revxlate_tty['f'] = 0x0e;
		revxlate_tty['g'] = 0x0f;

		revxlate_tty['w'] = 0x10;
		revxlate_tty['s'] = 0x11;
		revxlate_tty['\014'] = (byte)0x12; // ^L == shift down
		revxlate_tty['\010'] = (byte)0x13; // ^H == shift up
		revxlate_tty['i'] = 0x14;
		revxlate_tty['\''] = 0x15;
		revxlate_tty['.'] = 0x16;
		revxlate_tty['!'] = 0x17;
		revxlate_tty['\r'] = 0x18;
		revxlate_tty['o'] = 0x19;
		revxlate_tty['\n'] = 0x1a;	// index
		//revxlate_tty['\f'] = 0x1b;	// rev-index
		revxlate_tty['a'] = 0x1c;
		revxlate_tty['r'] = 0x1d;
		revxlate_tty['v'] = 0x1e;
		revxlate_tty['m'] = 0x1f;

		revxlate_tty['b'] = 0x20;
		revxlate_tty['h'] = 0x21;
		//revxlate_tty[''] = (byte)0x22; // plotter
		//revxlate_tty[''] = (byte)0x23; // plotter
		revxlate_tty['k'] = 0x24;
		revxlate_tty['e'] = 0x25;
		revxlate_tty['n'] = 0x26;
		revxlate_tty['t'] = 0x27;
		//revxlate_tty[''] = (byte)0x28; // plotter
		revxlate_tty['1'] = 0x29;
		//revxlate_tty[''] = (byte)0x2a; // plotter
		//revxlate_tty[''] = (byte)0x2b; // plotter
		revxlate_tty['c'] = 0x2c;
		revxlate_tty['d'] = 0x2d;
		revxlate_tty['u'] = 0x2e;
		revxlate_tty['x'] = 0x2f;

		revxlate_tty['9'] = 0x30;
		revxlate_tty['0'] = 0x31;
		//revxlate_tty[''] = (byte)0x32; // plotter
		//revxlate_tty[''] = (byte)0x33; // plotter
		revxlate_tty['6'] = 0x34;
		revxlate_tty['5'] = 0x35;
		revxlate_tty['2'] = 0x36;
		revxlate_tty['z'] = 0x37;
		//revxlate_tty[''] = (byte)0x38; // plotter
		revxlate_tty['4'] = 0x39;
		//revxlate_tty[''] = (byte)0x3a; // plotter
		//revxlate_tty[''] = (byte)0x3b; // plotter
		revxlate_tty['8'] = 0x3c;
		revxlate_tty['7'] = 0x3d;
		revxlate_tty['3'] = 0x3e;
		revxlate_tty['l'] = 0x3f;

		// shifted versions...
		//revxlate_tty['_'] = (byte)0x80;
		revxlate_tty['Y'] = (byte)0x81;
		//revxlate_tty[''] = (byte)0x82;
		//revxlate_tty[''] = (byte)0x83;
		revxlate_tty['Q'] = (byte)0x84;
		revxlate_tty['P'] = (byte)0x85;
		revxlate_tty['+'] = (byte)0x86;
		revxlate_tty['J'] = (byte)0x87;
		//revxlate_tty[''] = (byte)0x88;
		revxlate_tty['?'] = (byte)0x89;
		//revxlate_tty[''] = (byte)0x8a;
		//revxlate_tty[''] = (byte)0x8b;
		revxlate_tty[','] = (byte)0x8c;
		revxlate_tty[':'] = (byte)0x8d;
		revxlate_tty['F'] = (byte)0x8e;
		revxlate_tty['G'] = (byte)0x8f;

		revxlate_tty['W'] = (byte)0x90;
		revxlate_tty['S'] = (byte)0x91;
		//revxlate_tty[''] = (byte)0x92;
		//revxlate_tty[''] = (byte)0x93;
		revxlate_tty['I'] = (byte)0x94;
		revxlate_tty['"'] = (byte)0x95;
		revxlate_tty['.'] = (byte)0x96;
		//revxlate_tty['{'] = (byte)0x97;	// 1/4
		//revxlate_tty[''] = (byte)0x98;
		revxlate_tty['O'] = (byte)0x99;
		//revxlate_tty[''] = (byte)0x9a;
		//revxlate_tty[''] = (byte)0x9b;
		revxlate_tty['A'] = (byte)0x9c;
		revxlate_tty['R'] = (byte)0x9d;
		revxlate_tty['V'] = (byte)0x9e;
		revxlate_tty['M'] = (byte)0x9f;

		revxlate_tty['B'] = (byte)0xa0;
		revxlate_tty['H'] = (byte)0xa1;
		//revxlate_tty[''] = (byte)0xa2;
		//revxlate_tty[''] = (byte)0xa3;
		revxlate_tty['K'] = (byte)0xa4;
		revxlate_tty['E'] = (byte)0xa5;
		revxlate_tty['N'] = (byte)0xa6;
		revxlate_tty['T'] = (byte)0xa7;
		//revxlate_tty[''] = (byte)0xa8;
		//revxlate_tty['1'] = (byte)0xa9;
		//revxlate_tty[''] = (byte)0xaa;
		//revxlate_tty[''] = (byte)0xab;
		revxlate_tty['C'] = (byte)0xac;
		revxlate_tty['D'] = (byte)0xad;
		revxlate_tty['U'] = (byte)0xae;
		revxlate_tty['X'] = (byte)0xaf;

		revxlate_tty['('] = (byte)0xb0;
		revxlate_tty[')'] = (byte)0xb1;
		//revxlate_tty[''] = (byte)0xb2;
		//revxlate_tty[''] = (byte)0xb3;
		//revxlate_tty['^'] = (byte)0xb4;
		revxlate_tty['%'] = (byte)0xb5;
		revxlate_tty['@'] = (byte)0xb6;
		revxlate_tty['Z'] = (byte)0xb7;
		//revxlate_tty[''] = (byte)0xb8;
		revxlate_tty['$'] = (byte)0xb9;
		//revxlate_tty[''] = (byte)0xba;
		//revxlate_tty[''] = (byte)0xbb;
		revxlate_tty['*'] = (byte)0xbc;
		revxlate_tty['&'] = (byte)0xbd;
		revxlate_tty['#'] = (byte)0xbe;
		revxlate_tty['L'] = (byte)0xbf;
	}

	private void setup1200_revxlate() {
		setup_ibm_revxlate();
		revxlate_ibm['|'] = (byte)0x92;	// Set Tab
		revxlate_ibm['\n'] = 0x33;
		revxlate_ibm['\t'] = 0x23;
		revxlate_ibm['\b'] = 0x13;
		revxlate_ibm[' '] = 0x03;

		// Transale low 4 bits when CODE key on.
		code_xlate = new byte[16];
		code_xlate[0] = 8;
		code_xlate[1] = 1;
		code_xlate[2] = 2;
		code_xlate[3] = 2;
		code_xlate[4] = 4;
		code_xlate[5] = 5;
		code_xlate[6] = 6;
		code_xlate[7] = 7;
		code_xlate[8] = 8;
		code_xlate[9] = 10;
		code_xlate[10] = 10;
		code_xlate[11] = 11;
		code_xlate[12] = 12;
		code_xlate[13] = 13;
		code_xlate[14] = 14;
		code_xlate[15] = 11;
	}

	public Wang_CharConverter(boolean codeKey) {
		setup_ibm_xlate();
		if (codeKey) {
			setup1200_revxlate();
		} else {
			setup_ibm_revxlate();
			setup_tty_revxlate();
			setup_tty_xlate();
		}
	}

	public byte[] asciiToTiltrotate(byte code) {
		byte c = revxlate_ibm[code];
		if (c == (byte)0xff) {
			return null;
		}
		boolean shifted = ((c & 0x80) != 0);
		c &= 0x7f;
		if (shifted) {
			return new byte[] { 0x13, c };
		} else {
			return new byte[] { 0x12, c };
		}
	}

	public byte tiltrotateToCodedTiltrotate(byte code, boolean coded) {
		byte c = code;
		if (coded) {
			int ix = c & 0x0f; 
			c &= ~0x0f;
			c |= code_xlate[ix];
		}
		return c;
	}

	public byte asciiToCodedTiltrotate(byte code, boolean coded) {
		byte c = revxlate_ibm[code];
		if (c != (byte)0xff) {
			c = tiltrotateToCodedTiltrotate(c, coded);
		}
		return c;
	}

	public String tiltrotateToAscii(byte code, boolean shifted) {
		// Typically, caller has already decoded carriage control, plot, etc.
		// But if not, these get converted to ASCII equivalents if possible.
		String s;
		if (shifted) code |= 0x40;
		byte bb = xlate_ibm[code];
		if (bb == 0 || bb >= '\200') {
			// invalid, or plotting code...
			s = null;
		} else if ((bb & 0x00ff) < 0x07) {
			s = spcl_ibm[bb];
		} else {
			s = new String(new byte[]{bb});
		}
		return s;
	}

	public String tiltrotateToAsciiTty(byte code, boolean shifted) {
		// carriage control, etc. Only a few are legal...
		// (practically) all must have been decode separately.
		if (shifted) code |= 0x40;
		byte bb = xlate_tty[code];
		if (bb == 0) {
			return null;
		}
		return new String(new byte[]{bb});
	}

	public byte[] asciiTtyToTiltrotate(byte code) {
		byte c = revxlate_tty[code];
		if (c == (byte)0xff) {
			return null;
		}
		boolean shifted = ((c & 0x80) != 0);
		c &= 0x7f;
		if (shifted) {
			return new byte[] { 0x13, c };
		} else {
			return new byte[] { 0x12, c };
		}
	}
}
