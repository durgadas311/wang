// Copyright (c) 2026 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import javax.swing.*;

public class RefreshedDisplay extends JTextField {
	byte[] disp;
	byte dpv;
	boolean[] lit;
	int count;
	byte prev_n;
	byte prev_d;
	boolean prev_fxd;

	public RefreshedDisplay() {
		super();
		// rest of JTextField setup done by caller...
		disp = new byte[16];
		lit = new boolean[16];
		// caller should use do_blanking() before using...
	}

	public void do_blanking() {
		Arrays.fill(disp, (byte)' ');
		setText("");
		count = 0;
		dpv = 15;
	}

	public void do_refresh(byte nn, byte dd, boolean fxdx, boolean ok) {
		int dp;
		String ret = "";
		byte n = prev_n;
		byte d = prev_d;
		boolean fxd = prev_fxd;

		prev_n = nn;
		prev_d = dd;
		prev_fxd = fxdx;
		if (!ok) return;
		// if long enough to be visible, process *previous*
		// refresh data...
		++count;
		lit[n] = true;
		if (n == 15) dpv = d;
		if (n == 0 || n == 13) {
			if (d == 0) {
				d = '+';
			} else if (d == 1) {
				d = '-';
			} else {
				d = ' ';
			}
		} else {
			if (d < 10)
				d += '0';
			else
				d = ' ';
		}
		disp[n] = d;
		if (count >= 80) { // some number to approximate several full cycles
			count = 0;
			for (int x = 0; x < 16; ++x) {
				if (!lit[x]) {
					disp[x] = ' ';
					if (x == 13) dpv = 15;
				} else {
					lit[x] = false;
				}
			}
		}
		// (re)compute decimal point location
		if (fxd) {
			dp = 0;
		} else {
			dp = dpv; // invalid nums tolerated
		}
		// Real hardware actually blanks exponent digits if !FXD
		for (int x = 0; x < 16; ++x) {
			if (x < 13) {
				ret += (char)disp[x];
				if (x == dp) ret += '.';
			} else if (fxd) {
				ret += (char)disp[x];
			} else {
				ret += ' ';
			}
		}
		setText(ret);
	}
}
