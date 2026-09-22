// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public class WangRegBlock implements WangMemory {
	public byte[] mem;

	public WangRegBlock(WangInstructions wi) {
		// extra space for END tape code
		mem = new byte[wi.regBlkLen() + 2];
	}

	public int getMem(int adr) {
		if (adr < 0 || adr >= mem.length) return 0;
		return mem[adr] & 0xff;
	}

	public boolean putMem(int adr, int val) {
		if (adr < 0 || adr >= mem.length) return true;
		mem[adr] = (byte)val;
		return false;
	}
}
