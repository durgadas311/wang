
all: wpcpp

wpcpp: wpcpp.c
	$(CC) $(CFLAGS) wpcpp.c -o wpcpp

%.wng: %.wpc
	./wpcpp $^

all.o: junk.o crud.o wrte.o wang600.h
	gcc -nostartfiles  -nodefaultlibs  -nostdlib -T wang600.ld -o all.o junk.o crud.o wrte.o
