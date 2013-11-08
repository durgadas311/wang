// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_OutputDevice.java,v 1.3 2013/11/08 21:12:28 drmiller Exp $

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

	// Return the text-frame of the device, for handling events
	// and setting up action listeners
	JFrame getFrame();

	// Set the visibility of the output frame
	void onOff(boolean on);

	// Return current visibility of the output frame
	boolean onOff();

	// returns descriptive name of device
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
