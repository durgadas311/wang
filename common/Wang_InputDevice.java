// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputDevice.java,v 1.2 2013/02/22 14:12:56 drmiller Exp $


// e.g. Group 1/2 Devices attached to a Wang "CN-36" port (Input only)
interface Wang_InputDevice
{
	static String Model = "00";
	static String Description = "Unknown";

	// General-purpose device reset. Issued when IOB is set to 0,
	// either at PRIME or the end of an I/O sequence. For Group 1/2
	// devices this ends the "I own the bus" period.
	void reset();

	// Process "start" byte-pair in context of Wang Input Device.
	// Return "true" if this device "owns" that code.
	// This represents the "negotiation" protocol when the Group I/O
	// is first issued.
	boolean start_cn36(byte[] b);

	// Process normal byte-pair in context of Wang Input Device.
	// This is typically only used for ACK data in response to
	// codes sent from peripheral device.
	void do_cn36(byte[] b);

	// returns descriptive name of device. Since an interface can't
	// define static methods, these can't be enforced.
	//
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
