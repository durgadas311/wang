// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_TapeDevice.java,v 1.1 2013/01/27 16:02:32 drmiller Exp $

interface Wang_TapeDevice {
	// Process byte-pair as Wang Tape Device command/data
	void do_tape(byte[] b);

	// Process button event related to tape device.
	// Return desired new state of button: false=off true=on
	boolean do_button(Wang_Keys btn);
}
