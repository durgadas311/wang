#!/bin/sh
# $Id: Uninstall-wang1200.sh,v 1.1 2012/02/19 17:43:51 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

if [[ -d "${HOME}/Desktop/Wang1200.app" ]]; then
	echo "Removing Wang1200 App..."
	chmod -R u+w "${HOME}/Desktop/Wang1200.app"
	rm -rf "${HOME}/Desktop/Wang1200.app"
fi
