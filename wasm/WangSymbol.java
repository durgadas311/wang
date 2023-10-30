// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public class WangSymbol {
	public String nam;
	public int val;
	public int ref;

	public WangSymbol(String lab, int reg) {
		val = reg;
		nam = lab;
	}

	public WangSymbol(int ref, String lab) {
		val = 0xff;
		nam = lab;
		this.ref = ref;
	}
}
