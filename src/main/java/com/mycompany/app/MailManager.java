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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Stack;

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
	private String testmail_;
	private int forwardActionCode_ = -1;
	Writer writer_ = null;
	ArrayList<Message> incoming = new ArrayList<Message>();
	public void setWriter(Writer w){writer_ = w;}
	void write(String s)throws Exception{
		if(writer_!=null)
			writer_.write(s);
		else
			System.out.format("wanted to write \"%s\", but couldn't\n",s);
		}
	public MailManager(String testmail_in) throws Exception{
		testmail_ = testmail_in;
		mc_ = new MailAccount(KeyRing.getHost(),KeyRing.getUser(),KeyRing.getPassword(),993,KeyRing.getMyMail());
		mc_.addActor(MailAccount.IteratorList.OUTCOMING,
				new MailSearchPattern() {
				@Override
				public boolean test(Message m) throws Exception {
					Address[] recipients = m.getAllRecipients();
					final String tmail = testmail_; 
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
					m.setSubject(MailUtil.makeSubjectLine(message));
					mc_.sendMessage(m);
				}
			});
		mc_.openInboxFolder("INBOX"); 
		mc_.openSentFolder("1", "Sent Messages");
		mc_.addActor(MailAccount.IteratorList.INCOMING, 
				new IsFrom(testmail_), new MailAction() {
				@Override
				public void act(Message message) throws Exception {
					incoming.add(message);
					String line = String.format("new mail from %s!: %s\n", testmail_,message.getSubject());
					System.out.print(line);
					write(line);
				}
			});
		
		autoforward("true");
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
		try {
			m.invoke(this, tail);
		}
		catch(InvocationTargetException e) {
			write(String.format("te: %s", e.getCause().getMessage()));
		}
	}
	int replyActionCode_ = -1;
	private void autoreply(String tail) {
		boolean flag = Boolean.parseBoolean(tail);
		if(flag)
		{
			mc_.addActor(MailAccount.IteratorList.INCOMING,new IsFrom(testmail_),
					mc_.getReplyAction());
			System.out.format("replyActionCode_ = %d\n",replyActionCode_);
		}
		else
			mc_.removeIterator(MailAccount.IteratorList.INCOMING,replyActionCode_);
	}
	public void showbody(String tail) throws Exception{
		int messageNum = 0;
		try { messageNum = Integer.parseInt(tail); }
		catch(NumberFormatException e) {}
		Message msg = this.incoming.get(this.incoming.size() - messageNum - 1);
		
		String text = "";
		try{
 			//String replyText = "";
 			Object content = msg.getContent();
 			
 			if(content instanceof String)
 				text = (String)content;
 			if(content instanceof Multipart)
 				text = MailUtil.getText(((Multipart)msg.getContent()).getBodyPart(0));
 			
 			//replyText = text.replaceAll("(?m)^", "> ");
 		}
 		catch(Exception e)
 		{
 			e.printStackTrace(System.out);
 		}
 		/*System.out.println("before the end");
 		replyMessage.setText(body + "\n" + replyText);*/
		write(text);
	}
	/*
	 * [message] [reply num]
	 */
	public void reply(String tail) throws Exception{
		String[] parts = tail.split(" ");
		int messageNum = 0, replyNum = 0;
		try {
		if(parts.length >= 1)
			messageNum = Integer.parseInt(parts[0]);
		if(parts.length >= 2)
			replyNum = Integer.parseInt(parts[1]);
		}
		catch(NumberFormatException nfe) {}
		
		Message m = this.incoming.get(this.incoming.size() - messageNum - 1);
		this.mc_.getReplyAction().act(m);
	}
	public void listreplies(String tail) {
		//TODO
	}
	public void listmails(String tail) throws Exception {
		int num = 10;
		try {
			num = Integer.parseInt(tail);
		}
		catch(NumberFormatException e) {}
		TableBuilder tb = new TableBuilder()
				.newRow()
				.addToken("#")
				.addToken("date")
				.addToken("subject");
		for(int i = 0; i < incoming.size() && i < num; i++)
		{
			tb.newRow();
			Message m = incoming.get(incoming.size() - 1 - i);
			tb.addToken(i);
			Date rd = m.getReceivedDate();
			if(rd == null) rd = new Date();
			tb.addToken(String.format("%s %d, %02d:%02d",
					MailUtil.months[rd.getMonth()],
					rd.getDate(),
					rd.getHours(),
					rd.getMinutes()));
			tb.addToken(m.getSubject());
		}
		write(tb.toString("`", "`"));
	}
	public void autoforward(String tail) {
		boolean flag = Boolean.parseBoolean(tail);
		System.out.println("set autoforward "+flag);
		
		if(flag)
		{
			mc_.addActor(MailAccount.IteratorList.INCOMING,new IsFrom(testmail_),
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
		String ename;
		JSONObject obj = null;
		SearchStruct ss = null;

		obj = new JSONObject();
		curDate = new Date();
		ename = input;
		System.out.println("\tgot: "+ename);
		try{
			String[] fs = ename.split(" ");
			if(this.processAux(fs, "ktalk", "ktalk"))
				return;
			if(this.processAux(fs, "skype", "skypeCall"))
				return;
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
			if(!testmail_.equals(KeyRing.getMyMail()))
				obj.put("mail", testmail_);
			ss = new SearchStruct(obj);
			mc_.iterateThroughAllMessages(ss, this);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		write("that's all, folks!\n");
	}
	boolean processAux(String[] fs,String what, String to) throws JSONException, Exception
	{
		JSONObject obj = new JSONObject();
		if(fs[0].equals(what))
		{
			String[] s = fs[1].split(":");
			obj	.put("hours",Integer.parseInt(s[0]))
				.put("mins",Integer.parseInt(s[1]));
			write( String.format("%s; %s %d, %02d:%02d;\n",to,
				MailUtil.months[curDate.getMonth()],curDate.getDate(),
				obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
			return true;
		}
		return false;
	}
	@Override
	public void act(Message message) throws Exception {
		write(String.format("%s\n",MailUtil.makeSubjectLine(message)));
	}
}
