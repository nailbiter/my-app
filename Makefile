JARNAME=my-app-1.0-SNAPSHOT-jar-with-dependencies.jar
SRCDIR=src/main/java/com/mycompany/app/
SRCs=IMAPDemo Main SearchStruct ForwardEmail SmtpAuthenticator MailAccount
SRCs_all=$(SRCs) KeyRing
MAINCLASS=Main

.PHONY: all add


all: target/$(JARNAME)
	java -cp target/$(JARNAME) com.mycompany.app.$(MAINCLASS)
commit:
	git add $(addprefix $(SRCDIR),$(addsuffix .java,$(SRCs)))
	git commit -a
target/$(JARNAME) : $(addprefix $(SRCDIR),$(addsuffix .java,$(SRCs_all)))
	mvn package
