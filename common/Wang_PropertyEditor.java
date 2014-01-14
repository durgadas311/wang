// Copyright (c) 2011,2014 Douglas Miller
// $Id: Wang_PropertyEditor.java,v 1.2 2014/01/14 21:53:51 drmiller Exp $

interface Wang_PropertyEditor
{

	// Assume properties have change, re-check everything for defaults
	void processDefaults();

	// Allow user to edit properties.
	// Return true if (any) properties were changed.
	boolean editPreferences();
}
