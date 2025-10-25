# This makefile is defined to give you the following targets:
#
#    default: Compiles all .java files.
#    clean: Remove all the .class files produced by java compilation, 
#          all Emacs backup files, and testing output files.

JFLAGS = -g -Xlint:unchecked -Xlint:deprecation

# All .java files in this directory.
OBJECTS := $(wildcard *.java)

.PHONY: default clean 

# As a convenience, you can compile a single Java file X.java in this directory
# with 'make X.class'
%.class: %.java
	javac $(JFLAGS) $<

# First, and therefore, the default target.
default: compile

compile: $(OBJECTS)
	javac $(JFLAGS) $(OBJECTS)

# 'make clean' will clean up stuff you can reconstruct.
clean:
	$(RM) *.class

