CFLAGS += -I../../common

BINS = w6cpp w6dis w6lst
INCS = wang600.h wang600internals.h
LDS = wang600.ld wang600raw.ld wang600rawrom.ld wang600rom.ld

all: $(BINS)

w6cpp: w6cpp.c
	$(CC) $(CFLAGS) w6cpp.c -o w6cpp

w6lst: w6lst.c
	$(CC) $(CFLAGS) -std=c99 w6lst.c -o w6lst

install: $(BINS) $(INCS) $(LDS)
	mkdir -p /usr/local/wpcc/{bin,lib,include}
	cp $(LDS) /usr/local/wpcc/lib
	cp $(INCS) Make.rules.user /usr/local/wpcc/include
	cp $(BINS) /usr/local/wpcc/bin
