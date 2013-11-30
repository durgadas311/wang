// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_TapeDevice.java,v 1.3 2013/11/30 17:51:45 drmiller Exp $

interface Wang_TapeDevice {
	public void tape_on(int wr);
	public void tape_off(int wr);
	public void tape_record(int byt);
	public int tape_play();

	// Process button event related to tape device.
	// Return desired new state of button: false=off true=on
	public boolean do_button(Wang_Keys btn);
	public Wang_Keys ejectKey();
}
