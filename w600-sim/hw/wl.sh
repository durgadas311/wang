#!/bin/bash

for i in "${@}"; do
	awk "\$4==\"${i}\"{
		if (\$2 == \".\") print \$1 \".\" \$3;
		else print \$1 \".\" \$2 \".\" \$3;
	}" *.wl
done
