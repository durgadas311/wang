include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c *.W6C)
DATAS = $(wildcard *.txt)
PROGS = $(subst .w6c,.w6t,$(subst .W6C,.w6t,$(SRCS)))
DATAT = $(subst .txt,.w6t,$(DATAS))
SUBDIRS = libw6 libplot tests
ROMS = libplot.w6x

W6TLDFLAGS += -Llibw6 -Llibplot
W6TLIBS += -lw6 -lplot

all: $(SUBDIRS) $(ROMS) $(PROGS) $(DATAT)

plot_sin.w6t: plot_sin.w6o libplot.w6s libplot.w6x
	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

plot_dat.w6t: plot_dat.w6o libplot.w6s libplot.w6x
	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $< -lw6

graph_sin.w6t: graph_sin.w6o libplot.w6s libplot.w6x
	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
		-Wl,--just-symbols=libplot.w6s \
		-o $@ $<

zeller.w6t: zeller.W6C libw6/libw6.a

#plot_dat.w6t: plot_dat.W6C libw6/libw6.a libplot/libplot.a

#plot_sin.w6t: plot_sin.W6C libw6/libw6.a libplot/libplot.a

#graph_sin.w6t: graph_sin.W6C libw6/libw6.a libplot/libplot.a

%.w6t: %.txt
	$(WPCCBIN)/w6data2tape <$< >$@

libw6/libw6.a: libw6

libplot/libplot.a: libplot

$(SUBDIRS): __FRC__
	$(MAKE) -C $@

install: $(PROGS) $(ROMS) $(DATAT)
	cp $(PROGS) $(ROMS) $(DATAT) ~/Wang600Files/

ship: $(PROGS) $(ROMS) $(DATAT) $(DATAS)
	cp $(PROGS) $(ROMS) $(DATAT) $(DATAS) ..

# TODO: use "make... export" in subdirs, too
export: __FRC__
	mkdir -p $(DEST)
	cp Make* $(SRCS) $(DATAS) $(DEST)
	for d in $(SUBDIRS); do \
		mkdir -p $(DEST)/$$d; \
		cp $$d/Make* $$d/*.W6C $(DEST)/$$d; \
	done

.PHONEY: __FRC__

__FRC__:
