package com.mycompany.app;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;

public class IsFrom implements MailSearchPattern {
	String email_ = null;
	final boolean onlyNew_;
	public IsFrom(String email) { this(email,true); }
	public IsFrom(String email, boolean onlyNew)
	{
		email_ = email;
		onlyNew_ = onlyNew;
	}
	@Override
	public boolean test(Message m) throws Exception {
		if(onlyNew_)
			return !isSeen(m.getFlags()) && isFrom(m,email_);
		else
			return isFrom(m,email_);
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
	static boolean isSeen(Flags flags)
	{
		return flags.toString().contains("\\Seen");
	}
}
