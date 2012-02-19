# $Id: Makefile,v 1.24 2012/02/19 20:37:18 drmiller Exp $

INSNAME = wang700apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

MACOSX = mini-mac
SUBPATH = $(shell echo $${PWD\#$${HOME}/} )

PACKAGES = wang700apc.tgz wang700apc.dmg wang700apc.exe

COPTS =

VPATH = ../common

CFLAGS += -D__wang700__ -I../common -I.

OBJS = wang_sys.o wang_sim_cmd.o \
	w700_decode.o wang_exec.o wang_gui.o

TEST = $(shell echo -e this)
ifeq ($(TEST),this)
ECHOE = echo -e
else
ECHOE = echo
endif

FONTDIR = $(shell if [[ -d ~/.fonts ]]; then echo ~/.fonts; elif [[ -d ~/Library/Fonts ]]; then echo ~/Library/Fonts; fi)
FONTS = $(notdir $(wildcard ../common/fonts/*.ttf))
PNGS = $(wildcard icons/wang700-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS))
WICONS = $(subst .png,.ico,$(PNGS))

XTRA = 

DASM = dw700.o loaducode_txt.o

all: w700-sim w700_fe.class

CFLAGS += -g -O3 $(COPTS)

w700-dasm: wang-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w700-sim: wang-sim.o $(OBJS) $(XTRA) $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

w700_fe.class: w700_fe.java
	javac w700_fe.java

xinetd: w700-sim
	@echo "Ready for: cp $(PWD)/$? /usr/local/bin/wang"

clean:
	rm -f *.o

clobber: clean
	rm -f *.class w700-sim w700_fe w700-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

rom.diff: ROM720C.txt
	-cvs diff -u -r 1.1 ROM720C.txt >rom.diff

tar: rom.diff wang700.jar
	cd ..;\
	tar -czvf w700-sim/w700-sim.tgz \
		w700-sim/*.[ch] w700-sim/Makefile w700-sim/*.java \
		common/*.[ch] \
		w700-sim/README*  w700-sim/LICENSE* \
		w700-sim/rom.diff \
		w700-sim/icons/*.gif common/fonts/*.ttf \
		w700-sim/docs/*.html w700-sim/docs/*.gif

jar: wang700.jar

ship: tar
	mv -v w700-sim.tgz /http/durgadas.com/sebhc

Manifest.txt:
	echo "Main-Class: w700_fe" >$@

SHIP_DOCS = $(filter-out docs/wang700arch.html,$(wildcard docs/*.html))

wang700.jar: w700_fe.class Manifest.txt
	jar cfm $@ Manifest.txt *.class icons/*.{gif,png} $(SHIP_DOCS) \
		$(addprefix -C ../common/fonts ,$(FONTS))

update:
	cd ..; cvs -q update common w700-sim

packages: $(PACKAGES)

temp_dest: all $(ICNS) wang700.jar
	mkdir -p $(TMPDEST)
	cp wang700.jar w700-sim run INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/icons
	cp $(PNGS) $(TMPDEST)/icons
#	mkdir -p $(TMPDEST)/progs
#	cp progs/*.wng $(TMPDEST)/progs

wang700apc.tgz: temp_dest
	tar -czvf wang700apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

w700-sim.osx: w700-sim
	ssh $(MACOSX) cd $(SUBPATH)\; make clean update w700-sim
	scp $(MACOSX):$(SUBPATH)/w700-sim w700-sim.osx

# Need better way to estimate size of image...
wang700apc.dmg: all icns w700-sim.osx wang700.jar
	rm -f $@
	dd if=/dev/zero of=$@ bs=74k count=17
	hformat -l WANG700APC $@
	hmount $@
	hmkdir Wang700.app Wang700Files
	hcd Wang700.app
	hmkdir Contents
	hcd Contents
	hcopy wang700.plist.tmplt :Info.plist
	hmkdir Resources MacOS
	hcopy wang700.jar wang700.rom run :MacOS
	hcopy icons/wang700-*x*.icns :Resources
	hcd MacOS
	hcopy w700-sim.osx :w700-sim
	hcd
	hcopy Uninstall-wang700.sh :Uninstall-wang700
	hcopy -t README.osx :README.html
	humount $@
	rm -f w700-sim.osx
#	hcopy progs/*.wng :Wang700Files

wang700apc.exe: all winicons wang700.jar wang700.nsi
	makensis wang700.nsi

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cp ROM720C.txt $(INSDEST)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang700/wang700docs
	cp docs/*.html docs/*.gif /http/durgadas.com/wang700/wang700docs
	cp www/wang700* /http/durgadas.com/wang700

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
