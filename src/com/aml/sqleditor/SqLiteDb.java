package com.aml.sqleditor;

import java.util.ArrayList;
import java.util.Vector;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SqLiteDb {
	
	static final int DATABASE_VERSION = 1;
	static final String DATABASE_NAME = "TempDB";
    final Context context;

    DatabaseHelper DBHelper;
    SQLiteDatabase db;
    
    public SqLiteDb(Context ctx,String databasename)
    {
        this.context = ctx;
        DBHelper = new DatabaseHelper(context);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper
    {
        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
        }
    }
	
    public SqLiteDb open() throws SQLException 
    {
        db = DBHelper.getWritableDatabase();
        return this;
    }
	
    public void close() 
    {
        DBHelper.close();
    }
	
    public ArrayList<String> dbinfo(String args) {

    	ArrayList <String>list = new ArrayList<String>();

    	try {
    		open();

    		if(args == null){
    			Cursor c = db.rawQuery("SELECT name FROM sqlite_master", null);
    		    if(c != null ) {
    		        if(c.moveToFirst()) {
    		            do {
    						list.add(c.getString(0));
    		            } while (c.moveToNext());
    		        }
    		    }
    		} else {
    			Cursor c = db.rawQuery("PRAGMA table_info("+args+")", null);
    		    if(c != null ) {
    		        if(c.moveToFirst()) {
    		            do {
    						list.add(c.getString(1)+" "+c.getString(2));
    		            } while (c.moveToNext());
    		        }
    		    }    			
    		}

    	} catch (Exception ex) {
    		Log.d("SqLiteDb", "ERROR=" + ex);
        } finally {
        	close();
        }
    	return list;
    }
	
    public void tableFromExec(String sql,TableLayout table){
		
    	TableRow.LayoutParams params = new TableRow.LayoutParams(LayoutParams.FILL_PARENT,
                LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < table.getChildCount(); i++) {
            View child = table.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }
    	try {
    		open();

            if(sql.startsWith("select")){
            	
        		Vector <String>v = new Vector<String>();
        		int endindex = 0;
        		do{
        			int startindex = sql.indexOf('\'', endindex+1);
        			if(startindex == -1)
        				break;
        			endindex = sql.indexOf('\'', startindex+1);
        			v.add(sql.substring(startindex+1,endindex));
        		}while(endindex != -1);

        		for(int loop=0;loop<v.size();loop++)
        			sql = sql.replaceAll("'"+v.get(loop)+"'", "?");
        		String sqlk[] = new String[v.size()];
        		for(int loop=0;loop<v.size();loop++){
        			sqlk[loop] = (String)v.get(loop);
        		}
        		
	            Cursor c = db.rawQuery(sql, sqlk);
	            if(c != null ) {
		            int columnscount = c.getColumnCount();
		            TableRow row = new TableRow(context);
		            for (int j = 0; j < columnscount; j++) {
		            	TextView text = new TextView(context);
		            	text.setLayoutParams(params);
		            	text.setText((j==0) ? c.getColumnName(j) : "|"+c.getColumnName(j));
		            	row.addView(text);
		            }
		            table.addView(row);
		            if(c.moveToFirst()) {
		            	do {
							row = new TableRow(context);
					        for (int j = 0; j < columnscount; j++) {
					            TextView text = new TextView(context);
					            text.setLayoutParams(params);
					            text.setText((j==0) ? c.getString(j) : "|"+c.getString(j));
					            row.addView(text);
					        }
					        table.addView(row);
		            	} while (c.moveToNext());
		            }
	            }
            } else {
            	
				TableRow row = new TableRow(context);
				TextView text = new TextView(context);
				text.setLayoutParams(params);
				text.setText("OK");
				row.addView(text);
		        table.addView(row);            	
            	
            	db.execSQL(sql);
            }
            
    	} catch (Exception ex) {
			TableRow row = new TableRow(context);
			TextView text = new TextView(context);
			text.setLayoutParams(params);
			text.setText("Error: "+ex);
			row.addView(text);
	        table.addView(row);
        } finally {
        	close();
        }
    }
}
