# $Id: Makefile,v 1.40 2014/01/14 19:04:36 drmiller Exp $

include Makefile.ship

INSNAME = wang700apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

MACOSX = mini-mac
SUBPATH = $(shell echo $${PWD\#$${HOME}/} )

PACKAGES = wang700apc.tgz wang700apc.dmg wang700apc.exe
SRCTAR = w700-sim.tgz

PNGS = $(wildcard icons/wang700-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS) icons/setup.png)
WICONS = $(subst .png,.ico,$(PNGS))

EXE = w700-sim

w700ucode2rom: w700ucode2rom.c loaducode_txt.o
	$(CC) $(CFLAGS) -o $@ w700ucode2rom.c loaducode_txt.o

$(ROM): ROM720C.txt w700ucode2rom
	./w700ucode2rom ROM720C.txt
	mv ROM720C.rom $@

clobber: clean
	rm -f *.class w700_fe w700-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

rom.diff: $(ROM)
	-cvs diff -u -r 1.1 $(ROM) >rom.diff

tar: wang700.jar
	cd ..;\
	tar -czv --transform 's/.ship$$//' \
		-f w700-sim/$(SRCTAR) \
		w700-sim/Makefile.ship w700-sim/*.java \
		common/icons/*.png \
		w700-sim/README w700-sim/LICENSE \
		w700-sim/README.gui \
		w700-sim/progs/*.w7* w700-sim/progs/*.txt w700-sim/progs/*.wfl \
		w700-sim/icons/*.gif common/fonts/*.ttf \
		w700-sim/docs/*.html w700-sim/docs/*.gif

ship: tar packages
	mv -v $(SRCTAR) $(PACKAGES) /http/durgadas.com/sebhc/w700-sim

update:
	cd ..; cvs -q update common w700-sim

packages: $(PACKAGES)

temp_dest: all $(ICNS) wang700.jar
	mkdir -p $(TMPDEST)
	cp wang700.jar run edit INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/icons
	cp $(PNGS) $(TMPDEST)/icons
	cp $(ROM) $(TMPDEST)
	mkdir -p $(TMPDEST)/progs
	cp progs/*.w7* progs/*.txt progs/*.wfl $(TMPDEST)/progs

wang700apc.tgz: temp_dest
	tar -czvf wang700apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

# Need better way to estimate size of image...
wang700apc.dmg: all icns wang700.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=74k count=17
	hformat -l WANG700APC $@
	hmount $@
	hmkdir Wang700.app Wang700Files
	hcd Wang700.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang700.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang700.jar run edit :MacOS
	hcopy -r icons/wang700-*x*.icns :Resources
	hcd MacOS
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
	cp $(ROM) $(INSDEST)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang700/wang700docs
	cp docs/*.html docs/*.gif /http/durgadas.com/wang700/wang700docs
	cp www/wang700* /http/durgadas.com/wang700
	cp www/downloads/*.html www/downloads/.htaccess /http/durgadas.com/sebhc/w700-sim

reicon:
	cvs -q update icons
	$(MAKE) _icons
	cvs ci -m"update icons" icons

icns: $(ICNS)

winicons: $(WICONS)

_icons: $(ICNS) $(WICONS)

%.icns: %.png
	sips -s format icns $? --out $@

%.ico: %.png
	sips -s format ico $? --out $@

_FRC_:
