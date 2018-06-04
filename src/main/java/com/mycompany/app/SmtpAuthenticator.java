package com.mycompany.app;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

class SmtpAuthenticator extends Authenticator {
	String user_,password_;
    public SmtpAuthenticator(String user,String password) 
    {
    		super();
    		user_ = user;
    		password_ = password;
    	}
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
    				return new PasswordAuthentication(user_, password_);
    }
}
