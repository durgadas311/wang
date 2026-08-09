# This must build both plugin and standalone app.
# would like to avoid having duplicates of classes
# in ../sim, but standalone JAR needs them copied.
# Perhaps 'jdeps' can help with this.
CLASS_PATH = .:../w700-sim

JAVAS = $(wildcard *.java)
CLASSES = $(subst .java,.class,$(JAVAS))
JARS = Wang700asm.jar
SIM_CLASS = Wang700_CPU.class \
	Wang_FrontPanel.class \
	Wang700_Ucode.class

AUXS = $(wildcard docs/*.html)

all: $(CLASSES)

%.class: %.java
	javac -cp $(CLASS_PATH) $?

%.java:: ../w700-sim/%.java
	cp $? $@

jar: $(JARS)

Wang700asm.jar: $(CLASSES) $(SIM_CLASS) $(AUXS)
	echo "Main-Class: Wang700asm" >Manifest.txt
	jar -cmf Manifest.txt $@ *.class $(AUXS)

ship: jar
	rsync -uvW $(JARS) \
		durgadas.com:/var/www/html/sebhc/w700-sim/microcode

__FRC__:
