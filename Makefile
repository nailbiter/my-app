JARNAME=my-app-1.0-SNAPSHOT-jar-with-dependencies.jar
SRCDIR=src/main/java/com/mycompany/app/
SRCs=IMAPDemo KeyRing Main SearchStruct

.PHONY: all


all: target/$(JARNAME)
	java -cp target/$(JARNAME) com.mycompany.app.Main
target/$(JARNAME) : $(addprefix $(SRCDIR),$(addsuffix .java,$(SRCs)))
	mvn package
