.PHONY: test arch build dev clean

test:
	mvn test

arch:
	mvn test -Dtest=ArchitectureFitnessTest

build:
	mvn clean package -DskipTests

dev:
	mvn spring-boot:run

clean:
	mvn clean

