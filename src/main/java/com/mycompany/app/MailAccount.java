package com.mycompany.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;

import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import it.sauronsoftware.cron4j.Scheduler;

/**
 * 
 * @author nailbiter
 * This classes manages single email account.
 * It hides implementation details and provides useful low-level methods.
 * FIXME: change name?
 */
public class MailAccount {
	private Session sess;
	private Store st;
	private ArrayList<Folder> fols = new ArrayList<Folder>();
	private String address_;
	private Folder sentFolder_ = null;
	private Scheduler scheduler_;
	public class ForwardAction implements MailAction{
		private String to_ = null; 
		private boolean asAttachment_;
		ForwardAction(String to, boolean asAttachment){ to_ = to; asAttachment_ = asAttachment;}
		@Override
		public void act(Message message) throws Exception {
			System.out.format("here on act in %s\n", this.getClass().getName());
			if(asAttachment_)
            		sendMessage(createForward(message,to_));
			else {
				Message r = MailAccount.this.getReplyMessage("", message);
				r.setRecipient(Message.RecipientType.TO, new InternetAddress(to_));
			}
		}
	}
	public Message createForward(Message message, String to_) throws AddressException, MessagingException
	{
		Message forward = new MimeMessage(sess);
		forward.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse(to_));
		forward.setSubject("Fwd: " + message.getSubject());
		forward.setFrom(new InternetAddress(KeyRing.getMyMail()));
		
	     	// Create the message part
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		// Create a multipart message
		Multipart multipart = new MimeMultipart();
		// set content
		messageBodyPart.setContent(message, "message/rfc822");
		// Add part to multi part
		multipart.addBodyPart(messageBodyPart);
		// Associate multi-part with message
		forward.setContent(multipart);
		forward.saveChanges();
		return forward;
	}

	public MailAccount(String host, String user, String password, int port,String address,Scheduler scheduler) throws MessagingException{
		scheduler_ = scheduler;
		Properties props = System.getProperties();
		props.put("mail.smtp.host", host);
		props.put("mail.debug", "false");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.user", user);
		props.put("mail.smtp.password",password);
		props.put("mail.smtp.starttls.enable","true");
		props.put("mail.smtp.EnableSSL.enable","true");
		SmtpAuthenticator authentication = new SmtpAuthenticator(user,password);
		props.put("mail.smtp.port", 587);
	    	sess = Session.getInstance(props, authentication);
		address_ = address;

		st = sess.getStore("imaps");
		st.connect(host, port, user, password);
		for(Folder f : st.getFolder("1").list()){
			System.out.printf("%s\n",f.getName());
		}
		this.addActor(IteratorList.OUTCOMING,MailSearchPatternFactory.getTrivialPattern(),
				new MailAction() {
					@Override
					public void act(Message message) throws Exception {
						saveToSentFolder(message);
					}
		});	
	}
	void saveToSentFolder(Message m) throws MessagingException {
		m.setFlag(Flag.SEEN, true);
		this.sentFolder_.appendMessages(new Message[] {m});
	}
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
					for(SearchAndAct sa : sas) {
							if(sa.msp.test(msg))
								sa.ma.act(msg);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		@Override
		public void messagesRemoved(MessageCountEvent arg0) {
			return;
		}
	}
	public void openInboxFolder(String name) throws Exception
	{
		Folder fol = myGetFolder(name);
		fol.open(Folder.READ_ONLY);		
		fols.add(fol);
		fol.addMessageCountListener(new MyMessageCountListener(actorsIncoming));
		
		schedule(fol);
		
		//return fol;
	}
	private void schedule(final Folder fol)
	{
		scheduler_.schedule("* * * * *", 
				new Runnable() {public void run() {try {
			fol.getMessageCount();
		} catch (MessagingException e) {
			e.printStackTrace();
		}}});
	}
	public void openSentFolder(String name1, String name2) throws Exception
	{
		Folder fol = st.getFolder(name1).getFolder(name2);
		fol.open(Folder.READ_WRITE);
		sentFolder_ = fol;
		fols.add(fol);
		//return fol;
	}
	public Folder getFolder(int index) {return fols.get(index);}
	private Folder myGetFolder(String name) throws Exception
	{
		Folder res = null;
		res = st.getFolder(name);
		if(res.exists()){
			for(Folder f : res.list()){
				System.out.println(f.getName());
			}
		}
		else
			System.out.printf("%s is not exist.\n", name);
		return res;
	}
	public void iterateThroughAllMessages(MailSearchPattern msp, MailAction ma) throws MessagingException {
		for(int i = 0; i < fols.size(); i++){
			Folder fol = fols.get(i);
			if(!fol.isOpen())
				fol.open(Folder.READ_ONLY);
			System.out.println(String.format("\t%s",fol.getName()));
			Message[] ms = fol.getMessages();
			for(Message m : ms){
				try{
					if(msp.test(m))
						ma.act(m);
						//write(String.format("%s\n",makeSubjectLine(m)));
				}
				catch(Exception e){
					System.out.println("\t\t"+e.getMessage());
				}
			}
		}
	}
	public ForwardAction getForwardAction(String to, boolean asAttachment)
	{
		return new ForwardAction(to,asAttachment);
	}
	protected Message getReplyMessage(String body, Message msg) throws Exception{
		Message replyMessage = new MimeMessage(sess);
 		replyMessage = (MimeMessage) msg.reply(false);
 		replyMessage.setFrom(new InternetAddress(address_));
 		String replyText = "";
 		try{
 			String text = "";
 			Object content = msg.getContent();
 			
 			if(content instanceof String)
 				text = (String)content;
 			if(content instanceof Multipart)
 				text = MailUtil.getText(((Multipart)msg.getContent()).getBodyPart(0));
 			
 			replyText = text.replaceAll("(?m)^", "> ");
 		}
 		catch(Exception e)
 		{
 			e.printStackTrace(System.out);
 		}
 		System.out.println("before the end");
 		replyMessage.setText(body + "\n" + replyText);
 		return replyMessage;
	}
	public void reply(String body, Message msg) throws Exception {
		System.out.println("here reply goes!");
 		sendMessage(this.getReplyMessage(body, msg));
	}
	class SearchAndAct{
		public MailSearchPattern msp;
		public MailAction ma;
		SearchAndAct(MailSearchPattern Msp, MailAction Ma){
			msp = Msp;
			ma = Ma;
		}
	}
	public enum IteratorList{
		INCOMING,OUTCOMING
	};
	Hashtable<Integer,SearchAndAct> actorsOutcoming = new Hashtable<Integer,SearchAndAct>(),
			actorsIncoming = new Hashtable<Integer,SearchAndAct>();
	public void removeIterator(IteratorList il,int code) {
		switch(il) {
			case INCOMING:
				actorsIncoming.remove(code);
				break;
			case OUTCOMING:
				actorsOutcoming.remove(code);
				break;
		}
	}
	public int addActor(IteratorList il,MailSearchPattern msp, MailAction ma)
	{
		int res = new Random().nextInt();
		switch(il) {
		case INCOMING:
			actorsIncoming.put(res, new SearchAndAct(msp,ma));
			break;
		case OUTCOMING:
			actorsOutcoming.put(res, new SearchAndAct(msp,ma));
			break;
		}
		return res;
	}
	public void sendMessage(Message m) throws Exception
	{
		Transport.send(m);
		for(SearchAndAct sa : actorsOutcoming.values())
			if(sa.msp.test(m))
				sa.ma.act(m);
	}
}
