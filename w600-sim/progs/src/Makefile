WPCC = ../../wpcc

CPP = $(WPCC)/wpcpp

PROGS = plotter.wng countdown.wng

all: $(PROGS)

LDSCRIPT = $(WPCC)/wang600raw.ld

%.wng: %.wpc
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-I $(WPCC) \
		-o $@ $^

%.wng: %.WPC
	$(CPP) < $< > $*.wpe
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-I $(WPCC) \
		-o $@ $*.wpe
	@rm -f $*.wpe
