// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_CN36_Type2.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $

import javax.swing.*;

interface Wang_CN36_Type2 {
	public JMenuItem getMenu(int key); // get/create menu item

	public void pickFile(JMenuItem m); // install new (disk) image

	public void do_dev(int iob, int c);
	public void do_ack(int iob);

	public void reset(); // reset interface
}
