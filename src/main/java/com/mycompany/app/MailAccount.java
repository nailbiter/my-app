package com.mycompany.app;

import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
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
	public MailAction myMA = new MailAction(){

		@Override
		public void act(Message message) throws Exception {
			System.out.println("here I go");
			
			Message forward = new MimeMessage(sess);
			forward.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(KeyRing.getTrello()));
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
            Transport.send(forward);
            /*Transport t = session.getTransport("smtp");
            try {
               //connect to the smpt server using transport instance
		  //change the user and password accordingly
               t.connect("abc", "*****");
               t.sendMessage(forward, forward.getAllRecipients());
            } finally {
               t.close();
            }*/
		}
		
	};

	public MailAccount(String host, String user, String password, int port,String address) throws MessagingException
	{
		Properties props = System.getProperties();
		props.put("mail.debug", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.user", user);
        props.put("mail.smtp.password",password);
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.EnableSSL.enable","true");
        SmtpAuthenticator authentication = new SmtpAuthenticator(user,password);
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
}
