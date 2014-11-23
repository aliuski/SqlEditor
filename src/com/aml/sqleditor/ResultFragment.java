package com.aml.sqleditor;

import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ResultFragment extends Fragment{
	final static String ARG_SQLLINE = "sqlline";
    Vector<Vector<String>> row;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {

        return inflater.inflate(R.layout.result_view, container, false);
    }
    
    @Override
    public void onStart() {
        super.onStart();

        Bundle args = getArguments();
        if (args != null)
        	updateSqlView(args.getString(ARG_SQLLINE));
    }
    
    public void updateSqlView(String sql) {
    	if(sql.length() < 10)
    		return;
    	
    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

    	String userid = sharedPrefs.getString("sqleditorUserid", null);
    	String password = sharedPrefs.getString("sqleditorPassword", null);
    	String dbname = sharedPrefs.getString("sqleditorDBName", null);
    	String dbaddress = sharedPrefs.getString("sqleditorIPAddress", null);
    	int sqleditortype = Integer.parseInt(sharedPrefs.getString("sqleditorType","1"));
        
		if(sqleditortype == 1){
    		if(userid != null && password != null)
    			new SqlSearchWebServiceTask().execute(
    				"http://"+dbaddress+"/sqleditorret.php?sqlline="+
    						java.net.URLEncoder.encode(sql)+
    						"&userid="+java.net.URLEncoder.encode(userid)+
    						"&password="+java.net.URLEncoder.encode(password)+
    						"&dbname="+java.net.URLEncoder.encode(dbname));
		} else {
			SqLiteDb jdbc = new SqLiteDb(getActivity(),dbname);
    		jdbc.tableFromExec(sql,(TableLayout)getActivity().findViewById(R.id.txtSqlresult));
		} 
    }

    private Vector<Vector<String>> SqlSearchTable(String word) {
    	row = new Vector<Vector<String>>();
        try {
        	SAXParserFactory factory = SAXParserFactory.newInstance();
        	SAXParser saxParser = factory.newSAXParser();
         
        	DefaultHandler handler = new DefaultHandler() {
        		
        	Vector<String> column = new Vector<String>();
        	Vector<String> rowv = new Vector<String>();
        	
        	boolean rowtype = false;
        	
        	public void startElement(String uri, String localName,String qName, 
                        Attributes attributes) throws SAXException {
        		rowtype = !qName.equalsIgnoreCase("row") && !qName.equalsIgnoreCase("table");
        		if(rowv != null && rowtype)
         			rowv.add(qName);
        	}

        	public void endElement(String uri, String localName,
        		String qName) throws SAXException {
        		if(qName.equalsIgnoreCase("row")) {
        			row.add(column);
        			column = new Vector<String>();
        			if(rowv != null){
        				for(int loop=0;loop<rowv.size();loop++)
        					column.add(rowv.get(loop));
        				row.add(0,column);
        				column = new Vector<String>();
        				rowv = null;
        			}
        		}
        	}

        	public void characters(char ch[], int start, int length) throws SAXException {
		        if(rowtype){
		                column.add(new String(ch, start, length));
		        		rowtype = false;
		        }
        	}

             };
               saxParser.parse(word, handler);

             } catch (Exception e) {
               e.printStackTrace();
             }
        return row;
    }

    private class SqlSearchWebServiceTask extends AsyncTask
    <String, Void, Vector<Vector<String>>> {
        protected Vector<Vector<String>> doInBackground(String... urls) {
            return SqlSearchTable(urls[0]);
        }
        
        protected void onPostExecute(Vector<Vector<String>> result) {
    		TableRow.LayoutParams params = new TableRow.LayoutParams(LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT);
            TableLayout table = (TableLayout)getActivity().findViewById(R.id.txtSqlresult);
            int count = table.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = table.getChildAt(i);
                if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
            }
            for (int i = 0; i < result.size(); i++) {
            	Vector<String> v = (Vector<String>)result.get(i);
                TableRow row = new TableRow(getActivity());
                for (int j = 0; j < v.size(); j++) {
                    TextView text = new TextView(getActivity());
                    text.setLayoutParams(params);
                    text.setText((j==0) ? (String)v.get(j) : "|"+v.get(j));
                    row.addView(text);
                }
                table.addView(row);
            }
        }
    }
}
