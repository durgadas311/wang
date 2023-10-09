// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public interface WangInstructions {
	int encode(String line, byte[] mem, int start);
	int dreg(String line, byte[] mem, int start);
	WangInstruction decode(byte[] mem, int start);
}
