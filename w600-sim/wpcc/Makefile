PLAT=$(shell gcc -E -dM - </dev/null | awk '/__CYGWIN__/{print "cygwin";}/__APPLE__/{print "macosx";}')

ifeq ($(PLAT),)
	W6INTERNALS = wang600internals.h
	W6RAWLD = wang600raw.ld
	W6OPCODES = wang600opcodes.s
	W7INTERNALS = wang700internals.h
	W7RAWLD = wang700raw.ld
	W7OPCODES = wang700opcodes.s
	MAKERULES = Make.rules.user
else
	W6INTERNALS = wang600internals.h.$(PLAT)
	W6RAWLD = wang600raw.ld.$(PLAT)
	W6OPCODES = wang600opcodes.s.$(PLAT)
	W7INTERNALS = wang700internals.h.$(PLAT)
	W7RAWLD = wang700raw.ld.$(PLAT)
	W7OPCODES = wang700opcodes.s.$(PLAT)
	MAKERULES = Make.rules.user.$(PLAT)
endif

CFLAGS += -I../../common -g

BINS = wpcpp w6dis w6lst w6data2tape w6verify w6ldfix \
	w7dis w7lst w7verify w7ldfix \
	symbols2labels
INCS = wang600.h wang700.h 
LDS = wang600rawrom.ld fnordering1.ld fnordering2.ld

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

wang600opcodes.s.cygwin: wang600opcodes.s
	sed -e 's/^\.type [^;]*; //' $^ >$@

wang700opcodes.s.cygwin: wang700opcodes.s
	sed -e 's/^\.type [^;]*; //' $^ >$@

wang600opcodes.h: $(W6OPCODES)
	@echo -e "/* generated from $^ - used to build C utilities */"\
		"\n#ifndef __wang600opcodes_h__"\
		"\n#define __wang600opcodes_h__" >$@
	sed -e 's/^.*\.set/#define/' -e 's/,/ /' <$^ >>$@
	@echo -e "#endif /* __wang600opcodes_h__ */" >>$@

wang700opcodes.h: $(W7OPCODES)
	@echo -e "/* generated from $^ used by C utilities */"\
		"\n#ifndef __wang700opcodes_h__"\
		"\n#define __wang700opcodes_h__" >$@
	sed -e 's/^.*\.set/#define/' -e 's/,/ /' <$^ >>$@
	@echo -e "#endif /* __wang700opcodes_h__ */" >>$@

install: $(BINS) $(INCS) $(LDS) $(W6RAWLD) $(W6INTERNALS) $(W6OPCODES) \
		$(W7RAWLD) $(W7INTERNALS) $(W7OPCODES) $(MAKERULES)
	mkdir -p /usr/local/wpcc/{bin,lib,include}
	cp $(W6RAWLD) /usr/local/wpcc/lib/wang600raw.ld
	cp $(W6INTERNALS) /usr/local/wpcc/include/wang600internals.h
	cp $(W6OPCODES) /usr/local/wpcc/include/wang600opcodes.s
	cp $(W7RAWLD) /usr/local/wpcc/lib/wang700raw.ld
	cp $(W7INTERNALS) /usr/local/wpcc/include/wang700internals.h
	cp $(W7OPCODES) /usr/local/wpcc/include/wang700opcodes.s
	cp $(MAKERULES) /usr/local/wpcc/include/Make.rules.user
	cp $(LDS) /usr/local/wpcc/lib
	cp $(INCS) /usr/local/wpcc/include
	cp $(BINS) /usr/local/wpcc/bin

clean:
	rm -f *.o

clobber: clean
	rm -f $(BINS) wang?00opcodes.s.* wang?00opcodes.h

export:
	cp *.[sch] *.ld $(DEST)
	cp *.cygwin $(DEST)
	cp Make* symbols2labels $(DEST)
	mkdir -p $(DEST)/www
	cp www/*.html $(DEST)/www
