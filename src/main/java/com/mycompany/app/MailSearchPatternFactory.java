/**
 * 
 */
package com.mycompany.app;

import javax.mail.Message;

/**
 * @author nailbiter
 *
 */
public class MailSearchPatternFactory {
	private static class TrivialSearchPattern implements MailSearchPattern{
		@Override
		public boolean test(Message m) throws Exception {
			return true;
		}
	}
	private static TrivialSearchPattern tvp_ = new TrivialSearchPattern();
	public static MailSearchPattern getTrivialPattern() { return tvp_;}
}
