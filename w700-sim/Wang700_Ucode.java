// Copyright (c) 2011,2026 Douglas Miller <durgadas311@gmail.com>

public class Wang700_Ucode {
	public byte jl;
	public byte jh;
	public int jad;
	public byte st;
	public byte kk;
	public byte mop;
	public byte bd;
	public byte bc;
	public byte ac;
	public byte aop;
	public byte zo;
	public byte bi;
	public byte ai;
	public boolean brkpt;

	public Wang700_Ucode(byte[] instr) {
		// "LE", i.e. "jl" in byte[0]
		jl = (byte)((instr[0] >> 1) & 0x07);
		jh = (byte)((instr[0] >> 4) & 0x07);
		jad = (((instr[1] & 0x00ff) << 1) | ((instr[0] >> 7) & 1));
		st = (byte)(instr[2] & 0x0f);
		kk = (byte)((instr[2] >> 4) & 0x0f);
		mop = (byte)(instr[3] & 0x0f);
		bd = (byte)((instr[3] >> 4) & 1);
		bc = (byte)((instr[3] >> 5) & 0x03);
		ac = (byte)((instr[3] >> 7) & 1);
		aop = (byte)(instr[4] & 0x07);
		zo = (byte)((instr[4] >> 3) & 0x07);
		bi = (byte)(((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2));
		ai = (byte)((instr[5] >> 1) & 0x07);
		brkpt = ((instr[7] & 1) != 0);
	}

	public byte[] asBytes() {
		byte[] instr = new byte[8];
		instr[0] = (byte)((jl << 1) | (jh << 4) | ((jad & 1) << 7));
		instr[1] = (byte)(jad >> 1);
		instr[2] = (byte)(st | (kk << 4));
		instr[3] = (byte)(mop | (bd << 4) | (bc << 5) | (ac << 7));
		instr[4] = (byte)(aop | (zo << 3) | (bi << 6));
		instr[5] = (byte)((bi >> 2) | (ai << 1));
		return instr;
	}
}
