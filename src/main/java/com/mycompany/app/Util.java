package com.mycompany.app;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Template;

public class Util {
	public static ArrayList<String> getTemplateVars(String name){
		ArrayList<String> res = new ArrayList<String>();
		try {
//			System.out.println("template name: "+t.getSourceName());
			String src = StorageManager.getFile(name + StorageManager.TEMPLATEEXTENSION);
//			System.out.println("src: "+src);
			
			Pattern p = Pattern.compile("\\$\\{([^}]+)\\}");
			Matcher m = p.matcher(src); 
			while(m.find()) {
				res.add(m.group(1));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return res;
		}
		return res;
	}
}
