include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w7c)
OBJS = $(subst .w7c,.w7o,$(SRCS))

LIBS = libplot.a

all: $(LIBS)

libplot.a: $(OBJS)
	$(AR) -cr $@ $^
