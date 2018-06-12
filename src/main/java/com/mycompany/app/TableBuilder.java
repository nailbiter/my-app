package com.mycompany.app;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;

public class TableBuilder {
	ArrayList<ArrayList<String>> tokens = new ArrayList<ArrayList<String>>();
	ArrayList<Integer> lengths = new ArrayList<Integer>();
	public TableBuilder addRow(JSONArray row,int index)
	{
		if(row.length()==0)
			return this;
		
		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < row.length(); i++)
		{
			list.add(row.getString(i));
			lengths.add(i, Math.max(lengths.get(i), row.getString(i).length()));
		}
		tokens.add(index,list);
			
		/*for(int i = 0; i < row.length(); i++)
		{
			tokens.get(i).add(index,row.getString(i));
			lengths.add(i, Math.max(lengths.get(i), row.getString(i).length()));
		}*/
		return this;
	}
	public TableBuilder addCol(JSONArray col,int index)
	{
		if(col.length()==0)
			return this;
		
		int maxlen = -1;
		for(int i = 0; i < col.length(); i++)
		{
			tokens.get(i).add(index,col.getString(i));
			maxlen = Math.max(col.getString(i).length(), maxlen);
		}
		lengths.add(index,maxlen);
		return this;
	}
	public TableBuilder newRow()
	{
		tokens.add(new ArrayList<String>());
		return this;
	}
	public TableBuilder addNewlineAndTokens(String t1,String t2,String t3) 
	{
		newRow();
		addToken(t1);
		addToken(t2);
		addToken(t3);
		return this;
	}
	public TableBuilder addNewlineAndTokens(String t1,String t2) 
	{
		newRow();
		addToken(t1);
		addToken(t2);
		return this;
	}
	public TableBuilder addNewlineAndTokens(String[] tokens)
	{
		newRow();
		for(int i = 0; i < tokens.length; i++)
			addToken(tokens[i]);
		return this;
	}
	public TableBuilder addToken(String token)
	{
		tokens.get(tokens.size()-1).add(token);
		int idx = tokens.get(tokens.size()-1).size() - 1;
		if(lengths.size()<=idx)
			lengths.add(0);
		if(lengths.get(idx).compareTo(token.length())<0)
			lengths.set(idx, token.length());
		return this;
	}
	public TableBuilder addToken(int value) { return addToken(Integer.toString(value)); }
	protected final static int OFFSET = 3;
	@Override
	public String toString()
	{
		for(int i = 0;i < lengths.size(); i++)
			System.out.print(lengths.get(i)+" ");
		System.out.println("");
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tokens.size(); i++)
		{
			for(int j = 0; j < tokens.get(i).size(); j++)
			{
				sb.append(StringUtils.rightPad(tokens.get(i).get(j), lengths.get(j) + OFFSET));
			}
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
