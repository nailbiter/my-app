JARNAME=my-app-1.0-SNAPSHOT-jar-with-dependencies.jar

.PHONY: all
all:
	java -cp target/$(JARNAME) com.mycompany.app.Main
