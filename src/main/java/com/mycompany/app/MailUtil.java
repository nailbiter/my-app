/**
 * 
 */
package com.mycompany.app;

import java.util.Date;

import javax.mail.Address;
import javax.mail.Message;

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
}
