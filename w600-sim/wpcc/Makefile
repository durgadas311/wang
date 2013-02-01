CPP = ./w6cpp
CFLAGS += -I../../common

all: w6cpp w6dis w6lst

LDSCRIPT = wang600raw.ld

w6cpp: w6cpp.c
	$(CC) $(CFLAGS) w6cpp.c -o w6cpp

w6lst: w6lst.c
	$(CC) $(CFLAGS) -std=c99 w6lst.c -o w6lst

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
