include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w7c *.W7C)
DATAS = $(wildcard *.data)
PROGS = $(subst .w7c,.w7t,$(subst .W7C,.w7t,$(SRCS)))
DATAT = $(subst .data,.w7t,$(DATAS))
#SUBDIRS = libw7 libplot tests
SUBDIRS = libw7

#W7TLDFLAGS += -Llibw7 -Llibplot
#W7TLIBS += -lw7 -lplot
W7TLDFLAGS += -Llibw7
W7TLIBS += -lw7

all: $(SUBDIRS) $(PROGS) $(DATAT)

zeller.w7t: zeller.W7C libw7/libw7.a

plot_dat.w7t: plot_dat.W7C libw7/libw7.a libplot/libplot.a

plot_sin.w7t: plot_sin.W7C libw7/libw7.a libplot/libplot.a

graph_sin.w7t: graph_sin.W7C libw7/libw7.a libplot/libplot.a

%.w7t: %.data
	$(WPCCBIN)/w7data2tape <$< >$@

libw7/libw7.a: libw7

libplot/libplot.a: libplot

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS) $(ROMS) $(DATAT)
	cp $(PROGS) $(ROMS) $(DATAT) ~/Wang700Files/

ship: $(PROGS) $(ROMS) $(DATAT)
	cp $(PROGS) $(ROMS) $(DATAT) ..

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
