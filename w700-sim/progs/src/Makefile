include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w7c *.W7C)
DATAS = $(wildcard *.txt)
PROGS = $(subst .w7c,.w7t,$(subst .W7C,.w7t,$(SRCS)))
SUBDIRS = libw7 libplot libtrig test

W7LDADD += -Llibw7 -Llibplot -Llibtrig
W7TLIBS += -lw7 -lplot -ltrig
W7TLDEPS += libw7/libw7.a libplot/libplot.a libtrig/libtrig.a

all: $(SUBDIRS) $(PROGS)

%.a: __FRC__
	$(MAKE) -C $(dir $@)

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
