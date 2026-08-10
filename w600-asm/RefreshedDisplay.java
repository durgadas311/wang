// Copyright (c) 2026 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;
import javax.swing.*;

public class RefreshedDisplay extends JTextField {
	byte[] disp;
	boolean[] lit;
	int count;
	byte prev_n;
	byte prev_d;

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
	}

	public void do_refresh(byte nn, byte dd, boolean ok) {
		byte n = prev_n;
		byte d = prev_d;

		prev_n = nn;
		prev_d = dd;
		if (!ok) return;
		// if long enough to be visible, process *previous*
		// refresh data...
		++count;
		lit[n] = true;
		if (n == 0 || n == 13) {
			if (d == 15) {
				d = ' ';
			} else {
				d = (byte)((d & 1) != 0 ? '-' : '+');
			}
		} else {
			if (d == 10) d = '.';
			else if (d == 15) d = ' ';
			else if (d > 10) d += ('A' - 10);
			else d += '0';
		}
		disp[n] = d;
		if (count >= 80) { // some number to approximate several full cycles
			count = 0;
			for (int x = 0; x < 16; ++x) {
				if (!lit[x]) {
					disp[x] = ' ';
				} else {
					lit[x] = false;
				}
			}
		}
		setText(new String(disp));
	}
}
