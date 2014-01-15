// Copyright (c) 2011,2014 Douglas Miller
// $Id: w600_edit.java,v 1.6 2014/01/15 23:20:04 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

public class w600_edit
{
	final String ident = "$Id: w600_edit.java,v 1.6 2014/01/15 23:20:04 drmiller Exp $";

	public static void main(String[] args) {
		// only needed if we use runCommand()...
		// Wang_UI.Initialize();

		java.net.URL url = w600_edit.class.getResource("icons/WangX14Edit.png");
		Image img = Toolkit.getDefaultToolkit().getImage(url);
		Wang_UI.setIcon(new ImageIcon(img));

		Wang_UI.setProperties(new Wang600_Properties());
		//Wang_UI.setIcon(new ImageIcon(img));
		Wang_UI.setDir(Wang_UI.getProperties().getProperty("wang600_home"));
		Wang_UI.setSeries("6");

		JFrame frame = new JFrame("Wang 600-Series Card Editor");
		frame.setLayout(new FlowLayout());
		frame.setIconImage(img);

		Wang_MarkSenseCard card;
		Wang_InstructionDecoder deco = new Wang600_InstrDecoder();
		if (args.length > 0) {
			card = new Wang_MarkSenseCard(deco, args[0]);
		} else {
			card = new Wang_MarkSenseCard(deco, null);
		}
		frame.addKeyListener(card);

		JScrollPane scroll = new JScrollPane(card);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(new Dimension(512,800));
		frame.add(scroll);

		Wang614_Help help = new Wang614_Help();

		JMenuBar mb = new JMenuBar();
		mb.add(card.getMenu());
		// ...
		// Help goes last...
		mb.add(help.getMenu());

		frame.setJMenuBar(mb);
		frame.getContentPane().setBackground(Color.black);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
}

class Wang614_Help extends JComponent
	implements ActionListener, WindowListener, ComponentListener, HyperlinkListener
{
	static final long serialVersionUID = 311857692031L;
	private JFrame _frame;
	private JEditorPane _text;
	private JScrollPane _scroll;
	private int _xoff, _yoff;
	private JMenuItem _help;
	private JMenuItem _about;
	private boolean _help_on;
	private JMenu _menu;

	public JMenu getMenu() { return _menu; }

	public Wang614_Help() {
		_help = new JMenuItem("Show Help", KeyEvent.VK_H);;
		_help.addActionListener(this);
		_about = new JMenuItem("About", KeyEvent.VK_A);
		_about.addActionListener(this);
		_help_on = false;
		_menu = new JMenu("Help");
		_menu.add(_help);
		_menu.add(_about);

		java.net.URL url = this.getClass().getResource("docs/wang614.html");
		_frame = new JFrame("Wang 614 Editor Help");
		_frame.setLayout(new FlowLayout());
		try {
			_text = new JEditorPane(url);
		} catch (Exception ee) {
			Wang_UI.fatal("Help Setup", ee.getMessage());
		}
		_text.setEditable(false);
		_text.setFont(new Font("Sans-serif", Font.PLAIN, 12));
		int z = _text.getFont().getSize();
		// for some reason, this randomly messes up scroll size...
		//_text.setPreferredSize(new Dimension(60 * z, 32 * z));
		_text.addHyperlinkListener(this);

		_scroll = new JScrollPane(_text);
		_scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		_scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		_scroll.setPreferredSize(new Dimension(60 * z, 32 * z));

		JMenuBar mb = new JMenuBar();
		JMenu mu;
		mu = new JMenu("Topic");
		mb.add(mu);
		JMenuItem mi;
		mi = new JMenuItem("Basic Operation", KeyEvent.VK_B);
		mi.addActionListener(this);
		mu.add(mi);
//		mi = new JMenuItem("Using the Calculator", KeyEvent.VK_U);
//		mi.addActionListener(this);
//		mu.add(mi);
//		mi = new JMenuItem("Sample Programs", KeyEvent.VK_A);
//		mi.addActionListener(this);
//		mu.add(mi);
//		mi = new JMenuItem("Using the Tape Drive", KeyEvent.VK_D);
//		mi.addActionListener(this);
//		mu.add(mi);
//		mi = new JMenuItem("How to Program", KeyEvent.VK_P);
//		mi.addActionListener(this);
//		mu.add(mi);
//		mi = new JMenuItem("Programming Techniques", KeyEvent.VK_T);
//		mi.addActionListener(this);
//		mu.add(mi);

		_frame.setJMenuBar(mb);
		_frame.add(_scroll);
		_frame.pack();

		_frame.addWindowListener(this);
		_frame.addComponentListener(this);

		Dimension fdim = _frame.getSize();
		Dimension sdim = _scroll.getSize();
		_xoff = fdim.width - sdim.width;
		_yoff = fdim.height - sdim.height;
	}

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang614.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang 614 Mark Sense Card Editor<BR>"+
			"$Revision: 1.6 $ $Date: 2014/01/15 23:20:04 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang 614 Editor", JOptionPane.PLAIN_MESSAGE);
	}

	public void toggle() {
		setOn(!_help_on);
	}

	private void setOn(boolean on) {
		_help_on = on;
		if (on) {
			_frame.pack();
			_help.setText("Hide Help");
		} else {
			_help.setText("Show Help");
		}
		_frame.setVisible(on);
	}

	public void windowActivated(WindowEvent e) { }
	public void windowClosed(WindowEvent e) { }
	public void windowIconified(WindowEvent e) { }
	public void windowOpened(WindowEvent e) { }
	public void windowDeiconified(WindowEvent e) { }
	public void windowDeactivated(WindowEvent e) { }

	public void windowClosing(WindowEvent e) {
		if (e.getWindow() == _frame) {
			setOn(false);
			return;
		}
	}

	public void componentHidden(ComponentEvent e) { }
	public void componentMoved(ComponentEvent e) { }
	public void componentShown(ComponentEvent e) { }

	public void componentResized(ComponentEvent e) {
		if (e.getComponent() == _frame) {
			Dimension fdim = _frame.getSize();
			_scroll.setSize(fdim.width - _xoff, fdim.height - _yoff);
			_scroll.setPreferredSize(_scroll.getSize());
			_frame.setSize(fdim.width, fdim.height); // redundant?
			_frame.setPreferredSize(_frame.getSize());
			return;
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			java.net.URL url = null;
			// should use a table to lookup url?
			if (m.getMnemonic() == KeyEvent.VK_H) {
				toggle();
			} else if (m.getMnemonic() == KeyEvent.VK_A) {
				showAbout();
			} else if (m.getMnemonic() == KeyEvent.VK_B) {
				url = this.getClass().getResource("docs/wang614.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_U) {
//				url = this.getClass().getResource("docs/wang600calc.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_D) {
//				url = this.getClass().getResource("docs/wang600tape.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_A) {
//				url = this.getClass().getResource("docs/wang600samp.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_P) {
//				url = this.getClass().getResource("docs/wang600prog.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_F) {
//				url = this.getClass().getResource("docs/wang600func.html");
//			} else if (m.getMnemonic() == KeyEvent.VK_T) {
//				url = this.getClass().getResource("docs/wang600tech.html");
			} else {
				System.err.println("help menu " + e.getActionCommand() +
						" not implemented yet");
				return;
			}
			try {
				_text.setPage(url);
			} catch (IOException ee) {
			}
			return;
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent r) {
		if (r.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			if (r.getURL().getProtocol().compareTo("file") == 0 ||
			    r.getURL().getProtocol().compareTo("jar") == 0) {
				String doc = r.getURL().getFile();
				if (r.getURL().getProtocol().compareTo("jar") == 0) {
					// ugh! must be a better way...
					doc = doc.replaceFirst("/wang600\\.jar!/","/");
					doc = doc.replaceFirst("file:","");
				}
				try {
					Desktop.getDesktop().open(new File(doc));
				} catch (IOException e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				} catch(Exception e) {
					System.err.println("Exception "+e.getMessage()+" trying to open file "+
						r.getURL().getProtocol()+" "+r.getURL().getFile());
				}
			} else {
				try {
					Desktop.getDesktop().browse(r.getURL().toURI());
				} catch (IOException e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				} catch(Exception e) {
					System.err.println("Exception trying to follow link "+
						r.getURL().toString());
				}
			}
		}
	}
}
