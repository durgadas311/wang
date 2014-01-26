// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_RunCommand.java,v 1.2 2014/01/26 14:52:57 drmiller Exp $

import java.io.*;
import java.util.Arrays;

public class Wang_RunCommand
{
	final String ident = "$Id: Wang_RunCommand.java,v 1.2 2014/01/26 14:52:57 drmiller Exp $";

	static private boolean _init = false;
	static private boolean _cygwin;
	static private String[] _shell;
	static private boolean _windows;

	public static void Initialize() {
		if (_init) {
			return;
		}
		_init = true;
		_cygwin = false;
		_windows = (System.getProperty("os.name").indexOf("Windows") >= 0);
		if (_windows) {
			File shell = new File("c:\\cygwin64\\bin\\bash.exe");
			if (!shell.exists()) {
				shell = new File("c:\\cygwin32\\bin\\bash.exe");
			}
			if (!shell.exists()) {
				shell = new File("c:\\cygwin\\bin\\bash.exe");
			}
			if (shell.exists()) {
				_shell = new String[]{ shell.getAbsolutePath(),
						"--login", "-i", "-c" };
				_cygwin = true;
				_windows = false; // for all intents and purposes?
			} else {
				_shell = new String[]{ "cmd.exe", "/c" };
			}
		} else {
			String sh = System.getenv("SHELL");
			if (sh == null) {
				// what else to do?
				_shell = new String[]{ "sh", "-c" };
			} else {
				_shell = new String[]{ sh, "-c" };
			}
		}
	}

	// run command using same stdio as calling process...
	public static int runCommand(String cmd) {
		int x = -1;
		try {
			String[] args = Arrays.copyOf(_shell, _shell.length + 1);
			args[_shell.length] = cmd;
			ProcessBuilder pcmd = new ProcessBuilder(args);
			// eventually want: (but need Java 7)
			//pcmd.inheritIO();
			// instead have to get stream and copy to stdout...
			// yuk! plus can't handle stdin. would require 3 threads
			// to connect stdin, stdout, stderr between new process
			// and Runtime.
			// Could append/prepend shell redirection for "/dev/tty"
			// but that does not work on Windows. Also does not work
			// for stdin (for some reason). Things like "more" just
			// seem to get EOF.
			pcmd.redirectErrorStream(true);
			Process proc = pcmd.start();
			java.io.InputStream outf = proc.getInputStream();
			byte[] buf = new byte[256];
			while (outf != null) {
				try {
					int n = outf.read(buf);
					if (n > 0) {
						System.out.write(Arrays.copyOfRange(buf, 0, n));
					} else if (n < 0) {
						outf.close();
						outf = null;
						// do not destroy on EOF, process might
						// otherwise exit normally.
						//proc.destroy();
					}
				} catch(Exception ee) {
					outf.close();
					outf = null;
					proc.destroy();
				}
			}

			x = proc.waitFor();
			if (_cygwin && x == 1) { x = 0; } // todo: investigate this
		} catch(Exception ee) {
			x = 1;
		}
		return x;
	}

	// run command with stdout+stderr collected in "out[0]".
	// Also, on failure additional error messages may be appended to "out[1]".
	// "out" must have been initialized to (at least) new String[2] before call.
	// This is intended for short-running, deterministic, commands producing
	// little (or no) output. Does not support interactive commands (stdin).
	public static int runCommand(String cmd, String[] out) {
		out[0] = new String();
		out[1] = new String();
		int x = -1;
		try {
			String[] args = Arrays.copyOf(_shell, _shell.length + 1);
			args[_shell.length] = cmd;
			ProcessBuilder pcmd = new ProcessBuilder(args);
			// should keep stderr separate, but that requires an
			// additional thread in order to allow asynchronous operation
			// of stdout/stderr.
			pcmd.redirectErrorStream(true);
			Process proc = pcmd.start();
			java.io.InputStream outf = proc.getInputStream();
			byte[] buf = new byte[256];
			while (outf != null) {
				// assume both close/fail together...
				try {
					int n = outf.read(buf);
					if (n > 0) {
						out[0] += new String(Arrays.copyOfRange(buf, 0, n));
					} else if (n < 0) {
						outf.close();
						outf = null;
						// destroying process here (EOF) causes
						// return value to be non-zero...
						// even if process just exited normally.
						//proc.destroy();
					}
				} catch(Exception ee) {
					// this indicates severe system fault,
					// so go ahead and destroy process.
					outf.close();
					outf = null;
					proc.destroy();
				}
			}
			x = proc.waitFor();
			if (_cygwin && x == 1) { x = 0; } // todo: investigate this
			if (x != 0) {
				out[1] += "Exited " + Integer.toString(x);
			}
		} catch(Exception ee) {
			x = 1;
			out[1] += ee.toString();
		}
		return x;
	}

	public static boolean isWindows() { return _windows; }

}
