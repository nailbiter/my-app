package com.mycompany.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.Date;
import org.json.JSONObject;
import org.w3c.tidy.Tidy;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.util.Properties;
import java.util.Properties;
import java.util.Scanner;
import java.util.Scanner;
import java.util.Scanner;
import java.util.Scanner;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Folder;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
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

import com.lowagie.text.DocumentException;
import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.impl.*;
import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.listeners.SlackMessagePostedListener;

public class IMAPDemo {
	static Date curDate = null;
	static Session sess = null;
	static Store st = null;
	static Folder[] fol = null;
	static final String[] months = {
		"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug",
		"Sep","Oct","Nov","Dec"
	};
	Writer writer_ = null;
	public void setWriter(Writer w){writer_ = w;}
	void write(String s)throws Exception{if(writer_!=null)writer_.write(s);}
	public IMAPDemo() throws Exception{
		String host = KeyRing.getHost();
		int port = 993;
		String user = KeyRing.getUser();
		String password = KeyRing.getPassword();

		Properties props = System.getProperties();
		sess = Session.getInstance(props, null);

		st = sess.getStore("imaps");
		st.connect(host, port, user, password);
		for(Folder f : st.getFolder("1").list()){
			System.out.printf("%s\n",f.getName());
		}
		fol = new Folder[2];
		fol[0] = myGetFolder("INBOX");
		fol[1] = st.getFolder("1").getFolder("Sent Messages");
		fol[fol.length-1].open(Folder.READ_ONLY);
		
		
		boolean[] flags = new boolean[] {false,false,false};
		fol[0].open(Folder.READ_ONLY);
		Message msg = fol[0].getMessage(fol[0].getMessageCount());
		String filename = msg.getSubject();
		if(flags[0])
		{
	        Multipart mp = (Multipart) msg.getContent();
	        BodyPart bp = mp.getBodyPart(1);
	        FileOutputStream os = new FileOutputStream(filename + ".html");
	        bp.writeTo(os);
		}
		
        //use jtidy to clean up the html 
        if(flags[1])
        		cleanHtml(filename);
        //save it into pdf
        if(flags[2])
        		createPdf(filename);
	}
	public static void cleanHtml(String filename) {
	    File file = new File(filename + ".html");
	    InputStream in = null;
	    try {
	        in = new FileInputStream(file);
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    OutputStream out = null;
	    try {
	        out = new FileOutputStream(filename + ".xhtml");
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    }
	    final Tidy tidy = new Tidy();
	    tidy.setQuiet(false);
	    tidy.setShowWarnings(true);
	    tidy.setShowErrors(0);
	    tidy.setMakeClean(true);
	    tidy.setForceOutput(true);
	    org.w3c.dom.Document document = tidy.parseDOM(in, out);
	}
	public static void createPdf(String filename)
	        throws IOException, DocumentException {
	    OutputStream os = new FileOutputStream(filename + ".pdf");
	    ITextRenderer renderer = new ITextRenderer();
	    renderer.setDocument(new File(filename + ".xhtml"));
	    renderer.layout();
	    renderer.createPDF(os) ;
	    os.close();
	}
	void reply(String input) throws Exception
	{
		Scanner scanner = new Scanner(System.in).useDelimiter("\\n");
		String ename;
		JSONObject obj = null;
		SearchStruct ss = null;

		obj = new JSONObject();
		curDate = new Date();
		//System.out.print("> ");
		//ename = scanner.next();
		ename = input;
		System.out.println("\tgot: "+ename);
		/*if(ename.equals("exit"))
		  break;*/
		try{
			String[] fs = ename.split(" ");
			if(fs[0].equals("ktalk"))
			{
				String[] s = fs[1].split(":");
				obj	.put("hours",Integer.parseInt(s[0]))
					.put("mins",Integer.parseInt(s[1]));
				if(fs.length>=2)
					obj.put("diff",Integer.parseInt(fs[2]));
				write( String.format("ktalk; %s %d, %02d:%02d;\n",
					IMAPDemo.months[curDate.getMonth()],curDate.getDate()+obj.optInt("diff",0),
					obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
				return;
			}
			if(fs[0].equals("skype"))
			{
				String[] s = fs[1].split(":");
				obj	.put("hours",Integer.parseInt(s[0]))
					.put("mins",Integer.parseInt(s[1]));
				write( String.format("skypeCall; %s %d, %02d:%02d;\n",
					IMAPDemo.months[curDate.getMonth()],curDate.getDate(),
					obj.getInt("hours"),obj.getInt("mins")));//FIXME: replace with call to makeSubjectLine
				return;
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
							write(String.format("%s\n",makeSubjectLine(m)));
					}
					catch(Exception e){
						System.out.println("\t\t"+e.getMessage());
					}
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			//continue;
		}
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
