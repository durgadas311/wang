COPTS =

OBJS = w600_sys.o w600_sim_cmd.o \
	w600_decode.o w600_exec.o w600_gui.o

XTRA = 

DASM = dw600.o

all: w600-sim

CFLAGS += -g -O3 $(COPTS)

w600-dasm: w600-dasm.o $(DASM)
	$(CC) $(CFLAGS) -o $@ $< $(DASM)

w600-sim: w600-sim.o $(OBJS) $(XTRA) $(DASM) w600_fe w600_fe.class
	$(CC) $(CFLAGS) -o $@ $< $(OBJS) $(XTRA) $(DASM)

w600_fe.class: w600_fe.java
	javac w600_fe.java

clean:
	rm -f *.o

tar:
	cd ..;\
	tar -czvf w600-sim.tgz \
		w600-sim/*.[ch] w600-sim/Makefile w600-sim/README w600-sim/*.java \
		w600-sim/wang600.rom w600-sim/*.class w600-sim/icons/*.gif

install: tar
	mv -v w600-sim.tgz /http/durgadas.com/sebhc
