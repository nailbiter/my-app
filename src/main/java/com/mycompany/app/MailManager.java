package com.mycompany.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

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
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import com.lowagie.text.DocumentException;

import it.sauronsoftware.cron4j.Scheduler;

public class MailManager implements MailAction {
	static Date curDate = null;
	private MailAccount mc_ = null;
	static final String[] months = {
		"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug",
		"Sep","Oct","Nov","Dec"
	};
	Writer writer_ = null;
	class SearchAndAct{
		public MailSearchPattern msp;
		public MailAction ma;
		SearchAndAct(MailSearchPattern Msp, MailAction Ma){
			msp = Msp;
			ma = Ma;
		}
	}
	Hashtable<Integer,SearchAndAct> actors = new Hashtable<Integer,SearchAndAct>();
	static class MyMessageCountListener implements MessageCountListener{
		Hashtable<Integer,SearchAndAct> actors_ = null;
		MyMessageCountListener(Hashtable<Integer,SearchAndAct> actors)
		{
			actors_ = actors;
		}
		@Override
		public void messagesAdded(MessageCountEvent ev) {
			Message[] msgs = ev.getMessages();
			Collection<SearchAndAct> sas = actors_.values();
			for(Message msg : msgs)
			{
				try {
					//System.out.format("\t%s from %s\n", msg.getSubject(),msg.getFrom()[0].toString());
					for(SearchAndAct sa : sas) {
							if(sa.msp.test(msg))
								sa.ma.act(msg);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		@Override
		public void messagesRemoved(MessageCountEvent arg0) {
			return;
		}
		
	}
	public void setWriter(Writer w){writer_ = w;}
	void write(String s)throws Exception{
		if(writer_!=null)
			writer_.write(s);
		else
			System.out.format("wanted to write \"%s\", but couldn't\n",s);
		}
	public MailManager() throws Exception{
		String host = KeyRing.getHost();
		int port = 993;
		String user = KeyRing.getUser();
		String password = KeyRing.getPassword();
		mc_ = new MailAccount(host,user,password,port);
		final Folder fol = mc_.openFolder("INBOX"); 
		fol.addMessageCountListener(new MyMessageCountListener(actors));
		mc_.openFolder("1", "Sent Messages");
		this.addIterator(new IsFrom(true?KeyRing.getKMail():KeyRing.getGmail()), new MailAction() {

			@Override
			public void act(Message message) throws Exception {
				// TODO Auto-generated method stub
				System.out.format("new mail from K: %s\n", message.getSubject());
				write(String.format("new mail from K: %s\n", message.getSubject()));
			}
		});
		Scheduler scheduler = new Scheduler();
		scheduler.schedule("* * * * *", 
				new Runnable() {public void run() {try {
			fol.getMessageCount();
		} catch (MessagingException e) {
			e.printStackTrace();
		}}});
		scheduler.start();
		
		/*boolean[] flags = new boolean[] {false,false,false,false,true};
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
        }*/
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
	void command(String cmd, String tail)
	{
		System.out.format("got command: %s, tail: %s\n",cmd,tail);
		if(cmd.equals("forward"))
		{
			forward(Boolean.parseBoolean(tail));
			return;
		}
	}
	void forward(boolean flag) {
		//TODO
	}
	void reply(String input) throws Exception
	{
		if(input.startsWith("/"))
		{
			String[] opt = input.split(" ", 2);
			command(opt[0].substring(1),opt[1]);
			return;
		}
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
					MailManager.months[curDate.getMonth()],curDate.getDate()+obj.optInt("diff",0),
					obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
				return;
			}
			if(fs[0].equals("skype"))
			{
				String[] s = fs[1].split(":");
				obj	.put("hours",Integer.parseInt(s[0]))
					.put("mins",Integer.parseInt(s[1]));
				write( String.format("skypeCall; %s %d, %02d:%02d;\n",
					MailManager.months[curDate.getMonth()],curDate.getDate(),
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
			mc_.iterateThroughAllMessages(ss, this);
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
	@Override
	public void act(Message message) throws Exception {
		write(String.format("%s\n",makeSubjectLine(message)));
	}
	public void removeIterator(int code) {
		actors.remove(code);
	}
	public int addIterator(MailSearchPattern msp, MailAction ma)
	{
		int res = new Random().nextInt();
		actors.put(res, new SearchAndAct(msp,ma));
		return res;
	}
}
