# $Id: Makefile,v 1.25 2013/12/10 01:43:56 drmiller Exp $

include Makefile.ship

INSNAME = wang1200wps
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

SUBPATH = $(shell echo $${PWD\#$${HOME}/} )

PACKAGES = wang1200wps.tgz wang1200wps.dmg wang1200wps.exe
SRCTAR = w1200-sim.tgz

PNGS = $(wildcard icons/wang1200-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS))
WICONS = $(subst .png,.ico,$(PNGS))

clobber: clean
	rm -f *.class Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar:
	cd ..;\
	tar -czv --transform 's/.ship$$//' \
		-f w1200-sim/$(SRCTAR) \
		w1200-sim/Makefile.ship w1200-sim/*.java common/*.java \
		w1200-sim/README w1200-sim/LICENSE* \
		w1200-sim/INSTALL w1200-sim/run w1200-sim/txt2wng \
		w1200-sim/examples/*.wpt \
		w1200-sim/$(ROM) w1200-sim/icons/*.gif \
		w1200-sim/icons/*.png common/icons/*.png \
		w1200-sim/docs/*.html w1200-sim/docs/*.gif
#		w1200-sim/docs/*.pdf

ship: tar packages
	mv -v $(SRCTAR) $(PACKAGES) /http/durgadas.com/sebhc/w1200-sim

packages: $(PACKAGES)

temp_dest: all wang1200.jar
	mkdir -p $(TMPDEST)
	cp wang1200.jar run txt2wng INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/txt2wng $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/examples
	cp examples/*.wpt $(TMPDEST)/examples
	mkdir -p $(TMPDEST)/icons
	cp icons/wang1200-*x*.png $(TMPDEST)/icons
#	mkdir -p $(TMPDEST)/docs
#	cp docs/*.pdf $(TMPDEST)/docs

wang1200wps.tgz: temp_dest
	tar -czvf wang1200wps.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

wang1200wps.exe: all winicons wang1200.jar wang1200.nsi
	makensis wang1200.nsi

# Need better way to estimate size of image...
wang1200wps.dmg: all icns wang1200.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=64k count=16
	hformat -l WANG1200WPS $@
	hmount $@
	hmkdir Wang1200.app Wang1200Files
	hcd Wang1200.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang1200.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang1200.jar run txt2wng :MacOS
	hcopy -r icons/wang1200-*x*.icns :Resources
	hcd
	hcopy -r Uninstall-wang1200.sh :Uninstall-wang1200
	hcopy -r README :README.html
	hcopy -r examples/*.wpt :Wang1200Files
	humount $@

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang1200/wang1200docs
	cp docs/*.html docs/*.gif /http/durgadas.com/wang1200/wang1200docs
	cp www/wang1200* /http/durgadas.com/wang1200
	cp www/downloads/*.html www/downloads/.htaccess /http/durgadas.com/sebhc/w1200-sim

icns: $(ICNS)

%.icns: %.png
	sips -s format icns $? --out $@

winicons: $(WICONS)

%.ico: %.png
	sips -s format ico $? --out $@

_FRC_:
