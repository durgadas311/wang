# TODO: add Wang 700 Motherboard? ./wang700schematics/motherboard/Makefile...

SCHEMS = kicad/5929-core-drive/5929-core-drive.pdf \
	kicad/5969-wire-weave-rom/5969-wire-weave-rom.pdf

DOCS = wang_part_numbers.txt WangMagneticMemory.pdf

SHIPS = $(SCHEMS) $(DOCS)

all:

ship: $(SHIPS)
	rsync -Wuv $(SHIPS) durgadas.com:/var/www/html/sebhc/w600-sim/.
