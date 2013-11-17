
interface Wang_Core extends Runnable {
	public void chgMode0();
	public void chgMode1();
	public void pressCmd(int cmd);
	public void pressKey(int key);
	public void chgXROM();
	public void ackIO(int iob);
	public void replyIO(int iob, int rep);

	// These are needed by the debugging console...
	public void debugIntr();	// user requested debug mode...
	public int getPC();		// current CPU PC register
	public int getRamAdr();		// current CPU RAM address register(s)
	public int getRamSize();	// bytes
	public int getUcodeSize();	// words (instructions)
	public long getUcodeLong(int adr);	// word (instruction) address
	public byte[] getUcodeBytes(int adr);// word (instruction) address
	public byte getRam(int adr);	// byte address
	public void putRam(int adr, byte b);
	public int getXRomSize();	// bytes
	public byte getXRom(int adr);	// byte address
	public long relCycleLimit(long n);
	public void setRun(boolean run);
	public boolean breakPoint(int adr);	// toggles BP at adr
	public boolean getBreakPoint(int adr);

	public void run();
}
