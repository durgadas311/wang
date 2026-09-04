// Copyright (c) 2013,2014 Douglas Miller
// $Id: Wang_Core.java,v 1.7 2014/01/14 21:53:51 drmiller Exp $

interface Wang_Core extends Runnable {
	public void chgMode0();
	public void chgMode1();
	public void chgMode2();
	public void pressCmd(int cmd);
	public boolean isKeyOK();
	public void pressKey(int key);
	public void ackIO(int iob);
	public void replyIO(int iob, int rep);

	// These are needed by the debugging console...
	public void debugIntr();	// user requested debug mode...
	public Wang_Debugger getDebug();

	public void run();
}
