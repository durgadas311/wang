WPCC = ../../wpcc

CPP = $(WPCC)/w6cpp

include Make.rules

SRCS = $(wildcard *.wpc *.WPC)
PROGS = $(subst .wpc,.wng,$(subst .WPC,.wng,$(SRCS)))
SUBDIRS = libplot

all: $(SUBDIRS) $(PROGS)

LDSCRIPT = $(WPCC)/wang600raw.ld

%.wng: %.wpc
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-I $(WPCC) \
		-o $@ $<

%.wng: %.WPC
	$(CPP) < $< > $*.wpe
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-I $(WPCC) \
		-o $@ $*.wpe
	@rm -f $*.wpe

%.wpo: %.wpc
	$(CC) -x c -c -I $(WPCC) -o $@ $<

%.wpo: %.WPC
	$(CPP) < $< > $*.wpe
	$(CC) -x c -c -I $(WPCC) -o $@ $*.wpe
	@rm -f $*.wpe

plot_sin.w6t: plot_sin.w6o libplot/libplot.a
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-o $@ $< -Llibplot -lplot

graph_sin.w6t: graph_sin.w6o libplot/libplot.a
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-o $@ $< -Llibplot -lplot

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS)
	cp $(PROGS) ~/Wang600Files/

ship: $(PROGS)
	cp $(PROGS) ..

.PHONEY: __FRC__

__FRC__:
