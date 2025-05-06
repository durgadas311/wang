// Copyright (c) 2011,2025 Douglas Miller <durgadas311@gmail.com>

public class Wang600_Ucode {
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
	public boolean brkpt;

	public Wang600_Ucode(byte[] instr) {
		// "LE", i.e. "jl" in byte[0]
		jl = (byte)((instr[0] >> 2) & 0x07);
		jh = (byte)((instr[0] >> 5) & 0x07);
		jad = ((instr[1] & 0x00ff) | ((instr[2] & 1) << 8));
		sub = (byte)((instr[2] >> 1) & 1);
		st = (byte)((instr[2] >> 2) & 0x0f);
		kk = (byte)(((instr[2] >> 6) & 0x03) | ((instr[3] & 0x03) << 2));
		mop = (byte)((instr[3] >> 2) & 0x0f);
		bc = (byte)((instr[3] >> 6) & 1);
		ac = (byte)((instr[3] >> 7) & 1);
		aop = (byte)(instr[4] & 0x07);
		zo = (byte)((instr[4] >> 3) & 0x07);
		bi = (byte)(((instr[4] >> 6) & 0x03) | ((instr[5] & 1) << 2));
		ai = (byte)((instr[5] >> 1) & 0x07);
		brkpt = ((instr[7] & 1) != 0);
	}
}
