package com.aml.sqleditor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.aml.sqleditor.SqLiteDb;
import com.aml.sqleditor.UserSettingActivity;
import com.aml.sqleditor.SqliteDialog.SqliteDialogListener;
import com.aml.sqleditor.ResultFragment;

public class MainActivity extends FragmentActivity implements 
SqliteDialogListener,OnSharedPreferenceChangeListener,SelectFragment.OnSqlSelectedListener {
	private static final int RESULT_SETTINGS = 1;
	private int sqleditortype;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_main);

    	SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
    	sharedPrefs.registerOnSharedPreferenceChangeListener(this);
    	sqleditortype = Integer.parseInt(sharedPrefs.getString("sqleditorType","1"));

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null)
                return;
            SelectFragment firstFragment = new SelectFragment();
            firstFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
    	sqleditortype = Integer.parseInt(arg0.getString("sqleditorType","1"));
//    	initSystem();
	}
	
    public void onSqlSelected(String sqlline) {

    	ResultFragment resultFragment = (ResultFragment)
                getSupportFragmentManager().findFragmentById(R.id.result_fragment);

        if (resultFragment != null) {
        	resultFragment.updateSqlView(sqlline);
        } else {
            ResultFragment newFragment = new ResultFragment();
            Bundle args = new Bundle();
            args.putString(ResultFragment.ARG_SQLLINE, sqlline);
            newFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            transaction.commit();
        }
    }

    public void initSystem(){
    	SelectFragment selectFragment = (SelectFragment)
                getSupportFragmentManager().findFragmentById(R.id.select_fragment);

        if (selectFragment != null) {
        	selectFragment.initSystem();
        } else {
        	SelectFragment newFragment = new SelectFragment();
        	newFragment.setArguments(new Bundle());
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);
            transaction.commit();
        }
    }
    
    public void onFinishInputDialog(String directory, boolean in) {
    	CopyDatabase(directory,in);
    	initSystem();
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
        		if(txtsqlsearch != null)
        			txtsqlsearch.append(item.getTitle()+" ");
        		return true;
        } else if (id==R.id.command13){
            	EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);
            	if(txtsqlsearch != null)
            		txtsqlsearch.append("'"+(new java.sql.Date((new java.util.Date()).getTime())).toString()+"' ");
            	return true;
        } else if (id==R.id.special2||id==R.id.special3||id==R.id.special4||id==R.id.special5||
        	id==R.id.special6||id==R.id.special7||id==R.id.special8){
            	EditText txtsqlsearch = (EditText)findViewById(R.id.txtSqlsearch);
            	if(txtsqlsearch != null)
            		txtsqlsearch.append(item.getTitle());
            	return true;
        }
        return super.onOptionsItemSelected(item);
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

    public boolean IsExternalStorageAvailableAndWriteable() {
        boolean externalStorageAvailable = false;
        boolean externalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            externalStorageAvailable = externalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            externalStorageAvailable = true;
            externalStorageWriteable = false;
        } else {
            externalStorageAvailable = externalStorageWriteable = false;
        }
        return externalStorageAvailable && externalStorageWriteable;
    }
}
