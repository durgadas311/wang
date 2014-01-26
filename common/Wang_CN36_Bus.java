// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_CN36_Bus.java,v 1.1 2014/01/26 14:52:57 drmiller Exp $

public class Wang_CN36_Bus
{
	final String ident = "$Id: Wang_CN36_Bus.java,v 1.1 2014/01/26 14:52:57 drmiller Exp $";

	private static Wang_InputDevice[] _cn36;

	static public void registerCN36(Wang_InputDevice dev) {
		Wang_InputDevice[] newdevs;
		if (_cn36 == null) {
			newdevs = new Wang_InputDevice[1];
			newdevs[0] = dev;
		} else {
			int oldnum = _cn36.length;
			newdevs = new Wang_InputDevice[oldnum + 1];	
			System.arraycopy(_cn36, 0, newdevs, 0, oldnum);
			newdevs[oldnum] = dev;
		}
		_cn36 = newdevs;
	}
	static public void deregisterCN36(Wang_InputDevice dev) {
		int ix = -1;
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				if (_cn36[x].equals(dev)) {
					ix = x;
					break;
				}
			}
		}
		if (ix < 0) {
			return;
		}
		Wang_InputDevice[] newdevs;
		int oldnum = _cn36.length;
		newdevs = new Wang_InputDevice[oldnum - 1];
		if (ix > 0) {
			System.arraycopy(_cn36, 0, newdevs, 0, ix);
		}
		if (ix < oldnum - 1) {
			System.arraycopy(_cn36, ix + 1, newdevs, ix, oldnum - 1 - ix);
		}
		_cn36 = newdevs;
	}
	static public void resetCN36() {
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				_cn36[x].reset();
			}
		}
	}
	static public Wang_InputDevice startCN36(int iob, int c) {
		Wang_InputDevice dev = null;
		if (_cn36 != null) {
			for (int x = 0; x < _cn36.length; ++x) {
				if (_cn36[x].start_cn36(iob, c)) {
					dev = _cn36[x];
					break;
				}
			}
		}
		return dev;
	}
}
