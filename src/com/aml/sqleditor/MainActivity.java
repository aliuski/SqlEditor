package com.aml.sqleditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import com.aml.sqleditor.SqLiteDb;
import com.aml.sqleditor.UserSettingActivity;
import com.aml.sqleditor.SqliteDialog.SqliteDialogListener;

public class MainActivity extends FragmentActivity implements SqliteDialogListener,OnSharedPreferenceChangeListener {
	private static final int RESULT_SETTINGS = 1;

	private String userid;
	private String password;
	private String dbname;
	private String dbaddress;
	private int sqleditortype;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
    	sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    	userid = sharedPrefs.getString("sqleditorUserid", null);
    	password = sharedPrefs.getString("sqleditorPassword", null);
    	dbname = sharedPrefs.getString("sqleditorDBName", null);
    	dbaddress = sharedPrefs.getString("sqleditorIPAddress", null);
    	sqleditortype = Integer.parseInt(sharedPrefs.getString("sqleditorType","1"));
    	
    	if(dbaddress != null && dbname != null && userid != null && password != null || sqleditortype == 2)
    		initSystem();
    }

    public void onFinishInputDialog(String directory, boolean in) {
    	CopyDatabase(directory,in);
    	initSystem();
    }
    
    private void initSystem(){
    	
    	if(sqleditortype == 1)
	        new AccessWebServiceTask().execute("http://"+dbaddress+"/sqleditor.php?userid="+
	        		java.net.URLEncoder.encode(userid)+"&password="+java.net.URLEncoder.encode(password)+
	        		"&dbname="+java.net.URLEncoder.encode(dbname));
    	else
    		jdbcConnection();
    	
        final EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);

        Button tablebutton = (Button) findViewById(R.id.tableButton);
        tablebutton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Spinner tablespinner = (Spinner) findViewById(R.id.tableSpinner);
        		txtsqlsearch.append((String)tablespinner.getSelectedItem()+' ');
        	}
      	});
        Button columnbutton = (Button) findViewById(R.id.columnButton);
        columnbutton.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		Spinner columnpinner = (Spinner) findViewById(R.id.columnSpinner);        		
        		String s = (String)columnpinner.getSelectedItem();
        		txtsqlsearch.append(s.substring(0,s.indexOf(' '))+' ');
        	}
      	});
        Button btnExec = (Button) findViewById(R.id.btnExec);
        btnExec.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(sqleditortype == 1){
	        		if(userid != null && password != null)
	        			new SqlSearchWebServiceTask().execute(
	        				"http://"+dbaddress+"/sqleditorret.php?sqlline="+
	        						java.net.URLEncoder.encode(txtsqlsearch.getText().toString())+
	        						"&userid="+java.net.URLEncoder.encode(userid)+
	        						"&password="+java.net.URLEncoder.encode(password)+
	        						"&dbname="+java.net.URLEncoder.encode(dbname));
        		} else {
        			SqLiteDb jdbc = new SqLiteDb(MainActivity.this,dbname);
	        		jdbc.tableFromExec(txtsqlsearch.getText().toString(),(TableLayout)findViewById(R.id.txtSqlresult));
        		}
        	}
      	});
        Button btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		txtsqlsearch.setText("");
        	}
      	});
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {

        View v = getCurrentFocus();
        boolean ret = super.dispatchTouchEvent(event);

        if (v instanceof EditText) {
            View w = getCurrentFocus();
            int scrcoords[] = new int[2];
            w.getLocationOnScreen(scrcoords);
            float x = event.getRawX() + w.getLeft() - scrcoords[0];
            float y = event.getRawY() + w.getTop() - scrcoords[1];
            if (event.getAction() == MotionEvent.ACTION_UP && (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom()) ) { 
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getWindow().getCurrentFocus().getWindowToken(), 0);
            }
        }
    return ret;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            return true;
        } else if(id == R.id.action_copy && sqleditortype == 2) {
	    	FragmentManager fragmentManager = getSupportFragmentManager();
	    	SqliteDialog inputNameDialog = new SqliteDialog();
	    	inputNameDialog.setCancelable(false);
	    	inputNameDialog.setDialogTitle("Copy SqLite database",
	    			IsExternalStorageAvailableAndWriteable() ? getExternalFilesDir(null).getAbsolutePath() : "");
	    	inputNameDialog.show(fragmentManager, "input dialog");
	    	return true;
        } else if (id==R.id.command1||id==R.id.command2||id==R.id.command3||id==R.id.command4||
        	id==R.id.command5||id==R.id.command6||id==R.id.command7||id==R.id.command8||
        	id==R.id.command9||id==R.id.command10||id==R.id.command11||id==R.id.command12||id==R.id.special1){
        		EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);
        		txtsqlsearch.append(item.getTitle()+" ");
        		return true;
        } else if (id==R.id.command13){
            	EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);
            	txtsqlsearch.append("'"+(new java.sql.Date((new java.util.Date()).getTime())).toString()+"' ");
            	return true;
        } else if (id==R.id.special2||id==R.id.special3||id==R.id.special4||id==R.id.special5||
        	id==R.id.special6||id==R.id.special7||id==R.id.special8){
            	EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);
            	txtsqlsearch.append(item.getTitle());
            	return true;
        }
        return super.onOptionsItemSelected(item);
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
            	Toast.makeText(MainActivity.this,"Salasana virheellinen!"
            			,Toast.LENGTH_SHORT).show();
            	userid = null;
            	password = null;
        		return;
        	}
            Spinner tablespinner = (Spinner) findViewById(R.id.tableSpinner);
            List<String> list = new ArrayList<String>();
        	for(Map.Entry<String, ArrayList<String>> entry : result.entrySet())
        		list.add(entry.getKey());
            ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
    	    		android.R.layout.simple_spinner_item, list);
    	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	    tablespinner.setAdapter(dataAdapter);
    	    Spinner columnpinner = (Spinner) findViewById(R.id.columnSpinner);
    	    list = (ArrayList<String>)result.get((String)tablespinner.getSelectedItem());
    	    dataAdapter = new ArrayAdapter<String>(MainActivity.this,
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
    		  Spinner tablespinner = (Spinner) findViewById(R.id.tableSpinner);
    		  Spinner columnpinner = (Spinner) findViewById(R.id.columnSpinner);
    		  List<String> list = (ArrayList<String>)result.get((String)tablespinner.getSelectedItem());
    		  ArrayAdapter<String>dataAdapter = new ArrayAdapter<String>(MainActivity.this,
    				  android.R.layout.simple_spinner_item, list);
    		  dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		  columnpinner.setAdapter(dataAdapter);
    	  }
    	 
    	  @Override
    	  public void onNothingSelected(AdapterView<?> arg0) {
    		// TODO Auto-generated method stub
    	  }
    	}
    
    Vector<Vector<String>> row;
    
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
            TableLayout table = (TableLayout)findViewById(R.id.txtSqlresult);
            int count = table.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = table.getChildAt(i);
                if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
            }
            for (int i = 0; i < result.size(); i++) {
            	Vector<String> v = (Vector<String>)result.get(i);
                TableRow row = new TableRow(MainActivity.this);
                for (int j = 0; j < v.size(); j++) {
                    TextView text = new TextView(MainActivity.this);
                    text.setLayoutParams(params);
                    text.setText((j==0) ? (String)v.get(j) : "|"+v.get(j));
                    row.addView(text);
                }
                table.addView(row);
            }
        }
    }
    
    public void jdbcConnection() {

    	SqLiteDb jdbc = new SqLiteDb(MainActivity.this,dbname);
        
    	Spinner tablespinner = (Spinner) findViewById(R.id.tableSpinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(MainActivity.this,
	    		android.R.layout.simple_spinner_item, jdbc.dbinfo(null));
	    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    tablespinner.setAdapter(dataAdapter);
	    
	    Spinner columnpinner = (Spinner) findViewById(R.id.columnSpinner);
	    dataAdapter = new ArrayAdapter<String>(MainActivity.this,
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
    		Spinner tablespinner = (Spinner) findViewById(R.id.tableSpinner);
    		Spinner columnpinner = (Spinner) findViewById(R.id.columnSpinner);
    		ArrayAdapter<String>dataAdapter = new ArrayAdapter<String>(MainActivity.this,
				  android.R.layout.simple_spinner_item, jdbc.dbinfo((String)tablespinner.getSelectedItem()));
    		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    		columnpinner.setAdapter(dataAdapter);
	  }
	  @Override
	  public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	  }
	}
    
    public void CopyDB(InputStream inputStream, 
            OutputStream outputStream) throws IOException {
        //---copy 1K bytes at a time---
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        inputStream.close();
        outputStream.close();
    }

    public void CopyDatabase(String directory, boolean in) {
        String destDir = "/data/data/" + getPackageName() + "/databases/";
        String destPath = destDir + SqLiteDb.DATABASE_NAME;
        try {
        	if(in){
	        	File fdirectory = new File(destDir);
	        	if (!fdirectory.exists())
	        		fdirectory.mkdirs();
        		CopyDB(new FileInputStream(directory),
				        new FileOutputStream(destPath));
        	} else {
            	CopyDB(new FileInputStream(destPath),
				        new FileOutputStream(directory));
        	}
        } catch (FileNotFoundException e) {
            Toast.makeText(getBaseContext(),
            		"File not found",Toast.LENGTH_LONG).show();
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
    	userid = arg0.getString("sqleditorUserid", null);
    	password = arg0.getString("sqleditorPassword", null);
    	dbname = arg0.getString("sqleditorDBName", null);
    	dbaddress = arg0.getString("sqleditorIPAddress", null);
    	sqleditortype = Integer.parseInt(arg0.getString("sqleditorType","1"));
    	initSystem();
	}
	
    public boolean IsExternalStorageAvailableAndWriteable() {
        boolean externalStorageAvailable = false;
        boolean externalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            //---you can read and write the media---
            externalStorageAvailable = externalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            //---you can only read the media---
            externalStorageAvailable = true;
            externalStorageWriteable = false;
        } else {
            //---you cannot read nor write the media---
            externalStorageAvailable = externalStorageWriteable = false;
        }
        return externalStorageAvailable && externalStorageWriteable;
    }
}
