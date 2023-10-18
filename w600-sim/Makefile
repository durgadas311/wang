# $Id: Makefile,v 1.75 2014/01/25 23:50:35 drmiller Exp $
SHELL = /bin/bash

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
ICNS = $(subst .png,.icns,$(PNGS) icons/setup.png)
WICONS = $(subst .png,.ico,$(PNGS))

PACKAGES = wang600apc.tgz wang600apc.exe # wang600apc.dmg
SRCTAR = w600-sim.tgz

update:
#	cd ..; cvs -q update common w600-sim

clobber: clean
	rm -f *.class Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar:
	cd ..;\
	tar -czv --transform 's/.ship$$//' \
		-f w600-sim/$(SRCTAR) \
		w600-sim/Makefile.ship w600-sim/*.java common/*.java \
		wang600-docs/wang600-block-diag.pdf w600-sim/README w600-sim/LICENSE* \
		w600-sim/$(ROM) w600-sim/icons/*.gif \
		w600-sim/icons/*.png common/icons/*.png	\
		common/fonts/*.ttf	\
		w600-sim/progs/*.w6* w600-sim/progs/*.wfl w600-sim/progs/*.txt \
		w600-sim/docs/*.html w600-sim/docs/*.gif

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
	rsync -Wuv wang600.jar $(SRCTAR) $(PACKAGES) durgadas.com:/var/www/html/sebhc/w600-sim

packages: $(PACKAGES)

temp_dest: all wang600.jar
	mkdir -p $(TMPDEST)
	cp wang600.jar run edit INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/edit $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/progs
	cp progs/*.w6* progs/*.txt progs/*.wfl $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
	cp icons/wang600-*x*.png ../common/icons/WangX14Edit.png $(TMPDEST)/icons

wang600apc.tgz: temp_dest
	tar -czvf wang600apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

# Need better way to estimate size of image...
wang600apc.dmg: all icns wang600.jar wang600setup.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=64k count=19
	hformat -l WANG600APC $@
	hmount $@
	hmkdir Wang600.app Wang614.app Wang600Setup.app Wang600Files
	hcd Wang600.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang600.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang600.jar run :MacOS
	hcopy -r icons/wang600-*x*.icns :Resources
	hcd
	hcd Wang614.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang600edit.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r edit :MacOS
	hcopy -r ../common/icons/WangX14Edit-128x128.icns :Resources
	hcd
	hcd Wang600Setup.app
	hmkdir Contents
	hcd Contents
	hcopy -r wang600setup.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy -r wang600setup.jar init :MacOS
	hcopy -r icons/setup.icns :Resources
	hcd
	hcopy -r Uninstall-wang600.sh :Uninstall-wang600
	hcopy -r README.osx :README.html
	hcopy -r progs/*.w6* progs/*.txt progs/*.wfl :Wang600Files
	humount $@

wang600apc.exe: all winicons wang600.jar wang600.nsi
	makensis wang600.nsi

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

wpcc.tgz: _FRC_
	rm -rf temp_wpcc
	mkdir -p temp_wpcc/wpcc/{www,examples{600,700}}
	$(MAKE) -C wpcc export DEST=${PWD}/temp_wpcc/wpcc
	make -C progs/src export DEST=${PWD}/temp_wpcc/wpcc/examples600
	make -C ../w700-sim/progs/src export DEST=${PWD}/temp_wpcc/wpcc/examples700
	tar -czvf $@ -C temp_wpcc wpcc
	rm -rf temp_wpcc

www: _FRC_
	rsync -Wuv docs/*.html docs/*.gif durgadas.com:/var/www/html/wang600/wang600docs
	rsync -Wuv www/wang6* durgadas.com:/var/www/html/wang600
	rsync -Wuv www/downloads/*.html www/downloads/.htaccess durgadas.com:/var/www/html/sebhc/w600-sim

icns: $(ICNS)

%.icns: %.png
	false
#	sips -s format icns $? --out $@

winicons: $(WICONS)

%.ico: %.png
	convert $? -define icon:auto-resize=256,64,48,32,16 $@
#	sips -s format ico $? --out $@

_FRC_:
