all:

rom: wire-rom-0.rom wire-rom-1.rom wire-rom-2.rom

exe: xcode ycode zcode

%.rom: %.o
	objcopy -j .data -O binary $*.o $*.rom

xcode: xcode.o xcode-1.o xcode-2.o
	$(CC) -o $@ $^

zcode: zcode.o xcode-1.o xcode-2.o
	$(CC) -o $@ $^

wcode: wcode.o xcode-1.o xcode-2.o
	$(CC) -o $@ $^

xcode-%.c: wire-rom-%.txt romerr.awk
	awk -f romerr.awk -v tag=xcode_$* wire-rom-$*.txt >$@

wire-rom-%.c: wire-rom-%.txt rom2c.awk
	awk -f rom2c.awk wire-rom-$*.txt >$@
