package com.mycompany.app;

import java.util.Date;
import java.util.Date;
import org.json.JSONObject;
import java.util.Properties;
import java.util.Properties;
import java.util.Scanner;
import java.util.Scanner;
import java.util.Scanner;
import java.util.Scanner;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Folder;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Store;
import java.util.Properties;
import java.util.Scanner;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Address;
import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.impl.*;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;
import java.io.Writer;


public class Main{
	static IMAPDemo id = null;
	static SlackSession session = null;
	static SlackUser sb = null;
	static SlackWriter sw_ = null;
	static class SlackWriter extends Writer{
		public void flush(){}
		public void close(){}
		public SlackSession ss_ = null;
		public SlackChannel sc_ = null;
		public void write(char[] cbuf,int off,int len){
			String s = new String(cbuf,off,len);
			write(s);
		}
		@Override
		public void write(String s){
			if(ss_!=null && sc_!=null)
				ss_.sendMessage(sc_,s);
		}
	}
	public static void main(String[] args) throws Exception
	{
		
		id = new IMAPDemo();
		id.setWriter(sw_= new SlackWriter());
		String token = KeyRing.getBotWebToken();
		session = SlackSessionFactory.createWebSocketSlackSession(token);
		session.connect();
		sw_.ss_ = session;

		registeringAListener(session);
		
		for(;;);
	}
	public static void registeringAListener(SlackSession session)
	{
		// first define the listener
		SlackMessagePostedListener messagePostedListener = new SlackMessagePostedListener()
		{
			@Override
				public void onEvent(SlackMessagePosted event, SlackSession session)
				{
					SlackChannel channelOnWhichMessageWasPosted = event.getChannel();
					try{
						String messageContent = event.getMessageContent();
						//String channelName = channelOnWhichMessageWasPosted.getName();
						SlackUser messageSender = event.getSender();
						String senderUserName = messageSender.getUserName();

						if(senderUserName.equals(KeyRing.getUserName())){
							sw_.sc_ = channelOnWhichMessageWasPosted;
							id.reply(messageContent);
							sw_.write("that's all, folks!");
						}	
					}
					catch(Exception e)
					{
						session.sendMessage(channelOnWhichMessageWasPosted,
								String.format("e: %s",e.toString()));
					}
				}
		};
		//add it to the session
		session.addMessagePostedListener(messagePostedListener);
		//session.sendMessage(session.findChannelByName("general"),"hi");

		//that's it, the listener will get every message post events the bot can get notified on
		//(IE: the messages sent on channels it joined or sent directly to it)
	}
}
