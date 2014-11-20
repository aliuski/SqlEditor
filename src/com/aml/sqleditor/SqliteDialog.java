package com.aml.sqleditor;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.view.WindowManager.LayoutParams;

public class SqliteDialog extends DialogFragment {

    private Button btn;
    private Button btnc;
    private String dialogTitle;
    private String directory;

	public interface SqliteDialogListener {
        void onFinishInputDialog(String directory, boolean in);
    }

    public SqliteDialog() {
    }

    public void setDialogTitle(String title, String directory) {
    	this.dialogTitle = title;
    	this.directory = directory;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle savedInstanceState) {
    	final View view_ = inflater.inflate(R.layout.fragment_sqlite_dialog, container);
    	btn = (Button) view_.findViewById(R.id.btnDone);
    	btnc = (Button) view_.findViewById(R.id.btnCancel);
    	EditText tv = (EditText) view_.findViewById(R.id.txtDirectory);
    	tv.setText(directory);
    	
    	btn.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View view) {
            	SqliteDialogListener activity = (SqliteDialogListener) getActivity();
            	RadioButton rb = (RadioButton) view_.findViewById(R.id.rdb1);
            	EditText tv = (EditText) view_.findViewById(R.id.txtDirectory);
            	activity.onFinishInputDialog(tv.getText().toString(), rb.isChecked());
                dismiss();
            }
        });
        
    	btnc.setOnClickListener(new View.OnClickListener() 
        {
            public void onClick(View view) {
                dismiss();
            }
        });
    	
        getDialog().getWindow().setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        getDialog().setTitle(dialogTitle);
        
        return view_;
    }
}
