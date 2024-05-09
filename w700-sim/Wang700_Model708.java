class Wang700_Model708 implements Wang_BlockIODevice  {
	public static final String Model = "08";
	public static final String Description = "External Memory";

	private int _cmd;
	private int _adr;
	private boolean _wr;
	private int _len;
	private int _idx;
	byte[][] _ram;
	byte[] _buf;

	class WangExtMem {
		byte[] ram;
		LED led;

		public WangExtMem(int id, int size) {
		}
	}

	public JMenuItem getMenu(int key) {
		String status = "not installed";
		return new JMenuItem("708 Ext Memory - " + status, key);
	}

	public Wang700_Model708() {
		reset();
		_buf = new byte[256]; // largest transfer
		_ram = new byte[4096]; // larget config per unit
		_unit = 0; // TBD upper address select
	}

	public void do_ack(int iob) {
		// only respond to ACK if in a command already
		// and don't respond to an ACK of an ACK
		if (_cmd > 4 && !_wr) {
			do_dev(iob, 0);
		}
	}

	private void setSelect(boolean sel, int kb) {
		if (!sel) {
			// turn off LED...
			// _cur.led.set(false);
			// _cur = null;
			return;
		}
		// TODO: if sel...
		Wang_UI.getCore().replyIO(0, GO);
	}

	public void do_dev(int iob, int c) {
		int res;
		++_cmd;
//System.err.println("dev 2 ["+_cmd+"] "+b[0]);
		if ((iob & 0b100) != 0) { // GROUP 1 or GROUP 2
			setSelect(((iob & 0b001) != 0 && (c & 0xf0) == 0), c & 0x0f);
			return;
		}
		boolean dat = ((iob & 1) != 0);
		if (_cmd <= 4 && dat || _cmd > 4 && !dat) {
System.err.println("sync error");
			return;
		}
//try{
// Thread.currentThread().sleep(50);
//}
//catch(InterruptedException ie){
//}
		if (_cmd < 4) {
			_adr <<= 8;
			_adr |= (c & 0x00ff);
			Wang700.Core.ackIO(iob);
		} else if (_cmd == 4) {
			_wr = ((c & 0x80) != 0);
			_len = (c & 0x7f);
			if (_len == 0) {
				_len = 64;
			} else if (_len > 1) {
				_len <<= 2;
			}
//System.err.println("command "+_adr+" "+_wr+" "+_len);
			// TODO: compute address and copy _ram[*] to _buf[]
			_idx = 0;
			if (!_wr) {
				// TODO: ...
				Wang700.Core.replyIO(iob, res);
//System.err.println("rd result "+res+" ("+_len+")");
			} else {
				// TODO: ...
				Wang700.Core.ackIO(iob);
			}
		} else {
			if (_idx < _len) {
				if (_wr) {
					_buf[_idx] = (byte)c;
					Wang700.Core.ackIO(iob);
				} else {
					Wang700.Core.replyIO(iob, _buf[_idx]);
				}
			} else {
				if (_wr) {
					// TODO: ... copy _buf to _ram
					Wang700.Core.replyIO(iob, res);
				} else {
					Wang700.Core.ackIO(iob);
				}
				_cmd = 0;
//System.err.println("result "+res+" ("+_idx+")");
			}
			++_idx;
		}
//System.err.printf("got %02x%02x put %04x\n", b[1], b[0], bb);
	}

	public void reset() {
//System.err.println("clear ("+_len+")");
		_cmd = 0;
		_adr = 0;
		_len = 0;
		_wr = false;
		// cancel anything...
	}
}
