# $Id: Makefile,v 1.42 2014/01/25 23:52:27 drmiller Exp $
SHELL = /bin/bash

include Makefile.ship

INSNAME = wang700apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

MACOSX = mini-mac
SUBPATH = $(shell echo $${PWD\#$${HOME}/} )

PACKAGES = wang700apc.tgz wang700apc.exe # wang700apc.dmg
SRCTAR = w700-sim.tgz

PNGS = $(wildcard icons/wang700-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS) icons/setup.png)
WICONS = $(subst .png,.ico,$(PNGS))

EXE = w700-sim

w700ucode2rom: w700ucode2rom.c loaducode_txt.o
	$(CC) $(CFLAGS) -o $@ w700ucode2rom.c loaducode_txt.o

wang720c.rom: ROM720C.txt w700ucode2rom
	./w700ucode2rom ROM720C.txt
	mv ROM720C.rom $@

wang720b.rom: ROM720B.txt w700ucode2rom
	./w700ucode2rom ROM720B.txt
	mv ROM720B.rom $@

wang720a.rom: ROM720A.txt w700ucode2rom
	./w700ucode2rom ROM720A.txt
	mv ROM720A.rom $@

wang700c.rom: ROM700C.txt w700ucode2rom
	./w700ucode2rom ROM700C.txt
	mv ROM700C.rom $@

clobber: clean
	rm -f *.class w700_fe w700-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar: wang700.jar
	cd ..;\
	tar -czv --transform 's/.ship$$//' \
		-f w700-sim/$(SRCTAR) \
		w700-sim/Makefile.ship w700-sim/*.java w700-sim/*.rom \
		common/*.java common/icons/*.png \
		w700-sim/README w700-sim/LICENSE \
		w700-sim/README.gui \
		w700-sim/progs/*.w7* w700-sim/progs/*.txt w700-sim/progs/*.wfl \
		w700-sim/icons/*.gif common/fonts/*.ttf common/plotter_chrgen.dat \
		w700-sim/docs/*.html w700-sim/docs/*.gif

ifeq ($(MAKECMDGOALS),ship)
STATE = Rel
else
STATE = Exp
endif

Release.java: _FRC_
#	@if [[ ! -f $@ ]]; then cvs -Q update $@; fi
ifeq ($(MAKECMDGOALS),ship)
#	@cvs commit -f -m. $@
endif
#	@if ! grep -s -q 'State: $(STATE)' $@; then \
#		cvs admin -s $(STATE) $@; \
#		rm -f $@; \
#	fi
#	@cvs -Q update $@

ship: tar packages www
	rsync -Wuv wang700.jar $(SRCTAR) $(PACKAGES) durgadas.com:/var/www/html/sebhc/w700-sim

update:
#	cd ..; cvs -q update common w700-sim

packages: $(PACKAGES)

temp_dest: all $(ICNS) wang700.jar
	mkdir -p $(TMPDEST)
	cp wang700.jar run edit INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/icons
	cp $(PNGS) ../common/icons/WangX14Edit.png $(TMPDEST)/icons
	cp $(ROMS) $(TMPDEST)
	mkdir -p $(TMPDEST)/progs
	cp progs/*.w7* progs/*.txt progs/*.wfl $(TMPDEST)/progs

wang700apc.tgz: temp_dest
	tar -czvf wang700apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

# Need better way to estimate size of image...
wang700apc.dmg: all icns wang700.jar wang700setup.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=74k count=17
	hformat -l WANG700APC $@
	hmount $@
	hmkdir Wang700.app Wang714.app Wang700Setup.app Wang700Files
	hcd Wang700.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang700.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang700.jar run :MacOS
	hcopy -r icons/wang700-*x*.icns :Resources
	hcd
	hcd Wang714.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang700edit.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r edit :MacOS
	hcopy -r ../common/icons/WangX14Edit-128x128.icns :Resources
	hcd
	hcd Wang700Setup.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang700setup.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang700setup.jar init :MacOS
	hcopy -r icons/setup.icns :Resources
	hcd
	hcopy -r Uninstall-wang700.sh :Uninstall-wang700
	hcopy -r README.osx :README.html
	hcopy progs/*.w7* progs/*.txt progs/*.wfl :Wang700Files
	humount $@

wang700apc.exe: all winicons wang700.jar wang700.nsi
	makensis wang700.nsi

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cp $(ROMS) $(INSDEST)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	rsync -Wuv docs/*.html docs/*.gif durgadas.com:/var/www/html/wang700/wang700docs
	rsync -Wuv www/wang700* durgadas.com:/var/www/html/wang700
	rsync -Wuv www/downloads/*.html www/downloads/.htaccess durgadas.com:/var/www/html/sebhc/w700-sim

reicon:
#	cvs -q update icons
	$(MAKE) _icons
#	cvs ci -m"update icons" icons

icns: $(ICNS)

winicons: $(WICONS)

_icons: $(ICNS) $(WICONS)

%.icns: %.png
	false
#	sips -s format icns $? --out $@

%.ico: %.png
	convert $? -define icon:auto-resize=256,64,48,32,16 $@
#	sips -s format ico $? --out $@

_FRC_:
