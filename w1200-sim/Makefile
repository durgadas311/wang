# $Id: Makefile,v 1.2 2011/11/13 01:42:39 drmiller Exp $

INSNAME = wang1200apc
INSDIR = ~
INSDEST = $(INSDIR)/$(INSNAME)
TMPDIR = temp
TMPDEST = $(TMPDIR)/$(INSNAME)

COPTS =

VPATH = ../common

CFLAGS += -D__wang1200__ -I../common -I.

OBJS = wang_sys.o wang_sim_cmd.o \
	w1200_decode.o wang_exec.o wang_gui.o

TEST = $(shell echo -e this)
ifeq ($(TEST),this)
ECHOE = echo -e
else
ECHOE = echo
endif

FONTDIR = $(shell if [[ -d ~/.fonts ]]; then echo ~/.fonts; elif [[ -d ~/Library/Fonts ]]; then echo ~/Library/Fonts; fi)
FONTDEST =
FONTS = $(notdir $(wildcard ../common/fonts/*.ttf))

XTRA = 

DASM = dw1200.o loaducode_txt.o

all: w1200-sim w1200_fe.class

CFLAGS += -g -O3 $(COPTS)

w1200-dasm: wang-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w1200-sim: wang-sim.o $(OBJS) $(XTRA) $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

w1200_fe.class: w1200_fe.java
	javac w1200_fe.java

xinetd: w1200-sim
	@echo "Ready for: cp $(PWD)/$? /usr/local/bin/wang"

clean:
	rm -f *.o

clobber: clean
	rm -f *.class w1200-sim w1200_fe w1200-dasm Manifest.txt *.jar *.tgz
	rm -rf $(TMPDIR)

tar:
	cd ..;\
	tar -czvf w1200-sim/w1200-sim.tgz \
		w1200-sim/*.[ch] w1200-sim/Makefile w1200-sim/*.java \
		common/*.[ch] \
		w1200-sim/wang1200-block-diag.pdf w1200-sim/README*  w1200-sim/LICENSE* \
		w1200-sim/wang1200.rom w1200-sim/icons/*.gif \
		w1200-sim/icons/*.png common/fonts/*.ttf	\
		w1200-sim/progs/*.wng w1200-sim/docs/*.html w1200-sim/docs/*.gif

jar: wang1200.jar

ship: tar
	mv -v w1200-sim.tgz /http/durgadas.com/sebhc

Manifest.txt:
	echo "Main-Class: w1200_fe" >$@

SHIP_DOCS = $(filter-out docs/wang1200arch.html,$(wildcard docs/*.html))

wang1200.jar: w1200_fe.class Manifest.txt
	jar cfm $@ Manifest.txt *.class icons $(SHIP_DOCS) \
		$(addprefix -C ../common/fonts ,$(FONTS))

temp_dest: all wang1200.jar
	mkdir -p $(TMPDEST)
	cp wang1200.jar wang1200.rom w1200-sim run INSTALL $(TMPDEST)
	chmod +x $(TMPDEST)/run $(TMPDEST)/INSTALL
	mkdir -p $(TMPDEST)/progs
	cp progs/*.wng $(TMPDEST)/progs
	mkdir -p $(TMPDEST)/icons
	cp icons/wang1200-*x*.png $(TMPDEST)/icons

package: temp_dest
	tar -czvf wang1200apc.tgz -C $(TMPDIR) $(INSNAME)
	rm -rf $(TMPDIR)

install: temp_dest
	rm -rf $(INSDEST)
	mv $(TMPDEST) $(INSDIR)
	rmdir $(TMPDIR)
	cd $(INSDEST); ./INSTALL

www: _FRC_
	mkdir -p /http/durgadas.com/wang1200docs
	cp docs/*.html /http/durgadas.com/wang1200docs
	cp www/wang1200* /http/durgadas.com
#	cp docs/*.gif /http/durgadas.com/wang1200docs

_FRC_:
