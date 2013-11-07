// Copyright (c) 2011,2013 Douglas Miller
// $Id: Wang600_SimulatorJava.java,v 1.2 2013/11/07 21:18:46 drmiller Exp $

// Implements the Wang600 hardware. Does not provide any debug/trace support.

class Wang600_SimulatorJava
	implements Wang600_Core
{
	final String ident = "$Id: Wang600_SimulatorJava.java,v 1.2 2013/11/07 21:18:46 drmiller Exp $";
	// CPU registers.
	// ucode accessible
	byte s;
	byte t;
	byte u;
	byte v;
	byte ca;
	byte cb;
	byte ka;
	byte kb;
	// internal hardware accessible
	byte l;
	byte m;
	byte n;
	byte rb;
	byte gioa;
	byte giob;
	byte iob;
	byte d1;
	byte d2;

	// status flags (1 bit)
	byte zo;
	byte cc;
	byte sc;
	byte kbd;
	byte ov;
	byte err;
	byte glrn;

	// ucode subroutine stack
	int stk1;
	int stk2;

	// simulator (no direct h/w relation)
	int jam;
	int next;
	int pc;
	int cycles;
	int cylimit; // debug, not used

	byte pr_drum;
	int pr_hammers;
	byte pr_tach;
	int pr_col;

	static final int D10_FP = 0x01;
	static final int D11_LST_L_P = 0x02;
	static final int D12_LRN_L_P = 0x04;
	static final int D13_STEP = 0x08;

	static final int D20_DEGREES = 0x01;
	static final int D21_PRT_ON = 0x02;

	byte[] _ram;

	private class Wang600_Ucode {
		public byte jl;
		public byte jh;
		public int jad;
		public byte sub;
		public byte st;
		public byte kk;
		public byte mop;
		public byte bc;
		public byte ac;
		public byte aop;
		public byte zo;
		public byte bi;
		public byte ai;

		public Wang600_Ucode(byte[] instr) {
			// "LE", i.e. "jl" in byte[0]
			jl = (instr[0] >> 2) & 0x07;
			jh = (instr[0] >> 5) & 0x07;
			jad = instr[1] | ((instr[2] & 1) << 8);
			sub = (instr[2] >> 1) & 1;
			st = (instr[2] >> 2) & 0x0f;
			kk = ((instr[2] >> 6) & 0x03) | ((instr[3] & 0x03) << 2);
			mop = (instr[3] >> 2) & 0x0f;
			bc = (instr[3] >> 6) & 1;
			ac = (instr[3] >> 7) & 1;
			aop = (instr[4] & 0x07);
			zo = ((instr[4] >> 3) & 0x07);
			bi = ((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2);
			ai = ((instr[5] >> 1) & 0x07);
		}
	}
	private class Wang600_UcodeRom {
		private byte[] _ucode; // raw ucode from file, 64-bit words
		// right now, the only override is for mem size, so just hardcode
		// all that.

		public Wang600_UcodeRom(File img, int memsize) {
			// Can't change _ucode after initial setup (i.e. while running).
			// Can't run if _ucode is null... need to check
			// (right now, will throw NULL pointer exception when fetching)
			// Enforce fixed-size 2048-word x 64-bit ucode.
			if (_ucode == null && img != null) {
				FileInputStream f;
				try {
					f = new FileInputStream(img);
				} catch (FileNotFoundException ee) {
					return;
				}
				int n = 0;
				byte[] buf = new byte[16384];
				try {
					n = f.read(buf);
				} catch (IOException ee) {
					n = -1;
				}
				try {
					f.close();
				} catch (IOException ee) {
				}
				if (n == 16384) {
					_ucode = buf;

					// patch mem-size override instruction...
					int idx = 0x008 * 8;
					byte kk = (((memsize - 1) >> 8) & 0x0f);
					_ucode[idx + 2] |= ((kk & 0x03) << 6);
					_ucode[idx + 3] |= ((kk >> 2) & 0x03);
				}
			}
		}

		public Wang600_Ucode fetch(int adr) {
			int idx = adr * 8;
			return new Wang600_Ucode(Arrays.copyOfRange(_ucode, idx, idx + 7));
		}
	}

	private Wang600_UcodeRom _rom;

	public void chgMode0() { }	// don't care

	public void chgMode1() { }	// don't care

	public void pressCmd(int cmd) {
	}

	public void sendCN36(int rep) {
		// probably just set register
	}

	public void chgXROM() { }	// don't care

	java.util.LinkedList<Integer> keyCodes;

	public void pressKey(int key) {
		keyCodes.add(key);
	}

	public Wang600_SimulatorJava() {
		// at some point, get these from properties...
		int memsize = 2048; // based on Model (2TP, 6TP, 14TP, ...)
		_rom = new Wang600_UcodeRom(new File("wang600.rom"), memsize);
		_ram = new byte[memsize];

		pr_drum = 0;
		pr_hammers = 0;
		pr_tach = 0;
		pr_col = 0;

		// These need to come from the keyboard setup...
		d1 = 0;
		d2 = D20_DEGREES;

		Thread t = new Thread(this);
		t.start();
	}

	private byte add3_i(byte a, byte b, byte c) {
		byte s = a + b + c;
		zo = ((s & 0x0f) == 0);
		cc = ((s & 0x10) != 0);
		return s & 0x0f;
	}

	private byte sub3_i(byte a, byte b, byte c) {
		byte s = a - b - c;
		zo = ((s & 0x0f) == 0);
		cc = ((s & 0x10) != 0);
		return s & 0x0f;
	}

	private byte and2(byte a, byte b) {
		byte s = a & b;
		zo = ((s & 0x0f) == 0);
		return s & 0x0f;
	}

	private byte or2(byte a, byte b) {
		byte s = a | b;
		zo = ((s & 0x0f) == 0);
		return s & 0x0f;
	}

	private byte xor2(byte a, byte b) {
		byte s = a ^ b;
		zo = ((s & 0x0f) == 0);
		return s & 0x0f;
	}

	private byte add3_c(byte a, byte b, byte c) {
		byte s = add3_i(a, b, c);
		sc = cc;
		return s;
	}

	private byte sub3_c(byte a, byte b, byte c) {
		byte s = sub3_i(a, b, c);
		sc = cc;
		return s;
	}

	private void printer_status() {
		// we don't want to do this unless it is really the
		// drum printer we're looking at... can't tell?
		if ((d2 & D21_PRT_ON) == 0) {
			// only if running program doesn't get here...
			// printer is off, tach will never pulse, so don't spin
			if (pc == 0x6db) {
				// sleep until key event... incl PRT ON...
			}
			return;
		}
		if (pr_tach) {
			pr_col = 0;
			pr_drum = (pr_drum + 1) & 0x0f;
			pr_hammers = 0;
		}
		pr_tach ^= 0x08;
		ka = pr_drum;
		kb = pr_tach;
	}

	private void printer_hammers() {
		int x;
		uint32_t h;

		pr_hammers <<= 1;
		pr_hammers &= 0x0fffff;
		pr_hammers |= kb & 1;
		if (++pr_col >= 20) {
			h = pr_hammers;
			for (x = 0; h; ++x) {
				if (h & 1) {
					Wang600.Prt.do_printer(new byte[]{(byte)x, pr_drum});
				}
				h >>= 1;
			}
			pr_col = 0;
		}
	}

	private void printer_feed() {
		// now, actually print it...
		Wang600.Prt.do_printer(new byte[]{0x01, (byte)0xff});
	}

	private void rd_ram_i(byte ah, byte am, byte al) {
		uint16_t adr = (ah << 8) | (am << 4) | al;
		adr &= ram_mask;
		byte b = _ram[adr >> 1];
		if (adr & 1) {
			b >>= 4;
		} else {
			b &= 0x0f;
		}
		rb = ca = b;
		b = Wang600.XROM.getByte(adr >> 1);
		if (adr & 1) {
			b >>= 4;
		} else {
			b &= 0x0f;
		}
		cb = b;
	}

	private void wr_ram_i(byte ah, byte am, byte al) {
		uint16_t adr = (ah << 8) | (am << 4) | al;
		adr &= ram_mask;
		byte a = ca;
		byte b = _ram[adr >> 1];
		byte c = a;
		byte d;
		if (adr & 1) {
			a <<= 4;
			d = (b >> 4) & 0x0f;
			b &= 0x0f;
		} else {
			d = b & 0x0f;
			b &= 0xf0;
		}
		_ram[adr >> 1] = b | a;
	}

	private void refresh() {
		byte x = (n << 4) | rb;
		if (disp[n] != x) {
			disp[n] = x;
			good = 0;
		}
		if (++lastx >= 16) {
			lastx = 0;
			++good;
			Wang600.Disp.do_display(disp);
		}
	}

	private void display_check() {
		if (pc == 0x252) {
		}
		// 51c: begin display-refresh delay loop... short-cut to 51f...
		if (pc == 0x51c) {	// display refresh routine...
			next = 0x51f;	// update some regs too?
			cycles += 272;
			refresh();
			if (good > 4) {
				// OK to sleep now...
			}
		// 5c0: begin alpha-stop display-refresh delay loop... short-cut to 5c3...
		} else if (pc == 0x5c0) {	// alpha-stop refresh routine...
			next = 0x5c3;
			cycles += 272;
			refresh();
		} else if (pc == 0x5c6) {	// alpha-stop done... "return"...
			if (next == 0x27b) { // alpha-stop in running program...
				// observed 211975 cycles or about 0.53 second
				sys->keyboard(sys, NULL, 530);
			}
			Wang600.Disp.do_blanking();
		}
	}

	private int instr_exec() {
		Wang600_Ucode u = _rom.fetch(pc);
		int nxt;
		int rc = 0;
		static uint16_t key = 0;

		// F==7 && J==0:
		//	PC <= STK1, STK1 <= PC, STK2 <= STK1
		//
		// F==7 && J==1:
		//	PC <= STK1, STK1 <= STK2, STK2 <= STK1
		//
		// F!=7 && J==0:
		//	PC <= NEXT**
		//
		// F!=7 && J==1:
		//	STK2 = STK1, STK1 <= PC, PC <= NEXT**
		//
		// For conditional jump/call, these bits are latched early...
		byte br_acc = s;
		byte br_c = sc;
		byte br_k = u.kk;
		int opf7 = (u.jl == 7);
		if (opf7) {
			nxt = stk1 | 1;
			if (u.sub) {
				stk1 = stk2;
			} else {
				stk1 = stk2; // bugfix?
				//stk1 = pc;	// bad?
				// rc = 1;
			}
		} else {
			nxt = u.jad << 2;
		}

		if (u.mop >= 1 && u.mop <= 6) {
			l = t;
			m = u;
			n = v;
		}

		byte g = 0, h = 0;
		switch(u.ai) {
		case 0: h = s; break;
		case 1: h = t; break;
		case 2: h = u; break;
		case 3: h = v; break;
		case 4: h = ka; break;
		case 5: h = kb; break;
		case 6: h = ca; break;
		case 7: h = cb; break;
		}

		switch(u.bi) {
		case 0: g = 0; break;
		case 1: g = br_k; break;
		case 2:
			g = d1;
			d1 &= ~D13_STEP;
			// clear 0010 if glrn?
			g |= ((glrn & 1) << 2);
			break;
		case 3: g = d2; break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		if (!u.ac) h = 0; // "15"? "0"? ???
		switch (u.aop) {
		case 0:
			if (u.bc) alu = sub3_i(sys, h, g, 0);
			else alu = add3_i(sys, h, g, 0);
			break;
		case 1:
			if (u.bc) alu = sub3_i(sys, h, g, 1);
			else alu = add3_i(sys, h, g, 1);
			break;
		case 2:
			if (u.bc) alu = sub3_c(sys, h, g, 0);
			else alu = add3_c(sys, h, g, 0);
			break;
		case 3:
			if (u.bc) alu = sub3_c(sys, h, g, sc);
			else alu = add3_c(sys, h, g, sc);
			break;
		case 4:
			if (u.bc) alu = sub3_c(sys, h, g, 1);
			else alu = add3_c(sys, h, g, 1);
			break;
		case 5:
			alu = and2(sys, h, g);
			break;
		case 6:
			if (u.bc) alu = xor2(sys, h, g);
			else alu = or2(sys, h, g);
			break;
		case 7:
			// alu = 0;
			break;
		}

		switch(u.zo) {
		case 0:	if (u.st == 15) s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		case 4:	ka = alu; break;
		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		}

		switch(u.st) {
		case 0:
			// nop
			break;
		case 1:
			s |= 1;
			break;
		case 2:
			s |= 2;
			break;
		case 3:
			s |= 4;
			break;
		case 4:
			s |= 8;
			break;
		case 5:
			s &= ~1;
			break;
		case 6:
			s &= ~2;
			break;
		case 7:
			s &= ~4;
			break;
		case 8:
			s &= ~8;
			break;
		case 9:
			// T.B.D. reset 6184...
	//fprintf(stderr, "%03x: res (%04x)\n", sys.pc, key);
			kbd = 0;
			break;
		case 10:
			s = (s & 0x0e) | (zo ^ 1);
			break;
		case 11:
			s = (s & 0x0d) | (zo << 1);
			break;
		case 12:
			ov = 1;
			Wang600.Disp.setOv(ov);
			break;
		case 13:
			s = 0;
			break;
		case 14:
			err = 1;
			Wang600.Disp.setErr(err);
			break;
		}

		switch(u.mop) {
		case 1:	wr_ram_i(l, m, n); break;
		case 2:	wr_ram_i(15, br_k, n); break;
		case 3:	wr_ram_i(15, 15, br_k); break;
		case 4:	rd_ram_i(l, m, n); break;
		case 5:	rd_ram_i(15, br_k, n); break;
		case 6:	rd_ram_i(15, 15, br_k); break;
		case 7:	printer_hammers(); break;
		case 8:	printer_feed(); break;
		case 9:	rc = 2; break;
		case 10:
			kb = (kb & ~1) | tape_read(sys);
			break;
		case 11:
			tape_write(sys, kb & 1);
			break;
		case 12:
			printer_status(sys);
			// not just printer, but CN-24 as well...
			kb |= 2;
			break;
		case 13:
			tape_on(sys, u.bi & 1);
			break;
		case 14:
			tape_off(sys); // u.bi & 1 affects this...
			break;
		case 15:
			gioa = ka;	// gioa = g;
			giob = kb;	// giob = h;
			iob = br_k & 0x07;
			dev_out(sys);
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		if (!opf7) {
			if (u.sub) {
				stk2 = stk1;
				stk1 = sys.pc;
			}
			switch(u.jh) {
			case 0: nxt |= (0 << 1); break;
			case 1: nxt |= (1 << 1); break;
			case 2: nxt |= ((br_acc & 2) >> 0); break;
			case 3: nxt |= ((br_acc & 8) >> 2); break;
			case 4:
				nxt |= (ind.ind.ov << 1);
	//fprintf(stderr,"%03x: chk pe\n", sys.pc);
				ind.ind.ov = 0;
				sys->display(sys, -2);
				break;
			case 5: nxt |= (cc << 1); break;
			case 6:
				// todo: clean this up!
				if (key) {
	//fprintf(stderr,"%03x: pop %04x\n", sys.pc, key);
	//if (__keytrc) fprintf(stderr,"key %02d %02d\n", (key >> 4) & 0x0f, key & 0x0f);
					kbd = 1;
					ka = (key >> 4) & 0x0f;
					kb = key & 0x0f;
					sys->keyboard(sys, &key, 1);
				}
				nxt |= (kbd << 1);
				if (kbd) {
					kbd = 0;
					sys->display(sys, 0);
				}
				break;
			case 7: rc = 3; break;
			}
			switch(u.jl) {
			case 0: nxt |= (0 << 0); break;
			case 1: nxt |= (1 << 0); break;
			case 2: nxt |= ((br_acc & 1) >> 0); break;
			case 3: nxt |= ((br_acc & 4) >> 2); break;
			case 4: nxt |= (zo << 0); break;
			case 5: rc = 4; break;
			case 6: nxt |= (br_c << 0); break;
			case 7: rc = 5; break;
			}
		}

		++cycles;
		next = nxt;
		// the following are called in specific order...
		// keyboard injection of next pc must override all, so is last.

		display_check(sys);	// this might sleep until UI event...

		sys->keyboard(sys, &key, 0);

		if (jam) {
			next = jam & 0x0fff;
			jam = 0;
			ov = 0;
			Wang600.Disp.setOv(ov);
			if (next == 0) { // PRIME
				err = 0;
				Wang600.Disp.setErr(err);
			}
		}

		pc = next;
		return rc;
	}

	public void run() {
		// Run the simulator...
		int rc = 0;
		while (rc == 0) {
			rc = instr_exec();
		}
		Wang_UI.fatal("Wang600 Core", "Simulation error");
	}
}
