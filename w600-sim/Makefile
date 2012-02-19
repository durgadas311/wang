# $Id: Makefile,v 1.49 2012/02/19 23:17:50 drmiller Exp $

include Makefile.ship

INSNAME = wang600apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

MACOSX = mini-mac
SUBPATH = $(shell echo $${PWD\#$${HOME}/} )

PNGS = $(wildcard icons/wang600-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS))
WICONS = $(subst .png,.ico,$(PNGS))

PACKAGES = wang600apc.tgz wang600apc.dmg wang600apc.exe
SRCTAR = w600-sim.tgz

xinetd: w600-sim
	@echo "Ready for: cp $(PWD)/$? /usr/local/bin/wang"

update:
	cd ..; cvs -q update common w600-sim

clobber: clean
	rm -f *.class w600-sim w600_fe w600-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar:
	cd ..;\
	tar -czv --transform 's/.ship$$//' \
		-f w600-sim/$(SRCTAR) \
		w600-sim/*.[ch] w600-sim/Makefile.ship w600-sim/*.java \
		common/*.[ch] \
		w600-sim/wang600-block-diag.pdf w600-sim/README w600-sim/LICENSE* \
		w600-sim/README.{internals,gui} \
		w600-sim/wang600.rom w600-sim/icons/*.gif \
		w600-sim/icons/*.png common/fonts/*.ttf	\
		w600-sim/progs/*.wng w600-sim/docs/*.html w600-sim/docs/*.gif

ship: tar packages
	mv -v $(SRCTAR) $(PACKAGES) /http/durgadas.com/sebhc

packages: $(PACKAGES)

temp_dest: all wang600.jar
	mkdir -p $(TMPDEST)
	cp wang600.jar wang600.rom w600-sim run INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/progs
	cp progs/*.wng $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
	cp icons/wang600-*x*.png $(TMPDEST)/icons

wang600apc.tgz: temp_dest
	tar -czvf wang600apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

w600-sim.osx: w600-sim
	ssh $(MACOSX) cd $(SUBPATH)\; make clean update w600-sim
	scp $(MACOSX):$(SUBPATH)/w600-sim w600-sim.osx

# Need better way to estimate size of image...
wang600apc.dmg: all icns w600-sim.osx wang600.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=64k count=16
	hformat -l WANG600APC $@
	hmount $@
	hmkdir Wang600.app Wang600Files
	hcd Wang600.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang600.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang600.jar wang600.rom run :MacOS
	hcopy -r icons/wang600-*x*.icns :Resources
	hcd MacOS
	hcopy -r w600-sim.osx :w600-sim
	hcd
	hcopy -r Uninstall-wang600.sh :Uninstall-wang600
	hcopy -r README.osx :README.html
	hcopy -r progs/*.wng :Wang600Files
	humount $@
	rm -f w600-sim.osx

wang600apc.exe: all winicons wang600.jar wang600.nsi
	makensis wang600.nsi

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang600/wang600docs
	cp docs/*.html docs/*.gif /http/durgadas.com/wang600/wang600docs
	cp www/wang600* /http/durgadas.com/wang600

icns: $(ICNS)

%.icns: %.png
	sips -s format icns $? --out $@

winicons: $(WICONS)

%.ico: %.png
	sips -s format ico $? --out $@

_FRC_:
