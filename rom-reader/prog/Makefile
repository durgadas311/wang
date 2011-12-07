TESTS = cpr_test dat_test

ROMTESTS = loopback

all:

tests: $(TESTS) $(ROMTESTS)

$(TESTS): %: %.c pport.o
	$(CC) $(CFLAGS) -o $@ $^

$(ROMTESTS): %: %.c pport.o rom_reader.o
	$(CC) $(CFLAGS) -o $@ $^
