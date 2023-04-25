# -*- MakeFile -*-
.PHONY: build run clean
.DEFAULT_GOAL := build

#JFLAGS := -g

out/Main.class: Main.java
	@javac $^ ${JFLAGS} -d out

build: out/Main.class
run: build
	@java -cp out Main
clean:
	@rm -rf out
