# Now we just use gradle to build projections

.PHONY : clean all run

all bin/projections.jar:
	gradle copyJarToBin

clean: 
	gradle clean
	rm -rf bin/projections.jar .gradle

run: bin/projections.jar
	bin/projections test/hello.sts
