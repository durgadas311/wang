include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w7c *.W7C)
DATAS = $(wildcard *.txt)
PROGS = $(subst .w7c,.w7t,$(subst .W7C,.w7t,$(SRCS)))
SUBDIRS = libw7 libplot test

W7TLDFLAGS += -Llibw7 -Llibplot
W7TLIBS += -lw7 -lplot

all: $(SUBDIRS) $(PROGS)

zeller.w7t: zeller.W7C libw7/libw7.a

#plot_dat.w7t: plot_dat.W7C libw7/libw7.a libplot/libplot.a

#plot_sin.w7t: plot_sin.W7C libw7/libw7.a libplot/libplot.a

plot_alpha.w7t: plot_alpha.W7C libw7/libw7.a libplot/libplot.a

#graph_sin.w7t: graph_sin.W7C libw7/libw7.a libplot/libplot.a

libw7/libw7.a: libw7

libplot/libplot.a: libplot

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS) $(ROMS) $(DATAS)
	cp $(PROGS) $(ROMS) $(DATAS) ~/Wang700Files/

ship: $(PROGS) $(DATAS)
	cp $(PROGS) $(DATAS) ..

# TODO: use "make... export" in subdirs, too
export: __FRC__
	mkdir -p $(DEST)
	cp Make* $(SRCS) $(DATAS) $(DEST)
	for d in $(SUBDIRS); do \
		mkdir -p $(DEST)/$$d; \
		cp $$d/Make* $$d/*.W7C $(DEST)/$$d; \
	done

.PHONEY: __FRC__

__FRC__:
