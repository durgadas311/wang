#!/bin/sh
# $Id: Uninstall-wang700.sh,v 1.2 2014/01/16 13:00:08 drmiller Exp $

# paths might contain blanks!
cd "${HOME}"

for app in Wang700.app Wang714.app; do
	if [[ -d "${HOME}/Desktop/${app}" ]]; then
		echo "Removing ${app}..."
		chmod -R u+w "${HOME}/Desktop/${app}"
		rm -rf "${HOME}/Desktop/${app}"
	fi
done
