
all: wpcpp

wpcpp: wpcpp.c
	$(CC) $(CFLAGS) wpcpp.c -o wpcpp

%.wng: %.wpc
	./wpcpp $^
