// Copyright (c) 2011,2024 Douglas Miller

import java.util.Vector;

public class Wang_CN36_Bus
{
	private static Vector<Wang_GroupIODevice> _cn36;

	static public void registerCN36(Wang_GroupIODevice dev) {
		if (_cn36 == null) {
			_cn36 = new Vector<Wang_GroupIODevice>();
		}
		_cn36.add(dev);
	}
	static public void deregisterCN36(Wang_GroupIODevice dev) {
		_cn36.removeElement(dev);
	}
	static public void resetCN36() {
		if (_cn36 != null) {
			for (Wang_GroupIODevice dev : _cn36) {
				dev.reset();
			}
		}
	}
	static public Wang_GroupIODevice startCN36(int iob, int c) {
		Wang_GroupIODevice dev = null;
		if (_cn36 != null) {
			for (Wang_GroupIODevice _dev : _cn36) {
				if (_dev.start_cn36(iob, c)) {
					dev = _dev;
					break;
				}
			}
		}
		return dev;
	}
}
