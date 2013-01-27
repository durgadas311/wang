// Copyright (c) 2011,2012 Douglas Miller
// $Id: Wang_PropertyEditor.java,v 1.1 2013/01/27 17:05:15 drmiller Exp $

interface Wang_PropertyEditor
{

	// Assume properties have change, re-check everything for defaults
	void processDefaults();

	// Allow user to edit properties.
	// Return true if (any) properties were changed.
	boolean editPreferences();
}
