# $Id: Makefile,v 1.2 2012/02/25 19:45:07 drmiller Exp $

SUBDIRS = w600-sim w700-sim w1200-sim

xinetd:
	for d in $(SUBDIRS); do \
		$(MAKE) -$(MAKEFLAGS) -C $${d} $@; \
	done
