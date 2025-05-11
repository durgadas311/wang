# This must build both plugin and standalone app.
# would like to avoid having duplicates of classes
# in ../sim, but standalone JAR needs them copied.
# Perhaps 'jdeps' can help with this.
CLASS_PATH = .:../w600-sim

JAVAS = $(wildcard *.java)
CLASSES = $(subst .java,.class,$(JAVAS))
JARS = Wang600asm.jar
SIM_CLASS = Wang600_CPU.class \
	Wang_FrontPanel.class \
	Wang600_Ucode.class

AUXS = $(wildcard docs/*.html)

all: $(CLASSES)

%.class: %.java
	javac -cp $(CLASS_PATH) $?

%.java:: ../w600-sim/%.java
	cp $? $@

jar: $(JARS)

Wang600asm.jar: $(CLASSES) $(SIM_CLASS) $(AUXS)
	echo "Main-Class: Wang600asm" >Manifest.txt
	jar -cmf Manifest.txt $@ *.class $(AUXS)

ship: jar
	rsync -uvW $(JARS) --exclude='junk*' *.wuc \
		durgadas.com:/var/www/html/sebhc/w600-sim/microcode

__FRC__:
