include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.W6C)
OBJS = $(subst .W6C,.w6t,$(SRCS))

LIBS = 

all: $(OBJS) $(LIBS)

# libplot.a: $(OBJS)
# 	$(AR) -cr $@ $?
