class Wang_CharConverter {

	private byte[] cn24_xlate;
	private String[] cn24_spcl;

	private void setup_xlate() {
		cn24_xlate = new byte[256];
		cn24_xlate[0x00] = '-';
		cn24_xlate[0x01] = 'y';
		cn24_xlate[0x02] = ' ';		// space - carriage control
		cn24_xlate[0x03] = '\b';	// bkspace - carriage control
		cn24_xlate[0x04] = 'q';
		cn24_xlate[0x05] = 'p';
		cn24_xlate[0x06] = '=';
		cn24_xlate[0x07] = 'j';
		cn24_xlate[0x08] = '\t';	// tab - carriage control
		cn24_xlate[0x09] = '/';
		cn24_xlate[0x0a] = '|';		// set-tab - carriage control
		cn24_xlate[0x0b] = '\\';	// clr-tab - carriage control
		cn24_xlate[0x0c] = ',';
		cn24_xlate[0x0d] = ';';
		cn24_xlate[0x0e] = 'f';
		cn24_xlate[0x0f] = 'g';

		cn24_xlate[0x10] = 'w';
		cn24_xlate[0x11] = 's';
		cn24_xlate[0x12] = '`';		// shift dn - carriage control
		cn24_xlate[0x13] = '~';		// shift up - carriage control
		cn24_xlate[0x14] = 'i';
		cn24_xlate[0x15] = '\'';
		cn24_xlate[0x16] = '.';
		cn24_xlate[0x17] = '\001';	// 1/2...
		cn24_xlate[0x18] = '\r';	// return/index - carriage control
		cn24_xlate[0x19] = 'o';
		cn24_xlate[0x1a] = '\n';	// index - carriage control
		cn24_xlate[0x1b] = '\014';	// rev index - carriage control (FF/VT)
		cn24_xlate[0x1c] = 'a';
		cn24_xlate[0x1d] = 'r';
		cn24_xlate[0x1e] = 'v';
		cn24_xlate[0x1f] = 'm';

		cn24_xlate[0x20] = 'b';
		cn24_xlate[0x21] = 'h';
		cn24_xlate[0x22] = (byte)'\201';	// step x+
		cn24_xlate[0x23] = (byte)'\202';	// step x-
		cn24_xlate[0x24] = 'k';
		cn24_xlate[0x25] = 'e';
		cn24_xlate[0x26] = 'n';
		cn24_xlate[0x27] = 't';
		cn24_xlate[0x28] = (byte)'\300';	// print mode
		cn24_xlate[0x29] = 'l';
		cn24_xlate[0x2a] = (byte)'\210';	// step y+
		cn24_xlate[0x2b] = (byte)'\220';	// step y-
		cn24_xlate[0x2c] = 'c';
		cn24_xlate[0x2d] = 'd';
		cn24_xlate[0x2e] = 'u';
		cn24_xlate[0x2f] = 'x';

		cn24_xlate[0x30] = '9';
		cn24_xlate[0x31] = '0';
		cn24_xlate[0x32] = (byte)'\211';	// step x+y+
		cn24_xlate[0x33] = (byte)'\212';	// step x-y+
		cn24_xlate[0x34] = '6';
		cn24_xlate[0x35] = '5';
		cn24_xlate[0x36] = '2';
		cn24_xlate[0x37] = 'z';
		cn24_xlate[0x38] = (byte)'\200';	// plot mode
		cn24_xlate[0x39] = '4';
		cn24_xlate[0x3a] = (byte)'\221';	// step x+y-
		cn24_xlate[0x3b] = (byte)'\222';	// step x-y-
		cn24_xlate[0x3c] = '8';
		cn24_xlate[0x3d] = '7';
		cn24_xlate[0x3e] = '3';
		cn24_xlate[0x3f] = '1';

		// shifted versions...
		cn24_xlate[0x40] = '_';
		cn24_xlate[0x41] = 'Y';
		cn24_xlate[0x42] = ' ';		// space - carriage control
		cn24_xlate[0x43] = '\b';	// bkspace - carriage control
		cn24_xlate[0x44] = 'Q';
		cn24_xlate[0x45] = 'P';
		cn24_xlate[0x46] = '+';
		cn24_xlate[0x47] = 'J';
		cn24_xlate[0x48] = '\t';	// tab - carriage control
		cn24_xlate[0x49] = '?';
		cn24_xlate[0x4a] = '|';		// set-tab - carriage control
		cn24_xlate[0x4b] = '\\';	// clr-tab - carriage control
		cn24_xlate[0x4c] = ',';
		cn24_xlate[0x4d] = ':';
		cn24_xlate[0x4e] = 'F';
		cn24_xlate[0x4f] = 'G';

		cn24_xlate[0x50] = 'W';
		cn24_xlate[0x51] = 'S';
		cn24_xlate[0x52] = '`';		// shift dn - carriage control
		cn24_xlate[0x53] = '~';		// shift up - carriage control
		cn24_xlate[0x54] = 'I';
		cn24_xlate[0x55] = '"';
		cn24_xlate[0x56] = '.';
		cn24_xlate[0x57] = '\002';	// 1/4
		cn24_xlate[0x58] = '\r';	// return/index - carriage control
		cn24_xlate[0x59] = 'O';
		cn24_xlate[0x5a] = '\n';	// index - carriage control
		cn24_xlate[0x5b] = '\014';	// rev index - carriage control (FF/VT)
		cn24_xlate[0x5c] = 'A';
		cn24_xlate[0x5d] = 'R';
		cn24_xlate[0x5e] = 'V';
		cn24_xlate[0x5f] = 'M';

		cn24_xlate[0x60] = 'B';
		cn24_xlate[0x61] = 'H';
		cn24_xlate[0x62] = (byte)'\201';	// step x+
		cn24_xlate[0x63] = (byte)'\202';	// step x-
		cn24_xlate[0x64] = 'K';
		cn24_xlate[0x65] = 'E';
		cn24_xlate[0x66] = 'N';
		cn24_xlate[0x67] = 'T';
		cn24_xlate[0x68] = (byte)'\300';	// print mode
		cn24_xlate[0x69] = 'L';
		cn24_xlate[0x6a] = (byte)'\210';	// step y+
		cn24_xlate[0x6b] = (byte)'\220';	// step y-
		cn24_xlate[0x6c] = 'C';
		cn24_xlate[0x6d] = 'D';
		cn24_xlate[0x6e] = 'U';
		cn24_xlate[0x6f] = 'X';

		cn24_xlate[0x70] = '(';
		cn24_xlate[0x71] = ')';
		cn24_xlate[0x72] = (byte)'\211';	// step x+y+
		cn24_xlate[0x73] = (byte)'\212';	// step x-y+
		cn24_xlate[0x74] = '\003';	// cent
		cn24_xlate[0x75] = '%';
		cn24_xlate[0x76] = '@';
		cn24_xlate[0x77] = 'Z';
		cn24_xlate[0x78] = (byte)'\200';	// plot mode
		cn24_xlate[0x79] = '$';
		cn24_xlate[0x7a] = (byte)'\221';	// step x+y-
		cn24_xlate[0x7b] = (byte)'\222';	// step x-y-
		cn24_xlate[0x7c] = '*';
		cn24_xlate[0x7d] = '&';
		cn24_xlate[0x7e] = '#';
		cn24_xlate[0x7f] = '!';

		cn24_spcl = new String[8];
		cn24_spcl[0x01] = "\u00BD";	// 1/2
		cn24_spcl[0x02] = "\u00BC";	// 1/4
		cn24_spcl[0x03] = "\u00A2";	// cent
	}

	public Wang_CharConverter() {
		setup_xlate();
	}

	public String tiltrotateToAscii(byte code, boolean shifted) {
		// Typically, caller has already decoded carriage control, etc.
		// But if not, these get converted to ASCII equivalents.
		String s;
		if (shifted) code |= 0x40;
		byte[] bb = new byte[1];
		bb[0] = cn24_xlate[code];
		if (bb[0] == 0) {
			s = null;
		} else if ((bb[0] & 0x00ff) < 0x07) {
			s = cn24_spcl[bb[0]];
		} else {
			s = new String(bb);
		}
		return s;
	}
}
