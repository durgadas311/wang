// Copyright (c) 2013,2014 Douglas Miller
// $Id: txt2wng.java,v 1.3 2014/01/14 21:53:51 drmiller Exp $ 

import java.io.*;

public class txt2wng
{
	// converts stdin to stdout... stdout is binary 108-block formatted...
	public static void main(String[] args) {
		Wang1200_TextToTape cnv = new Wang1200_TextToTape();
		if (args.length > 0) {
			for (int x = 0; x < args.length; ++x) {
				String f = args[x];
				int ix = f.lastIndexOf(".txt");
				if (ix > 0) {
					f = f.substring(0, ix);
				}
				f += ".wpt";
				FileInputStream fin = null;
				FileOutputStream fout = null;
				try {
					fin = new FileInputStream(args[x]);
				} catch(Exception ee) {
					System.err.println(args[x] + ": " + ee.getMessage());
				}
				try {
					fout = new FileOutputStream(f);
				} catch(Exception ee) {
					System.err.println(f + ": " + ee.getMessage());
				}
				if (fin != null && fout != null) {
					cnv.textToTape(args[x], fin, fout);
					try {
						fin.close();
					} catch(Exception ee) { }
					try {
						fout.close();
					} catch(Exception ee) {
						System.err.println(f + ": " + ee.getMessage());
					}
				}
			}
		} else {
			cnv.textToTape("stdin", System.in, System.out);
		}
	}
}
