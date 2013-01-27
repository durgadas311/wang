// Copyright (c) 2011,2012 Douglas Miller
// $Id: SuffFileFilter.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $

import java.io.*;

class SuffFileFilter extends javax.swing.filechooser.FileFilter {
	private String _sfx;
	private String _dsc;

	private String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 &&  i < s.length() - 1) {
			ext = s.substring(i+1).toLowerCase();
		}
		return ext;
	}

	public SuffFileFilter(String suffix, String desc) {
		_sfx = suffix;
		_dsc = desc;
	}

	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		String extension = getExtension(f);
		if (extension != null &&
				extension.equals(_sfx)) {
			return true;
		}
		return false;
	}

	public String getDescription() {
		return _dsc;
	}
}
