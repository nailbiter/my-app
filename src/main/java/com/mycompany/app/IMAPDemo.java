package com.mycompany.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import org.json.JSONObject;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.lowagie.text.DocumentException;

public class IMAPDemo {
	static Date curDate = null;
	static Session sess = null;
	static Store st = null;
	static Folder[] fol = null;
	static final String[] months = {
		"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug",
		"Sep","Oct","Nov","Dec"
	};
	Writer writer_ = null;
	public void setWriter(Writer w){writer_ = w;}
	void write(String s)throws Exception{if(writer_!=null)writer_.write(s);}
	public IMAPDemo() throws Exception{
		String host = KeyRing.getHost();
		int port = 993;
		String user = KeyRing.getUser();
		String password = KeyRing.getPassword();

		Properties props = System.getProperties();
		sess = Session.getInstance(props, null);

		st = sess.getStore("imaps");
		st.connect(host, port, user, password);
		for(Folder f : st.getFolder("1").list()){
			System.out.printf("%s\n",f.getName());
		}
		fol = new Folder[2];
		fol[0] = myGetFolder("INBOX");
		fol[1] = st.getFolder("1").getFolder("Sent Messages");
		fol[fol.length-1].open(Folder.READ_ONLY);
		
		
		boolean[] flags = new boolean[] {false,false,false,false,true};
		fol[0].open(Folder.READ_ONLY);
		Message msg = this.getTestMessage(fol[0]);
		String filename = msg.getSubject();
		System.out.format("subject: %s\n", filename);
		if(flags[0])
		{
			try {
		        Multipart mp = (Multipart) msg.getContent();
		        System.out.format("subject: %s\ncount: %d\n", filename,mp.getCount());
		        //System.out.println(x);mp.getCount()
		        BodyPart bp = mp.getBodyPart(1);
			}
			catch(Exception e)
			{
				System.out.format("got exception: %s\n", e.getMessage());
			}
	        FileOutputStream os = new FileOutputStream(filename + ".html");
	        msg.writeTo(os);
		}
		
        //use jtidy to clean up the html 
        if(flags[1])
        		cleanHtml(filename);
        //save it into pdf
        if(flags[2])
        		createPdf(filename);
        if(flags[3])
        		inmain(msg,"tesime");
        if(flags[4]) {
        		System.out.println(msg.getSubject());
        		Message replyMessage = new MimeMessage(sess);
        		replyMessage = (MimeMessage) msg.reply(false);
            replyMessage.setFrom(new InternetAddress(KeyRing.getGmail()));
            if(true)
            {
            		String text = (String)msg.getContent();
            		String replyText = text.replaceAll("(?m)^", "> ");
            		// allow user to edit replyText,
            		// e.g., using a Swing GUI or a web form
            		replyMessage.setText("Thanks\n"+replyText);         
            }
            else {
            		replyMessage.setText("Thanks");
            }
            replyMessage.setReplyTo(msg.getReplyTo());
            replyMessage.writeTo(new FileOutputStream(new File("./mail.eml")));
        }
	}
	public void inmain(Message m, String string) throws Exception {
		String  to, subject = null, from = KeyRing.getMyMail(), 
			cc = KeyRing.getGmail(), bcc = null, url = null;
		String mailhost = null;
		String mailer = "msgsend";
		String file = null;
		String protocol = null, host = null, user = null, password = KeyRing.getPassword();
		String record = null;	// name of folder in which to record mail
		boolean debug = true;

	to = cc;
	//logger_.info(String.format("to=%s", to));
	
	Random r = new Random();
	subject = String.format("test: %d", r.nextInt());
	System.out.format("subject: %s",subject);

    /*
     * Initialize the JavaMail Session.
     */
    Properties props = System.getProperties();
    // XXX - could use Session.getTransport() and Transport.connect()
    // XXX - assume we're using SMTP
    mailhost = KeyRing.getHost();
    if (mailhost != null)
	    props.put("mail.smtp.host", mailhost);
    props.put("mail.smtp.port", 587);

    props.put("mail.debug", "true");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.user", KeyRing.getUser());
    props.put("mail.smtp.password",KeyRing.getPassword());
    props.put("mail.smtp.starttls.enable","true");
    props.put("mail.smtp.EnableSSL.enable","true");
    password = KeyRing.getPassword();
    host = KeyRing.getHost();
    user = KeyRing.getUser();

    System.out.format("host=%s\n",mailhost);

    // Get a Session object
    String user_ = KeyRing.getUser(),
    		password_ = KeyRing.getPassword();
    SmtpAuthenticator authentication = new SmtpAuthenticator(user_,password_);
    props.put("mail.smtp.port", 587);
    System.out.format("port=%d\n", props.get("mail.smtp.port"));
    Session session = Session.getInstance(props, authentication);
    if (debug)
	    session.setDebug(true);

    /*
     * Construct the message and send it.
     */
    Message msg = new MimeMessage(session);
    if(m != null) {
    		msg.setReplyTo(m.getFrom());
    		Message rm = m.reply(true);
    		String subj = rm.getSubject();
    		//logger_.info(String.format("subj: %s", subj));
    		msg.setSubject(subj);
    }
    if (from != null)
    		msg.setFrom(new InternetAddress(from));
    else
	msg.setFrom();

    msg.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(to, false));
    if (cc != null)
	msg.setRecipients(Message.RecipientType.CC,
				InternetAddress.parse(cc, false));
    if (bcc != null)
	msg.setRecipients(Message.RecipientType.BCC,
				InternetAddress.parse(bcc, false));

    String text = string;

    if (file != null) {
	MimeBodyPart mbp1 = new MimeBodyPart();
	mbp1.setText(text);
	MimeBodyPart mbp2 = new MimeBodyPart();
	mbp2.attachFile(file);
	MimeMultipart mp = new MimeMultipart();
	mp.addBodyPart(mbp1);
	mp.addBodyPart(mbp2);
	msg.setContent(mp);
    } else {
	// If the desired charset is known, you can use
	// setText(text, charset)
	msg.setText(text);
    }

    msg.setHeader("X-Mailer", mailer);
    msg.setSentDate(new Date());

    // send the thing off
    Transport.send(msg);

    System.out.println("\nMail was sent successfully.");

    /*
     * Save a copy of the message, if requested.
     */
    if (record != null) {
	// Get a Store object
	Store store = null;
	if (url != null) {
	    URLName urln = new URLName(url);
	    store = session.getStore(urln);
	    store.connect();
	} else {
	    if (protocol != null)		
		    store = session.getStore(protocol);
	    else
		    store = session.getStore();
		    store.connect();
	}

	// Get record Folder.  Create if it does not exist.
	Folder folder = store.getFolder(record);
	if (folder == null) {
	    System.err.println("Can't get record folder.");
	    System.exit(1);
	}
	if (!folder.exists())
	    folder.create(Folder.HOLDS_MESSAGES);

	Message[] msgs = new Message[1];
	msgs[0] = msg;
	folder.appendMessages(msgs);

	System.out.println("Mail was recorded successfully.");
		    
		}
	    }
	Message getTestMessage(Folder fol) throws Exception{
		Message[] ms = fol.getMessages();
		for(int i = ms.length-1; i>=0; i--) {
			Message m = ms[i];
			if(isFrom(m, KeyRing.getKMail()))
				return m;
		}
		throw new Exception();
	}
	public static void cleanHtml(String filename) {
	    File file = new File(filename + ".html");
	    InputStream in = null;
	    try {
	        in = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    OutputStream out = null;
	    try {
	        out = new FileOutputStream(filename + ".xhtml");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    final Tidy tidy = new Tidy();
	    tidy.setQuiet(false);
	    tidy.setShowWarnings(true);
	    tidy.setShowErrors(0);
	    tidy.setMakeClean(true);
	    tidy.setForceOutput(true);
	    org.w3c.dom.Document document = tidy.parseDOM(in, out);
	}
	public static void createPdf(String filename)
	        throws IOException, DocumentException {
	    OutputStream os = new FileOutputStream(filename + ".pdf");
	    ITextRenderer renderer = new ITextRenderer();
	    renderer.setDocument(new File(filename + ".xhtml"));
	    renderer.layout();
	    renderer.createPDF(os) ;
	    os.close();
	}
	void reply(String input) throws Exception
	{
		Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
		String ename;
		JSONObject obj = null;
		SearchStruct ss = null;

		obj = new JSONObject();
		curDate = new Date();
		//System.out.print("> ");
		//ename = scanner.next();
		ename = input;
		System.out.println("\tgot: "+ename);
		/*if(ename.equals("exit"))
		  break;*/
		try{
			String[] fs = ename.split(" ");
			if(fs[0].equals("ktalk"))
			{
				String[] s = fs[1].split(":");
				obj	.put("hours",Integer.parseInt(s[0]))
					.put("mins",Integer.parseInt(s[1]));
				if(fs.length>=2)
					obj.put("diff",Integer.parseInt(fs[2]));
				write( String.format("ktalk; %s %d, %02d:%02d;\n",
					IMAPDemo.months[curDate.getMonth()],curDate.getDate()+obj.optInt("diff",0),
					obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
				return;
			}
			if(fs[0].equals("skype"))
			{
				String[] s = fs[1].split(":");
				obj	.put("hours",Integer.parseInt(s[0]))
					.put("mins",Integer.parseInt(s[1]));
				write( String.format("skypeCall; %s %d, %02d:%02d;\n",
					IMAPDemo.months[curDate.getMonth()],curDate.getDate(),
					obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
				return;
			}
			String[] s = fs[0].split(":");
			if(fs.length >= 2){
				//month/day
				if(fs[1].contains("/"))
				{
					String[] pieces = fs[1].split("/");
					obj	.put("month",Integer.parseInt(pieces[0])-1)
						.put("day",Integer.parseInt(pieces[1]));
				}
				else
					obj.put("diff",Integer.parseInt(fs[1]));
			}
			obj	.put("hours",Integer.parseInt(s[0]))
				.put("mins",Integer.parseInt(s[1]));
			System.out.printf("\t%s\n",obj.toString());
			ss = new SearchStruct(obj);
			for(int i = 0; i < fol.length; i++){
				if(!fol[i].isOpen())
					fol[i].open(Folder.READ_ONLY);
				System.out.println(String.format("\t%s",fol[i].getName()));
				Message[] ms = fol[i].getMessages();
				for(Message m : ms){
					try{
						if(ss.test(m))
							write(String.format("%s\n",makeSubjectLine(m)));
					}
					catch(Exception e){
						System.out.println("\t\t"+e.getMessage());
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			//continue;
		}
	}
	static String makeSubjectLine(Message m) throws Exception
	{
		Date rd = m.getReceivedDate();
		String res =
			(isFrom(m,KeyRing.getKMail())?"fromK":"")+
			"; "+
			months[rd.getMonth()]+" "+rd.getDate()+", "+
			String.format("%02d:%02d",rd.getHours(),rd.getMinutes())+
			"; "+trimmedSubject(m.getSubject());
		return res;
	}
	static Folder myGetFolder(String name) throws Exception
	{
		Folder res = null;
		res = st.getFolder(name);
		if(res.exists()){
			for(Folder f : res.list()){
				System.out.println(f.getName());
			}
			//res.open(Folder.READ_ONLY);
		}
		else
			System.out.printf("%s is not exist.\n", name);
		return res;
	}

	static boolean isSeen(Flags flags)
	{
		//TODO
		//String flagstring = flags.toString
		return flags.toString().contains("\\Seen");
	}
	static boolean isFrom(Message m,String tmail) throws Exception
	{
		//logger_.info(String.format("compare subj=%s, tmail=%s", m.getSubject(),tmail));
		Address[] senders = m.getFrom();
		for(int i = 0; i < senders.length; i++)
		{
			//logger_.info(String.format("\tsender=%s", senders[i].toString()));
			if(senders[i].toString().contains(tmail))
				return true;
		}
		return false;
	}
	static String trimmedSubject(String subject)
	{
		String res=subject, tmp;
		do{
			subject = res;
			//res = processed(subject)
			res = subject.replaceFirst("^Re: *","").replaceFirst("^Fwd: *","");
		}
		while(!res.equals(subject));
		return res;
	}
}
