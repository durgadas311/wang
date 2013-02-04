include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.W6C)
OBJS = $(subst .W6C,.w6o,$(SRCS))

LIBS = libw6.a

all: $(LIBS)

libw6.a: $(OBJS)
	$(AR) -cr $@ $?
