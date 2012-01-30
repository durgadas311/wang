# $Id: Makefile,v 1.13 2012/01/30 12:07:46 drmiller Exp $

INSNAME = wang700apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

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
	jar cfm $@ Manifest.txt *.class icons $(SHIP_DOCS) \
		$(addprefix -C ../common/fonts ,$(FONTS))

temp_dest: all wang700.jar
	mkdir -p $(TMPDEST)
	cp wang700.jar w700-sim run INSTALL $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL
	mkdir -p $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
#	cp progs/*.wng $(TMPDEST)/progs
#	cp icons/wang700-*x*.png $(TMPDEST)/icons

package: temp_dest
	tar -czvf wang700apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang700/wang700docs
	cp docs/*.html docs/*.gif /http/durgadas.com/wang700/wang700docs
	cp www/wang700* /http/durgadas.com/wang700

_FRC_:
