CFLAGS += -I../../common -g

BINS = w6cpp w6dis w6lst w6data2tape
INCS = wang600.h wang600internals.h wang600opcodes.s
LDS = wang600.ld wang600raw.ld wang600rawrom.ld wang600rom.ld

all: $(BINS)

w6cpp: w6cpp.c w6cpp_data.o
	$(CC) $(CFLAGS) w6cpp.c -o w6cpp w6cpp_data.o

w6data2tape: w6data2tape.c w6cpp_data.o
	$(CC) $(CFLAGS) w6data2tape.c -o w6data2tape w6cpp_data.o

w6lst: w6lst.c
	$(CC) $(CFLAGS) -std=c99 w6lst.c -o w6lst

install: $(BINS) $(INCS) $(LDS)
	mkdir -p /usr/local/wpcc/{bin,lib,include}
	cp $(LDS) /usr/local/wpcc/lib
	cp $(INCS) Make.rules.user /usr/local/wpcc/include
	cp $(BINS) /usr/local/wpcc/bin

clean:
	rm -f *.o

clobber: clean
	rm -f $(BINS)
