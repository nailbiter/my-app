package com.mycompany.app;

import javax.mail.Message;

public interface MailAction {
	void act(Message message) throws Exception;
}
