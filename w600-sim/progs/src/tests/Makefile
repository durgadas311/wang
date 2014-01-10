include /usr/local/wpcc/include/Make.rules.user

SRCS = $(wildcard *.w6c)
OBJS = $(subst .w6c,.w6t,$(SRCS))

LIBS = 

all: $(OBJS) $(LIBS)

# libplot.a: $(OBJS)
# 	$(AR) -cr $@ $?
