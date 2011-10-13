
all: wpcpp

wpcpp: wpcpp.c
	$(CC) $(CFLAGS) wpcpp.c -o wpcpp

%.wpo: %.wpc
	$(CC) -x c $(CFLAGS) -c $^ -o $@

# %.wng: %.wpc
# 	./wpcpp $^

all.wng: junk.wpo crud.wpo wang600.h
	gcc -nostartfiles  -nodefaultlibs  -nostdlib -T wang600.ld \
		-o all.wng junk.wpo crud.wpo
