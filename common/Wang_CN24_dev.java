// Copyright (c) 2011,2026 Douglas Miller

import javax.swing.*;

public class Wang_CN24_dev
{
	private static Wang_OutputDevice _cn24; // only one at a time

	static public void connect(Wang_OutputDevice dev) {
		_cn24 = dev;	// may be null
	}

	// caller must test for null
	static public Wang_OutputDevice get() { return _cn24; }

	static public void reset() {
		if (_cn24 == null) return;
		_cn24.reset();
	}

	static public void do_cn24(byte c) {
		if (_cn24 == null) return;
		_cn24.do_cn24(c);
	}

	static public int getRBS() {
		if (_cn24 == null) return 1; // do not hang if no device?
		return _cn24.getRBS();
	}

	static public JFrame getFrame() {
		if (_cn24 == null) return null;
		return _cn24.getFrame();
	}

	static public boolean onOff() {
		if (_cn24 == null) return false;
		return _cn24.onOff();
	}

	static public void onOff(boolean on) {
		if (_cn24 == null) return;
		_cn24.onOff(on);
	}
}
