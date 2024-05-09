// Copyright (c) 2011,2026 Douglas Miller

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
}
