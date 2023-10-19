// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>

public interface WangInstructions {
	static final int NONE = 0;	// one-step instructions
	static final int MARK = 1;
	static final int REG = 2;
	static final int FMT = 3;
	static final int LABEL = 4;
	static final int ALPHA = 5;
	static final int IO = 6;
	static final int INDIR = 7;
	static final int REG100 = 8;	// Register is +100

	int encode(String[] line, int first, byte[] mem, int start);
	int regPad(byte[] mem, int start);
	String adrRegStr(int adr);
	int dreg(String[] line, int first, byte[] mem, int start);
	char lastError();
	WangSymbolTable getSymTab();
	WangInstruction decode(byte[] mem, int start);
	WangInstruction decodeOp(int op);
	int maxPC();
	int maxReg();
	int endProg();
	String printHelp(); // help string for PRINT/WRITE command (FMT)
	String regHelp();
}
