# Now we just use ant to build projections

.PHONY : clean all run

all: 
	ant

clean: 
	ant clean

run: bin/projections.jar
	bin/projections test/hello.sts
