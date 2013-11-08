import javax.swing.*;

interface Wang_Printer
{
	public void onOff(boolean on); // i.e. Visible/Not

	public JFrame getFrame(); // Access visible window

	public void feed(); // advance one (blank) line

	public void do_printer(int col, int drm); // print char 'drm' at column 'col',
	public void do_line(); // advance (and print) line
}
