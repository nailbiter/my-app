package com.mycompany.app;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import gnu.getopt.Getopt;
import it.sauronsoftware.cron4j.Scheduler;

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

import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.OutputStreamWriter;
import java.io.Writer;


public class Main{
	static MailManager id = null;
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
			//System.out.format("plan to write: %s\n", s);
			if(ss_!=null && sc_!=null)
				ss_.sendMessage(sc_,s);
			else
				System.out.format("could not write: %s\n", s);
		}
	}
	public static void main(String[] args) throws Exception
	{
		Getopt g = new Getopt("testprog", args, "kt:");
		boolean kflag = false;
		int c = 0;
		String templateFolder = null;
		while ((c = g.getopt()) != -1) {
			if(c=='k')
			{
				//System.out.format("arg: %s",g.getOptarg());
				kflag = true;
				System.out.format("set kflag=true\n");
			}
			if(c=='t')
			{
				templateFolder = g.getOptarg();
				System.out.format("template folder: %s\n",templateFolder);
			}
		}
		if(templateFolder == null)
			throw new Exception("templateFolder == null");
		
		Scheduler scheduler = new Scheduler();
		StorageManager.init(templateFolder, scheduler);
		if(!false) {
			ArrayList<String> names =  StorageManager.getMailTemplateNames();
			for(String name : names) {
				System.out.format("template: %s\n", name);
			}
			
			Template temp = StorageManager.getTemplate("enchou");
			Map root = new HashMap();
	        root.put("user", "Big Joe");
	        /*Product latest = new Product();
	        latest.setUrl("products/greenmouse.html");
	        latest.setName("green mouse");
	        root.put("latestProduct", latest);*/
	        Writer out = new OutputStreamWriter(System.out);
	        try {
	        		temp.process(root, out);
	        }
	        catch(TemplateException e) {
	        		System.out.format("exception: %s,\n", e.getMessage());
	        }
	        ArrayList<String> vars = Util.getTemplateVars("enchou");
	        System.out.format("vars:\n");
	        for(String varname : vars) {
	        		System.out.format("\t%s\n", varname);
	        }
	        /*Map vars = temp.getMacros();
	        System.out.println("vars="+vars);
	        System.out.println("vars.len="+vars.size());
	        for(String s : temp.getCustomAttributeNames())
	        		System.out.format("\t%s\n",s);*/
//			template.render(JtwigModel.newModel(), System.out);
			
			return;
		}
		
		id = new MailManager( kflag? KeyRing.getKMail() : KeyRing.getMyMail(), scheduler);
		id.setWriter(sw_= new SlackWriter());
		
		scheduler.start();
		
		String token = KeyRing.getBotWebToken();
		session = SlackSessionFactory.createWebSocketSlackSession(token);
		session.connect();
		sw_.ss_ = session;
		sw_.sc_ = session.findChannelByName("general");
		if(sw_.sc_==null)
			System.out.format("NOPE\n");

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
							System.out.format("got message: %s\n", messageContent);
							id.process(messageContent);
							//sw_.write("that's all, folks!");
						}	
					}
					catch(Exception e)
					{
						session.sendMessage(channelOnWhichMessageWasPosted,
								String.format("e: %s",e.toString()));
					}
				}
		};
		session.addMessagePostedListener(messagePostedListener);
	}
}
