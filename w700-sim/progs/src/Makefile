# Makefile for building with wasm

WASM = wasm
#VPATH = libw7:libplot:lib712
VPATH = libw7:libplot:libtrig
WFLAGS = path=$(VPATH)


SRCS = $(wildcard *.w7a)
PROGS = $(subst .w7a,.w7t,$(SRCS))
LISTS = $(subst .w7a,.lst,$(SRCS))
DATAS = $(wildcard *.txt)
DATAF = $(subst .txt,.wdf,$(DATAS))
SUBDIRS = libw7 libplot libtrig test

PLOT_DEPS = plot_home.w7i plot_move.w7i plot_draw.w7i plot_chrsize.w7i \
	plot_Xaxis.w7i plot_Yaxis.w7i pt_scale.w7i

SINCOS_DEPS = sinX.w7i cosX.w7i

all: $(PROGS) $(DATAF)

%.a: __FRC__
	$(MAKE) -C $(dir $@)

# Convert LF to CR-LF
%.wdf: %.txt
	unix2dos -n $^ $@

%.w7t: %.w7a
	$(WASM) $(WFLAGS) asm=$< lst=$*.lst out=$@ tape

craps.w7t: craps.w7a random.w7i
calendar.w7t: calendar.w7a zeller.w7i
zeller.w7t: zeller.w7a zeller.w7i
graph_sin.w7t: graph_sin.w7a grapher.w7i pt_scale.w7i $(SINCOS_DEPS)
plot_sin.w7t: plot_sin.w7a $(PLOT_DEPS) $(SINCOS_DEPS)
plot_dat.w7t: plot_dat.w7a $(PLOT_DEPS)

w700progs.tgz: $(PROGS) $(ROMS) $(FLABS) $(DATAT) $(DATAS) $(LISTS)
	tar -czf w700progs.tgz --transform="s;^;w700progs/;" $^

install: $(PROGS) $(ROMS) $(DATAS)
	cp $(PROGS) $(ROMS) $(DATAS) ~/Wang700Files/

ship: $(PROGS) $(DATAF) $(DATAS)
	cp $(PROGS) $(DATAF) $(DATAS) ..

dist: w700progs.tgz
	scp $^ durgadas.com:/var/www/html/sebhc/w700-sim/

# TODO: use "make... export" in subdirs, too
export: __FRC__
	mkdir -p $(DEST)
	cp Make* $(SRCS) $(FSRCS) $(DATAS) $(DEST)
	for d in $(SUBDIRS); do \
		mkdir -p $(DEST)/$$d; \
		cp $$d/Make* $$d/*.w7[ch] $(DEST)/$$d; \
	done

.PHONEY: __FRC__

__FRC__:
