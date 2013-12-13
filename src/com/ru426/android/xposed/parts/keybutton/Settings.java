package com.ru426.android.xposed.parts.keybutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.Toast;

import com.ru426.android.xposed.parts.keybutton.view.SeekBarPreference;
import com.ru426.android.xposed.parts.keybutton.view.SeekBarPreference.OnSeekBarPreferenceChangeListener;

public class Settings extends PreferenceActivity {
	public static final String KEY_SETTINGS_CHANGED = Settings.class.getPackage().getName() + ".intent.action.KEY_SETTINGS_CHANGED";
	public static final String BACKKEY_OPTION_ENABLED = Settings.class.getPackage().getName() + ".intent.extra.BACKKEY_OPTION_ENABLED";
	public static final String BACKKEY_WAIT_TIME = Settings.class.getPackage().getName() + ".intent.extra.BACKKEY_WAIT_TIME";
	public static final String HOMEKEY_OPTION_ENABLED = Settings.class.getPackage().getName() + ".intent.extra.HOMEKEY_OPTION_ENABLED";
	static Context mContext;
	static SharedPreferences prefs;
	static Resources mResources;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		mResources = mContext.getResources();
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		if(prefs.getBoolean(getString(R.string.ru_use_light_theme_key), false)){
			setTheme(android.R.style.Theme_DeviceDefault_Light);
		}
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_fragment_back_button);
		init();
	    initOption();
	}

	@Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			finish();
			break;
		}
        return super.onMenuItemSelected(featureId, item);
    }
	
	private static void showHomeButton(){
		if(mContext != null && ((Activity) mContext).getActionBar() != null){
			((Activity) mContext).getActionBar().setHomeButtonEnabled(true);
	        ((Activity) mContext).getActionBar().setDisplayHomeAsUpEnabled(true);
		}		
	}
	
	static void showRestartToast(){
		Toast.makeText(mContext, R.string.ru_restart_message, Toast.LENGTH_SHORT).show();
	}
	
	@SuppressWarnings("deprecation")
	private void init(){
        final SeekBarPreference hookBackKeyWait = (SeekBarPreference) findPreference(getString(R.string.settings_hook_keybuttonview_back_wait_key));
        String unit = mResources.getQuantityString(R.plurals.settings_hook_keybuttonview_back_wait_unit, prefs.getInt(getString(R.string.settings_hook_keybuttonview_back_wait_key), 15));
        hookBackKeyWait.setSummary(mContext.getString(R.string.settings_hook_keybuttonview_back_wait_summary, String.format("%.1f", (double)((Integer) prefs.getInt(getString(R.string.settings_hook_keybuttonview_back_wait_key), 15) / 10.0)), unit));
        setPreferenceChangeListener(getPreferenceScreen());
        hookBackKeyWait.setOnSeekBarPreferenceChangeListener(new OnSeekBarPreferenceChangeListener(){
    		@Override
    		public void onStopTrackingTouch(SeekBar seekBar, String unit) {
    			hookBackKeyWait.setSummary(mContext.getString(R.string.settings_hook_keybuttonview_back_wait_summary, String.format("%.1f", (double)((Integer) seekBar.getProgress() / 10.0)), unit));
    			Intent intent = new Intent(KEY_SETTINGS_CHANGED);
    			intent.putExtra(BACKKEY_OPTION_ENABLED, prefs.getBoolean(getString(R.string.settings_hook_keybuttonview_back_key), false));
    			intent.putExtra(BACKKEY_WAIT_TIME, seekBar.getProgress());
    			mContext.sendBroadcast(intent);
    		}		
    	});
	}
	
	@SuppressWarnings("deprecation")
	private void initOption(){
		showHomeButton();
		setPreferenceChangeListener(getPreferenceScreen());
	}

	private static void setPreferenceChangeListener(PreferenceScreen preferenceScreen){
		for(int i = 0; i < preferenceScreen.getPreferenceCount(); i++){
			if(preferenceScreen.getPreference(i) instanceof PreferenceCategory){
				for(int j = 0; j < ((PreferenceCategory) preferenceScreen.getPreference(i)).getPreferenceCount(); j++){
					((PreferenceCategory) preferenceScreen.getPreference(i)).getPreference(j).setOnPreferenceChangeListener(onPreferenceChangeListener);
				}
			}else{
				preferenceScreen.getPreference(i).setOnPreferenceChangeListener(onPreferenceChangeListener);
			}
		}
	}
	
	private static OnPreferenceChangeListener onPreferenceChangeListener = new OnPreferenceChangeListener(){
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(prefs == null){
	        	prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	        }
			switch(preference.getTitleRes()){
			case R.string.settings_hook_keybuttonview_back_title:
			case R.string.settings_hook_keybuttonview_home_title:
				sendKeyButtonStateChangeIntent(preference, (Boolean) newValue);
				break;
			}
			
			return true;
		}		
	};
	
	private static void sendKeyButtonStateChangeIntent(Preference preference, boolean newValue){
		String key = preference.getTitleRes() == R.string.settings_hook_keybuttonview_back_title ? mContext.getString(R.string.settings_hook_keybuttonview_back_key) : mContext.getString(R.string.settings_hook_keybuttonview_home_key);
		if(!prefs.getBoolean(key, false) && (Boolean) newValue){
			showRestartToast();
		}
		Intent intent = new Intent(KEY_SETTINGS_CHANGED);
		intent.putExtra(BACKKEY_OPTION_ENABLED, preference.getTitleRes() == R.string.settings_hook_keybuttonview_back_title ? newValue : prefs.getBoolean(mContext.getString(R.string.settings_hook_keybuttonview_back_key), false));
		intent.putExtra(BACKKEY_WAIT_TIME, prefs.getInt(mContext.getString(R.string.settings_hook_keybuttonview_back_wait_key), 15));
		
		intent.putExtra(HOMEKEY_OPTION_ENABLED, preference.getTitleRes() == R.string.settings_hook_keybuttonview_home_title ? newValue : prefs.getBoolean(mContext.getString(R.string.settings_hook_keybuttonview_home_key), false));
		mContext.sendBroadcast(intent);
	}
}
