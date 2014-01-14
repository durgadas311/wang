// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Help.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $

import javax.swing.*;

interface Wang_Help
{
	public JMenuItem getMenuItemHelp(); // Get "Help" menu item
	public JMenuItem getMenuItemAbout(); // get "About" menu item
	public void showAbout(); // pop-up "About" window
	public void toggle(); // toggle "Help" window visible/not
}
