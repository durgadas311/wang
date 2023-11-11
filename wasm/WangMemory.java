// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public interface WangMemory {
	int getMem(int adr);
	boolean putMem(int adr, int val);	// true if error
}
