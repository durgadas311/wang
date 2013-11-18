
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

	public void run();
}
