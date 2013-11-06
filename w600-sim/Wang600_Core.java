
interface Wang600_Core extends Runnable {
	public void chgMode0();
	public void chgMode1();
	public void pressCmd(int cmd);
	public void pressKey(int key);
	public void chgXROM();
	public void sendCN36(int rep);

	public void run();
}
