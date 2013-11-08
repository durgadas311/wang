// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_TapeDevice.java,v 1.2 2013/11/08 21:12:28 drmiller Exp $

interface Wang_TapeDevice {
	void tape_on(int wr);
	void tape_off(int wr);
	void tape_record(int byt);
	int tape_play();

	// Process button event related to tape device.
	// Return desired new state of button: false=off true=on
	boolean do_button(Wang_Keys btn);
}
