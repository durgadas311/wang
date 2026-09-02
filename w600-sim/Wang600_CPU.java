// Copyright (c) 2011,2025 Douglas Miller <durgadas311@gmail.com>

import java.util.Arrays;

// Implements the Wang600 hardware. Does not provide any debug/trace support.

class Wang600_CPU
{
	// CPU registers.
	// ucode accessible
	public byte s;
	public byte t;
	public byte u;
	public byte v;
	public byte ca;
	public byte cb;
	public byte ka;
	public byte kb;
	// internal hardware accessible
	public byte l;
	public byte m;
	public byte n;
	public byte rb;
	public byte gioa;
	public byte giob;
	public byte iob;

	// status flags (1 bit)
	public byte zo;
	public byte cc;
	public byte sc;
	public byte kbd;
	public byte ov;
	public byte err;

	public boolean kbl;
	public boolean ioc;
	public boolean z2;
	private byte _ka;
	private byte _kb;
	private boolean kbd3;

	// ucode subroutine stack
	public int stk1;
	public int stk2;

	// simulator (no direct h/w relation)
	int jam;
	int next;
	int pc;
	long cycles;

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

	public int memsize;
	public int memmask;
	public byte[] _rom; // microcode
	public byte[] _ram; // program memory
	public byte[] _xrom;// extension/add-on program ROM
	Wang_FrontPanel fp;

	public Wang600_CPU(byte[] uc, byte[] ram, byte[] xrom, Wang_FrontPanel fp) {
		_rom = uc;
		_ram = ram;
		_xrom = xrom;
		if (fp != null) {
			memsize = fp.getMemSize();
			memmask = fp.getMemMask();
		}
		this.fp = fp;
		odd_parity = new byte[] { 1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1 };
		reset();
	}

	public void reset() { // different than PRIME
		pr_drum = 0;
		pr_hammers = 0;
		pr_tach = 0;
		pr_col = 0;
		// actual state of all these is indeterminate
		_kb = (byte)0x0f;
		_ka = (byte)0x0f;
		kb = (byte)0x0f;
		ka = (byte)0x0f;
		kbd3 = true;
		kbd = 0;
		z2 = false;
		// On real machines, did not always happen that power-on asserted PRIME...
		pc = 0x000;	// force PRIME on power-up...
	}

	public Wang600_Ucode fetchUcode(int adr) {
		int idx = adr * 8;
		byte[] instr = Arrays.copyOfRange(_rom, idx, idx + 8);
		return new Wang600_Ucode(instr);
	}

	public String disas(Wang600_Ucode uu, boolean raw) {
		String stack = new String();;
		int k = uu.kk;
		int nxt = uu.jad << 2;
		if (uu.jh < 2) {
			nxt |= (uu.jh << 1);
		}
		if (uu.jl < 2) {
			nxt |= (uu.jl << 0);
		}
		if (uu.jl == 7) {
			stack += "return";
		} else {
			if (uu.sub != 0) {
				stack += "call";
			} else {
				stack += "jump";
			}
			stack += String.format(" %03x", nxt);
			if (uu.jh >= 2 || uu.jl >= 2) {
				stack += "[";
				switch(uu.jh) {
				case 2: stack += "S<1>"; break;
				case 3: stack += "S<3>"; break;
				case 4: stack += "OV"; break;
				case 5: stack += "CC"; break;
				case 6: stack += "KBD"; break;
				}
				stack += ":";
				switch(uu.jl) {
				case 2: stack += "S<0>"; break;
				case 3: stack += "S<2>"; break;
				case 4: stack += "Zo"; break;
				case 5: stack += "Q?"; break;
				case 6: stack += "SC"; break;
				case 7: stack += "1?"; break;
				}
				stack += "]";
			}
		}
		String h;
		switch(uu.ai) {
		case 0: h = "S"; break;
		case 1: h = "T"; break;
		case 2: h = "U"; break;
		case 3: h = "V"; break;
		case 4: h = "KA"; break;
		case 5: h = "KB"; break;
		case 6: h = "CA"; break;
		case 7: h = "CB"; break;
		default: h = ""; break;
		}
		String g;
		switch(uu.bi) {
		case 0: g = "0"; break;
		case 1: g = Integer.toString(k); break;
		case 2: g = "D1"; break;
		case 3: g = "D2"; break;
		case 4: g = "KA"; break;
		case 5: g = "KB"; break;
		case 6: g = "CA"; break;
		case 7: g = "CB"; break;
		default: g = ""; break;
		}

		if (uu.ac == 0) h = "0"; // "15"? "0"? ???
		String ops = "+++++&|$";
		if (uu.bc != 0) ops = "-----&^$";
		String alu;
		if (uu.aop == 7) {
			alu = "0";
		} else {
			alu = h + " " + ops.substring(uu.aop, uu.aop + 1) + " " + g;
			switch (uu.aop) {
			case 1:
			case 4:
				alu += " " + ops.substring(uu.aop, uu.aop + 1) + " 1";
				break;
			case 3:
				alu += " " + ops.substring(uu.aop, uu.aop + 1) + " SC";
				break;
			}
			alu += " ->[Zo";
			if (uu.aop < 5) {
				alu += ",CC";
				switch (uu.aop) {
				case 2:
				case 3:
				case 4:
					alu += ",SC";
					break;
				}
			}
			alu += "]";
		}
		String acc = null;
		String mach = null;
		if (uu.st >=1 && uu.st <= 8) {
			acc = String.format("S<%d>=%d", (uu.st - 1) & 3, ((uu.st - 1) >> 2) ^ 1);
		} else {
			switch(uu.st) {
			case 0: /* sprintf(mach, "NOP"); */ break;
			case 9: mach = "RESET"; break;
			case 10: acc = "S<0>=!Z"; break;
			case 11: acc = "S<1>=Z"; break;
			case 12: mach = "OV=1"; break;
			case 13: acc = "S=0"; break;
			case 14: mach = "ERR=1"; break;
			}
		}
		String targ = new String();
		if (targ.length() > 0 && uu.zo != 7) {	// always false???
			targ += " = ";
		}
		switch(uu.zo) {
		case 0:	if (uu.st == 15) targ += "S"; break;
		case 1:	targ += "T"; break;
		case 2:	targ += "U"; break;
		case 3:	targ += "V"; break;
		case 4:	targ += "KA"; break;
		case 5:	targ += "KB"; break;
		case 6:	targ += "CA"; break;
		}
		if (targ.length() > 0) {
			targ += " = ";
		}

		String opA = null;
		switch(uu.mop) {
		case 1:	opA = "mem(T,U,V) = CA"; break;
		case 2:	opA = String.format("mem(15,%d,V) = CA", k); break;
		case 3:	opA = String.format("mem(15,15,%d) = CA", k); break;
		case 4:	opA = "CA = mem(T,U,V), CB = rom(T,U,V)"; break;
		case 5:	opA = String.format("CA = mem(15,%d,V), CB = rom(15,%d,V)", k, k); break;
		case 6:	opA = String.format("CA = mem(15,15,%d), CB = rom(15,15,%d)", k, k); break;
		case 7:	opA = "KBP <<+ KB<0>"; break;
		case 8:	opA = "PPF=1"; break;
		case 9:	opA = "<A9>"; break;
		case 10:	opA = "KB<0> = MHG/MHO"; break;
		case 11:	opA = "WDT = KB<0>"; break;
		case 12:	opA = "KA=PC0-3, KB<3>=PC4, KB<1>=RBS"; break;
		case 13:	opA = "TMR=1(";
				if ((uu.bi & 1) != 0) opA += "WR";
				else opA += "RD";
				opA += ")";
				break;
		case 14:	opA = "TMR=0";
				if ((uu.bi & 1) != 0) opA += "(noreset)";
				break;
		case 15:	opA = "GIOA=KA, GIOB=KB, IOB=";
				opA += Integer.toString(k);
				break;
		}

		String buf = targ + alu;
		if (acc != null) {
			buf += "; " + acc;
		}
		if (mach != null) {
			buf += "; " + mach;
		}
		if (opA != null) {
			buf += "; " + opA;
		}
		if (stack.length() > 0) {
			buf += "; " + stack;
		}
		if (raw) {
			buf = String.format("[%x%x%x%x%x%x%x%x%x%x%03x%x%x] ",
				uu.ai, uu.bi, uu.zo, uu.aop, uu.ac, uu.bc, uu.mop, uu.kk, uu.st,
				uu.sub, uu.jad << 2, uu.jh, uu.jl) + buf;
		}
		return buf;
	}

	public String disas(int adr, boolean raw) {
		Wang600_Ucode uu = fetchUcode(adr);
		return disas(uu, raw);
	}

	// these three might need "synchronized"
	public void setKaKb(int key) { 
		kbd = 1;
		_ka = (byte)((key >> 4) & 0x0f);
		_kb = (byte)(key & 0x0f);
		z2 = true;
		kbd3 = false;
	}

	private void chkKaKb() {
		// "kbd3" here does not directly emulate the hardware KBD3.
		if (!kbd3) {
			// On the 6184, this happens continuously while KBD3
			// is off, at each Rs clock pulse (end of instr).
			// Microcode must ensure that KA/KB remain zero
			// until input is received (key press or device
			// input).  Data is pre-latched immediately when a
			// key is pressed, and after a delay turns on KBD3.
			// Device input pre-latches data on leading edge of
			// GISN and sets KBD3 on trailing edge.  This window
			// ensures that data is copied from the pre-latch
			// into KA/KB on the next Rs.  KBD3 is off between
			// ST=9 (RESET) and input.
			//
			ka |= _ka;
			kb |= _kb;
			kbd3 = true; // do this only once per input
		}
	}

	private void clrKaKb() {
		_ka = 0;
		_kb = 0;
		ka = 0;
		kb = 0;
		kbd3 = true;
		kbd = 0;
		z2 = false;
	}

	private byte[] odd_parity;

	byte to_last;
	byte to_data;
	byte to_byte;
	int to_nibc;
	int to_bitc;
	int to_sigc;

	private void tape_write(int dat) {
		to_last <<= 1;
		to_last |= dat;
		to_sigc ^= 1;
		if (to_sigc != 0) return;
		byte bit = 0; 
		byte h = (byte)(to_last & 0x03);
		if (h == 0x02 || h == 0x01) bit = 1;
		if (++to_bitc == 5) {
			to_nibc ^= 1;
			if (to_nibc != 0) {
				to_byte = (byte)((to_byte & 0x0f) | (to_data << 4));
			} else {
				to_byte = (byte)((to_byte & 0xf0) | to_data);
				fp.tape_record(to_byte);
			}
			to_data = 0;
			to_bitc = 0;
		} else {
			to_data <<= 1;
			to_data |= bit;
		}
	}

	byte ti_last;
	int ti_data;
	int ti_bitc;
	int ti_sigc;
	long ti_repc;
	byte ti_bit;

	private int do_repc() {
		return ti_bit;
	}
	private int do_sigc() {
		--ti_sigc;
		ti_bit = (byte)(ti_last & 1);
		ti_last >>= 1;
		ti_repc = cycles + 97;	// very sensitive...
		return do_repc();
	}
	private int do_bitc() {
		--ti_bitc;
		int mask = (1 << ti_bitc);
		if ((ti_data & mask) != 0) {
			ti_last = 0x05;	// lsb first out...
		} else {
			ti_last = 0x01;	// lsb first out...
		}
		ti_sigc = 4;
		return do_sigc();
	}

	private int tape_read() {
		// wait for TD 0->1
		// delay 56 cycles
		// wait 220 cycles (sample TD for end of loop)
		// [15,15,6] ^= DL	; compute parity?
		// CY = 0 - DL		; CY = bit0
		// [15,15,5] <<= 1	; make space
		// [15,15,5] += CY	; insert new bit
		// ACC += 1		; count bits
		// wait up to 256 cycles for TD 0->1
		//         __    __
		// "1" = _|  |__|  |_
		//         __
		// "0" = _|  |_______
		//
		if (cycles < ti_repc) {
			return do_repc();
		}
		if (ti_sigc > 0) {
			return do_sigc();
		}
		if (ti_bitc > 0) {
			return do_bitc();
		}
		int ti = fp.tape_play();
		if (ti < 0) { // EOF
			ti_repc = cycles + 700;	// expects at least 650?
			ti_bit = 0;
			return do_repc();
		}
		if ((ti & 0x00ff00) != 0) {
			byte b1 = (byte)((ti >> 8) & 0x0ff);
			byte b2 = (byte)(ti & 0x0ff);
			int x = ((b1 >> 4) & 0x0f);
			ti_data = (x << 1) | odd_parity[x];
			x = (b1 & 0x0f);
			ti_data <<= 5;
			ti_data |= (x << 1) | odd_parity[x];

			x = ((b2 >> 4) & 0x0f);
			ti_data = (x << 1) | odd_parity[x];
			x = (b2 & 0x0f);
			ti_data <<= 5;
			ti_data |= (x << 1) | odd_parity[x];

			ti_bitc = 20;
		} else {
			int x = ((ti >> 4) & 0x0f);
			ti_data = (x << 1) | odd_parity[x];
			x = (ti & 0x0f);
			ti_data <<= 5;
			ti_data |= (x << 1) | odd_parity[x];
			ti_bitc = 10;
		}
		return do_bitc();
	}

	private void tape_on(int wr) {
		fp.tape_on(wr);
		if (wr == 0) {
			ti_bit = 0;
			ti_last = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_repc = 0;
		} else {
			to_last = 0;
			to_data = 0;
			to_byte = 0;
			to_nibc = 0;
			to_bitc = 0;
			to_sigc = 0;
		}
	}

	private void tape_off() {
		fp.tape_off(0);
	}

	private void dev_out() {
		byte c = (byte)((gioa << 4) | giob);
		fp.dev_out(iob, c);
	}

	private byte add3_i(byte a, byte b, byte c) {
		byte s = (byte)(a + b + c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte sub3_i(byte a, byte b, byte c) {
		byte s = (byte)(a - b - c);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((s & 0x10) != 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte and2(byte a, byte b) {
		byte s = (byte)(a & b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte or2(byte a, byte b) {
		byte s = (byte)(a | b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
	}

	private byte xor2(byte a, byte b) {
		byte s = (byte)(a ^ b);
		zo = (byte)((s & 0x0f) == 0 ? 1 : 0);
		return (byte)(s & 0x0f);
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
		if ((fp.getMode1(false) & D21_PRT_ON) == 0) {
			// only if running program doesn't get here...
			// printer is off, tach will never pulse, so don't spin
			//if (pc == 0x6db) {
				// sleep until key event... incl PRT ON...
			//}
			return;
		}
		if (pr_tach != 0) {
			pr_col = 0;
			pr_drum = (byte)((pr_drum + 1) & 0x0f);
			pr_hammers = 0;
		}
		pr_tach ^= 0x08;
		ka |= pr_drum;
		kb |= pr_tach;
	}

	private void printer_hammers() {
		int x;
		int h;

		pr_hammers <<= 1;
		pr_hammers &= 0x0fffff;
		pr_hammers |= kb & 1;
		if (++pr_col >= 20) {
			h = pr_hammers;
			for (x = 0; h != 0; ++x) {
				if ((h & 1) != 0) {
					fp.do_printer(x, pr_drum);
				}
				h >>= 1;
			}
			pr_col = 0;
		}
	}

	private void printer_feed() {
		// now, actually print it...
		fp.do_line();
	}

	private void rd_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		adr &= memmask;
		boolean odd = ((adr & 1) != 0);
		adr >>= 1;
		byte b = _ram[adr];
		if (odd) {
			b >>= 4;
		}
		rb = ca = (byte)(b & 0x0f);
		// ROM should always be >= RAM in size... always 2K...
		// (if present)
		if (_xrom != null && adr < _xrom.length) {
			b = _xrom[adr];
			if (odd) {
				b >>= 4;
			}
		} else {
			b = 0x0f;
		}
		cb = (byte)(b & 0x0f);
	}

	private void wr_ram_i(byte ah, byte am, byte al) {
		int adr = ((ah & 0x0f) << 8) | ((am & 0x0f) << 4) | (al & 0x0f);
		adr &= memmask;
		byte a = ca;
		byte b = _ram[adr >> 1];
		if ((adr & 1) != 0) {
			a <<= 4;
			b &= 0x0f;
		} else {
			b &= 0xf0;
		}
		_ram[adr >> 1] = (byte)(b | a);
	}

	public void new_iob(int io) {
		iob = (byte)(io & 0x07);
		ioc = ((iob & 0b110) == 0b010);
		kbl = ((iob & 0b110) != 0);
	}

	public int instr_exec() {
		Wang600_Ucode uu = fetchUcode(pc);
		int nxt;
		int rc = 0;
		boolean mr = false;

		if (uu.brkpt) {
			fp.breakpoint(pc);
			return 0;
		}

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
		byte br_k = uu.kk;
		int opf7 = (uu.jl == 7 ? 1 : 0);
		if (opf7 != 0) {
			nxt = stk1 | 1;
			if (uu.sub != 0) {
				stk1 = stk2;
			} else {
				stk1 = stk2; // bugfix?
				//stk1 = pc;	// bad?
				// rc = 1;
			}
		} else {
			nxt = uu.jad << 2;
		}

		if (uu.mop >= 1 && uu.mop <= 6) {
			l = t;
			m = u;
			n = v;
			mr = (uu.mop >= 4);
		}

		byte g = 0;
		byte h = 0;
		switch(uu.ai) {
		case 0: h = s; break;
		case 1: h = t; break;
		case 2: h = u; break;
		case 3: h = v; break;
		case 4: h = ka; break;
		case 5: h = kb; break;
		case 6: h = ca; break;
		case 7: h = cb; break;
		}

		switch(uu.bi) {
		case 0: g = 0; break;
		case 1: g = br_k; break;
		case 2: g = (byte)fp.getMode0(true); break;
		case 3: g = (byte)(fp.getMode1(true) ^ D20_DEGREES); break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		if (uu.ac == 0) h = 0; // "15"? "0"? ???
		switch (uu.aop) {
		case 0:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)0);
			else alu = add3_i(h, g, (byte)0);
			break;
		case 1:
			if (uu.bc != 0) alu = sub3_i(h, g, (byte)1);
			else alu = add3_i(h, g, (byte)1);
			break;
		case 2:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)0);
			else alu = add3_c(h, g, (byte)0);
			break;
		case 3:
			if (uu.bc != 0) alu = sub3_c(h, g, sc);
			else alu = add3_c(h, g, sc);
			break;
		case 4:
			if (uu.bc != 0) alu = sub3_c(h, g, (byte)1);
			else alu = add3_c(h, g, (byte)1);
			break;
		case 5:
			alu = and2(h, g);
			break;
		case 6:
			if (uu.bc != 0) alu = xor2(h, g);
			else alu = or2(h, g);
			break;
		case 7:
			// alu = 0;
			break;
		}

		switch(uu.zo) {
		case 0:	if (uu.st == 15) s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		case 4:	ka = alu; break;
		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		}

		switch(uu.st) {
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
			clrKaKb();
			break;
		case 10:
			s = (byte)((s & 0x0e) | (zo ^ 1));
			break;
		case 11:
			s = (byte)((s & 0x0d) | (zo << 1));
			break;
		case 12:
			ov = 1;
			fp.setOv(ov);
			break;
		case 13:
			s = 0;
			break;
		case 14:
			err = 1;
			fp.setErr(err);
			break;
		}

		switch(uu.mop) {
		case 1:	wr_ram_i(l, m, n); break;
		case 2:	wr_ram_i((byte)15, br_k, n); break;
		case 3:	wr_ram_i((byte)15, (byte)15, br_k); break;
		case 4:	rd_ram_i(l, m, n); break;
		case 5:	rd_ram_i((byte)15, br_k, n); break;
		case 6:	rd_ram_i((byte)15, (byte)15, br_k); break;
		case 7:	printer_hammers(); break;
		case 8:	printer_feed(); break;
		case 9:	rc = 2; break;
		case 10:
			// This should match hardware, but breaks LOAD PROG:
			// kb |= (byte)tape_read(); why???
			kb = (byte)((kb & ~1) | tape_read());
			break;
		case 11:
			tape_write(kb & 1);
			break;
		case 12:
			printer_status();
			// not just printer, but CN-24 (RBS) as well...
			kb |= 2;
			break;
		case 13:
			tape_on(uu.bi & 1);
			break;
		case 14:
			tape_off(); // uu.bi & 1 affects this...
			break;
		case 15:
			gioa = ka;	// gioa = g;
			giob = kb;	// giob = h;
			new_iob(br_k);
			// hardware triggers GISO here (at CK5).
			// ucode waits for KBD (after RESET).
			dev_out();
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		if (opf7 == 0) {
			if (uu.sub != 0) {
				stk2 = stk1;
				stk1 = pc;
			}
			switch(uu.jh) {
			case 0: nxt |= (0 << 1); break;
			case 1: nxt |= (1 << 1); break;
			case 2: nxt |= ((br_acc & 2) >> 0); break;
			case 3: nxt |= ((br_acc & 8) >> 2); break;
			case 4:
				nxt |= (ov << 1);
				ov = 0;
				fp.setOv(ov);
				break;
			case 5: nxt |= (cc << 1); break;
			case 6: nxt |= (kbd << 1); break;
			case 7: rc = 3; break;
			}
			switch(uu.jl) {
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

		chkKaKb();

		fp.debug_check();

		// the following are called in specific order...
		// keyboard injection of next pc must override all, so is last.

		fp.display_check(mr);

		if (jam != 0) {
			next = jam & 0x0fff;
			jam = 0;
			ov = 0;
			fp.setOv(ov);
			if (next == 0) { // PRIME
				err = 0;
				fp.setErr(err);
			}
		}

		pc = next;
		return rc;
	}
}
