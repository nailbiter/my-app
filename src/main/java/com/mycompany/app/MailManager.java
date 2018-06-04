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
		mc_ = new MailAccount(KeyRing.getHost(),KeyRing.getUser(),KeyRing.getPassword(),993,KeyRing.getMyMail());
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
		
		if(!false)
		{
			reply(true);
		}
	}
	void command(String cmd, String tail)
	{
		System.out.format("got command: %s, tail: %s\n",cmd,tail);
		if(cmd.equals("forward"))
		{
			forward(Boolean.parseBoolean(tail));
			return;
		}
		if(cmd.equals("reply"))
		{
			reply(Boolean.parseBoolean(tail));
			return;
		}
	}
	int replyActionCode_;
	private void reply(boolean flag) {
		if(flag)
		{
			replyActionCode_ = addIterator(new IsFrom(false?KeyRing.getKMail():KeyRing.getGmail()),
					mc_.getReplyAction());
			System.out.format("replyActionCode_ = %d\n",replyActionCode_);
		}
		else
			removeIterator(replyActionCode_);
	}
	void forward(boolean flag) {
		if(flag)
		{
			forwardActionCode_ = addIterator(new IsFrom(false?KeyRing.getKMail():KeyRing.getGmail()),
					mc_.getForwardAction(KeyRing.getTrello()));
			System.out.format("forwardCode = %d\n",forwardActionCode_);
		}
		else
			removeIterator(forwardActionCode_);
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
