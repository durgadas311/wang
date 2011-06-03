# $Id: Makefile,v 1.23 2011/06/03 14:25:02 drmiller Exp $

INSNAME = wang600apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

COPTS =

OBJS = w600_sys.o w600_sim_cmd.o \
	w600_decode.o w600_exec.o w600_gui.o

TEST = $(shell echo -e this)
ifeq ($(TEST),this)
ECHOE = echo -e
else
ECHOE = echo
endif

FONTDIR = $(shell if [[ -d ~/.fonts ]]; then echo ~/.fonts; elif [[ -d ~/Library/Fonts ]]; then echo ~/Library/Fonts; fi)
FONTDEST =

ifneq ($(FONTDIR),)
FONTDEST = $(FONTDIR)/Wang600Display.ttf
endif

XTRA = 

DASM = dw600.o

all: w600-sim

CFLAGS += -g -O3 $(COPTS)

w600-dasm: w600-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w600-sim: w600-sim.o $(OBJS) $(XTRA) $(DASM) w600_fe w600_fe.class
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

w600_fe.class: w600_fe.java
	javac w600_fe.java

$(FONTDEST): fonts/Wang600Display.ttf
	cp $? $@

wang600-block-diag.pdf: ../wang600-docs/wang600-block-diag.pdf
	cp $? $@

clean:
	rm -f *.o

clobber: clean
	rm -f *.class w600-sim w600_fe w600-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar: wang600-block-diag.pdf
	cd ..;\
	tar -czvf w600-sim/w600-sim.tgz \
		w600-sim/*.[ch] w600-sim/Makefile w600-sim/*.java \
		w600-sim/wang600-block-diag.pdf w600-sim/README*  w600-sim/LICENSE* \
		w600-sim/wang600.rom w600-sim/icons/*.gif \
		w600-sim/wang600.rom w600-sim/icons/*.png w600-sim/fonts/*.ttf	\
		w600-sim/docs/*.html

ship: tar
	mv -v w600-sim.tgz /http/durgadas.com/sebhc

Manifest.txt:
	echo "Main-Class: w600_fe" >$@

wang600.jar: w600_fe.class Manifest.txt
	jar cfm $@ Manifest.txt *.class icons fonts/*.ttf docs/*.html

# INSTALL goes away, or becomes empty, once we figure out how to get ttf from jar
temp_dest: all wang600.jar
	mkdir -p $(TMPDEST)
	cp wang600.jar wang600.rom w600-sim run INSTALL $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL
	mkdir -p $(TMPDEST)/progs
	cp progs/*.lst progs/*.wng $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
	cp icons/wang600-*x*.png $(TMPDEST)/icons

package: temp_dest
	tar -czvf wang600apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang600docs
	cp docs/*.html /http/durgadas.com/wang600docs
	cp www/wang600* /http/durgadas.com

_FRC_:
