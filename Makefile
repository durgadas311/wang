# $Id: Makefile,v 1.3 2012/02/25 20:53:35 drmiller Exp $

SUBDIRS = w600-sim w700-sim w1200-sim

all:

xinetd jar:
	for d in $(SUBDIRS); do \
		$(MAKE) -$(MAKEFLAGS) -C $${d} $@; \
	done
