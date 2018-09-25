package com.mycompany.app;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
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

import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;

import it.sauronsoftware.cron4j.Scheduler;

public class ProcessAndSendMail {
	static boolean CheckPragma(JSONObject obj, String line,String pragma) {
		if(line.startsWith("#"+pragma)) {
			obj.put(pragma, line.substring(pragma.length()+2));
			return true;
		}else
			return false;
	}
	private JSONObject obj;
	private MailAccount mc_;
	private Session sess;
	private String address_;
	private Store st;
	ProcessAndSendMail(String filename) throws IOException, MessagingException{
//		mc_ = new MailAccount(KeyRing.getHost(),KeyRing.getUser(),KeyRing.getPassword(),993,KeyRing.getMyMail(),
//				new Scheduler());
	    obj = new JSONObject();
	    StringBuilder body = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if(CheckPragma(obj,line,"TOPIC"))
		    		continue;
		    	if(CheckPragma(obj,line,"TO"))
		    		continue;
		    	else if(CheckPragma(obj,line,"CC"))
		    		continue;
		    	else if(CheckPragma(obj,line,"ATTACHMENT"))
		    		continue;
		    	else
		    		body.append(line+"\n");
		    }
		}
		obj.put("BODY", body.toString());
	}
	void send() throws MessagingException {
		send(KeyRing.getHost(),KeyRing.getUser(),KeyRing.getPassword(),993,KeyRing.getMyMail());
	}
	private void send(String host, String user, String password, int port,String address) throws MessagingException {
		System.out.println(obj.toString(2));
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
		
		try {
	         MimeMessage message = new MimeMessage(sess);
	         message.setFrom(new InternetAddress("leontiev@ms.u-tokyo.ac.jp"));
	         message.addRecipient(Message.RecipientType.TO, new InternetAddress(obj.getString("TO")));
	         if(obj.has("CC"))
	        	 message.addRecipient(Message.RecipientType.CC, new InternetAddress(obj.getString("CC")));
	         message.setSubject(obj.getString("TOPIC"));
	         if(!obj.has("ATTACHMENT"))
	        	 message.setText(obj.getString("BODY"));
	         else {
	             BodyPart messageBodyPart = new MimeBodyPart();
	             messageBodyPart.setText(obj.getString("BODY"));
	             Multipart multipart = new MimeMultipart();
	             multipart.addBodyPart(messageBodyPart);
	             messageBodyPart = new MimeBodyPart();
	             String filename = obj.getString("ATTACHMENT");
	             DataSource source = new FileDataSource(filename);
	             messageBodyPart.setDataHandler(new DataHandler(source));
	             messageBodyPart.setFileName(FilenameUtils.getName(filename));
	             multipart.addBodyPart(messageBodyPart);
	             message.setContent(multipart );
	         }
	         Transport.send(message);
	         System.out.println("Sent message successfully....");
	      } catch (MessagingException mex) {
	         mex.printStackTrace();
	      }
	}
}
