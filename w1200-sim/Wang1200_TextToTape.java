// Copyright (c) 2013,2014 Douglas Miller
// $Id: Wang1200_TextToTape.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $ 

import java.io.*;
import java.util.Arrays;

public class Wang1200_TextToTape
{
	private class w1200_line {
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

	private BufferedReader _in;
	private byte[] escapes = new byte[256];
	private Wang_CharConverter _cnv;
	private boolean _eod;
	private String _name;
	private int _line;
	w1200_line _buf;

	private int cvt_line(String inb, w1200_line buf) {
		buf.reset();
		char c = '\0';
		int y = 0;
		int x = 0;
		byte t;
try {
		while (y < buf.line.length && c != '\n') {
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
						System.err.format("%s:%d: invalid escape character '%c'\n", _name, _line, cx);
						return 1;
					}
				}
				if (inb.charAt(x) != ']') {
					System.err.format("%s:%d: invalid code string for '%c' @%d:\"%s\"\n", _name, _line, c, x, inb);
					return 1;
				}
				if (c == '/') {	// EOD - strip off \n
					if (inb.charAt(x) == '\n') ++x;
					_eod = true;
				}
			}
			t = _cnv.asciiToCodedTiltrotate((byte)c, code);
			buf.line[y++] = t;
			if (t == (byte)0x0a) { c = '\n'; }
		}
} catch(IndexOutOfBoundsException ee) {
			// should check context... e.g. not in CODE sequence.
			c = '\n';
}
		if (c != '\n') {
			System.err.format( "%s:%d: line overflow\n", _name, _line);
			return 1;
		}
		return 0;
	}

	public Wang1200_TextToTape() {
		_buf = new w1200_line();
		escapes['b'] = '\b';
		escapes['n'] = '\n';
		escapes['t'] = '\t';
		_cnv = new Wang_CharConverter(true);
	}

	// converts stdin to stdout... stdout is binary 108-block formatted...
	public void textToTape(String name, InputStream in, OutputStream out) {
		_in = new BufferedReader(new InputStreamReader(in));
		_name = name;

		int e = 0;
		_line = 0;
		_eod = false;
		while (e == 0) {
			++_line;
			try {
				e = -1;
				String s = _in.readLine();
				if (s != null) {
					e = cvt_line(s + '\n', _buf);
				}
			} catch(Exception ee) { }
			if (e == 0) {
				_buf.write(out);
			}
		}
		if (!_eod) {
			++_line;
			e = cvt_line("[/]\n", _buf);
			if (e == 0) {
				_buf.write(out);
			}
		}
	}
}
