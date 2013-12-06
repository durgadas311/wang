// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_OutputDevice.java,v 1.7 2013/12/06 20:54:06 drmiller Exp $

import java.awt.*;
import javax.swing.*;


// e.g. Devices attached to a Wang 600 "CN-24" port (Output Only)
// TODO: handle Input/Output devices like IBM Selectric Typewriter (Wang1200)?
interface Wang_OutputDevice
{
	static String Model = "00";
	static String Description = "Unknown";

	// General-purpose device reset
	void reset();

	// Process byte-pair in context of Wang Output Device
	void do_cn24(byte b);
	void do_cn24_direct(char c);
	// carriage-control, etc.
	void do_space();
	void do_backspace();
	void do_crlf();
	void do_index();
	void do_tab();
	void do_settab();
	void do_clrtab();
	void do_shift_up();
	void do_shift_dn();
	void do_lock(int lk);
	void do_bell();

	// Return the text-frame of the device, for handling events
	// and setting up action listeners
	JFrame getFrame();
	Component getComponent();

	// Set the visibility of the output frame
	void onOff(boolean on);

	// Return current visibility of the output frame
	boolean onOff();

	public void setProperties(Wang_Properties p);

	// returns descriptive name of device
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
