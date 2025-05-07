// Copyright (c) 2025 Douglas Miller <durgadas311@gmail.com>

import java.util.Properties;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Wang600asm {
	private Wang600Assembler front_end;

	public Wang600asm(String[] args) {
		front_end = new Wang600Assembler(args);
	}

	public static void main(String[] args) {
		new Wang600asm(args);
	}
}
