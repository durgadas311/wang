#!/bin/sh
# $Id: Uninstall-wang700.sh,v 1.1 2012/02/19 20:37:18 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

if [[ -d "${HOME}/Desktop/Wang700.app" ]]; then
	echo "Removing Wang700 App..."
	chmod -R u+w "${HOME}/Desktop/Wang700.app"
	rm -rf "${HOME}/Desktop/Wang700.app"
fi
