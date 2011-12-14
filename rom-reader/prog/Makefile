TESTS = cpr_test dat_test wr_test rd_test

ROMTESTS = loopback adr_test

all:

tests: $(TESTS) $(ROMTESTS)

$(TESTS): %: %.c pport.o
	$(CC) $(CFLAGS) -o $@ $^

$(ROMTESTS): %: %.c pport.o rom_reader.o
	$(CC) $(CFLAGS) -o $@ $^
