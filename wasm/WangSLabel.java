// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public class WangSLabel {
	private static final int START = 2;
	public WangSymbol low;
	public WangSymbol high;
	public int count;
	public int err;

	static private int refnum = START;

	public WangSLabel() {
		int ref = refnum++;
		low = new WangSymbol(-ref);
		high = new WangSymbol(-ref);
		count = 1;
		err = 0;
	}

	static public void reset() {
		refnum = START;
	}
}
