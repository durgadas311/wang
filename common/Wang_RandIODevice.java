// Copyright (c) 2011,2024 Douglas Miller

import javax.swing.*;

// See the Wang_GroupIODevice interface and "I/O" codes.
//
// do_dev() is called by the calculator to implement the bidirectional
// protocols. It serves as either an acknowledge or the data/command,
// depending on the phase of the protocol. do_dev() and
// Wang_UI.getCore().replyIO() form a matched pair for each byte
// exchanged.

interface Wang_RandIODevice extends Wang_GroupIODevice {
	JMenuItem getMenu(int key); // get/create menu item
	void pickFile(JMenuItem m); // install new (disk) image

	// Process output byte in context of Wang Group I/O Device.
	// These are for the random I/O device protocol, as used
	// by the "I/O" commands.
	void do_dev(int iob, int c);
}
