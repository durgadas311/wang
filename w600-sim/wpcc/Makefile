CPP = ./wpcpp
CFLAGS += -I../../common

all: wpcpp dis lst

LDSCRIPT = wang600raw.ld

wpcpp: wpcpp.c
	$(CC) $(CFLAGS) wpcpp.c -o wpcpp

lst: lst.c
	$(CC) $(CFLAGS) -std=c99 lst.c -o lst

%.wpo: %.wpc
	$(CC) -x c -c $^ -o $@

%.wng: %.wpc
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-o $@ $^

%.wng: %.WPC
	$(CPP) < $< > $*.wpe
	$(CC) -x c -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(LDSCRIPT) \
		-o $@ $*.wpe
	@rm -f $*.wpe

all.wng: junk.wpo crud.wpo wang600.h
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -T wang600.ld \
		-o $@ junk.wpo crud.wpo

rom.wng: junk.wpo crud.wpo wang600.h
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -T wang600rom.ld \
		-o $@ junk.wpo crud.wpo
