include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c *.W6C)
PROGS = $(subst .w6c,.w6t,$(subst .W6C,.w6t,$(SRCS)))
SUBDIRS = libw6 libplot tests
ROMS = libplot.w6x

W6TLDFLAGS += -Llibw6 -Llibplot
W6TLIBS += -lw6 -lplot

all: $(SUBDIRS) $(ROMS) $(PROGS)

plot_sin.w6t: plot_sin.w6o libplot.w6s
	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

graph_sin.w6t: graph_sin.w6o libplot.w6s
	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

zeller.w6t: zeller.W6C libw6/libw6.a

libw6/libw6.a: libw6

libplot/libplot.a: libplot

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS)
	cp $(PROGS) ~/Wang600Files/

ship: $(PROGS)
	cp $(PROGS) ..

.PHONEY: __FRC__

__FRC__:
