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
  static SlackUser sb = null;
  public static void main(String[] args) throws Exception
  {
    id = new IMAPDemo();
    String token = KeyRing.getBotWebToken();
		//System.out.print(String.format("secret: %s\n",token));
		session = SlackSessionFactory.createWebSocketSlackSession(token);
		session.connect();
		//SlackChannel channel = session.findChannelByName("general"); //make sure bot is a member of the channel.
		//session.sendMessage(channel, "hi im a bot" );

    /*System.out.println("users");
    for(SlackUser su : session.getUsers())
      System.out.println("\t"+su.toString());
    System.out.println("bots");
    for(SlackBot su : session.getBots())
      System.out.println("\t"+su.toString());
    sb = session.findUserById("UAKCACQKC");
    System.out.println("\tbot: "+sb.toString());*/

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
              String channelName = channelOnWhichMessageWasPosted.getName();
              SlackUser messageSender = event.getSender();
              String senderUserName = messageSender.getUserName();

							/*System.out.println(String.format("got message %s on channel %s by user %s,bot %s!: %s",
							       messageContent,channelName,messageSender.getUserName(),
                     event.getBot(),event.toString()));*/
              if(senderUserName.equals(KeyRing.getUserName()))
                session.sendMessage(channelOnWhichMessageWasPosted,
                  id.reply(messageContent));
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
