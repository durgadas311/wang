// Copyright (c) 2011,2026 Douglas Miller

// e.g. Devices attached to a Wang 600 "CN-24" port (Output Only)
// TODO: handle Input/Output devices like IBM Selectric Typewriter (Wang1200)?
interface Wang_OutputDevice extends Wang_Peripheral
{
	static String Model = "00";
	static String Description = "Unknown";

	// Process byte-pair in context of Wang Output Device
	void do_cn24(byte b);
	void do_cn24_direct(char c);
	int getRBS();
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
}
