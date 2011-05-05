import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class w600_fe {
	public static void main(String[] args) {
		JFrame front_end = new JFrame("Wang 600 Keyboard");
		front_end.add(new Wang600_Keyboard());
		front_end.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		front_end.setSize(1000,300);
		front_end.setVisible(true);
	}
}

class Wang600_Keyboard extends JComponent
	implements ActionListener
{

	JButton prime;
	JButton rad_deg;
	JButton shift;
	JButton sin;
	JButton tan;
	JButton logex;
	JButton x2;
	JButton cos;
	JButton inv;
	JButton ex;
	JButton sqrt;
	JButton t_l;
	JButton q_l;
	JButton m_l;
	JButton s_l;
	JButton d_l;
	JButton a_l;
	JButton r_l;
	//
	JButton t_r;
	JButton q_r;
	JButton m_r;
	JButton s_r;
	JButton d_r;
	JButton a_r;
	JButton r_r;

	static final long serialVersionUID = 31145769203L;

	GridBagLayout gridbag = new GridBagLayout();

	private JButton addButton(GridBagConstraints c,
				String text, int height) {
		final Insets inset = new Insets(2,2,2,2);
		final Dimension dim1 = new Dimension(50, 50);
		final Dimension dim2 = new Dimension(50, 100);

		c.gridheight = height;
		JButton butt = new JButton(text);
		if (height == 1) {
			butt.setPreferredSize(dim1);
		} else {
			butt.setPreferredSize(dim2);
		}
		butt.setMargin(inset);
		gridbag.setConstraints(butt, c);
		add(butt);
		butt.addActionListener(this);
		c.gridy += height;
		if (c.gridy >= 4) {
			c.gridy = 0;
			++c.gridx;
		}
		return butt;
	}

	public Wang600_Keyboard() {
		GridBagConstraints c = new GridBagConstraints();

		setLayout(gridbag);

		//c.fill = GridBagConstraints.BOTH;
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;

		prime = addButton(c, "PRIME", 1);
		rad_deg = addButton(c, "RAD-DEG", 1);
		shift = addButton(c, "SHIFT", 2);

		sin = addButton(c, "SIN", 1);
		tan = addButton(c, "TAN", 1);
		logex = addButton(c, "LOGeX", 1);
		x2 = addButton(c, "X2", 1);

		cos = addButton(c, "COS", 1);
		inv = addButton(c, "1/X", 1);
		ex = addButton(c, "eX", 1);
		sqrt = addButton(c, "vX", 1);

		//++c.gridx;
		t_l = addButton(c, "TOTAL", 1);
		q_l = addButton(c, "/=", 1);
		m_l = addButton(c, "*=", 1);
		s_l = addButton(c, "STORE", 1);

		d_l = addButton(c, "-", 1);
		a_l = addButton(c, "+", 2);
		r_l = addButton(c, "RECALL", 1);

		// number keys...

		d_r = addButton(c, "-", 1);
		a_r = addButton(c, "+", 2);
		r_r = addButton(c, "RECALL", 1);

		t_r = addButton(c, "TOTAL", 1);
		q_r = addButton(c, "/=", 1);
		m_r = addButton(c, "*=", 1);
		s_r = addButton(c, "STORE", 1);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == prime) {
			System.out.println("PRIME!");
		} else if (e.getSource() == rad_deg) {
			System.out.println("RAD-DEG!");
		} else if (e.getSource() == shift) {
			System.out.println("SHIFT!");
		}
	}

}
