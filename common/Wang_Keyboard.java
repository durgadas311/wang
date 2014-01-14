// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Keyboard.java,v 1.5 2014/01/14 21:53:51 drmiller Exp $

import java.awt.event.*;
import javax.swing.*;

abstract class Wang_Keyboard extends JComponent
			implements KeyListener
{
	static final long serialVersionUID = 31100000004L;
	public abstract int getMode0(boolean clear); // mode bits, 0-3, a.k.a D1
	public abstract int getMode1(boolean clear); // mode bits, 0-3, a.k.a D2
}
