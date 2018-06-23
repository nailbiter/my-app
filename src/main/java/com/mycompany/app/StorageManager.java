package com.mycompany.app;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jtwig.JtwigTemplate;

import it.sauronsoftware.cron4j.Scheduler;

public class StorageManager {
	protected static String jarFolder_;
	protected static Hashtable<String,JSONObject> registeredObjects = new Hashtable<String,JSONObject>();
	protected static Logger logger_ = Logger.getLogger(StorageManager.class.getName());
	protected static MyManager myManager = null;
	public static MyManager getMyManager() {return myManager;}
	protected static Scheduler scheduler_ = null;
	public static void init(String jarFolder, Scheduler scheduler) throws Exception
	{
		jarFolder_ = jarFolder;
		myManager = new MyManager()
		{
			@Override
			public String getResultAndFormat(JSONObject res) throws Exception {
				if(res.has("name"))
				{
					System.out.println(this.getClass().getName()+" got comd: /"+res.getString("name"));
					if(res.getString("name").compareTo("dump")==0)
					{
						dumpAllObjects();
						StringBuilder sb = new StringBuilder();
						sb.append("dumped:\n");
						Iterator<String> itr = registeredObjects.keySet().iterator();
				    		String str;
				    		while (itr.hasNext()) {
			    			 try {
			    				   str = itr.next();
			    				   sb.append("\t"+str+"\n");
			    			 }
			    			 catch(Exception e)
			    			 {
			    				 e.printStackTrace(System.out);
			    			 }
			    		    }
				    		return sb.toString();
					}
				}
				return null;
			}
			
			@Override
			public JSONArray getCommands() {
				return new JSONArray("[{\"name\":\"dump\",\"args\":[],\"help\":\"dump all text files\"}]");
			}

			@Override
			public String processReply(int messageID,String msg) {
				return null;
			}
	
		};
		Runtime.getRuntime().addShutdownHook(new Thread()

	    {
	        @Override
	        public void run()
	        {
	            System.out.println("Shutdown hook ran!");
	            dumpAllObjects();
	        }});
		scheduler_ = scheduler;
		scheduler_.schedule("1,31 * * * *", new Runnable() {
			public void run() {
				StorageManager.dumpAllObjects();
			}
		});
		//scheduler_.start();
	}
	static JtwigTemplate getTemplate(String name) {
		
		return JtwigTemplate.classpathTemplate(String.format("%s/%s.twig", jarFolder_,name));
	}
	static ArrayList<String> getMailTemplateNames(){
		File f = new File(jarFolder_);
		ArrayList<File> files = new ArrayList<File>(Arrays.asList(f.listFiles()));
		ArrayList<String> res = new ArrayList<String>();
		for(File file : files) {
			String name = file.getName(); 
			if(name.endsWith(".twig"))
				res.add(name.substring(0, name.length() - 5));
		}
		
		return res;
	}
	public static String getFile(String name) throws Exception
	{
		FileReader fr = null;
		String fname = jarFolder_+name;
		logger_.info(String.format("fname=%s", fname));
		
		fr = new FileReader(fname);
		StringBuilder sb = new StringBuilder();
        int character;
        while ((character = fr.read()) != -1) {
        		sb.append((char)character);
            //System.out.print((char) character);
        }
        System.out.println("found "+sb.toString());
		fr.close();
		String res = sb.toString();
		logger_.info(String.format("res=%s", res));
		return res;
	}
	public static JSONObject get(String name,boolean register)
	{
		FileReader fr = null;
		JSONObject res = null;
		try {
			System.out.println("StorageManager got "+name);
			String fname = jarFolder_+name+".json";
			System.out.println("storageManager gonna open: "+fname);
			fr = new FileReader(fname);
			StringBuilder sb = new StringBuilder();
            int character;
            while ((character = fr.read()) != -1) {
            		sb.append((char)character);
                //System.out.print((char) character);
            }
            System.out.println("found "+sb.toString());
			fr.close();
			res = (JSONObject) (new JSONTokener(sb.toString())).nextValue();
		}
		catch(Exception e) {
			System.out.println(String.format("found nothing: name=%s", name));
			e.printStackTrace(System.out);
			res = new JSONObject();
		}
		if(register)
			register(name,res);
		return res;
	}
	protected static void register(String name, JSONObject ref)
	{
		System.out.println("register "+name);
		registeredObjects.put(name, ref);
	}
	protected static void dumpAllObjects()
	{
		Iterator<String> itr = registeredObjects.keySet().iterator();
		String str;
		FileWriter fw;
		System.out.println("was here on shutdown");
		while (itr.hasNext()) {
			 try {
				   str = itr.next();
			       System.out.println("Key: "+str+" & Value: "+registeredObjects.get(str));
			       fw = new FileWriter(jarFolder_+str+".json");
			       fw.write(registeredObjects.get(str).toString(2));
			       fw.close(); 
			 }
			 catch(Exception e)
			 {
				 e.printStackTrace(System.out);
			 }
		    }
	}
}
