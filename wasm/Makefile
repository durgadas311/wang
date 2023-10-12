JAVAS = $(wildcard *.java)
CLASSES = $(subst .java,.class,$(JAVAS))
SHIPS = wasm.jar wasm.pdf

all: wasm.jar

%.class: %.java
	javac $?

wasm.jar: $(CLASSES)
	echo "Main-Class: wasm" >Manifest.txt
	jar -cmf Manifest.txt $@ *.class

ship: wasm.jar
	rsync -Wuv $(SHIPS) durgadas.com:/var/www/html/sebhc/w700-sim
	rsync -Wuv $(SHIPS) durgadas.com:/var/www/html/sebhc/w600-sim
