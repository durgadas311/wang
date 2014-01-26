// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_DebugConsole.java,v 1.10 2014/01/26 14:52:57 drmiller Exp $

import java.io.*;
import java.util.Arrays;

class Wang_DebugConsole
{
	private Wang_Debugger _dbg;
	private BufferedReader _in;

	private abstract class DbgFunc {
		abstract int do_cmd(String[] line);
	}
	private class DbgCmd {
		public String cmd;
		public String args;
		public String help;
		public DbgFunc fnc;
		public DbgCmd(String c, String a, String h, DbgFunc f) {
			cmd = c;
			args = a;
			help = h;
			fnc = f;
		}
	}

	public Wang_DebugConsole(Wang_Debugger dbg) {
		Wang_RunCommand.Initialize();
		_dbg = dbg;
		_in = new BufferedReader(new InputStreamReader(System.in));
	}

	private DbgCmd[] commands = new DbgCmd[]{
		new DbgCmd( "quit", null, "End simulation",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				return 1;
			}
		}
		),
		new DbgCmd( "help", null, "Display this help",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int x;
				int m = 0;
				int n;
				for (x = 0; x < commands.length; ++x) {
					n = commands[x].cmd.length();
					if (commands[x].args != null) {
						n += commands[x].args.length();
					}
					if (n > m) {
						m = n;
					}
				}
				System.out.format("Wang %s00 Simulator Commands:\n", Wang_UI.getSeries());
				for (x = 0; x < commands.length; ++x) {
					n = commands[x].cmd.length();
					System.out.format("  %s %-" + (m - n) + "s %s\n",
						commands[x].cmd,
						commands[x].args != null ? commands[x].args : "",
						commands[x].help);
				}
				String s = "<cmd>";
				n = 1;
				System.out.format("  ! %-" + (m - n) + "s Execute cmd in shell\n", s);
				return 0;
			}
		}
		),
		new DbgCmd( "dump", null, "Dump processor state/registers",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int pc = _dbg.getPC();
				System.out.format("%9d %03x: %s\n",
					_dbg.getCycles(), pc, _dbg.disas(pc, true));
				System.out.format("%s", _dbg.getRegisters());
				System.out.format("[%s]\n", _dbg.getMachine());
				return 0;
			}
		}
		),
		new DbgCmd( "disas", "[addr [instrs]]", "Disassemble ucode ROM at PC [or hex addr]",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int pc = _dbg.getPC();
				int len = 16;
				int max = _dbg.getUcodeSize();

				if (line.length > 1) {
					if (!line[1].equals(".")) {
						pc = Integer.valueOf(line[1], 16);
					}
					if (line.length > 2) {
						len = Integer.valueOf(line[2]);
					}
				}
				pc &= (max - 1);
				if (max - pc < len) len = max - pc;

				while (len > 0) {
					System.out.format("%03x: %s\n", pc, _dbg.disas(pc, true));
					--len;
					++pc;
				}
				return 0;
			}
		}
		),
		new DbgCmd( "exam", "[addr [words]]", "Examine RAM at L,M,N [or hex addr]",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int adr = _dbg.getRamAdr();
				int len = 256;

				if (line.length > 1) {
					if (!line[1].equals(".")) {
						adr = Integer.valueOf(line[1], 16);
					}
					if (line.length > 2) {
						len = Integer.valueOf(line[2]);
					}
				}
				System.out.format("%s", _dbg.ramDump(adr, len));
				return 0;
			}
		}
		),
		new DbgCmd( "set", "reg=value [...]", "Set register(s)",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				for (int x = 1; x < line.length; ++x) {
					int q = line[x].indexOf('=');
					if (q == -1) {
						System.out.format("'set' sytax error at \"%s\"\n", line[x]);
						break;
					}
					String reg = line[x].substring(0, q);
					int val = Integer.valueOf(line[x].substring(q + 1), 16);
					if (_dbg.setReg(reg, val) == -1) {
						System.out.format("Unknown register \"%s\"\n", reg);
						break;
					}
					System.out.format("%s = %x\n", reg, val);
				}
				return 0;
			}
		}
		),
		new DbgCmd( "store", "[@addr] value...", "Store hex val(s) in RAM at L,M,N [or hex addr]",
new DbgFunc() {
			public int do_cmd(String[] line) {
				int adr = _dbg.getRamAdr();

				if (line.length > 1) {
					int x = 1;
					if (line[x].indexOf('@') == 0) {
						adr = Integer.valueOf(line[x].substring(1), 16);
						++x;
					}
					while (x < line.length) {
						int v = Integer.valueOf(line[x], 16);
						_dbg.ramSet(adr, (byte)v);
						++adr;
						++x;
					}
				}
				return 0;
			}
		}
		),
		new DbgCmd( "step", null, "Single-step one instruction",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				_dbg.relCycleLimit(1);
				_dbg.setRun(true);
				return 0;
			}
		}
		),
		new DbgCmd( "core", "file", "Dump all of RAM (2K) to <file>",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				if (line.length > 1) {
					try {
						FileOutputStream file =
							new FileOutputStream(line[1]);
						_dbg.core(file);
					} catch(Exception ee) {
						System.out.format("Can't create/write file \"%s\": %s\n", line[1], ee.getMessage());
					}
				}
				return 0;
			}
		}
		),
		new DbgCmd( "break", "[addr ...]", "Set/Clear/Show one-shot breakpoint(s)",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int pc = _dbg.getPC();
				int max = _dbg.getUcodeSize();

				int n = 0;
				for (int x = 1; x < line.length; ++x) {
					pc = Integer.valueOf(line[x], 16);
					++n;
					if (pc < max) {
						boolean bp = _dbg.breakPoint(pc);
						System.out.format("%03x: breakpoint %s\n", pc, bp ? "on" : "off");
					} else {
						System.out.format("%03x: out of bounds\n", pc);
					}
				}
				if (n == 0) {
					for (pc = 0; pc < max; ++pc) {
						boolean bp = _dbg.getBreakPoint(pc);
						if (bp) {
							if (n == 0) System.out.format("Breakpoints at:");
							System.out.format(" %03x", pc);
							++n;
						}
					}
					if (n == 0) System.out.format("no breakpoints");
					System.out.format("\n");
				}
				return 0;
			}
		}
		),
		new DbgCmd( "go", "[+cycles]", "Resume program at current PC [break after <cycles>]",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				if (line.length > 1) {
					if (line[1].indexOf('+') == 0) {
						long n = Integer.valueOf(line[1].substring(1));
						long limit = _dbg.relCycleLimit(n);
						System.out.format("breakpoint at %d cycles (now + %d)\n",
							limit, n);
					}
				}
				_dbg.setRun(true);
				System.out.format("resuming at %03x\n", _dbg.getPC());
				return 0;
			}
		}
		),
		new DbgCmd( "trace", "[file]", "Set trace [off | {on|<file>} [cycles] [raw]]",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				for (int x = 1; x < line.length; ++x) {
					if (line[x].equals("on")) {
						try {
							_dbg.setTrace(true);
						} catch(Exception ee) {}
					} else if (line[x].equals("off")) {
						try {
							_dbg.setTrace(false);
						} catch(Exception ee) {}
					} else if (line[x].equals("cycles")) {
							_dbg.setTraceCycles(true);
					} else if (line[x].equals("raw")) {
							_dbg.setTraceRaw(true);
					} else {
						try {
							FileOutputStream file =
								new FileOutputStream(line[x]);
							_dbg.setTraceFile(file);
						} catch(Exception ee) {
							System.out.format("Can't create/close trace file: %s\n", ee.getMessage());
						}
					}
				}
				System.out.println(_dbg.getTrace());
				return 0;
			}
		}
		),
		// these two should be conditionally added based on getXRomSize()
		new DbgCmd( "rom", "[addr [words]]", "Examine ROM at L,M,N [or hex addr]",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				int adr = _dbg.getRamAdr();
				int len = 256;

				if (line.length > 1) {
					if (!line[1].equals(".")) {
						adr = Integer.valueOf(line[1], 16);
					}
					if (line.length > 2) {
						len = Integer.valueOf(line[2]);
					}
				}
				System.out.format("%s", _dbg.romDump(adr, len));
				return 0;
			}
		}
		),
		new DbgCmd( "dup", null, "Copy program space into ROM",
		new DbgFunc() {
			public int do_cmd(String[] line) {
				_dbg.dup();
				return 0;
			}
		}
		),
	};

	public int command() {
		int x;
		int rc = 0;

		System.out.format("%% ");
		String s;
		try {
			s = _in.readLine();
		} catch(Exception ee) {
			s = null;
		}
		if (s == null) {
			System.out.format("Wang %s00 Simulation done.\n", Wang_UI.getSeries());
			return 1;
		}
		if (s.length() == 0) {
			return 0;
		}
		if (s.substring(0, 1).equals("!")) {
			x = Wang_RunCommand.runCommand(s.substring(1));
			if (x != 0) {
				// does the user already know it failed?
				System.out.println("! command failed");
			}
			return 0;
		}

		String[] l = s.split("[ \t]");
		if (l.length == 0) {
			return 0;
		}
		for (x = 0; x < commands.length; ++x) {
			if (l[0].equals(commands[x].cmd)) {
				rc = commands[x].fnc.do_cmd(l);
				break;
			}
		}
		if (!(x < commands.length)) {
			System.out.format("%s ?\n", s);
			return 0;
		}
		if (rc != 0) {
			System.out.format("%s00 Simulation done.\n", Wang_UI.getSeries());
			return 1;
		}
		return 0;
	}

	public void instr_trace() {
		try {
			_dbg.putTrace();
		} catch(Exception ee) {
			// either abort or ignore  - can't print message and continue
			// or flood will ensue.
		}
	}

	public void warp(String tag, int next, int cycles) {
		try {
			_dbg.putWarp(tag, next, cycles);
		} catch(Exception ee) {
			// either abort or ignore  - can't print message and continue
			// or flood will ensue.
		}
	}
}
