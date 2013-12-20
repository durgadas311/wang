// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_PaperTapeReader.java,v 1.1 2013/12/20 01:31:11 drmiller Exp $

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

class Wang_PaperTapeReader
		implements Wang_InputDevice, Runnable
{
	final String ident = "$Id: Wang_PaperTapeReader.java,v 1.1 2013/12/20 01:31:11 drmiller Exp $";

	public static final String Model = "03";
	public static final String Description = "Paper Tape Reader";

	// Group 2 00 00 = skip non-num, tread numeric until non-numeric, GO
	// Group 2 00 07 = skip until CR, GO

	public void reset() {
		_input = false;
	}

	public void run() {
	}

	public boolean start_cn36(int iob, int c) {
		if (iob != 4) return false;
		if (c == 0x00) {
			_input = true;
			// arrange to read number and send GO
			start();
			return true;
		}
		if (c == 0x07) {
			_input = false; // do not send codes to Calculator...
			// arrange to skip to CR and send GO
			start();
			return true;
		}
		return false;
	}

	public void pick_file(JMenuItem m) {
		tape_close();
		SuffFileChooser ch = new SuffFileChooser(_mountLabel,
			_fileType, _pickLabel, Wang_UI.getDir());
		if (_file != null) {
			ch.setSelectedFile(_file);
		}
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			_file = ch.getSelectedFile();
			m.setText(getName() + " - " + _file.getName());
		} else {
			_file = null;
			m.setText(getName() + " - not mounted");
		}
		try { // if this fails, oh well.
			Wang_UI.getProperties().setAndSaveProperty(
				Wang_UI.getProperties().getClass().newInstance(),
				_file_prop,
				_file == null ? "" : _file.getName());
		} catch(Exception ee) {}
		tape_open();
	}

	public void do_ack(int iob) {
		// should be ACK for previous code... should enable next...
		// TODO
		// check mode of operation and send next code...
	}

	public void do_dev(int iob, int b) {
		// right now only ACK happens
		// TODO
	}

	public int getGLRN() { return 0; }

	public void showAbout() {
		java.net.URL url = this.getClass().getResource("icons/wang603.png");
		JLabel lab = new JLabel("<HTML><CENTER>"+
			"Wang " + getName() + " Emulation<BR>"+
			"$Revision: 1.1 $ $Date: 2013/12/20 01:31:11 $<BR>"+
			"<BR>"+
			"<IMG SRC=\""+url.toString()+"\">"+
			"<BR>"+
			"Developed by Douglas Miller<BR>"+
			"http://wang600.durgadas.com<BR>"+
			"</CENTER></HTML>");
		JOptionPane.showMessageDialog(null, lab,
			"About: Wang " + getModel() + " Emulation", JOptionPane.PLAIN_MESSAGE);
	}

	boolean _input;

//	private void sendACK() {
//		Wang_UI.getCore().ackIO(4);
//	}

	private void sendCode(byte b) {
		if (!_input) return;
		Wang_UI.getCore().replyIO(4, (b & 0x0ff));
	}

	public void actionPerformed(ActionEvent e) {
// This goes somewhere else...
//				// might have to notify Simulator?
//				if (!_input) return;
//				Wang_UI.getCore().replyIO(5, GO);
//				return;
		if (e.getSource() instanceof JMenuItem) {
			JMenuItem m = (JMenuItem)e.getSource();
			if (m.getMnemonic() == KeyEvent.VK_U) {
				//setup();
				return;
			}
			if (m.getMnemonic() == KeyEvent.VK_T) {
				//return; fall through and perform base class too...
			}
			if (m.getMnemonic() == KeyEvent.VK_B) { 
				showAbout();
				return;
			}
		}
		super.actionPerformed(e);
	}

	public JMenuItem getMenu(int key) {
		String status = "not mounted";
		if (_file != null) {
			status = _file.getName();
		}
		return new JMenuItem(getName() + " - " + status, key);
	}

	public Wang_PaperTapeReader() {
		super(Wang_UI.getSeries() + Model, Description);

		_input = false;
		_mountLabel = "Mount Tape";
		_pickLabel = "Wang Data files";
		_fileType = "wdf";
		_file = Wang_UI.getProperties().getFile("wang600_603_image",
			true, Wang_UI.getDir());
		if (_file != null) {
			tape_open();
		}

		Wang_UI.registerCN36(this);
	}

	static public String getModel() {
		return Wang_UI.getSeries() + Model;
	}

	static public String getName() {
		return getModel() + " " + Description;
	}
}
