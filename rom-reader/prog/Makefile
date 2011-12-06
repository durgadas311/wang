all:

tests: cpr_test dat_test

cpr_test: cpr_test.c pport.o
	$(CC) $(CFLAGS) -o $@ cpr_test.c pport.o

dat_test: dat_test.c pport.o
	$(CC) $(CFLAGS) -o $@ dat_test.c pport.o
