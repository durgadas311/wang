include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c *.W6C)
PROGS = $(subst .w6c,.w6t,$(subst .W6C,.w6t,$(SRCS)))
SUBDIRS = libplot tests
ROMS = libplot.w6x

all: $(SUBDIRS) $(ROMS) $(PROGS)

plot_sin.w6t: plot_sin.w6o libplot.w6s
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(T_LDSCRIPT) \
		-L$(WPCCLIB) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

graph_sin.w6t: graph_sin.w6o libplot.w6s
	$(CC) -nostartfiles -nodefaultlibs -nostdlib -Wl,-T,$(T_LDSCRIPT) \
		-L$(WPCCLIB) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS)
	cp $(PROGS) ~/Wang600Files/

ship: $(PROGS)
	cp $(PROGS) ..

.PHONEY: __FRC__

__FRC__:
