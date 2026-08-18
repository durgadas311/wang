all: motherboard.pdf

motherboard-wire.pdf: motherboard-wire.png
	convert motherboard-wire.png $@

motherboard.pdf: motherboard-diag.pdf motherboard-wire.pdf
	pdftk motherboard-diag.pdf motherboard-wire.pdf cat output $@

ship: motherboard.pdf
	scp motherboard.pdf durgadas.com:/var/www/html/sebhc/w700-sim/
