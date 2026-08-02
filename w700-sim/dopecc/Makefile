# add "DFLAGS=-v" to the make command to get full disassembly (with codes)
DFLAGS =
CFLAGS = -Wformat-overflow=0

SRC = $(wildcard *.c) $(wildcard *.h)
ROM = $(wildcard *.rom) $(wildcard *.dis)
TAR = Makefile $(SRC) $(ROM)

all: refresh.dis keypress.dis

%.rom: %.o
	objcopy -j .data -O binary $*.o $*.rom

%.c: ../%.c
	ln -s $^ $@

%.dis: %.rom
	./ucdis $(DFLAGS) -a $< >$@

ucdis: ucdis.o dw700.o loaducode_txt.o
	$(CC) $(CFLAGS) -o $@ ucdis.o dw700.o loaducode_txt.o

tgz: w700ucode.tgz

w700ucode.tgz: $(TAR)
	tar -czf w700ucode.tgz -h $(TAR)

ship: tgz
	rsync -Wuv w700ucode.tgz durgadas.com:/var/www/html/sebhc/w700-sim/

.PRECIOUS: %.rom
