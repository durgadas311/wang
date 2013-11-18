import javax.swing.*;

interface Wang_XROM {
	public JMenuItem getMenu(int key); // get/create menu that responds to 'key'

	public void Initialize(); // Load initial contents, if any

	public byte getByte(int adr); // Get byte from ROM at location 'adr'

	public void pickFile(JMenuItem m); // Install new ROM image

	public void setXROM(byte[] img);
	public int getSize(); // bytes
}
