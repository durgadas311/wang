include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c)
OBJS = $(subst .w6c,.w6o,$(SRCS))

LIBS = libplot.a

all: $(LIBS)

libplot.a: $(OBJS)
	$(AR) -cr $@ $^
