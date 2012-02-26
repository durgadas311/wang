# $Id: Makefile,v 1.4 2012/02/26 01:40:34 drmiller Exp $

SUBDIRS = w600-sim w700-sim w1200-sim

all:

xinetd jar ship:
	for d in $(SUBDIRS); do \
		$(MAKE) -$(MAKEFLAGS) -C $${d} $@; \
	done
