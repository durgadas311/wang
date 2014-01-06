include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.W7C)
OBJS = $(subst .W7C,.w7o,$(SRCS))

LIBS = libtrig.a

all: $(LIBS)

libtrig.a: $(OBJS)
	$(AR) -cr $@ $^
