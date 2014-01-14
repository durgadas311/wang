// Copyright (c) 2011,2014 Douglas Miller
// $Id: w600initProps.java,v 1.2 2014/01/14 19:04:36 drmiller Exp $

public class w600initProps
{
	final String ident = "$Id: w600initProps.java,v 1.2 2014/01/14 19:04:36 drmiller Exp $";

	public static void main(String[] args) {
		boolean gui = (args.length > 0 && args[0].compareTo("-g") == 0);
		if (gui) {
			Wang_UI.Initialize();
		}
		String title = "Wang 600 Setup";
		String err = null;
		String info = null;
		Wang600_Properties props = new Wang600_Properties();
		if (props.isNew()) {
			info = "Initializing Properties file from defaults";
			try {
				props.save();
			} catch (Exception ee) {
				err = ee.toString();
			}
		} else if (props.isDirty()) {
			info = "Updating Properties file";
			try {
				props.save();
			} catch (Exception ee) {
				err = ee.toString();
			}
		}
		if (err != null) {
			if (info != null) {
				err = info + ": " + err;
			}
			if (gui) {
				Wang_UI.fatal(title, err);
			} else {
				System.err.println(err);
				System.exit(1);
			}
		} else if (info != null) {
			if (gui) {
				Wang_UI.inform(title, info);
			} else {
				System.out.println(info);
			}
		} else if (gui) {
			Wang_UI.inform(title, "Properties file is up to date");
		}
	}
}
