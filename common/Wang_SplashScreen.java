// Copyright (c) 2014 Douglas Miller
// $Id: Wang_SplashScreen.java,v 1.1 2014/01/17 16:09:04 drmiller Exp $

import java.awt.*;

class Wang_SplashScreen {

	static private java.awt.SplashScreen splash = null;

	static public void starting() {
		splash = java.awt.SplashScreen.getSplashScreen();
		// might paint "Starting..." over the top...
	}

	static public void finished() {
		if (splash != null && splash.isVisible()) splash.close();
	}
}
