#!/bin/sh
# $Id: Uninstall-wang600.sh,v 1.2 2012/02/19 02:41:35 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

if [[ -d "${HOME}/Desktop/Wang600.app" ]]; then
	echo "Removing Wang600 App..."
	chmod -R u+w "${HOME}/Desktop/Wang600.app"
	rm -rf "${HOME}/Desktop/Wang600.app"
fi
