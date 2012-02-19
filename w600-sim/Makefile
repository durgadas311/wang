# $Id: Makefile,v 1.44 2012/02/19 02:42:57 drmiller Exp $

INSNAME = wang600apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

COPTS =

VPATH = ../common

CFLAGS += -D__wang600__ -I../common -I.

OBJS = wang_sys.o wang_sim_cmd.o \
	w600_decode.o wang_exec.o wang_gui.o

TEST = $(shell echo -e this)
ifeq ($(TEST),this)
ECHOE = echo -e
else
ECHOE = echo
endif

FONTDIR = $(shell if [[ -d ~/.fonts ]]; then echo ~/.fonts; elif [[ -d ~/Library/Fonts ]]; then echo ~/Library/Fonts; fi)
FONTDEST =
FONTS = $(notdir $(wildcard ../common/fonts/*.ttf))
PNGS = $(wildcard icons/wang600-*x*.png)
PNGS := $(filter %32x32.png %48x48.png %128x128.png, $(PNGS))
# sips expected: 16, 32, 48, 128, 256, 512, or 1024
ICNS = $(subst .png,.icns,$(PNGS))
WICONS = $(subst .png,.ico,$(PNGS))

PACKAGES = wang600apc.tgz wang600apc.dmg wang600apc.exe

XTRA = 

DASM = dw600.o loaducode_txt.o

all: w600-sim w600_fe.class

CFLAGS += -g -O3 $(COPTS)

w600-dasm: wang-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w600-sim: wang-sim.o $(OBJS) $(XTRA) $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

w600_fe.class: w600_fe.java
	javac w600_fe.java

xinetd: w600-sim
	@echo "Ready for: cp $(PWD)/$? /usr/local/bin/wang"

clean:
	rm -f *.o

clobber: clean
	rm -f *.class w600-sim w600_fe w600-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar:
	cd ..;\
	tar -czvf w600-sim/w600-sim.tgz \
		w600-sim/*.[ch] w600-sim/Makefile w600-sim/*.java \
		common/*.[ch] \
		w600-sim/wang600-block-diag.pdf w600-sim/README*  w600-sim/LICENSE* \
		w600-sim/wang600.rom w600-sim/icons/*.gif \
		w600-sim/icons/*.png common/fonts/*.ttf	\
		w600-sim/progs/*.wng w600-sim/docs/*.html w600-sim/docs/*.gif

jar: wang600.jar

ship: tar
	mv -v w600-sim.tgz /http/durgadas.com/sebhc

Manifest.txt:
	echo "Main-Class: w600_fe" >$@

SHIP_DOCS = $(filter-out docs/wang600arch.html,$(wildcard docs/*.html))

wang600.jar: w600_fe.class Manifest.txt
	jar cfm $@ Manifest.txt *.class icons/*.{gif,png} $(SHIP_DOCS) \
		$(addprefix -C ../common/fonts ,$(FONTS))

packages: $(PACKAGES)

temp_dest: all wang600.jar
	mkdir -p $(TMPDEST)
	cp wang600.jar wang600.rom w600-sim run INSTALL UNINSTALL *.desktop.tmplt $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL $(TMPDEST)/UNINSTALL
	mkdir -p $(TMPDEST)/progs
	cp progs/*.wng $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
	cp icons/wang600-*x*.png $(TMPDEST)/icons

temp_app: all icns wang600.jar
	mkdir -p $(TMPDEST)/Wang600.app/Contents/{Resources,MacOS}
	cp wang600.jar wang600.rom w600-sim run $(TMPDEST)/Wang600.app/Contents/MacOS
	cp icons/wang600-*x*.icns $(TMPDEST)/Wang600.app/Contents/Resources/
	cp wang600.plist.tmplt $(TMPDEST)/Wang600.app/Contents/Info.plist
	cp Uninstall-wang600.sh $(TMPDEST)/Uninstall-wang600
	chmod +x $(TMPDEST)/Wang600.app/Contents/MacOS/run
	chmod +x $(TMPDEST)/Uninstall-wang600
	mkdir -p $(TMPDEST)/Wang600Files/
	cp progs/*.wng $(TMPDEST)/Wang600Files/

wang600apc.tgz: temp_dest
	tar -czvf wang600apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

wang600apc.dmg: temp_app
	cd $(TMPDEST); genisoimage -r -apple -no-desktop \
		-V "WANG600APC" -hfs-volid "WANG600APC" \
		-o ../../wang600apc.dmg .
	rm -rf $(TMPDIR)

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
