#!/bin/sh
# $Id: Uninstall-wang600.sh,v 1.1 2012/02/19 01:51:10 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

if [[ -d "${HOME}/Desktop/Wang600.app" ]]; then
	echo "Removing Wang600 App..."
	rm -rf "${HOME}/Desktop/Wang600.app"
fi
