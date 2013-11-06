// Receives single byte-stream input from BackEnd simulator and directs
// messages to components.
class Wang600_SimulatorJava
	implements Wang600_Core
{
	final String ident = "$Id: Wang600_SimulatorJava.java,v 1.1 2013/11/06 21:53:35 drmiller Exp $";

	public void chgMode0() { }

	public void chgMode1() { }

	public void pressCmd(int cmd) {
	}

	public void sendCN36(int rep) {
		// probably just set register
	}

	public void chgXROM() { }

	java.util.LinkedList<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
	}

	public Wang600_SimulatorJava() {
		Thread t = new Thread(this);
		t.start();
	}

	public void run() {
		// Run the simulator...
	}
}
