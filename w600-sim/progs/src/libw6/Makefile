include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c)
OBJS = $(subst .w6c,.w6o,$(SRCS))

LIBS = libw6.a

all: $(LIBS)

libw6.a: $(OBJS)
	$(AR) -cr $@ $?
