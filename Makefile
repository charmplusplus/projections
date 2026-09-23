# Now we just use gradle to build projections, via the checked-in wrapper
# so that every machine builds with the same Gradle version.

.PHONY : clean all run

all bin/projections.jar:
	./gradlew copyJarToBin

clean: 
	./gradlew clean
	rm -rf bin/projections.jar .gradle

run: bin/projections.jar
	bin/projections test/hello.sts

test: bin/projections.jar
	bin/projections --exit test/hello.sts
