// Copyright (c) 2011,2025 Douglas Miller <durgadas311@gmail.com>

public interface Wang_FrontPanel {
	void breakpoint(int pc);
	void debug_check();
	void tape_record(byte to_byte);
	int tape_play();
	void tape_on(int wr);
	void tape_off(int wr);
	void dev_out(byte iob, byte c);
	int getMode0(boolean clear);
	int getMode1(boolean clear);
	void do_printer(int x, byte pr_drum);
	void do_line();
	void setOv(byte on);
	void setErr(byte on);
	void display_check(boolean mr);
	int getMemSize();
	int getMemMask();
}
