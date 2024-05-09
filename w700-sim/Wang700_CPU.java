// Copyright (c) 2011,2026 Douglas Miller

import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

// Implements the Wang700 CPU hardware. Does not provide any debug/trace support.

class Wang700_CPU
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
	public byte ra;
	public byte rb;
	public byte gioa;
	public byte giob;
	public byte iob;

	// status flags (1 bit)
	public byte zo;
	public byte cc;
	public byte sc;
	public byte q;
	public byte kbd;
	public byte ov;
	public byte err;

	// internal signals and latches
	public boolean kbd1;
	public boolean kbl;
	public boolean ioc;
	public boolean z2;
	public byte _ka;
	public byte _kb;
	public byte _gi;
	public boolean _gin;

	// simulator (no direct h/w relation)
	public int jam;
	public int next;
	public int last;
	public int pc;
	public long cycles;

	// memory error injection:
	Random rnd;
	int inj_adr = -1;
	int inj_clr;
	int inj_set;
	int inj_flp;

	int memsize;
	int memmask;
	public byte[] _rom; // microcode
	public byte[] _ram; // program memory
	Wang_FrontPanel fp;
	Properties props;
	String pfx;

	public Wang700_CPU(Properties props, byte[] uc, byte[] ram, Wang_FrontPanel fp) {
		_rom = uc;
		_ram = ram;
		memsize = fp.getMemSize();
		memmask = fp.getMemMask();
		this.fp = fp;
		this.props = props;
		pfx = props.getProperty("prefix");
		if (pfx == null) pfx = "";
		errorInject();
		odd_parity = new byte[] { 1,0,0,1,0,1,1,0,0,1,1,0,1,0,0,1 };
		reset();
	}

	public void reset() {
		// actual state of all these is indeterminate
		_kb = (byte)0x0f;
		_ka = (byte)0x0f;
		kb = (byte)0x0f;
		ka = (byte)0x0f;
		kbd = 1; // this allows use of KA/KB
		kbd1 = true; // this allows use of KA/KB
		z2 = false;
		fp.setGKBD(true);
		// On real machines, did not always happen that power-on asserted PRIME...
		pc = 0x000;	// force PRIME on power-up...
		l = 0;
		m = 0;
		n = 0;
	}

	private void errorInject() {
		rnd = new Random(System.nanoTime());
		String p = props.getProperty(pfx + "inject");
		if (p == null) return;
		String[] ss = p.split(",");
		if (ss.length != 4) {
			System.err.format("Invalid inject \"%s\", usage: inject=adr,clr,set,flip\n", p);
			return;
		}
		try {
			inj_adr = Integer.decode(ss[0]);
			inj_clr = Integer.decode(ss[1]);
			inj_set = Integer.decode(ss[2]);
			inj_flp = Integer.decode(ss[3]);
		} catch (Exception ee) {
			inj_adr = -1;
			System.err.format("Invalid inject \"%s\", usage: inject=adr,clr,set,flip\n", p);
			return;
		}
	}

	public Wang700_Ucode fetchUcode(int adr) {
		int idx = adr * 8;
		byte[] instr = Arrays.copyOfRange(_rom, idx, idx + 8);
		return new Wang700_Ucode(instr);
	}

	public String disas(Wang700_Ucode uu, boolean raw) {
		String stack = new String();;
		int k = uu.kk;
		int nxt = uu.jad << 2;
		if (uu.jh < 2) {
			nxt |= (uu.jh << 1);
		}
		if (uu.jl < 2) {
			nxt |= (uu.jl << 0);
		}
		stack += "jump";
		stack += String.format(" %03x", nxt);
		if (uu.jh >= 2 || uu.jl >= 2) {
			stack += "[";
			switch(uu.jh) {
			case 2: stack += "S<1>"; break;
			case 3: stack += "S<3>"; break;
			case 4: stack += "OV"; break;
			case 5: stack += "CC"; break;
			case 6: stack += "KBD"; break;
			case 7: stack += "{7}"; break;
			}
			stack += ":";
			switch(uu.jl) {
			case 2: stack += "S<0>"; break;
			case 3: stack += "S<2>"; break;
			case 4: stack += "Zo"; break;
			case 5: stack += "Q"; break;
			case 6: stack += "SC"; break;
			case 7: stack += "{7}"; break;
			}
			stack += "]";
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
		case 3: g = "0?"; break;
		case 4: g = "KA"; break;
		case 5: g = "KB"; break;
		case 6: g = "CA"; break;
		case 7: g = "CB"; break;
		default: g = ""; break;
		}

		if (uu.ac == 0) h = "0"; // "15"? "0"? ???
		String gx = (uu.bd != 0 ? "9" : "15");
		switch(uu.bc) {
		case 0: g = "0"; break;
		case 1: break;
		case 2: g = gx; break;
		case 3: g = "(" + gx + "-" + g + ")"; break;
		}

		String ops = "+++++&^+";
		String alu = new String();
		alu += h + " " + ops.substring(uu.aop, uu.aop + 1) + " " + g;
		switch (uu.aop) {
		case 1:
		case 4:
			alu += " " + ops.substring(uu.aop, uu.aop + 1) + " 1";
			break;
		case 3:
			alu += " " + ops.substring(uu.aop, uu.aop + 1) + " SC";
			break;
		}
		if (uu.bd != 0) {
			alu = "BCD(" + alu + ")";
		}
		if (uu.aop == 7) {
			alu += " >> 1";
		}
		alu += " ->[Zo";
		if (uu.aop == 7) {
			alu += ",CC,SC]";
		} else {
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
		// P9 is where this result gets stored/computed.

		// P4
		String mp4 = null;
		if (uu.mop >= 2 && uu.mop <= 5) {
			if (uu.mop >= 4) {
				mp4 = String.format("L=15,M=%d,N=V", k);
			} else {
				mp4 = "L=T,M=U,N=V";
			}
		}

		// P4-5
		String mp45 = null;
		switch(uu.mop) {
		case 10: mp45 = "KB<0>=Dot"; break;
		case 11: mp45 = "Din=KB<0>"; break;
		case 12:
			mp45 = "TMR=1,";
			mp45 += ((uu.bi & 1) != 0 ? "WR" : "RD");
			break;
		case 13: mp45 = "TMR=0"; break;
		}

		// P10
		String stp10 = null;
		if (uu.st >=1 && uu.st <= 8) {
			stp10 = String.format("S<%d>=%d", (uu.st - 1) & 3, ((uu.st - 1) >> 2) ^ 1);
		} else {
			switch(uu.st) {
			case 0: /* sprintf(stp10, "NOP"); */ break;
			case 9: stp10 = "RESET"; break;
			case 10: stp10 = "S<0>=!Z"; break;
			case 11: stp10 = "S<1>=Z"; break;
			case 12: stp10 = "OV=1"; break;
			case 13: stp10 = "S=0"; break;
			case 14: stp10 = "ERR=1"; break;
			}
		}

		// P9
		String targ = new String();
		switch(uu.zo) {
		case 0:	targ += "S"; break;
		case 1:	targ += "T"; break;
		case 2:	targ += "U"; break;
		case 3:	targ += "V"; break;
		case 4:	targ += "KA"; break;
		case 5:	targ += "KB"; break;
		case 6:	targ += "CA"; break;
		case 7:	targ += "CB"; break;
		}
		if (targ.length() > 0) {
			targ += " = ";
		}

		// P5-6
		String mp56 = null;
		switch(uu.mop) {
		case 7: mp56 = "IOB=KB<0:2>"; break;
		case 14: mp56 = "GIOA,GIOB=KA,KB"; break;
		}

		// P9
		String opA = null;
		switch(uu.mop) {
		case 0:	opA = "mem(LMN) = RA=alu,RB"; break;
		case 1:	opA = "mem(LMN) = RA,RB=alu"; break;
		case 2:	opA = "CA,CB=RA,RB = mem(LMN)"; break;
		case 3:	opA = "RA,RB = mem(LMN)"; break;
		case 4:	opA = "CA,CB=RA,RB = mem(LMN)"; break;
		case 5:	opA = "RA,RB = mem(LMN)"; break;
		case 6:	opA = "KB<0>=RBS"; break;
		case 7:	break; // done at P5-6
		case 8:	break;
		case 9:
			if (uu.aop == 7) {
				opA = "Q=SC";
			} else {
				opA = "Q=CC";
			}
			break;
		case 10:
		case 11:
		case 12:
		case 13:
			break; // done at P4-5
		case 14: break; // done at P5-6
		case 15: opA = "{15}"; break;
		}

		String buf = new String();
		if (mp4 != null) {
			if (buf.length() > 0) buf += "; ";
			buf += mp4;
		}
		if (mp45 != null) {
			if (buf.length() > 0) buf += "; ";
			buf += mp45;
		}
		if (mp56 != null) {
			if (buf.length() > 0) buf += "; ";
			buf += mp56;
		}
		if (buf.length() > 0) buf += "; ";
		buf += targ + alu;
		if (opA != null) {
			buf += "; " + opA;
		}
		if (stp10 != null) {
			buf += "; " + stp10;
		}
		if (stack.length() > 0) {
			buf += "; " + stack;
		}
		if (raw) {
			buf = String.format("[%x%x%x%x%x%x%x%x%x%x%03x%x%x] ",
				uu.ai, uu.bi, uu.zo, uu.aop, uu.ac, uu.bc, uu.bd,
				uu.mop, uu.kk, uu.st,
				uu.jad << 2, uu.jh, uu.jl) + buf;
		}
		return buf;
	}

	public String disas(int adr, boolean raw) {
		Wang700_Ucode uu = fetchUcode(adr);
		return disas(uu, raw);
	}

	public synchronized void setJam(int key) {
		fp.setGKBD(true);
		jam = 0x1000 | key;
		kbd = 1;
		ov = 0;
		fp.setOv(ov);
		if (key == 0) { // PRIME
			err = 0;
			fp.setErr(err);
		}
	}

	public synchronized void setStep() {
		kbd1 = true;
	}

	public synchronized void setKaKb(int key) {
		_ka |= (byte)((key >> 4) & 0x0f);
		_kb |= (byte)(key & 0x0f);
		// On 5919, _ka/_kb is pass-thru to KA/KB while kbd=0
		if (kbd == 0) {
			ka = _ka;
			kb = _kb;
		}
		kbd = 1;
		z2 = true;
		fp.setGKBD(true);
	}

	public void setGi(int key) {
		fp.setGKBD(true);
		// On 5919, GIA/B is pass-thru to KA/KB while kbd=0
		_gi = (byte)(key & 0x0ff);
		if (kbd == 0) {
			ka = (byte)((_gi >> 4) & 0x0f);
			kb = (byte)(_gi & 0x0f);
		}
		kbd = 1;
		_gin = true;
		z2 = true;
	}

	private synchronized void chkKaKb() {
		// The 700 does not handle keyboard data in every instruction cycle,
		// but rather restricts KA/KB to keyboard/device data between
		// RESET and input strobe (kbd == 0).
	}

	private synchronized void clrKaKb() {
		_ka = 0; 
		_kb = 0;
		// On 5919, _ka/_kb is pass-thru to KA/KB while kbd=0
		kbd = 0;
		kbd1 = false;
		ka = _ka;
		ka = _ka;
		z2 = false;
		_gin = false;
		fp.setGKBD(false);
	}

	private byte[] odd_parity;

	private byte even_parity8(byte data) {
		byte p = odd_parity[data & 0x0f];
		p ^= odd_parity[(data >> 4) & 0x0f];
		return p;
	}

	byte to_last;
	byte to_data;
	int to_bitc;
	int to_sigc;

	private void tape_write(int dat) {
		to_last <<= 1;
		to_last |= dat;
		++to_sigc;
		if ((to_sigc & 0x03) != 0) return;
		byte bit = 0;
		byte h = (byte)(to_last & 0x0f);
		if (h == 0x05) bit = 1;
		if (++to_bitc == 9) {
			// ignore parity bit...
			fp.tape_record(to_data);
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

	private boolean do_repc() {
		return (cycles < ti_repc);
	}
	private boolean do_sigc() {
		// can only arrive here if cycles >= ti_repc...
		while (ti_sigc > 0) {
			--ti_sigc;
			ti_bit = (byte)(ti_last & 1);
			ti_last >>= 1;
			// must maintain real-time aspect...
			ti_repc += 390;	// very sensitive? 390/391
			if (do_repc()) {
				return true;
			}
		}
		return false;
	}
	private boolean do_bitc() {
		if (ti_bitc > 0) {
			--ti_bitc;
			int mask = (1 << ti_bitc);
			if ((ti_data & mask) != 0) {
				ti_last = 0x07;	// lsb first out...
			} else {
				ti_last = 0x01;	// lsb first out...
			}
			ti_sigc = 4;
			if (do_sigc()) {
				return true;
			}
		}
		return false;
	}

	private int tape_read() {
		// wait for TD 0->1
		// delay 815 cycles
		// sample for 170 cycles (sample TD at end of loop)
		// delay 353 cycles
		// wait up to 800? cycles for TD 0->1
		//         __    __
		// "1" = _|  |__|  |_ (each "bit" is 390/391 cycles)
		//         __
		// "0" = _|  |_______
		//
		if (do_repc()) {
			return ti_bit;
		}
		// might have been a lot of elapsed time since last call,
		// so have to account for the extra, possibly skipping bits...
		// i.e. must keep "real time" representation of bit stream.
		// This is because the '700 uses delay loops that do not call
		// tape_read().
		if (do_sigc()) {
			return ti_bit;
		}
		if (do_bitc()) {
			return ti_bit;
		}
		int ti = fp.tape_play();
		if (ti < 0) { // EOF
			ti_bit = 0; // "dead air"...
			// relative or absolute?
			//ti_repc += 1000; // how long is needed?
			ti_repc = cycles + 1000; // how long is needed?
			return ti_bit;
		} else {
			if ((ti & 0x00ff00) != 0) {
				byte b1 = (byte)((ti >> 8) & 0x0ff);
				byte b2 = (byte)(ti & 0x0ff);
				ti_data = ((b1 << 1) | even_parity8(b1)) << 9;
				ti_data |= (b2 << 1) | even_parity8(b2);
				ti_bitc = 18;
			} else {
				ti_data = (ti << 1) | even_parity8((byte)ti);
				ti_bitc = 9;
			}
			if (do_bitc()) { // must always return true?
				return ti_bit;
			}
		}
		return 0; // run-out the clock...
	}

	private void tape_on(int wr) {
		fp.tape_on(wr);
		if (wr == 0) {
			ti_bit = 0;
			ti_last = 0;
			ti_sigc = 0;
			ti_bitc = 0;
			ti_repc = cycles + 10; // how much time before it starts looking?
		} else {
			to_last = 0;
			to_data = 0;
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

	private byte bin_add3_i(byte a, byte b, byte c) {
		byte _s = (byte)(a + b + c);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		cc = (byte)((_s & 0x10) != 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_add3_i(byte a, byte b, byte c) {
		byte _s = (byte)(a + b + c);
		byte _cc = 0;
		while (_s >= 10) {
			_s -= 10;
			_cc = 1;
		}
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		cc = _cc;
		return (byte)(_s & 0x0f);
	}

	private byte bin_add3_c(byte a, byte b, byte c) {
		byte _s = bin_add3_i(a, b, c);
		sc = cc;
		return (byte)(_s & 0x0f);
	}

	private byte bcd_add3_c(byte a, byte b, byte c) {
		byte _s = bcd_add3_i(a, b, c);
		sc = cc;
		return (byte)(_s & 0x0f);
	}

	private byte bin_shift3_c(byte a, byte b, byte c) {
		byte _s = bin_add3_i(a, b, (byte)0);
		_s |= (byte)(c << 4);
		sc = (byte)(_s & 1);
		_s >>= 1;
		return (byte)(_s & 0x0f);
	}

	private byte bcd_shift3_c(byte a, byte b, byte c) {
		byte _s = bcd_add3_i(a, b, (byte)0);
		s |= (byte)(c << 4);
		sc = (byte)(_s & 1);
		_s >>= 1;
		return (byte)(_s & 0x0f);
	}

	private byte bin_and2(byte a, byte b) {
		bin_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a & b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_and2(byte a, byte b) {
		bcd_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a & b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bin_xor2(byte a, byte b) {
		bin_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a ^ b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private byte bcd_xor2(byte a, byte b) {
		bcd_add3_i(a, b, (byte)0);	// set CC
		byte _s = (byte)(a ^ b);
		zo = (byte)((_s & 0x0f) == 0 ? 1 : 0);
		return (byte)(_s & 0x0f);
	}

	private void rd_ram_i() {
		int adr = ((l & 0x0f) << 8) | ((m & 0x0f) << 4) | (n & 0x0f);
		adr &= memmask;
		byte b = _ram[adr];
		_ram[adr] = 0; // core memory: destructive read
		if (inj_adr == adr) {
			if (rnd.nextInt(512) == 0) {
				if (inj_clr != 0) b &= ~inj_clr;
				if (inj_set != 0) b |= inj_set;
				if (inj_flp != 0) b ^= inj_flp;
			}
		}
		ra = (byte)((b >> 4) & 0x0f);
		rb = (byte)(b & 0x0f);
	}

	private void wr_ram_i(byte _ra, byte _rb) {
		int adr = ((l & 0x0f) << 8) | ((m & 0x0f) << 4) | (n & 0x0f);
		adr &= memmask;
		// core memory: writes effectively only change "1" bits
		_ram[adr] |= (byte)((_ra << 4) | _rb);
	}

	public void new_iob(int io) {
		iob = (byte)(io & 0x07);
		ioc = ((iob & 0b110) == 0b010);
		kbl = ((iob & 0b110) != 0);
	}

	public int instr_exec() {
		Wang700_Ucode uu = fetchUcode(pc);
		int nxt;
		int ret = 0;
		boolean mr = false;

		if (uu.brkpt) {
			fp.breakpoint(pc);
			return 0;
		}

		// On 5919, KA/KB can only be used if KBD1 or KBD
		boolean kakbzo = (kbd1 || // check STEP
				kbd != 0);
		// For conditional jump, these bits are latched early...
		byte br_s = s;
		byte br_sc = sc;
		byte br_q = q;
		byte br_k = uu.kk;
		nxt = uu.jad << 2;

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
		case 2:
			g = (byte)fp.getMode0(true);
			break;
		case 3: g = 0; break;
		case 4: g = ka; break;
		case 5: g = kb; break;
		case 6: g = ca; break;
		case 7: g = cb; break;
		}

		byte alu = 0;

		byte gx = (byte)(uu.bd != 0 ? 9 : 15);
		if (uu.ac == 0) h = 0;
		switch(uu.bc) {
		case 0: g = 0; break;
		case 1: break;
		case 2: g = gx; break;
		case 3: g = (byte)((gx - g) & 0x0f); break;
		}

		if (uu.bd != 0) {
			switch (uu.aop) {
			case 0:
				alu = bcd_add3_i(h, g, (byte)0);
				break;
			case 1:
				alu = bcd_add3_i(h, g, (byte)1);
				break;
			case 2:
				alu = bcd_add3_c(h, g, (byte)0);
				break;
			case 3:
				alu = bcd_add3_c(h, g, br_sc);
				break;
			case 4:
				alu = bcd_add3_c(h, g, (byte)1);
				break;
			case 5:
				alu = bcd_and2(h, g);
				break;
			case 6:
				alu = bcd_xor2(h, g);
				break;
			case 7:
				alu = bcd_shift3_c(h, g, br_sc);
				break;
			}
		} else {
			switch (uu.aop) {
			case 0:
				alu = bin_add3_i(h, g, (byte)0);
				break;
			case 1:
				alu = bin_add3_i(h, g, (byte)1);
				break;
			case 2:
				alu = bin_add3_c(h, g, (byte)0);
				break;
			case 3:
				alu = bin_add3_c(h, g, br_sc);
				break;
			case 4:
				alu = bin_add3_c(h, g, (byte)1);
				break;
			case 5:
				alu = bin_and2(h, g);
				break;
			case 6:
				alu = bin_xor2(h, g);
				break;
			case 7:
				alu = bin_shift3_c(h, g, br_sc);
				break;
			}
		}

		// Now we start changing machine state... must adhere
		// to designated P-clock cycles...

		// P4
		if (uu.mop >= 2 && uu.mop <= 5) {
			l = (uu.mop >= 4 ? (byte)15 : t);
			m = (uu.mop >= 4 ? br_k : u);
			n = v;
			mr = true;
		}

		// P4-5
		switch(uu.mop) {
		case 10: kb = (byte)((kb & ~1) | tape_read()); break;
		case 11: tape_write(kb & 1); break;
		case 12: tape_on(uu.bi & 1); break;
		case 13: tape_off(); break;
		}

		// P5-6
		switch(uu.mop) {
		case 7:
			new_iob(kb & 0x07);
			break;
		case 14:
			gioa = ka;
			giob = kb;
			dev_out();
			break;
		}

		// P9
		switch(uu.zo) {
		case 0:	s = alu; break;
		case 1:	t = alu; break;
		case 2:	u = alu; break;
		case 3:	v = alu; break;
		// 5919: KBD forces PRE/CLR on KA/KB and prevents use by Zo,
		// but this breaks 720C microcode...
		case 4:	if (kakbzo) ka = alu; break;
		case 5:	if (kakbzo) kb = alu; break;
//		case 4:	ka = alu; break;
//		case 5:	kb = alu; break;
		case 6:	ca = alu; break;
		case 7:	cb = alu; break;
		}

		// P10
		switch(uu.st) {
		case 0: break;
		case 1: s |= 1; break;
		case 2: s |= 2; break;
		case 3: s |= 4; break;
		case 4: s |= 8; break;
		case 5: s &= ~1; break;
		case 6: s &= ~2; break;
		case 7: s &= ~4; break;
		case 8: s &= ~8; break;
		case 9:
			clrKaKb();
			break;
		case 10: s = (byte)((s & 0x0e) | (zo ^ 1)); break;
		case 11: s = (byte)((s & 0x0d) | (zo << 1)); break;
		case 12: ov = 1; fp.setOv(ov); break;
		case 13: s = 0; break;
		case 14: err = 1; fp.setErr(err); break;
		}

		// P9 (non-conflict with P10 ST ops?)
		switch(uu.mop) {
		case 0: wr_ram_i(alu, rb); break; // L,M,N setup at P5-6
		case 1: wr_ram_i(ra, alu); break; // L,M,N setup at P5-6
		case 2:	rd_ram_i(); ca = ra; cb = rb; break; // L,M,N setup at P5-6
		case 3:	rd_ram_i(); break; // L,M,N setup at P5-6
		case 4:	rd_ram_i(); ca = ra; cb = rb; break; // L,M,N setup at P5-6
		case 5:	rd_ram_i(); break; // L,M,N setup at P5-6
		case 6:
			kb |= fp.getRBS();
			break;
		case 7:	break; // done at P5-6
		case 8:	break;
		case 9:	q = (uu.aop == 7 ? sc : cc); break;
		case 10:
		case 11:
		case 12:
		case 13:
		case 14:
			// done at P4-5 or P5-6
			break;
		case 15:
			ret = 2;
			break;
		}

		// This is done "late" to ensure we use most recent flags for I and Z
		// P9
		switch(uu.jh) {
		case 0: nxt |= (0 << 1); break;
		case 1: nxt |= (1 << 1); break;
		case 2: nxt |= ((br_s & 2) >> 0); break;
		case 3: nxt |= ((br_s & 8) >> 2); break;
		case 4:
			nxt |= (ov << 1);
			ov = 0;
			fp.setOv(ov);
			break;
		case 5: nxt |= (cc << 1); break;
		case 6: nxt |= (kbd << 1); break;
		case 7: ret = 3; break;
		}
		switch(uu.jl) {
		case 0: nxt |= (0 << 0); break;
		case 1: nxt |= (1 << 0); break;
		case 2: nxt |= ((br_s & 1) >> 0); break;
		case 3: nxt |= ((br_s & 4) >> 2); break;
		case 4: nxt |= (zo << 0); break;
		case 5: nxt |= (br_q << 0); break;
		case 6: nxt |= (br_sc << 0); break;
		case 7: ret = 5; break;
		}

		++cycles;
		next = nxt;

		chkKaKb();

		fp.debug_check();

		// the following are called in specific order...
		// keyboard injection of next pc must override all, so is last.

		fp.display_check(mr);	// this might sleep until UI event...

		//sys->keyboard(sys, &key, 0);

		if (jam != 0) {
			next = jam & 0x0fff;
			jam = 0;
		}

		last = pc;
		pc = next;
		return ret;
	}
}
