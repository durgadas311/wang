// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_InputDevice.java,v 1.7 2014/01/03 23:48:40 drmiller Exp $


// e.g. Group 1/2 Devices attached to a Wang "CN-36" port (Input only)
interface Wang_InputDevice
{
	static String Model = "00";
	static String Description = "Unknown";

	// Let core translate to proper codes
	public static int GO = 0x0f00;		// GO
	public static int START = 0x0f01;	// WRITE ALPHA or just ALPHA
	public static int END = 0x0f02;		// "end alpha"
	public static int EOT = 0x0f03;		// End Of (paper) Tape
	public static int E0 = 0x0f04;		// Digit Entry, 0
	public static int E1 = 0x0f05;		// Digit Entry, 1
	public static int E2 = 0x0f06;		// Digit Entry, 2
	public static int E3 = 0x0f07;		// Digit Entry, 3
	public static int E4 = 0x0f08;		// Digit Entry, 4
	public static int E5 = 0x0f09;		// Digit Entry, 5
	public static int E6 = 0x0f0a;		// Digit Entry, 6
	public static int E7 = 0x0f0b;		// Digit Entry, 7
	public static int E8 = 0x0f0c;		// Digit Entry, 8
	public static int E9 = 0x0f0d;		// Digit Entry, 9
	public static int DP = 0x0f0e;		// Decimal Point
	public static int CHG_SIGN = 0x0f0f;	// Change Sign
	public static int SET_EXP = 0x0f10;
	public static int CLR_DSP = 0x0f11;
	public static int SR0 = 0x0f20;		// Search-Return 0000
	public static int SR1 = 0x0f21;		// Search-Return 0001
	public static int SR2 = 0x0f22;		// Search-Return 0002
	// support all? many?
	public static int SREND = 0x0f30;

	// General-purpose device reset. Issued when IOB is set to 0,
	// either at PRIME or the end of an I/O sequence. For Group 1/2
	// devices this ends the "I own the bus" period.
	void reset();

	// Process "start" byte-pair in context of Wang Input Device.
	// Return "true" if this device "owns" that code.
	// This represents the "negotiation" protocol when the Group I/O
	// is first issued.
	boolean start_cn36(int iob, int c);

	// Process output byte in context of Wang Input Device.
	// There are typically only ACKs sent in response to
	// codes sent from peripheral device.
	void do_dev(int iob, int c);

	// Process ACK for previous send
	void do_ack(int iob);

	// Get device's assertion state for GLRN signal (0/1)
	int getGLRN();

	// returns descriptive name of device. Since an interface can't
	// define static methods, these can't be enforced.
	//
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
