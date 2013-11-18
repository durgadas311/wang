// Copyright (c) 2013 Douglas Miller
// $Id: Wang_Core.java,v 1.5 2013/11/18 18:19:10 drmiller Exp $

interface Wang_Core extends Runnable {
	public void chgMode0();
	public void chgMode1();
	public void pressCmd(int cmd);
	public void pressKey(int key);
	public void ackIO(int iob);
	public void replyIO(int iob, int rep);

	// These are needed by the debugging console...
	public void debugIntr();	// user requested debug mode...

	public void run();
}
