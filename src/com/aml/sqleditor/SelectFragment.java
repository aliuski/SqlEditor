package com.aml.sqleditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SelectFragment extends Fragment{
    OnSqlSelectedListener mCallback;

	private String userid;
	private String password;
	private String dbname;
	private String dbaddress;
	private int sqleditortype;
	
    public interface OnSqlSelectedListener {
        public void onSqlSelected(String sqlline);
    }
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (OnSqlSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSqlSelectedListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, 
        Bundle savedInstanceState) {
    	
    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
    	userid = sharedPrefs.getString("sqleditorUserid", null);
    	password = sharedPrefs.getString("sqleditorPassword", null);
    	dbname = sharedPrefs.getString("sqleditorDBName", null);
    	dbaddress = sharedPrefs.getString("sqleditorIPAddress", null);
    	sqleditortype = Integer.parseInt(sharedPrefs.getString("sqleditorType","1"));

        return inflater.inflate(R.layout.select_view, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(dbaddress != null && dbname != null && userid != null && password != null || sqleditortype == 2)
        	initSystem();
    }
    
    public void initSystem(){
    	
    	if(sqleditortype == 1)
	        new AccessWebServiceTask().execute("http://"+dbaddress+"/sqleditor.php?userid="+
	        		java.net.URLEncoder.encode(userid)+"&password="+java.net.URLEncoder.encode(password)+
	        		"&dbname="+java.net.URLEncoder.encode(dbname));
    	else
    		jdbcConnection();
    	
        final EditText txtsqlsearch = (EditText) getActivity().findViewById(R.id.txtSqlsearch);

        Button tablebutton = (Button) getActivity().findViewById(R.id.tableButton);
        tablebutton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Spinner tablespinner = (Spinner) getActivity().findViewById(R.id.tableSpinner);
        		txtsqlsearch.append((String)tablespinner.getSelectedItem()+' ');
        	}
      	});
        Button columnbutton = (Button) getActivity().findViewById(R.id.columnButton);
        columnbutton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Spinner columnpinner = (Spinner) getActivity().findViewById(R.id.columnSpinner);        		
        		String s = (String)columnpinner.getSelectedItem();
        		txtsqlsearch.append(s.substring(0,s.indexOf(' '))+' ');
        	}
      	});

        Button btnExec = (Button) getActivity().findViewById(R.id.btnExec);
        btnExec.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		mCallback.onSqlSelected(txtsqlsearch.getText().toString());
        	}
      	});

        Button btnClear = (Button) getActivity().findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		txtsqlsearch.setText("");
        	}
      	});
    }

    private Map<String,ArrayList<String>> TableDefinition(String word) {
    	final Map<String,ArrayList<String>> hm = new HashMap<String,ArrayList<String>>();
        try {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
    	SAXParser saxParser = factory.newSAXParser();
     
    	DefaultHandler handler = new DefaultHandler() {
     
    	String colname = null;
    	String tablename = null;
    	ArrayList<String> v = new ArrayList<String>();
    	
    	public void startElement(String uri, String localName,String qName, 
                    Attributes attributes) throws SAXException {
    		if(qName.equalsIgnoreCase("table"))
     			tablename = attributes.getValue("name");
    		else if (qName.equalsIgnoreCase("column"))
    			colname = attributes.getValue("name");
    	}

    	public void endElement(String uri, String localName,
    		String qName) throws SAXException {
    		if(qName.equalsIgnoreCase("table")) {
    			hm.put(tablename,v);
    			v = new ArrayList<String>();
    		}
    	}
     
    	public void characters(char ch[], int start, int length) throws SAXException {
    		if (colname != null) {
    			v.add(colname + ' ' + new String(ch, start, length));
    			colname = null;
    		}
    	}
         };
     
           saxParser.parse(word, handler);

         } catch (Exception e) {
           e.printStackTrace();
           return null;
         }
        return hm;
    }

    private class AccessWebServiceTask extends AsyncTask
    <String, Void, Map<String,ArrayList<String>>> {
        protected Map<String,ArrayList<String>> doInBackground(String... urls) {
            return TableDefinition(urls[0]);
        }
        
        protected void onPostExecute(Map<String,ArrayList<String>> result) {
        	if(result == null){
            	Toast.makeText(getActivity(),"Salasana virheellinen!"
            			,Toast.LENGTH_SHORT).show();
            	userid = null;
            	password = null;
        		return;
        	}
            Spinner tablespinner = (Spinner) getActivity().findViewById(R.id.tableSpinner);
            List<String> list = new ArrayList<String>();
        	for(Map.Entry<String, ArrayList<String>> entry : result.entrySet())
        		list.add(entry.getKey());
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
    	    		android.R.layout.simple_spinner_item, list);
    	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	    tablespinner.setAdapter(dataAdapter);
    	    Spinner columnpinner = (Spinner) getActivity().findViewById(R.id.columnSpinner);
    	    list = (ArrayList<String>)result.get((String)tablespinner.getSelectedItem());
    	    dataAdapter = new ArrayAdapter<String>(getActivity(),
    	    		android.R.layout.simple_spinner_item, list);
    	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	    columnpinner.setAdapter(dataAdapter);

    	    tablespinner.setOnItemSelectedListener(new CustomOnItemSelectedListener(result));
        }
    }
    
    public class CustomOnItemSelectedListener implements OnItemSelectedListener {
    	Map<String,ArrayList<String>> result;
    	public CustomOnItemSelectedListener(Map<String,ArrayList<String>> result){
    		this.result = result;
    	}
    	
    	  public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
    		  Spinner tablespinner = (Spinner) getActivity().findViewById(R.id.tableSpinner);
    		  Spinner columnpinner = (Spinner) getActivity().findViewById(R.id.columnSpinner);
    		  List<String> list = (ArrayList<String>)result.get((String)tablespinner.getSelectedItem());
    		  ArrayAdapter<String>dataAdapter = new ArrayAdapter<String>(getActivity(),
    				  android.R.layout.simple_spinner_item, list);
    		  dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		  columnpinner.setAdapter(dataAdapter);
    	  }
    	 
    	  @Override
    	  public void onNothingSelected(AdapterView<?> arg0) {
    		// TODO Auto-generated method stub
    	  }
    	}
    
    public void jdbcConnection() {

    	SqLiteDb jdbc = new SqLiteDb(getActivity(),dbname);
        
    	Spinner tablespinner = (Spinner) getActivity().findViewById(R.id.tableSpinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(getActivity(),
	    		android.R.layout.simple_spinner_item, jdbc.dbinfo(null));
	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    tablespinner.setAdapter(dataAdapter);
	    
	    Spinner columnpinner = (Spinner) getActivity().findViewById(R.id.columnSpinner);
	    dataAdapter = new ArrayAdapter<String>(getActivity(),
	    		android.R.layout.simple_spinner_item, jdbc.dbinfo((String)tablespinner.getSelectedItem()));
	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    columnpinner.setAdapter(dataAdapter);

	    tablespinner.setOnItemSelectedListener(new CustomOnItemSelectedListenerJdbc(jdbc));
    }

    public class CustomOnItemSelectedListenerJdbc implements OnItemSelectedListener {
    	SqLiteDb jdbc;
    	public CustomOnItemSelectedListenerJdbc(SqLiteDb jdbc){
    		this.jdbc = jdbc;
    	}
    	public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
    		Spinner tablespinner = (Spinner) getActivity().findViewById(R.id.tableSpinner);
    		Spinner columnpinner = (Spinner) getActivity().findViewById(R.id.columnSpinner);
    		ArrayAdapter<String>dataAdapter = new ArrayAdapter<String>(getActivity(),
				  android.R.layout.simple_spinner_item, jdbc.dbinfo((String)tablespinner.getSelectedItem()));
    		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		columnpinner.setAdapter(dataAdapter);
	  }
	  @Override
	  public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	  }
	}
}
