COPTS =

OBJS = w600_sys.o w600_sim_cmd.o \
	w600_decode.o w600_exec.o w600_gui.o

XTRA = 

DASM = dw600.o

all: w600-sim

CFLAGS += -g -O3 $(COPTS)

w600-dasm: w600-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w600-sim: w600-sim.o $(OBJS) $(XTRA) $(DASM) w600_fe
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

clean:
	rm -f *.o

tar:
	tar -czvf w600-sim.tgz *.[ch] Makefile README

install: tar
	mv -v w600-sim.tgz /http/durgadas.com/sebhc
