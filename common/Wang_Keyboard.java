import java.awt.event.*;
import javax.swing.*;

abstract class Wang_Keyboard extends JComponent
			implements KeyListener
{
	static final long serialVersionUID = 31100000004L;
	public abstract int getMode0(); // mode bits, 0-3
	public abstract int getMode1(); // mode bits, 0-3
}
