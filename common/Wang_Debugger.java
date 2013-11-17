// Copyright (c) 2011, 2013 Douglas Miller
// $Id: Wang_Debugger.java,v 1.1 2013/11/17 03:20:55 drmiller Exp $

interface Wang_Debugger
{
	public String disas(Wang_Core core, int pc); // disassemble microcode instruction at 'pc'

	// The addresses, lengths, and values for these are "natural" units,
	// either bytes or nibbles depending on the architecture model.
	// Since these are called from debugger, it is the user that
	// provided these values and must then know what units to assume.
	public String ramDump(Wang_Core core, int adr, int len);
	public void ramSet(Wang_Core core, int adr, byte val);
	public String romDump(Wang_Core core, int adr, int len);
	public String getRegisters(Wang_Core core);
	public String getMachine(Wang_Core core);
}
