include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w7c)
OBJS = $(subst .w7c,.w7o,$(SRCS))

LIBS = libw7.a

all: $(LIBS)

libw7.a: $(OBJS)
	$(AR) -cr $@ $?
