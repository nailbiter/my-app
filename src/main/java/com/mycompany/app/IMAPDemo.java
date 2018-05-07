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

public class IMAPDemo {

	/**
	 * @param args
	 */
	static Date curDate = null;
	static Session sess = null;
	static Store st = null;
	static final String[] months = {
		"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug",
		"Sep","Oct","Nov","Dec"
	};
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
              SlackUser messageSender = event.getSender();
							System.out.println(String.format("got message %s on channel %s!",
							messageContent,
							channelOnWhichMessageWasPosted.getName()));
          }
      };
      //add it to the session
      session.addMessagePostedListener(messagePostedListener);

      //that's it, the listener will get every message post events the bot can get notified on
      //(IE: the messages sent on channels it joined or sent directly to it)
  }
	public static void main(String[] args) throws Exception{
		String token = KeyRing.getWebToken();
		//System.out.print(String.format("secret: %s\n",token));
		SlackSession session = SlackSessionFactory.createWebSocketSlackSession(token);
		session.connect();
		SlackChannel channel = session.findChannelByName("general"); //make sure bot is a member of the channel.
		session.sendMessage(channel, "hi im a bot" );

		registeringAListener(session);
		for(;;);
	}
	public IMAPDemo(String[] args) throws Exception{
		String host = KeyRing.getHost()
		int port = 993;
		String user = KeyRing.getUser();
		String password = KeyRing.getPassword();

		Properties props = System.getProperties();
		sess = Session.getInstance(props, null);
//		sess.setDebug(true);

		st = sess.getStore("imaps");
		st.connect(host, port, user, password);
		for(Folder f : st.getFolder("1").list()){
			System.out.printf("%s\n",f.getName());
		}
		Folder[] fol = {myGetFolder("INBOX"),
			st.getFolder("1").getFolder("Sent Messages")};
		fol[fol.length-1].open(Folder.READ_ONLY);

		Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
		String ename;
		JSONObject obj = null;
		SearchStruct ss = null;
		for(;;)
		{
			obj = new JSONObject();
			curDate = new Date();
			System.out.print("> ");
			ename = scanner.next();
			System.out.println("\tgot: "+ename);
			if(ename.equals("exit"))
				break;
			try{
				String[] fs = ename.split(" ");
				if(fs[0].equals("skype"))
				{
					String[] s = fs[1].split(":");
					obj	.put("hours",Integer.parseInt(s[0]))
							.put("mins",Integer.parseInt(s[1]));
					System.out.printf("\t\tskypeCall; %s %d, %02d:%02d;\n",
						IMAPDemo.months[curDate.getMonth()],curDate.getDate(),
						obj.getInt("hours"),obj.getInt("mins"));
					continue;
				}
				String[] s = fs[0].split(":");
				if(fs.length >= 2){
					//month/day
					if(fs[1].contains("/"))
					{
						String[] pieces = fs[1].split("/");
						obj	.put("month",Integer.parseInt(pieces[0])-1)
								.put("day",Integer.parseInt(pieces[1]));
					}
					else
						obj.put("diff",Integer.parseInt(fs[1]));
				}
				obj	.put("hours",Integer.parseInt(s[0]))
						.put("mins",Integer.parseInt(s[1]));
				System.out.printf("\t%s\n",obj.toString());
				ss = new SearchStruct(obj);
				for(int i = 0; i < fol.length; i++){
					if(!fol[i].isOpen())
						fol[i].open(Folder.READ_ONLY);
					System.out.println(String.format("\t%s",fol[i].getName()));
					Message[] ms = fol[i].getMessages();
					for(Message m : ms){
						try{
							if(ss.test(m))
								System.out.printf("\t\t%s\n",makeSubjectLine(m));
						}
						catch(Exception e){
							System.out.println("\t\t"+e.getMessage());
						}
					}
				}
			}
			catch(Exception e){
				e.printStackTrace();
				continue;
			}
		}
		for(int i = 0; i < fol.length; i++)
			fol[i].close(false);
		st.close();
	}
	static String makeSubjectLine(Message m) throws Exception
	{
		Date rd = m.getReceivedDate();
		String res =
		(isFrom(m,KeyRing.getKMail())?"fromK":"")+
		"; "+
		months[rd.getMonth()]+" "+rd.getDate()+", "+
		String.format("%02d:%02d",rd.getHours(),rd.getMinutes())+
		"; "+trimmedSubject(m.getSubject());
		return res;
	}
	static Folder myGetFolder(String name) throws Exception
	{
		Folder res = null;
		res = st.getFolder(name);
		if(res.exists()){
			for(Folder f : res.list()){
				System.out.println(f.getName());
			}
			//res.open(Folder.READ_ONLY);
		}
		else
			System.out.printf("%s is not exist.\n", name);
		return res;
	}

	static boolean isSeen(Flags flags)
	{
		//TODO
		//String flagstring = flags.toString
		return flags.toString().contains("\\Seen");
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
	static String trimmedSubject(String subject)
	{
		String res=subject, tmp;
		do{
			subject = res;
			//res = processed(subject)
			res = subject.replaceFirst("^Re: *","").replaceFirst("^Fwd: *","");
		}
		while(!res.equals(subject));
		return res;
	}
}
