package com.mycompany.app;


import org.json.JSONArray;
import org.json.JSONObject;

public interface MyManager extends Replier{
	abstract public String getResultAndFormat(JSONObject res) throws Exception;
	public JSONArray getCommands();
}
