// Copyright (c) 2011,2026 Douglas Miller


// e.g. Group 1/2 Devices attached to a Wang "CN-36" port (typically Input only)
//
// When the calculator executes GROUP [12] XX-XX it sets up IOB and
// then strobes XX-XX out to select the device, then waits for input
// from the device, but otherwise processes that as keyboard input.
//
// Mode   GROUP   IOB
// STOP     1     100	(keyboard mode)
// RUN      1     110	(executing program)
// STOP     2     101	(keyboard mode)
// RUN      2     111	(executing program)
//
// Device then controls the calculator input and typically issues a
// GO command to resume normal operation. Device may also control
// LEARN mode (getGLRN()), typically used to force new code/data into
// program memory.
//
// When the calulator executes the GROUP [12] XX-XX code, the
// simulator ends up calling start_cn36() for all registered
// devices. The first one returning true  becomes the chosen
// (currently selected) device.
//
// do_ack() is called by the calculator to acknowledge each byte
// sent by the device (using Wang_UI.getCore().replyIO()).
//
// Devices may use the constants "GO", ... to send those commands
// to the calculator, while still maintaining independence from
// 700 Series vs. 600 Series code differences. Example:
//	Wang_UI.getCore().replyIO(iob, GO);
// Note that the 'iob' parameter is ignored.
//
// There are two classes of devices, "block I/O" and "input only".
// It may be a convention that GROUP-2 devices are block I/O and
// GROUP-1 devices are input-only. Block I/O devices will immediately
// reply with GO when selected, remain selected, and perform their work
// during the IOB=2/3 protocol phases. Input only devices will send
// data to the calculator after being selected, and finish with a GO
// or some other terminating command code.
//

interface Wang_GroupIODevice extends Wang_Peripheral
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

	boolean isBlockIO(); // supports IOB2/3 protocols?
	boolean isDevEnabled(); // did last start_cn36() return true?

	// Process "start" byte-pair in context of Wang Input Device.
	// Return "true" if this device is enabled/selected.
	// There may be a maximum of two devices enabled at a time,
	// although a given GROUP-1/2 command can only enable 1.
	// A block I/O device may stay enabled while a input-only
	// device is being selected and used.
	// This represents the "negotiation" protocol when the Group I/O
	// is first issued.
	boolean start_cn36(int iob, int c);

	// Process ACK for previous send
	void do_ack(int iob);

	// an ACK of sorts that input char has been processed
	void setGKBD(boolean state);

	// Get device's assertion state for GLRN signal (0/1)
	int getGLRN();

	// returns descriptive name of device.
	// Used before instantiating class.
	// Java still can't do static right...
	// static String getName();
}
