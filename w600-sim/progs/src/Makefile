# Makefile for building with wasm

WASM = wasm
VPATH = libw6:libplot:lib612
WFLAGS = path=$(VPATH)


SRCS = $(wildcard *.w6a)
PROGS = $(subst .w6a,.w6t,$(SRCS))
LISTS = $(subst .w6a,.lst,$(SRCS))
DATAS = $(wildcard *.txt)
DATAT = $(subst .txt,.w6t,$(DATAS))
SUBDIRS = libw6 libplot tests
ROMS = libplot.w6x
# might need to separate FLABS for ROM (implicit build) from RAM (explicit build)
# TODO: need a way to generate the labels
#FLABS = libplot.wfl
# The modules that comprise libplot ROM:
LPLOT = grapher.w6i \
	plot_chrsize.w6i \
	plot_data.w6i \
	plot_draw.w6i \
	plot_home.w6i \
	plot_move.w6i \
	plotter.w6i \
	plot_Xaxis.w6i \
	plot_Yaxis.w6i \
	pt_scale.w6i

all: $(ROMS) $(PROGS) # $(DATAT)

#plot_sin.w6t: plot_sin.w6a libplot.w6s libplot.w6x
#	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
#		-Wl,--just-symbols=libplot.w6s \
#		-o $(basename $@).w6$$ $<
#	$(W6LDFIX) $(basename $@).w6$$
#	@rm -f $(basename $@).w6$$
#
#plot_dat.w6t: plot_dat.w6o libplot.w6s libplot.w6x
#	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
#		-Wl,--just-symbols=libplot.w6s \
#		-o $(basename $@).w6$$ $< -lw6
#	$(W6LDFIX) $(basename $@).w6$$
#	@rm -f $(basename $@).w6$$
#
#graph_sin.w6t: graph_sin.w6o libplot.w6s libplot.w6x
#	$(CC) $(W6CCFLAGS) $(W6TLDFLAGS) \
#		-Wl,--just-symbols=libplot.w6s \
#		-o $(basename $@).w6$$ $<
#	$(W6LDFIX) $(basename $@).w6$$
#	@rm -f $(basename $@).w6$$

%.w6x: %.w6a
	$(WASM) $(WFLAGS) asm=$< lst=$*.lst out=$@ rom

%.w6t: %.w6a
	$(WASM) $(WFLAGS) asm=$< lst=$*.lst out=$@ tape

%.w6t: %.txt
	$(WASM) dat=$< out=$@

craps.w6t: craps.w6a random.w6i
calendar.w6t: calendar.w6a zeller.w6i
zeller.w6t: zeller.w6a zeller.w6i
libplot.w6x: libplot.w6a libplot.w6i $(LPLOT)
plot_sin.w6t: plot_sin.w6a libplot.w6h
plot_dat.w6t: plot_dat.w6a load_tape_data.w6i
list_dat.w6t: list_dat.w6a load_tape_data.w6i

w600progs.tgz: $(PROGS) $(ROMS) $(FLABS) $(DATAT) $(DATAS) $(LISTS)
	tar -czf w600progs.tgz --transform="s;^;w600progs/;" $^

install: $(PROGS) $(ROMS) $(DATAT)
	cp $(PROGS) $(ROMS) $(DATAT) ~/Wang600Files/

ship: $(PROGS) $(ROMS) $(FLABS) $(DATAT) $(DATAS)
	cp $(PROGS) $(ROMS) $(FLABS) $(DATAT) $(DATAS) ..

dist: w600progs.tgz
	scp $^ durgadas.com:/var/www/html/sebhc/w600-sim/

# TODO: use "make... export" in subdirs, too
export: __FRC__
	mkdir -p $(DEST)
	cp Make* $(SRCS) $(RSRCS) $(DATAS) $(DEST)
	for d in $(SUBDIRS); do \
		mkdir -p $(DEST)/$$d; \
		cp $$d/Make* $$d/*.w6[ch] $(DEST)/$$d; \
	done

.PHONEY: __FRC__

__FRC__:
