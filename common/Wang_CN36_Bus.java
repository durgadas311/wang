// Copyright (c) 2011,2026 Douglas Miller

import java.util.Vector;
import javax.swing.*;

public class Wang_CN36_Bus
{
	private static Vector<Wang_GroupIODevice> _cn36;

	// If _cn36 is null then these also will be null.
	private static Wang_GroupIODevice _idev;
	private static Wang_BlockIODevice _bdev;

	static public void registerCN36(Wang_GroupIODevice dev) {
		if (_cn36 == null) {
			_cn36 = new Vector<Wang_GroupIODevice>();
		}
		if (_cn36.contains(dev)) return;
		_cn36.add(dev);
	}
	static public void deregisterCN36(Wang_GroupIODevice dev) {
		if (_cn36 == null) return; // caller error
		_cn36.removeElement(dev);
	}
	static public void unPlugAll(JMenu mu) {
		if (_cn36 == null) return; // caller error
		_idev = null;
		_bdev = null;
		// Cannot remove elements while iterating them,
		// need to make a list that maintains the original
		// element objects.
		Vector<Wang_GroupIODevice> list = new Vector<Wang_GroupIODevice>();
		for (Wang_GroupIODevice dev : _cn36) {
			list.add(dev);
		}
		for (Wang_GroupIODevice dev : list) {
			dev.unPlug(mu); // calls back into deregisterCN36()...
		}
		_cn36.clear(); // all should have been removed, already
	}
	static public void resetCN36() {
		if (_cn36 == null) return;
		for (Wang_GroupIODevice dev : _cn36) {
			dev.reset();
		}
		_idev = null;
		_bdev = null;
	}
	// Calculator has strobed IOB and GIoA/GIoB. IOB meanings:
	// IOB=0: end of current operation. device may or may not be disabled
	// IOB=2/3: block I/O protocol
	// IOB=4: GROUP-1 from keyboard (user)
	// IOB=5: GROUP-2 from keyboard (user)
	// IOB=6: GROUP-1 from running program
	// IOB=7: GROUP-2 from running program
	static public void doCN36(int iob, int c) {
		if (iob == 2 || iob == 3) { // block I/O
			// only one device can be active, and it
			// should already know what this byte means.
			if (_bdev != null) {
				_bdev.do_dev(iob, c);
			}
			return;
		}
		// Every call is a new GROUP 1/2 command,
		// there are no other strobes of GISO for IOB 4,5,6,7.
		if (_cn36 == null) return; // no devices registered, nothing to do
		// pass to all devices and see who is still enabled.
		_idev = null;
		_bdev = null;
		for (Wang_GroupIODevice dev : _cn36) {
			if (!dev.start_cn36(iob, c)) continue;
			// dev is (now/still) enabled
			if (dev.isBlockIO()) { // by definition, must be Wang_BlockIODevice
				_bdev = (Wang_BlockIODevice)dev;
			} else if (_idev == null) {
				_idev = dev;
			}
		}
	}
	static public void setGKBD(boolean state) {
		// TODO: does Block I/O use this?
		if (_idev != null) _idev.setGKBD(state);
		if (_bdev != null) _bdev.setGKBD(state);
	}
	static public int getGLRN() {
		if (_idev != null) {
			return _idev.getGLRN();
		} else if (_bdev != null) {
			return _bdev.getGLRN();
		} else {
			return 0;
		}
	}
}
