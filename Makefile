# Process Scheduling Simulator. Needs a JDK, Java 8 or newer.
#
#   make                 compile into out/
#   make run             run on the default random workload
#   make run ARGS="..."  run with options, e.g. make run ARGS="--seed 7 --gantt"
#   make example         run the 4-process textbook example, with Gantt charts
#   make test            run the tests
#   make clean           delete build output

SOURCES := $(wildcard src/*.java)
TESTS   := $(wildcard tests/*.java)
ARGS    ?=

all: out/Main.class

out/Main.class: $(SOURCES)
	mkdir -p out
	javac -d out $(SOURCES)

run: all
	java -cp out Main $(ARGS)

example: all
	java -cp out Main --example

test: $(SOURCES) $(TESTS)
	mkdir -p out-test
	javac -d out-test $(SOURCES) $(TESTS)
	java -cp out-test SchedulerTests

clean:
	rm -rf out out-test

.PHONY: all run example test clean
