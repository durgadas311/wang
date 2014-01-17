include /usr/local/wpcc/include/Make.rules.user

# for now, exclude new, untested, modules
#SRCS = $(wildcard *.w7c)
SRCS = com_1X.w7c cos_1X.w7c cosX.w7c sin_1X.w7c sinX.w7c \
	tan_1X.w7c tanX.w7c trig_end.w7c \
intersect.w7c ecc_centr.w7c safe_regs.w7c

OBJS = $(subst .w7c,.w7o,$(SRCS))

LIBS = libtrig.a

all: $(LIBS)

libtrig.a: $(OBJS)
	$(AR) -cr $@ $^
