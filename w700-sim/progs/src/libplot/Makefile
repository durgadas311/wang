include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.W7C)
OBJS = $(subst .W7C,.w7o,$(SRCS))

LIBS = libplot.a

all: $(LIBS)

libplot.a: $(OBJS)
	$(AR) -cr $@ $^
