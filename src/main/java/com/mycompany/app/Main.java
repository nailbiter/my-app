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


public class Main{
  static IMAPDemo id = null;
  static SlackSession session = null;
  public static void main(String[] args) throws Exception
  {
    id = new IMAPDemo();
    String token = KeyRing.getWebToken();
		//System.out.print(String.format("secret: %s\n",token));
		session = SlackSessionFactory.createWebSocketSlackSession(token);
		session.connect();
		//SlackChannel channel = session.findChannelByName("general"); //make sure bot is a member of the channel.
		//session.sendMessage(channel, "hi im a bot" );

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
              String messageContent = event.getMessageContent();
              String channelName = channelOnWhichMessageWasPosted.getName();
              SlackUser messageSender = event.getSender();
							System.out.println(String.format("got message %s on channel %s by user %s!",
							       messageContent,channelName,messageSender.getUserName()));
              if(!true)
              session.sendMessage(channelOnWhichMessageWasPosted,
                String.format("was posted: %s",messageContent));
          }
      };
      //add it to the session
      session.addMessagePostedListener(messagePostedListener);

      //that's it, the listener will get every message post events the bot can get notified on
      //(IE: the messages sent on channels it joined or sent directly to it)
  }
}
