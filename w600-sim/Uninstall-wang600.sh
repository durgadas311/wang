#!/bin/sh
# $Id: Uninstall-wang600.sh,v 1.3 2014/01/16 13:00:08 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

for app in Wang600.app Wang614.app; do
	if [[ -d "${HOME}/Desktop/${app}" ]]; then
		echo "Removing ${app}..."
		chmod -R u+w "${HOME}/Desktop/${app}"
		rm -rf "${HOME}/Desktop/${app}"
	fi
done
