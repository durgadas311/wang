JAVAS = $(wildcard *.java)
CLASSES = $(subst .java,.class,$(JAVAS))

all: wasm.jar

%.class: %.java
	javac $?

wasm.jar: $(CLASSES)
	echo "Main-Class: wasm" >Manifest.txt
	jar -cmf Manifest.txt $@ *.class
