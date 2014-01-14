// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_Debugger.java,v 1.7 2014/01/14 21:53:51 drmiller Exp $

import java.io.*;

interface Wang_Debugger
{
	public String disas(int pc, boolean raw); // disassemble microcode instruction at 'pc'

	// The addresses, lengths, and values for these are "natural" units,
	// either bytes or nibbles depending on the architecture model.
	// Since these are called from debugger, it is the user that
	// provided these values and must then know what units to assume.
	public String ramDump(int adr, int len);
	public void ramSet(int adr, byte val);
	public String romDump(int adr, int len);
	public String getRegisters();
	public String getMachine();
	public void putTrace() throws Exception ;
	public void putWarp(String tag, int next, int cycles) throws Exception;
	public void core(FileOutputStream file) throws Exception;
	public void setTrace(boolean on) throws Exception;
	public void setTraceCycles(boolean on);
	public void setTraceRaw(boolean on);
	public void setTraceFile(FileOutputStream file) throws Exception;
	public String getTrace();
	public void dup();
	public long relCycleLimit(long n);
	public void setRun(boolean run);
	public boolean breakPoint(int adr);	// toggles BP at adr
	public boolean getBreakPoint(int adr);
	public int setReg(String reg, int val);
	public int getReg(String reg);
	public long getCycles();
	public int getPC();		// current CPU PC register
	public int getRamAdr();		// current CPU RAM address register(s)
	public int getUcodeSize();	// words (instructions)
}
