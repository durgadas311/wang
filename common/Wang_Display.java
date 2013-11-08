import javax.swing.*;

abstract class Wang_Display extends JComponent
{
	static final long serialVersionUID = 31100000003L;

	public abstract void copy(); // Copy (primary) display to clipboard

	public abstract void setProperties(Wang_Properties prop); // Inform display of properties changes
	public abstract void setErr(byte on); // e.g. Mach Error

	public abstract void setOv(byte on); // e.g. Prog Error

	public abstract void do_blanking(); // Blank display - i.e. when refreshing stops

	public abstract void do_display(byte[] b); // Update display contents

	public abstract Wang_ErrorLight getOv();
	public abstract Wang_ErrorLight getErr();
}
