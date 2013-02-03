WPCC = ../../../wpcc

CPP = $(WPCC)/w6cpp

include ../Make.rules

SRCS = $(wildcard *.W6C)
OBJS = $(subst .W6C,.w6o,$(SRCS))

LIBS = libplot.a

all: $(LIBS)

libplot.a: $(OBJS)
	$(AR) -cr $@ $?
