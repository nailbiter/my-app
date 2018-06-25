/**
 * 
 */
package com.mycompany.app;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

/**
 * @author nailbiter
 *
 */
public class MailUtil {
	public static final String[] months = {
			"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug",
			"Sep","Oct","Nov","Dec"
		};
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
	static boolean isTo(Message m,String tmail) throws Exception
	{
		//Address[] receivers = m.getRecipients(Message.RecipientType.TO);
		Address[] receivers = m.getAllRecipients();
		for(int i = 0; i < receivers.length; i++)
		{
			if(receivers[i].toString().contains(tmail))
				return true;
		}
		return false;
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
	private static boolean textIsHtml = false;

    /**
     * Return the primary text content of the message.
     */
    static String getText(Part p) throws
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
    static String EMLFOLDER="emls";
    static Random rand = new Random();
    static int HASHLEN = 10;
    static String RANDSYMBOLS = "ABCDEFGHIJKLMNOPQRSTUWXYZ"+"abcdefghijklmnopqrstuwxyz"+"0123456789";
    static void saveMessageToFile(Message message) throws Exception {
    		StringBuilder sb = new StringBuilder();
    		for(int i = 0; i < HASHLEN; i++)
    			sb.append(RANDSYMBOLS.charAt(rand.nextInt(RANDSYMBOLS.length())));
    		String fileNameBase = EMLFOLDER + "/" + sb.toString();
    		OutputStream out = new FileOutputStream(fileNameBase+".eml");
    		message.writeTo(out);
    		out.close();

    		PrintWriter out2 = new PrintWriter(fileNameBase+".txt");
    		out2.print(MailUtil.makeSubjectLine(message));
    		out2.close();
    }
}
