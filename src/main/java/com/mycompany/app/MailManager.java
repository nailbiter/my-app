package com.mycompany.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
	static final String testmail = true ? KeyRing.getKMail() : KeyRing.getMyMail();
	private int forwardActionCode_ = -1;
	Writer writer_ = null;
	class SearchAndAct{
		public MailSearchPattern msp;
		public MailAction ma;
		SearchAndAct(MailSearchPattern Msp, MailAction Ma){
			msp = Msp;
			ma = Ma;
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
		mc_ = new MailAccount(KeyRing.getHost(),KeyRing.getUser(),KeyRing.getPassword(),993,KeyRing.getMyMail());
		mc_.addIterator(MailAccount.IteratorList.OUTCOMING,
				new MailSearchPattern() {
				@Override
				public boolean test(Message m) throws Exception {
					Address[] recipients = m.getAllRecipients();
					final String tmail = testmail; 
					for(int i = 0; i < recipients.length; i++)
					{
						if(recipients[i].toString().contains(tmail))
							return true;
					}
					return false;
				}
			}, 
			new MailAction()
			{

				@Override
				public void act(Message message) throws Exception {
					Message m = mc_.createForward(message, KeyRing.getKmailsTrello());
					m.setSubject(makeSubjectLine(message));
					mc_.sendMessage(m);
				}
			});
		mc_.openInboxFolder("INBOX"); 
		mc_.openSentFolder("1", "Sent Messages");
		mc_.addIterator(MailAccount.IteratorList.INCOMING, 
				new IsFrom(true?KeyRing.getKMail():KeyRing.getGmail()), new MailAction() {
				@Override
				public void act(Message message) throws Exception {
					String line = String.format("new mail from %s: %s\n", testmail,message.getSubject());
					System.out.print(line);
					write(line);
				}
			});
		
		//reply(true);
	}
	void command(String cmd, String tail) throws Exception
	{
		System.out.format("got command: %s, tail: %s\n",cmd,tail);
		Method m = null;
		try {
			m = this.getClass().getMethod(cmd,String.class);
		}
		catch(NoSuchMethodException e)
		{
			write(String.format("no method %s\n", cmd));
			return;
		}
		m.invoke(this, tail);
	}
	int replyActionCode_ = -1;
	private void autoreply(String tail) {
		boolean flag = Boolean.parseBoolean(tail);
		if(flag)
		{
			mc_.addIterator(MailAccount.IteratorList.INCOMING,new IsFrom(testmail),
					mc_.getReplyAction());
			System.out.format("replyActionCode_ = %d\n",replyActionCode_);
		}
		else
			mc_.removeIterator(MailAccount.IteratorList.INCOMING,replyActionCode_);
	}
	void showbody(String tail) {
		int index = 0;
		try { index = Integer.parseInt(tail); }
		catch(NumberFormatException e) {}
		//TODO
	}
	void reply(String tail) {
		//TODO
	}
	void replylist(String tail) {
		//TODO
	}
	void forward(String tail) {
		boolean flag = Boolean.parseBoolean(tail);
		if(flag)
		{
			mc_.addIterator(MailAccount.IteratorList.INCOMING,new IsFrom(testmail),
					mc_.getForwardAction(KeyRing.getTrello()));
			System.out.format("forwardCode = %d\n",forwardActionCode_);
		}
		else
			mc_.removeIterator(MailAccount.IteratorList.INCOMING,forwardActionCode_);
	}
	void process(String input) throws Exception
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
		write("that's all, folks!\n");
	}
	static String makeSubjectLine(Message m) throws Exception
	{
		Date rd = m.getReceivedDate();
		if(rd == null) rd = new Date();
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
		Address[] senders = m.getFrom();
		for(int i = 0; i < senders.length; i++)
		{
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
			res = subject.replaceFirst("^Re: *","").replaceFirst("^Fwd: *","");
		}
		while(!res.equals(subject));
		return res;
	}
	@Override
	public void act(Message message) throws Exception {
		write(String.format("%s\n",makeSubjectLine(message)));
	}
}
