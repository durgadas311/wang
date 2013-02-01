WPCC = ../../wpcc

CPP = $(WPCC)/w6cpp

SRCS = $(wildcard *.wpc *.WPC)
PROGS = $(subst .wpc,.wng,$(subst .WPC,.wng,$(SRCS)))

all: $(PROGS)

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

install: $(PROGS)
	cp $(PROGS) ~/Wang600Files/


ship: $(PROGS)
	cp $(PROGS) ..
