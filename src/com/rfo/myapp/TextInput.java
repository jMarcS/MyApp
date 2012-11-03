/****************************************************************************************************

BASIC! is an implementation of the Basic programming language for
Android devices.


Copyright (C) 2010, 2011, 2012 Paul Laughton

This file is part of BASIC! for Android

    BASIC! is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    BASIC! is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with BASIC!.  If not, see <http://www.gnu.org/licenses/>.

    You may contact the author, Paul Laughton at basic@laughton.com
    
	*************************************************************************************************/


package com.rfo.myapp;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import com.rfo.myapp.R;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;


public class TextInput extends Activity {
    private Button finishedButton;			// The buttons
    private EditText theTextView;		//The EditText TextView

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)  {
    	// if BACK key restore original text
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
//     	   Run.TextInputString = theTextView.getText().toString();   // Grab the text that the user is seeing
     	   Run.HaveTextInput =true;
     	   finish();
           return true;
        	}
        return super.onKeyUp(keyCode, event);
    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       this.setContentView(R.layout.text_input);  // Layouts xmls exist for both landscape or portrait modes
       
       this.finishedButton = (Button) findViewById(R.id.finished_button);		 // The buttons
       
       this.theTextView = (EditText) findViewById(R.id.the_text);		// The text display area
       theTextView.setText(Run.TextInputString);							// The Editor's display text
       theTextView.setTypeface(Typeface.MONOSPACE);
       theTextView.setSelection(Run.TextInputString.length());
       if (Settings.getEditorColor(this).equals("BW")){
    	   theTextView.setTextColor(0xff000000);
    	   theTextView.setBackgroundColor(0xffffffff);
       } else
         if (Settings.getEditorColor(this).equals("WB")){
        	 theTextView.setTextColor(0xffffffff);
        	 theTextView.setBackgroundColor(0xff000000);
       } else 
           if (Settings.getEditorColor(this).equals("WBL")){
        	   theTextView.setTextColor(0xffffffff);
        	   theTextView.setBackgroundColor(0xff006478);
             }  
       
       theTextView.setTextSize(1, Settings.getFont(this));
       
       this.finishedButton.setOnClickListener(new OnClickListener() {		// **** Done Button ****
           
           public void onClick(View v) {
        	   Run.TextInputString = theTextView.getText().toString();   // Grab the text that the user is seeing
        	   Run.HaveTextInput =true;
        	   finish();
              return;
           	}
       	});

   
    }
}