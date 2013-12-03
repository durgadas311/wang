// Copyright (c) 2013 Douglas Miller
// $Id: txt2wng.java,v 1.1 2013/12/03 23:05:36 drmiller Exp $ 

import java.io.*;
import java.util.Arrays;

	class w1200_line {
		byte[] hdr = new byte[8];
		byte[] line = new byte[100];
		public void write(OutputStream out) {
			try {
				out.write(hdr);
				out.write(line);
			} catch(Exception ee) {
			}
		}
		public void reset() {
			Arrays.fill(hdr, (byte)0x00);
			Arrays.fill(line, (byte)0xaa);
		}
	}

public class txt2wng
{
	static private BufferedReader _in;

	static private byte[] escapes = new byte[256];
	static private Wang_CharConverter _cnv;

	static private boolean _eod;
	static private int _line;

	static private int cvt_line(String inb, w1200_line buf) {
		buf.reset();
		char c = '\0';
		int y = 0;
		int x = 0;
		byte t;
try {
		while (y < buf.line.length) {
			_eod = false;
			c = inb.charAt(x++);
			boolean code = (c == '['); // CODE characters...
			if (code) {
				c = inb.charAt(x++);
				if (inb.charAt(x) == '\\') {
					// ASCII escapes...
					++x;
					char cx = inb.charAt(x++);
					c = (char)escapes[Character.toLowerCase(cx)];
					if (c == '\0') {
						System.err.format("%d: invalid escape character '%c'\n", _line, cx);
						return 1;
					}
				}
				if (inb.charAt(x) != ']') {
					System.err.format("%d: invalid code string for '%c' @%d:\"%s\"\n", _line, c, x, inb);
					return 1;
				}
				if (c == '/') {	// EOD - strip off \n
					if (inb.charAt(x) == '\n') ++x;
					_eod = true;
					c = '\n';
				}
			}
			t = _cnv.asciiToCodedTiltrotate((byte)c, code);
			buf.line[y++] = t;
			if (c == '\n' || t == (byte)0x0a) break;
		}
} catch(IndexOutOfBoundsException ee) {
}
		if (c != '\n') {
			System.err.format( "%d: line overflow\n", _line);
			return 1;
		}
		return 0;
	}

	// converts stdin to stdout... stdout is binary 108-block formatted...
	public static void main(String[] args) {
		w1200_line buf = new w1200_line();
		_in = new BufferedReader(new InputStreamReader(System.in));
		escapes['b'] = '\b';
		escapes['n'] = '\n';
		escapes['t'] = '\t';
		_cnv = new Wang_CharConverter(true);

		int e = 0;

		_line = 0;

		while (e == 0) {
			++_line;
			try {
				String s = _in.readLine();
				e = cvt_line(s, buf);
			} catch(Exception ee) {
				e = -1;
			}
			if (e == 0) {
				buf.write(System.out);
			}
		}
		if (!_eod) {
			++_line;
			e = cvt_line("[/]", buf);
			if (e == 0) {
				buf.write(System.out);
			}
		}
	}
}
