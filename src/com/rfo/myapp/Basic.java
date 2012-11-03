/****************************************************************************************************

BASIC! is an implementation of the Basic programming language for
Android devices.

This file is part of BASIC! for Android

Copyright (C) 2010, 2011, 2012 Paul Laughton

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import 	java.io.DataOutputStream;
import 	java.io.DataInputStream;

import android.app.ListActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.ArrayAdapter;

import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import java.util.*;

import com.rfo.myapp.AddProgramLine;


public class Basic extends ListActivity {
    private static final String LOGTAG = "Basic";
    private static final String CLASSTAG = Basic.class.getSimpleName();
    
    public static ArrayList<String> lines;       //Program lines for execution
    
    public static Boolean UseSDCard = true;	// Set by preferences
    public static Boolean Echo = false;			// Set by preferences
    public static Context BasicContext;			// saved so we do not have to pass it around
    public static Context theRunContext = null;
    
    public static String DataPath = "";			// Used in RunProgram to determine where data is stored
    public static String SD_ProgramPath = "";		// Used by Load/Save
    public static String IM_ProgramPath = "help";	// Used by Load/Save
    
    public static Boolean DoAutoRun = true;     // Used to signal direct exit at end
    public static Intent theProgramRunner ;
    public static boolean BlockFlag = false;
    public static String AppPath = "";
    public static int ScreenOrientation = 0;     // For command setting the editor orientation
    
    public static ArrayAdapter AA;
    public static ListView lv ;							    // The output screen list view
    public static ArrayList<String> output;					// The output screen text lines
    public static Background theBackground;					// Background task ID
    
	private static String stemp ="";


    
    /** Called when the Application is first created. */
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
//      Log.v(Basic.LOGTAG, " " + Basic.CLASSTAG + " started Auto Run" );
    	

        super.onCreate(savedInstanceState);					// Set up of fresh start
        BasicContext = getApplicationContext();
        ScreenOrientation = 0;        
// Set AppPath for the name of the directory in the sdcard root where you will be storing data and database files
// The name can be anything you want it to be as long as it does not interfere with some other applications directory
        
        AppPath = "rfo-myapp";                              // Set the I/O path for this application
        
// Establish an output screen so that file load progress can be shown.        
        
  	  	output = new ArrayList<String>();
    	AA=new ArrayAdapter<String>(this, R.layout.simple_list_layout, output);  // Establish the output screen
  	  	lv = getListView();
  	  	lv.setTextFilterEnabled(false);
  	  	lv.setTextFilterEnabled(false);
  	  	lv.setSelection(0);
  	  	
// In order to show progress in the user interface we have to start a background thread to 
// do the actual loading. That background thread will make calls to the UI thread to show
// show the progress.
//
// Once the files are loaded, the background task will start the loaded program running.
  	  	
    	theBackground = new Background();						// Start the background task to load
    	theBackground.execute("");								// sample and graphics


    }
    
    public static void InitDirs(){
    	
    	// Initializes (creates) the directories used by Basic
    	    	
    	// Start by making sure the SD Card is available and writable
    	    	
    	    	boolean mExternalStorageAvailable = false;
    	    	boolean mExternalStorageWriteable = false;
    	    	String state = Environment.getExternalStorageState();

    	    	
    	    	if (Environment.MEDIA_MOUNTED.equals(state)) {			// Insure that SD is mounted
    	    	    // We can read and write the media
    	    	    mExternalStorageAvailable = mExternalStorageWriteable = true;
    	    	} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
    	    	    // We can only read the media
    	    	    mExternalStorageAvailable = true;
    	    	    mExternalStorageWriteable = false;
    	    	} else {
    	    	    // Something else is wrong. It may be one of many other states, but all we need
    	    	    //  to know is we can neither read nor write
    	    	    mExternalStorageAvailable = mExternalStorageWriteable = false;
    	    	}   	
    	 
    	    	boolean CanWrite = mExternalStorageAvailable && mExternalStorageWriteable;
    	    	
    	// The SD Card is available and can be written to. 
    	    	
    	    	if (CanWrite){
    	         	String PathA = "/sdcard/"+ AppPath + "/data";            // data directory
    	        	File sdDir = new File(PathA);
    	        	sdDir.mkdirs();
    	        	PathA = "/sdcard/"+ AppPath + "/databases";		// databases directory
    	        	sdDir = new File(PathA);
    	        	sdDir.mkdirs();
    	    	}
    	   	
    	    }

// This is the background class thread that controls the loading of the files. It contains that methods that does
// the loading of the graphics files and the program to run
    
    public class Background extends AsyncTask<String, String, String>{
        @Override
        protected  String doInBackground(String...str ) {

           InitDirs();											// Initialize Basic directories every time
            													// The user may have put in a new SD Card
           loadGraphics();										// Load the graphics files
           
      	   Basic.lines = new ArrayList <String>();              // Program will be loaded into this array list
      	   BlockFlag = false;                                   // Signals block quote skip in progress
      	   
           LoadTheFile();                                       // Load the basic program file into memory

           theProgramRunner = new Intent(BasicContext, Run.class);		//now go run the program
           theRunContext = null;
           DoAutoRun = true;
           startActivity(Basic.theProgramRunner);               // The program is now running
           finish();
           return "";
        }
        
// This method writes the message telling the user about the file loading.
        
        @Override
        protected void onPreExecute(){
        	output.add("Loading files");  // The message that tells what we are doing.
        	output.add("");
        }

// This method runs in the UI thread. 
// It is called by the publishProgress() method.
// It displays the progress on the screen
        
        public double lineCount = 0;
        @Override    
        protected void onProgressUpdate(String... str ) {     // Called when publishProgress() is executed.
        	int CHAR_PER_LINE = 20;					  // Number of dots per line
        	int LINES_PER_UPDATE = 30;				  // Number of line between Progress Messages
        	int MAX_UPDATE_COUNT = 0;				  // If 0, the count of progress messages will be shown
        											  // if not zero progress will be shown as a percent
        	
        	for (int i=0; i<str.length; ++i){				  			  // Form line of CHAR_PER_LINE progress characters
        		String s = output.get(output.size() -1);
        		s = s + str[i];
        		output.set(output.size()-1, s);
        		if (s.length() >= CHAR_PER_LINE){						  // After the CHAR_PER_LINEth character
        			++lineCount;										  // output a new line
        																  // After every LINES_PER_UPDATE
        			double dd = lineCount%LINES_PER_UPDATE;
        			if (dd == 0) {				   
        			    double updates = Math.floor(lineCount/LINES_PER_UPDATE);  // Output a progress message

        			    if (MAX_UPDATE_COUNT > 0 ) {								 // if the max is known
        			    	updates = updates/MAX_UPDATE_COUNT * 100;				 // convert progress to a percent
        					output.add("Continuing load (" + updates + "%)");
        			    } else 														 // else just use the number of progress line
        			    	output.add("Continuing load (" + (int)updates + ")");
        			}
        			output.add("");							  // start a new line
        		}
        	}
        	
        	setListAdapter(AA);						// show the output
	    	lv.setSelection(output.size()-1);		// set last line as the selected line to scroll
        }
   
        
// This call will load any images or mp3 files that your program may need. If you do not need any images or mp3
// files, then you just comment out the following statement.
// 
// If you do have files to load, you will need to make the appropriate changes in the loadGraphics method.
//
// Note: The load1Graphic method checks to see if the file already exist. If it does, the file will not be
// re-loaded. This will save start up time.
        
    
    
    private void loadGraphics(){
    	
// Loads the image and audio used for this program
// The files are copied from res.raw to the SD Card
//
// The filenames in res.raw must be in all lower case characters and
// must have file extensions.
    	
 		
    	InputStream  inputStream = BasicContext.getResources().openRawResource(R.raw.cartman);
  		String PathA = "/sdcard/"+ AppPath + "/data/a1.png";
  		Load1Graphic(inputStream, PathA);

        inputStream = BasicContext.getResources().openRawResource(R.raw.boing);
        PathA = "/sdcard/"+ AppPath + "/data/boing.mp3";
  		Load1Graphic(inputStream, PathA);

  		inputStream = BasicContext.getResources().openRawResource(R.raw.whee);
        PathA = "/sdcard/"+ AppPath + "/data/whee.mp3";
  		Load1Graphic(inputStream, PathA);
                  
    }
    
    private void Load1Graphic(InputStream inputStream, String PathA ){
    	
// Does the actual loading of one icon or audio file
    	
        DataInputStream dis = new DataInputStream(inputStream);
 		File aFile = new File(PathA);
 		if (aFile.exists())return;                // If the file has previously been loaded, skip loading.
 		DataOutputStream dos1 = null;

    	
  		 dos1 = null;
  		 
// Note the I/O methods used here specialized for reading and writing
// non-text data files; 
  		
         int Byte = 0;
         int count = 0;
         try {
             FileOutputStream fos = new FileOutputStream(aFile);
              dos1 = new DataOutputStream(fos);
               	 do {
         	 Byte = dis.readByte();
         	 dos1.writeByte(Byte);
        	 ++count;
        	 if (count >= 4096){			// Show progress every 4k bytes
        		 publishProgress(".");
        		 count = 0;
        	 }
         	 } while (dis != null);
              
          } catch (IOException e) {
          }
          finally {
  			if (dos1 != null) {
  				try {
  					dos1.flush();
  					dos1.close();
  				} catch (IOException e) {
  				}
  			}
          }

    }

    

    private void LoadTheFile(){
    	
    	// Reads the program file from res.raw/my_program and 
        // puts it into memory
    	AddProgramLine APL = new AddProgramLine();
    	APL.AddProgramLine();
    	String ResName = "com.rfo.myapp:raw/my_program";
    	int ResId = BasicContext.getResources().getIdentifier(ResName, null, null);
        InputStream inputStream = BasicContext.getResources().openRawResource(ResId);
        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader, 8192);
        
        String line = "";
        int count = 0;
        
         try {
           while (( line = buffreader.readLine()) != null) {			 // Read and write one line at a time
        	   APL.AddLine(line, false);                         		 // add the line to memory
        	   ++count;
        	   if (count >= 200){										 // Show progress every 250 lines.
        		   publishProgress(" ");
        		   count = 0;
        	   }
             }
         } catch (IOException e) {
         }
 		    
     }
    
/*    private void AddLine(String line){				// Build Basic.lines
 	   
    	String Temp = "";

    	// Look for block comments. All lines between block comments
    	// are tossed out

    	
 	if (BlockFlag) {                     // If block flag is true and
 		if (line.startsWith("!!")){      // we have reached the end of the block
 			BlockFlag = false;           // off
 		}
 		return;                          // Return without adding line if block flag is true
 	}
 	
 	if (line.startsWith("!!")){         // Block flag is off, if we find a start of block
 		BlockFlag = true;               // turn the block flag on.
 		return;
 	}

		for (int i=0; i < line.length(); ++i) {		// do not mess with characters
		char c = line.charAt(i);				// between quote marks
		if (c == '“') c = '"';                    // Change funny quote to real quote
		if (c == '"') {
			do {
				try {
					if (line.charAt(i + 1) == '=') {
						if (c == '+') {
							Temp += "{+&=}";
							++i;c = 0;
						} else if (c == '-') {
							Temp += "{-&=}";
							++i;c = 0;
						} else if (c == '*') {
							Temp += "{*&=}";
							++i;c = 0;
						} else if (c == '/') {
							Temp += "{/&=}";
							++i;c = 0;
						}
					}
				} catch (StringIndexOutOfBoundsException e) {
//					Log.e(Editor.LOGTAG, e.getMessage(), e);
				}

				if (c == '\\') {					// look for \"
					if (i + 1 >= line.length()) {
						line = line + ' ';
					}
					if (line.charAt(i + 1) == '"') {    // and retain it 
						++i;						// so that user can have quotes in strings
						Temp = Temp + '\\';
						Temp = Temp + '"';
					} else if (line.charAt(i + 1) == 'n') {
						++i;
						Temp = Temp + '\r';
					}
				} else Temp = Temp + c;

				++i;
				if (i >= line.length()) { c = '"';} else {c = line.charAt(i);}				// just add it in
				if (c == '�') c = '"';					// Change funny quote to real quote
			}while (i < line.length() && c != '"');
			Temp = Temp + c;
		} else if (c == '%') {					// if the % character appears,
			break;								// drop it and the rest of the line
		} else if (c == ' ') {
			if ((i + 1) < line.length())
				if (line.charAt(i + 1) == '_') {
					Temp = Temp + "{+nl}";
					break;
				}
		} else if (c != ' ' && c != '\t') {		// toss out spaces and tabs
			c = Character.toLowerCase(c);		// convert to lower case
			Temp = Temp + c;						// and add it to the line
		}
	}

		
   		if (Temp.startsWith("rem")){Temp = "";}		// toss out REM lines
   		if (Temp.startsWith("!"))Temp = "";			// and pseudo rem lines
  		if (!Temp.equals("")) {						// and empty lines
			if (Temp.endsWith("{+nl}")) {    			// test for include next line sequence
				stemp = stemp + Temp.substring(0, Temp.length() - 5);  // remove the include next line sequence and collect it
				return;
			} else {
				Temp = stemp + Temp; 					// add stemp collection to line
				stemp = "";							// clear the collection
			}
		    Temp = shorthand(Temp);
		    Temp = Temp.replace("{+&=}", "+=");
		    Temp = Temp.replace("{-&=}", "-=");
		    Temp = Temp.replace("{*&=}", "*=");
		    Temp = Temp.replace("{/&=}", "/=");
  			Temp = Temp + "\n";						// end the line with New Line
   			Basic.lines.add(Temp);					// add to Basic.lines
   		}

    }
    
	private String shorthand(String line) {
		int ll = line.length();
		int then = line.indexOf("then");
		if (then == -1) {
			if (line.startsWith("++") || line.startsWith("--")) {
				line = line.substring(2, ll) + "=1" + line.substring(1, ll);
			}
			if (line.endsWith("++") || line.endsWith("--")) {
				line = line.substring(0, ll - 2) + "=" + line.substring(0, ll - 1) + "1";
			}
			int pe = line.indexOf("+=");
			int me = line.indexOf("-=");
			int te = line.indexOf("*=");
			int de = line.indexOf("/=");
			int tt=0;
			if (pe >= 0) {tt = pe;line = line.substring(0, tt) + "=" + line.substring(0, tt + 1) + line.substring(tt + 2, ll);}
			if (me >= 0) {tt = me;line = line.substring(0, tt) + "=" + line.substring(0, tt + 1) + line.substring(tt + 2, ll);}
			if (te >= 0) {tt = te;line = line.substring(0, tt) + "=" + line.substring(0, tt + 1) + line.substring(tt + 2, ll);}
			if (de >= 0) {tt = de;line = line.substring(0, tt) + "=" + line.substring(0, tt + 1) + line.substring(tt + 2, ll);}
			return line;
		}
		then += 4;
	    String tline = line.substring(0, then);
		line = line.substring(then, ll);
		line = shorthand(line);
		return tline + line;
	}
*/



    }
}

