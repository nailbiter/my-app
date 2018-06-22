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

class SearchStruct implements MailSearchPattern{
  protected Date curDate = null;
  protected int mins_,
  hours_,month_,date_;
  String mail_ = null;
  SearchStruct(JSONObject obj) throws Exception{
    curDate = new Date();
    mins_ = obj.getInt("mins");
    hours_ = obj.getInt("hours");
    date_ = (obj.optInt("day",curDate.getDate())+obj.optInt("diff",0));
    month_ = obj.optInt("month",curDate.getMonth());
    mail_ = obj.optString("mail");
  }
  public boolean test(Message m) throws Exception
  {
    Date sd = null;
    if(false)
			sd = m.getSentDate();
		else
			sd = m.getReceivedDate();
    boolean res = sd.getMinutes()==mins_ &&
    		sd.getHours()==hours_ &&
    		date_==sd.getDate()&&
    		month_==sd.getMonth();
    if(mail_!=null)
    		res = res && (MailUtil.isFrom(m, mail_) || MailUtil.isTo(m, mail_));
    
    return res;
  }
}
