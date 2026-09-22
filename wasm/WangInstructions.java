// Copyright (c) 2023 Douglas Miller <durgadas311@gmail.com>
import java.io.PrintStream;

public interface WangInstructions {
	static final int NONE = 0;	// one-step instructions
	static final int MARK = 1;
	static final int REG = 2;
	static final int FMT = 3;
	static final int LABEL = 4;
	static final int ALPHA = 5;
	static final int IO = 6;	// GROUP 1/2 prefix
	static final int INDIR = 7;
	static final int REG100 = 8;	// Register is +100
	static final int ROMARK = 9;	// Wang 600 ROM target
	static final int FCALL = 10;	// f(x) calls
	static final int FROM = 11;	// Wang 600 ROM f(x) 
	static final int IOKEY = 12;	// Wang 600 I/O prefix

	int encode(String[] line, int first, WangMemory mem, int start);
	void endPC(int pc);
	int verifyProg(byte[] mem, int end);
	int regSteps(int nreg);
	int regPad(WangMemory mem, int start);
	int regPad(int start);
	int adrReg(int adr);
	int regAdr(int reg);
	String adrRegStr(int adr);
	int setReg(int reg, String val, WangMemory mem, int start);
	int dreg(String[] line, int first, WangMemory mem, int start);
	int xlab(String[] line, int first);
	int def(String[] line, int first);
	int setOutput(String dev);
	int setOutput(String[] line, int first);
	char lastError();
	WangSymbolTable getSymTab();
	WangInstruction decode(byte[] mem, int start);
	WangInstruction decodeOp(int op);
	int maxPC();
	int maxRomPC();
	int maxReg();
	int endProg();
	int endData();
	int regsPerBlk();
	int regBlkLen();
	int stop();
	boolean finalPass();
	void finalPass(boolean p);
	String printHelp(); // help string for PRINT/WRITE command (FMT)
	String regHelp();
	void alphaHelp(PrintStream out); // help string for ALPHA commands
	void iokeyHelp(PrintStream out); // help string for I/O commands (600)
}
