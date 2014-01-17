CFLAGS += -I../../common -g

BINS = wpcpp w6dis w6lst w6data2tape w6verify w6ldfix \
	w7dis w7lst w7verify w7ldfix \
	symbols2labels
INCS = wang600.h wang600internals.h wang600opcodes.s \
	wang700.h wang700internals.h wang700opcodes.s
LDS = wang600raw.ld wang600rawrom.ld wang700raw.ld \
	fnordering1.ld fnordering2.ld

all: $(BINS)

wpcpp: wpcpp.c w6cpp_data.o w7cpp_data.o
	$(CC) $(CFLAGS) wpcpp.c -o wpcpp w6cpp_data.o w7cpp_data.o

w6data2tape: w6data2tape.c w6cpp_data.o
	$(CC) $(CFLAGS) w6data2tape.c -o w6data2tape w6cpp_data.o

w6lst: w6lst.c
	$(CC) $(CFLAGS) -std=c99 w6lst.c -o w6lst

w6verify: w6verify.c
	$(CC) $(CFLAGS) -std=c99 w6verify.c -o w6verify

w6ldfix: w6ldfix.c wang600opcodes.h
	$(CC) $(CFLAGS) -std=c99 w6ldfix.c -o $@

w7lst: w7lst.c
	$(CC) $(CFLAGS) -std=c99 w7lst.c -o w7lst

w7verify: w7verify.c
	$(CC) $(CFLAGS) -std=c99 w7verify.c -o w7verify

w7ldfix: w7ldfix.c w7interlacepair.o wang700opcodes.h
	$(CC) $(CFLAGS) -std=c99 w7ldfix.c w7interlacepair.o -o $@

wang600opcodes.h: wang600opcodes.s
	@echo -e "/* generated from wang600opcodes.s - used by C utilities */"\
		"\n#ifndef __wang600opcodes_h__"\
		"\n#define __wang600opcodes_h__" >$@
	sed -e 's/^\.type/#define/' -e 's/STT_OBJECT;.*,//' <$^ >>$@
	@echo -e "#endif /* __wang600opcodes_h__ */" >>$@

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
