// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_MarkSenseCard.java,v 1.12 2014/01/14 21:53:51 drmiller Exp $

import java.awt.*;
import javax.swing.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.awt.event.*;
import java.util.Arrays;

import java.awt.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

class Wang_MarkSenseCard extends JLabel
		implements MouseListener, KeyListener, ActionListener, Printable, java.awt.image.ImageObserver {
	static final long serialVersionUID = 311614000000L;

	Font font1 = new Font("Sans-serif", Font.PLAIN, 18);
	Font font2 = new Font("Sans-serif", Font.PLAIN, 11);
	ImageIcon _image;

	byte[] _code;
	byte[] _skips;
	int _code_used;
	String _title;
	File _file;
	int _pgix;
	int _npg;
	boolean _changed;
	java.text.SimpleDateFormat _timestamp =
		new java.text.SimpleDateFormat("MMM" + "\u2003 " + "d" + "\u2003\u2003" + "y");
	JMenu _menu;
	String _date;
	Rectangle _top, _bottom;

	public JMenu getMenu() { return _menu; }

	double _bit_spacing = 38.4;
	double _bit_start = 168.0; // not including SKIP
	double _row_spacing = 28.8;
	double _row_start = 48.0;
	int _bit_width = 20;
	int _bit_height = 10;
	int _rows_per_card = 40;

	private Wang_InstructionDecoder _decoder;

	public void paint(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		super.paint(g2d);
		g2d.setColor(Color.black);
		g2d.setFont(font2);
		int s;
		for (s = 0; s < _rows_per_card; ++s) {
			int cx = _pgix * _rows_per_card + s;
			byte c = 0;
			if (cx < _code_used) c = _code[cx];
			double ry = s * _row_spacing + _row_start;
			String step = String.format("%03d", cx / 10);
			g2d.drawString(step, 90, (int)Math.round(ry + 8));
			if (_skips[cx] != 0) {
				g2d.fillRect((int)Math.round(_bit_start - _bit_spacing),
					(int)Math.round(ry), _bit_width, _bit_height);
			}
			int b;
			for (b = 0; b < 8; ++b) {
				double rx = (b * _bit_spacing) + _bit_start;
				boolean m = ((c & 0x80) != 0);
				c <<= 1;
				if (m) {
					g2d.fillRect((int)Math.round(rx),
						(int)Math.round(ry),
						_bit_width, _bit_height);
				}
			}
		}
		g2d.setFont(font1);
		for (s = 0; s < _rows_per_card; ++s) {
			double ry = s * _row_spacing + _row_start + 14.0;
			int cx = _pgix * _rows_per_card + s;
			if (cx >= _code_used) break;
			byte c = _code[cx];
			int h = (c >> 4) & 0x0f;
			int l = (c & 0x0f);
			String t = _decoder.decode(h, l);
			g2d.drawString(t, 30, (int)Math.round(ry));
		}

		AffineTransform orig = g2d.getTransform();
		g2d.rotate(-Math.PI/2);
		g2d.drawString(_title, -1100, 20);
		g2d.drawString(Integer.toString(_pgix + 1), -550, 20);
		g2d.drawString(Integer.toString(_npg), -460, 20);
		g2d.drawString(_date, -400, 20);
		g2d.setTransform(orig);
	}

	public Wang_MarkSenseCard(Wang_InstructionDecoder deco, String pgm) {
		super();

		_decoder = deco;

		_image = new ImageIcon(getClass().getResource("icons/Wang_MarkSenseCard.png"));
		setIcon(_image);
		setBackground(Color.black);
		setOpaque(true);
		setPreferredSize(new Dimension(getIcon().getIconWidth(), getIcon().getIconHeight()));
		_top = new Rectangle(0, 0, 10, 10);
		_bottom = new Rectangle(0, getIcon().getIconHeight() - 10, 10, 10);
		addMouseListener(this);

		_code = new byte[2048];
		_skips = new byte[2048];

		JMenu mu;
		JMenuItem mi;
		mu = new JMenu("File");
		mi = new JMenuItem("New", KeyEvent.VK_N);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Open", KeyEvent.VK_O);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Save", KeyEvent.VK_S);
		mi.addActionListener(this);
		mu.add(mi);
		mi = new JMenuItem("Print", KeyEvent.VK_P);
		mi.addActionListener(this); 
		mu.add(mi);
		mi = new JMenuItem("Quit", KeyEvent.VK_Q);
		mi.addActionListener(this);
		mu.add(mi);
		_menu = mu;

		if (pgm == null) {
			newFile();
		} else {
			setupFile(new File(Wang_UI.getDir() + "/" + pgm));
		}
	}

	private void newFile() {
		_title = "untitled";
		_file = null;
		_code_used = 0;
		Arrays.fill(_skips, (byte)1);
		_pgix = 0;
		_npg = 1;
		_changed = false;
		_date = _timestamp.format(new java.util.Date());
	}

	private File pickFile(String purpose) {
		File file;
		SuffFileChooser ch = new SuffFileChooser(purpose,
			Wang_UI.getDir());
		int rv = ch.showDialog(this);
		if (rv == JFileChooser.APPROVE_OPTION) {
			file = ch.getSelectedFile();
		} else {
			file = null;
		}
		return file;
	}

	private void setupFile(File file) {
		if (file == null) {
			// change nothing in this case...
			return;
		}
		_title = file.getName();
		_file = file;
		if (file.exists()) {
			_date = _timestamp.format(file.lastModified());
			FileInputStream f;
			try {
				f = new FileInputStream(file);
				_code_used = f.read(_code);
			} catch (Exception ee) {
			}
			if (_code_used >= 2 &&
					(_code[_code_used - 2] & 0x0ff) == 0x9e) {
				if ((_code[_code_used - 1] & 0x0ff) == 0x9e) {
					_code_used -= 1;
				} else {
					_code_used -= 2;
				}
			}
			for (int x = 0; x < _skips.length; ++x) {
				_skips[x] = (byte)(x >= _code_used ? 1 : 0);
			}
			_changed = false;
			_pgix = 0;
			_npg = (_code_used + _rows_per_card - 1) / _rows_per_card;
			// or, always have +1 cards?
			if (_npg == 0) _npg = 1;
		} else {
			_code_used = 0;
			Arrays.fill(_skips, (byte)1);
			_pgix = 0;
			_npg = 1;
			_changed = false;
			_date = _timestamp.format(new java.util.Date());
		}
	}

	private void saveFile() {
		FileOutputStream f = null;
		try {
			f = new FileOutputStream(_file);
		} catch (Exception ee) {
		}
		if (f == null) {
			return;
		}
		_date = _timestamp.format(new java.util.Date());
		// need to restore "EOF" marker...
		int saved = _code_used;
		if (saved >= 1 && (_code[saved - 1] & 0x0ff) == 0x9e) {
			_code[saved++] = (byte)0x9e;
		} else {
			_code[saved++] = (byte)0x9e;
			_code[saved++] = (byte)0xff;
		}
		try {
			f.write(_code, 0, saved);
		} catch (Exception ee) {
		}
	}

	public void keyTyped(KeyEvent e) { }

	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			if (_pgix * _rows_per_card == _code_used && _npg > 1) {
				--_npg;
			}
			--_pgix;
			if (_pgix < 0) {
				_pgix = 0;
			}
			repaint();
		} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			++_pgix;
			if (_pgix >= _npg) {
				if (_pgix * _rows_per_card == _code_used) {
					++_npg;
				} else {
					_pgix = _npg - 1;
				}
			}
			// allow for adding new page???
			repaint();
		} else if (e.getKeyCode() == KeyEvent.VK_UP) {
			scrollRectToVisible(_top);
		} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
			scrollRectToVisible(_bottom);
		}
	}

	public void keyReleased(KeyEvent e) { }

	public void mouseClicked(MouseEvent e) {
		double x = e.getX();
		double y = e.getY();
		y = (y - _row_start) / _row_spacing;
		x = (x - _bit_start) / _bit_spacing;
		boolean iny = ((y - Math.floor(y)) * _row_spacing <= _bit_height);
		boolean inx = ((x - Math.floor(x)) * _bit_spacing <= _bit_width);
		if (inx && iny && y >= 0 && y < _rows_per_card) {
			int cx = _pgix * _rows_per_card + (int)y;
			if (x >= 0 && x < 8) {
				// click data bit box
				int bit = 0x80 >> (int)x;
				if (cx >= _code_used) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						// extend to new mark...
						while (_code_used <= cx) {
							_skips[_code_used++] = 0;
						}
					} else { 
						_code_used = cx + 1;
					}
				}
				_skips[cx] = 0;
				_code[cx] ^= bit;
				_changed = true;
				repaint();
//System.err.println("step " + Math.floor(y) + " bit " + Math.floor(x));
			} else if (Math.floor(x) == -1.0) {
				// click SKIP box
				if (cx < _code_used) {
					if (e.getButton() == MouseEvent.BUTTON3) {
						// shrink to new mark...
						while (cx < _code_used) {
							_skips[_code_used--] = 1;
						}
						_skips[cx] = 1;
					} else {
						_skips[cx] ^= 1;
					}
				} else if (cx >= _code_used) {
					// assumes SKIP is current set...
					if (e.getButton() == MouseEvent.BUTTON3) {
						// extend to new mark...
						while (_code_used <= cx) {
							_skips[_code_used++] = 0;
						}
					} else {
						_skips[cx] = 0;
						_code_used = cx + 1;
					}
				}
				repaint();
				_changed = true;
			} else {
				// corner cases?
System.err.println("step " + Math.floor(y) + " bit " + Math.floor(x));
			}
		}
	}
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) { }

	private boolean confirmChanges(String op) {
		if (_code_used > 0 && _changed) {
			int res = Wang_UI.confirm(op, "Changes have not been saved. " +
							"Discard changes?");
			if (res == JOptionPane.YES_OPTION) {
				return true;
			}
			return false;
		}
		return true;
	}

	public void actionPerformed(ActionEvent e) {
		if (!(e.getSource() instanceof JMenuItem)) {
			return;
		}
		JMenuItem m = (JMenuItem)e.getSource();
		if (m.getMnemonic() == KeyEvent.VK_N) {
			if (!confirmChanges("New File")) {
				return;
			}
			newFile();
			repaint();
			return;
		} else if (m.getMnemonic() == KeyEvent.VK_O) {
			if (!confirmChanges("Open File")) {
				return;
			}
			setupFile(pickFile("Load Card Deck"));
			repaint();
			return;
		} else if (m.getMnemonic() == KeyEvent.VK_S) {
			if (_file == null) {
				File nu = pickFile("Save Card Deck As");
				if (nu == null) return;
				_file = nu;
				_title = _file.getName();
			}
			saveFile();
			_changed = false;
			repaint();
			return;
		} else if (m.getMnemonic() == KeyEvent.VK_P) {
			PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
			aset.add(OrientationRequested.LANDSCAPE);
			aset.add(new javax.print.attribute.standard.MediaPrintableArea(
				(float)0.5, (float)0.5, (float)7.5, (float)10.0, MediaPrintableArea.INCH));
			PrinterJob pj = PrinterJob.getPrinterJob();
			pj.setPrintable(this);
			boolean print = pj.printDialog(aset);
			if (print) {
				try {
					pj.print(aset);
				} catch (PrinterException ee) {
					System.out.println("print failed");
				}
			}
			return;
		} else if (m.getMnemonic() == KeyEvent.VK_Q) {
			if (!confirmChanges("Quit")) {
				return;
			}
			System.exit(0);
			return;
		}
	}

	public int print(Graphics g, PageFormat pf, int pageIndex) {
		double x0 = pf.getImageableX();
		double y0 = pf.getImageableY();
		double w0 = pf.getImageableWidth();
		double h0 = pf.getImageableHeight();
		Graphics2D g2d = (Graphics2D)g;
		g2d.translate(x0, y0);
		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, (int)w0, (int)h0);
		g2d.setColor(Color.black);

		// determine scaling to fit cards along long edge of landscape page.
		
		double oh0 = getIcon().getIconHeight();
		double ow0 = getIcon().getIconWidth();
		double gs = h0 / oh0;
		g2d.scale(gs, gs);

		int ncards = (int)Math.round((w0 / gs) / ow0);
		int fcard = pageIndex * ncards;
		if (fcard >= _npg) {
			return Printable.NO_SUCH_PAGE;
		}
		int saveIx = _pgix;
		int lcard = fcard + ncards;
		int gap = (int)Math.floor(((w0 / gs) - (ncards * ow0)) / (ncards - 1));
		gap += (int)ow0;

		int pos = 0;
		for (int ccard = fcard; ccard < _npg && ccard < lcard; ++ccard) {
			AffineTransform orig = g2d.getTransform();
			g2d.translate(pos, 0);
			_pgix = ccard;
			paint(g2d);
			g2d.setTransform(orig);
			pos += gap;
		}
		_pgix = saveIx;
		return Printable.PAGE_EXISTS;
	}
}
