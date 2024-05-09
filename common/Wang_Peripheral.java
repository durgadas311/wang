// Copyright (c) 2011,2026 Douglas Miller

import java.awt.*;
import javax.swing.*;


interface Wang_Peripheral
{
	// General-purpose device reset
	void reset();

	// Return the text-frame of the device, for handling events
	// and setting up action listeners
	JFrame getFrame();		// might be null
	Component getComponent();	// might be null

	// Set the visibility of the output frame
	void onOff(boolean on);

	// Return current visibility of the output frame
	boolean onOff();

	public void setProperties(Wang_Properties p);

	// returns descriptive name of device
	// static String getName();
	// needs to be static, but "java to the rescue" again...
}
