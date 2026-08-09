// Copyright (c) 2025 Douglas Miller <durgadas311@gmail.com>

import java.io.*;
import java.util.Properties;
import java.util.Arrays;
import java.util.Random;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;

public class Wang700RamPortal extends JPanel
		implements ActionListener, FocusListener {

	static final int btn_w = 80;
	static final int btn_h = 30;

	Wang700_CPU cpu;

	GridBagLayout gb;
	GridBagConstraints gc;

	int num_rows;
	int[] row_adrs;
	JTextField[] ram_adrs;
	JTextArea[] ram_rows;

	class RowTextField extends JTextField {
		public int idx;
		public RowTextField(int idx) {
			super();
			this.idx = idx;
		}
	}

	class MultiTextField extends JTextArea {
		public int idx;
		public MultiTextField(int idx) {
			super();
			this.idx = idx;
		}
	}

	public Wang700RamPortal(Wang700_CPU cpu, int num_rows) {
		super();
		this.cpu = cpu;
		this.num_rows = num_rows;
		Font font = new Font("Monospaced", Font.PLAIN, 10);

		row_adrs = new int[num_rows];
		ram_adrs = new RowTextField[num_rows];
		ram_rows = new MultiTextField[num_rows];
		JTextField tf;
		JTextArea ta;
		for (int x = 0; x < num_rows; ++x) {
			row_adrs[x] = -1;
			tf = new RowTextField(x);
			tf.setFont(font);
			tf.setPreferredSize(new Dimension(30, 20));
			tf.setHorizontalAlignment(SwingConstants.RIGHT);
			tf.setEditable(true);
			tf.setFocusable(true);
			tf.addFocusListener(this);
			tf.addActionListener(this);
			ram_adrs[x] = tf;
			ta = new MultiTextField(x);
			ta.setFont(font);
			ta.setPreferredSize(new Dimension(300, 30));
			ta.setEditable(false);
			ta.setFocusable(false);
			ram_rows[x] = ta;
		}

		gb = new GridBagLayout();
		setLayout(gb);
		gc = new GridBagConstraints();
		gc.fill = GridBagConstraints.NONE;
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weightx = 0;
		gc.weighty = 0;
		gc.gridwidth = 1;
		gc.gridheight = 1;
		gc.anchor = GridBagConstraints.CENTER;

		setGap(5);

		gc.gridx = 1;
		gc.gridy = 1;
		setLabel("Row Addr");
		++gc.gridx;
		setGap(5);
		++gc.gridx;
		setLabel("RAM Row Contents");
		++gc.gridx;
		++gc.gridy;
		for (int x = 0; x < num_rows; ++x) {
			gc.gridx = 1;
			gb.setConstraints(ram_adrs[x], gc);
			add(ram_adrs[x]);
			++gc.gridx;
			++gc.gridx;
			gb.setConstraints(ram_rows[x], gc);
			add(ram_rows[x]);
			++gc.gridx;
			++gc.gridy;
			setGap(2);
			++gc.gridy;
		}
		setGap(5);
	}

	private void setGap(int wid) {
		JPanel pan = new JPanel();
		pan.setPreferredSize(new Dimension(wid, 10));
		gb.setConstraints(pan, gc);
		add(pan);
	}

	private void setLabel(String str) {
		JLabel lab = new JLabel(str);
		lab.setOpaque(true);
		gb.setConstraints(lab, gc);
		add(lab);
	}

	private void dumpRow(int idx) {
		String str1 = new String();
		String str2 = new String();
		int aa = row_adrs[idx];
		int yy;
		for (yy = 0; yy < 16; ++yy) {
			byte bb = cpu._ram[aa++];
			str1 += String.format("%02d", (bb >> 4) & 0x0f);
			str2 += String.format("%02d", (bb & 0x0f));
			if (yy != 15) {
				str1 += ' ';
				str2 += ' ';
			}
		}
		ram_rows[idx].setText(str1 + '\n' + str2);
	}

	public void refresh() {
		for (int x = 0; x < num_rows; ++x) {
			if (row_adrs[x] < 0) {
				continue;
			}
			dumpRow(x);
		}
		repaint();
	}

	private void do_adr_field(RowTextField tf) {
		String a = tf.getText();
		int x = tf.idx;
		int ad;
		try {
			ad = Integer.valueOf(a, 16);
		} catch (Exception ee) { // includes a.length() == 0
			tf.setText("");
			row_adrs[x] = -1;
			return;
		}
		ad &= 0xff0;
		tf.setText(String.format("%03x", ad));
		row_adrs[x] = ad & cpu.memmask;
		dumpRow(x);
		repaint();
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src instanceof RowTextField) {
			// "Enter" pressed in text field
			do_adr_field((RowTextField)src);
			return;
		}
	}

	public void focusGained(FocusEvent e) {}
	public void focusLost(FocusEvent e) {
		Object src = e.getSource();
		if (src instanceof RowTextField) {
			// Leaving text field
			do_adr_field((RowTextField)src);
			return;
		}
	}
}
