// Copyright (c) 2011,2012 Douglas Miller
// $Id: SuffFileChooser.java,v 1.2 2013/02/02 01:39:04 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;

class SuffFileChooser extends JFileChooser {
	static final long serialVersionUID = 311457692041L;
	private String _btn;
	private class TapeProt extends JComponent {
		static final long serialVersionUID = 31170769203L;
		public Checkbox btn;
		public TapeProt(String b) {
			btn = new Checkbox(b);
			setLayout(new FlowLayout());
			add(btn);
		}
	}
	private TapeProt _prot;
	public SuffFileChooser(String btn, String sfx, String dsc, File dir) {
		super(dir);
		SuffFileFilter f = new SuffFileFilter(sfx, dsc);
		setFileFilter(f);
		_btn = btn;
		setApproveButtonText(btn);
		setApproveButtonToolTipText(btn);
		setDialogTitle(btn);
		setDialogType(JFileChooser.SAVE_DIALOG);
		_prot = new TapeProt("Protect");
		setAccessory(_prot);
	}
	public SuffFileChooser(String btn, String[] sfx, String[] dsc, File dir) {
		super(dir);
		SuffFileFilter f = new SuffFileFilter(sfx[0], dsc[0]);
		setFileFilter(f);
		for (int i = 1; i < dsc.length; ++i) {
			f = new SuffFileFilter(sfx[i], dsc[i]);
			addChoosableFileFilter(f);
		}
		_btn = btn;
		setApproveButtonText(btn);
		setApproveButtonToolTipText(btn);
		setDialogTitle(btn);
		setDialogType(JFileChooser.SAVE_DIALOG);
		_prot = new TapeProt("Protect");
		setAccessory(_prot);
	}
	public int showDialog(Component frame) {
		int rv = super.showDialog(frame, _btn);
		if (rv == JFileChooser.APPROVE_OPTION) {
			SuffFileFilter fi = (SuffFileFilter)getFileFilter();
			String sfx = "." + fi.getSuffix();
			if (getSelectedFile().getName().endsWith(sfx)) {
				return rv;
			}
			File f = new File(getSelectedFile().getAbsolutePath().concat(sfx));
			setSelectedFile(f);
		}
		return rv;
	}
	public boolean isProtected() {
		return _prot.btn.getState();
	}
}
