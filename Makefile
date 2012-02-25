# $Id: Makefile,v 1.1 2012/02/25 19:42:32 drmiller Exp $

SUBDIRS = w600-sim w700-sim w1200-sim

xinetd:
	for d in $(SUBDIRS); do \
		$(MAKE) $(MAKEFLAGS) $@; \
	done
