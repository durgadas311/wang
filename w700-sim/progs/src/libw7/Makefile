include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.W7C)
OBJS = $(subst .W7C,.w7o,$(SRCS))

LIBS = libw7.a

all: $(LIBS)

libw7.a: $(OBJS)
	$(AR) -cr $@ $?
