package com.mycompany.app;

import javax.mail.Message;

public interface MailSearchPattern {
	public boolean test(Message m) throws Exception;
}
