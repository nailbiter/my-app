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
			return !isSeen(m.getFlags()) && MailUtil.isFrom(m,email_);
		else
			return MailUtil.isFrom(m,email_);
	}
	static boolean isSeen(Flags flags)
	{
		return flags.toString().contains("\\Seen");
	}
}
