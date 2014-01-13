// Copyright (c) 2011,2014 Douglas Miller
// $Id: w700initProps.java,v 1.1 2014/01/13 17:49:05 drmiller Exp $

public class w700initProps
{
	final String ident = "$Id: w700initProps.java,v 1.1 2014/01/13 17:49:05 drmiller Exp $";

	public static void main(String[] args) {
		Wang700_Properties props = new Wang700_Properties();
		if (props.isNew()) {
			System.err.println("Initializing Wang700_Properties file from defaults");
			try {
				props.save();
			} catch (Exception ee) {
				System.err.println(ee.toString());
				System.exit(1);
			}
		} else if (props.isDirty()) {
			System.err.println("Updating Wang700_Properties file");
			try {
				props.save();
			} catch (Exception ee) {
				System.err.println(ee.toString());
				System.exit(1);
			}
		}
	}
}
