package com.mycompany.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

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
	public class ForwardAction implements MailAction{
		private String to_ = null; 
		ForwardAction(String to){ to_ = to;}
		@Override
		public void act(Message message) throws Exception {
			System.out.println("here I go");
			
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
            System.out.format("subj: %s\n", forward.getSubject());
            
            Transport.send(forward);
		}
	
	}

	public MailAccount(String host, String user, String password, int port,String address) throws MessagingException
	{
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
	}
	public Folder openFolder(String name) throws Exception
	{
		Folder fol = myGetFolder(name);
		fol.open(Folder.READ_ONLY);		
		fols.add(fol);
		return fol;
	}
	public Folder openFolder(String name1, String name2) throws Exception
	{
		Folder fol = st.getFolder(name1).getFolder(name2);
		fol.open(Folder.READ_ONLY);
		fols.add(fol);
		return fol;
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
	/**
	 * FIXME: maybe, it should be taken away from this class, as it is too complicated for it
	 * @param msp
	 * @param ma
	 * @throws MessagingException
	 */
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
	public ForwardAction getForwardAction(String to)
	{
		return new ForwardAction(to);
	}
	class ReplyAction implements MailAction{
		final String body = 
				"先生\n" + 
				"\n" + 
				"ご指導どうもありがとうございます！\n" + 
				"お忙しい中、貴重なお時間を取っていただき、まことに有難うございます。\n" + 
				"\n" +
				"先生のメールを頂きました。しかし、今は他の研究タスクを致しますので、直ぐに返事ができません。\n"+
				"今のタスクを終わったら、直ぐにご返事致します。大変申し訳ございません。\n"+
				"\n"+
				"アレックス";
		ReplyAction()
		{
			
		}
		@Override
		public void act(Message msg) throws Exception {
			System.out.println("here reply goes!");
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
     				text = getText(((Multipart)msg.getContent()).getBodyPart(0));
     			
     			replyText = text.replaceAll("(?m)^", "> ");
     		}
     		catch(Exception e)
     		{
     			e.printStackTrace(System.out);
     		}
     		System.out.println("before the end");
     		replyMessage.setText(body + "\n" + replyText);
     		Transport.send(replyMessage);
		}
	}
	private boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws
                MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }
	public MailAction getReplyAction() {
		return new ReplyAction();
	}
}
