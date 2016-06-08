# Now we just use gradle to build projections

.PHONY : clean all run

all: 
	gradle copyJarToBin

clean: 
	gradle clean

run: bin/projections.jar
	bin/projections test/hello.sts
