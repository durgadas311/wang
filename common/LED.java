// Copyright (c) 2017 Douglas Miller <durgadas311@gmail.com>

import java.awt.*;
import javax.swing.*;

public abstract class LED extends JPanel {
	public static enum Colors { RED, AMBER, YELLOW, GREEN,
				INCAND, NEON };
	private static final Color btnRedOff = new Color(50, 0, 0);
	private static final Color btnRedOn = new Color(255, 0, 0);
	private static final Color btnAmberOff = new Color(80,60, 0);
	private static final Color btnAmberOn = new Color(255,190, 0);
	private static final Color btnYellowOff = new Color(80,80, 0);
	private static final Color btnYellowOn = new Color(255,255, 0);
	private static final Color btnGreenOff = new Color(0, 80, 0);
	private static final Color btnGreenOn = new Color(0, 255, 0);
	private static final Color btnIncandOff = new Color(150, 150, 150);
	private static final Color btnIncandOn = new Color(255, 255, 200);
	private static final Color btnNeonOff = new Color(80, 80, 80);
	private static final Color btnNeonOn = new Color(244, 157, 33);

	protected Color off;
	protected Color on;

	public LED(Colors color) {
		super();
		if (color == null) {
			color = Colors.RED;
		}
		switch (color) {
		case RED:
			off = btnRedOff;
			on = btnRedOn;
			break;
		case AMBER:
			off = btnAmberOff;
			on = btnAmberOn;
			break;
		case YELLOW:
			off = btnYellowOff;
			on = btnYellowOn;
			break;
		case GREEN:
			off = btnGreenOff;
			on = btnGreenOn;
			break;
		case INCAND:
			off = btnIncandOff;
			on = btnIncandOn;
			break;
		case NEON:
			off = btnNeonOff;
			on = btnNeonOn;
			break;
		}
	}

	public abstract void set(boolean onf);
}
