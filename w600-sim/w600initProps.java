// Copyright (c) 2011,2014 Douglas Miller
// $Id: w600initProps.java,v 1.1 2014/01/13 17:49:05 drmiller Exp $

public class w600initProps
{
	final String ident = "$Id: w600initProps.java,v 1.1 2014/01/13 17:49:05 drmiller Exp $";

	public static void main(String[] args) {
		Wang600_Properties props = new Wang600_Properties();
		if (props.isNew()) {
			System.err.println("Initializing Wang600_Properties file from defaults");
			try {
				props.save();
			} catch (Exception ee) {
				System.err.println(ee.toString());
				System.exit(1);
			}
		} else if (props.isDirty()) {
			System.err.println("Updating Wang600_Properties file");
			try {
				props.save();
			} catch (Exception ee) {
				System.err.println(ee.toString());
				System.exit(1);
			}
		}
	}
}
