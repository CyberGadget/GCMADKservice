package edu.rosehulman.gcmadkservice;

import android.util.Log;

public class Script {
	
	public static final String TAG = "Script";

	public static enum ScriptType {
		NO_SCRIPT("NO_SCRIPT", null),
		TUNNEL("TUNNEL", new String[]{"FORWARD 255", "$ROTATE 90"}),
		ZIGZAG("ZIGZAG", new String[]{"FORWARD 255", "$ROTATE 90", "FORWARD 255", "$ROTATE -90"}),
		SPIRAL("SPIRAL", null),
		ONE("ONE", new String[]{"FORWARD 255", "$ROTATE 90", "FORWARD 255"});

		private final String name;
		private final String[] commands;

		private ScriptType(String txt, String[] cmd) {
			this.name = txt;
			this.commands = cmd;
		}
	};
	
	private ScriptType mScriptType;
	private int mScriptStage;
	
	public Script(){
		mScriptType = ScriptType.NO_SCRIPT;
		mScriptStage = 0;
	}
	
	public void clearScript(){
		mScriptType = ScriptType.NO_SCRIPT;
		mScriptStage = 0;
	}
	
	public void startScript(ScriptType type){
		mScriptType = type;
		mScriptStage = 0;
	}
	
	public void startScript(String typeName){
		mScriptType = ScriptType.valueOf(typeName);
		mScriptStage = 0;
	}

	public String getScriptName(){
		return mScriptType.name;
	}
	
	public String nextCommand(){
		try {
			return mScriptType.commands[mScriptStage++];
		} catch(Exception e) {
			Log.d(TAG, "Things happened during nextCommand. Defaulting to no script.");
			e.printStackTrace();
			mScriptStage = 0;
			mScriptType = ScriptType.NO_SCRIPT;
			return null;
		}
	}
}
