.PHONY: all commit

JARNAME=my-app-1.0-SNAPSHOT-jar-with-dependencies.jar
SRCDIR=src/main/java/com/mycompany/app/
SRCs=MailManager Main SearchStruct ForwardEmail SmtpAuthenticator MailAccount\
     MailSearchPattern MailAction IsFrom TableBuilder
MAINCLASS=Main
# -k = kobayashi
KEYS=-k
#KEYS=-m me 
SRCs_all=$(SRCs) KeyRing


all: target/$(JARNAME)
	java -cp target/$(JARNAME) com.mycompany.app.$(MAINCLASS) $(KEYS)
commit:
	git add $(addprefix $(SRCDIR),$(addsuffix .java,$(SRCs)))
	git commit -a
target/$(JARNAME): $(addprefix $(SRCDIR),$(addsuffix .java,$(SRCs_all))) pom.xml
	mvn package
