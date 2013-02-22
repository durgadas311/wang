// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputDevice.java,v 1.1 2013/02/22 01:37:05 drmiller Exp $

import javax.swing.*;


// e.g. Group 1/2 Devices attached to a Wang 600 "CN-36" port (Input only)
interface Wang_InputDevice
{
	static String Model = "00";
	static String Description = "Unknown";

	// General-purpose device reset
	void reset();

	// Process "start" byte-pair in context of Wang Input Device.
	// Return "true" if this device "owns" that code.
	boolean start_cn36(byte[] b);

	// Process normal byte-pair in context of Wang Input Device.
	void do_cn36(byte[] b);

	// returns descriptive name of device
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
