CFLAGS += -I../../common -g

BINS = w6cpp w6dis w6lst w6data2tape w6verify \
	w7cpp w7dis w7lst w7data2tape w7verify w7ldfix
INCS = wang600.h wang600internals.h wang600opcodes.s \
	wang700.h wang700internals.h wang700opcodes.s
LDS = wang600.ld wang600raw.ld wang600rawrom.ld wang600rom.ld \
	wang700raw.ld

all: $(BINS)

w6cpp: w6cpp.c w6cpp_data.o
	$(CC) $(CFLAGS) w6cpp.c -o w6cpp w6cpp_data.o

w6data2tape: w6data2tape.c w6cpp_data.o
	$(CC) $(CFLAGS) w6data2tape.c -o w6data2tape w6cpp_data.o

w6lst: w6lst.c
	$(CC) $(CFLAGS) -std=c99 w6lst.c -o w6lst

w6verify: w6verify.c
	$(CC) $(CFLAGS) -std=c99 w6verify.c -o w6verify

w7cpp: w7cpp.c w7cpp_data.o
	$(CC) $(CFLAGS) w7cpp.c -o w7cpp w7cpp_data.o

w7data2tape: w7data2tape.c w7cpp_data.o
	$(CC) $(CFLAGS) w7data2tape.c -o w7data2tape w7cpp_data.o

w7lst: w7lst.c
	$(CC) $(CFLAGS) -std=c99 w7lst.c -o w7lst

w7verify: w7verify.c
	$(CC) $(CFLAGS) -std=c99 w7verify.c -o w7verify

w7ldfix: w7ldfix.c w7interlacepair.o wang700opcodes.h wang700opcodes.o
	$(CC) $(CFLAGS) -std=c99 w7ldfix.c w7interlacepair.o wang700opcodes.o -o w7ldfix

wang700opcodes.h: wang700opcodes.s
	@echo -e "/* generated from wang700opcodes.s - used by C utilities */"\
		"\n#ifndef __wang700opcodes_h__"\
		"\n#define __wang700opcodes_h__" >$@
	sed -e 's/^\.type/#define/' -e 's/STT_OBJECT;.*,//' <$^ >>$@
	@echo -e "#endif /* __wang700opcodes_h__ */" >>$@

install: $(BINS) $(INCS) $(LDS)
	mkdir -p /usr/local/wpcc/{bin,lib,include}
	cp $(LDS) /usr/local/wpcc/lib
	cp $(INCS) Make.rules.user /usr/local/wpcc/include
	cp $(BINS) /usr/local/wpcc/bin

clean:
	rm -f *.o

clobber: clean
	rm -f $(BINS)
