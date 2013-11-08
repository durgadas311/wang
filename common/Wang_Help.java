import javax.swing.*;

interface Wang_Help
{
	public JMenuItem getMenuItemHelp(); // Get "Help" menu item
	public JMenuItem getMenuItemAbout(); // get "About" menu item
	public void showAbout(); // pop-up "About" window
	public void toggle(); // toggle "Help" window visible/not
}
