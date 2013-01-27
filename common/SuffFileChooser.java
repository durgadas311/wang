// Copyright (c) 2011,2012 Douglas Miller
// $Id: SuffFileChooser.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $

import java.awt.*;
import java.io.*;
import javax.swing.*;

class SuffFileChooser extends JFileChooser {
	static final long serialVersionUID = 311457692041L;
	private String _sfx;
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
		_sfx = "." + sfx;
		_prot = new TapeProt("Protect");
		setAccessory(_prot);
	}
	public int showDialog(Component frame) {
		int rv = super.showDialog(frame, _btn);
		if (rv == JFileChooser.APPROVE_OPTION) {
			if (getSelectedFile().getName().endsWith(_sfx)) {
				return rv;
			}
			File f = new File(getSelectedFile().getAbsolutePath().concat(_sfx));
			setSelectedFile(f);
		}
		return rv;
	}
	public boolean isProtected() {
		return _prot.btn.getState();
	}
}
