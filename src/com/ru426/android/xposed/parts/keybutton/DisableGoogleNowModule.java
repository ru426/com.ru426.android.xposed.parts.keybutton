package com.ru426.android.xposed.parts.keybutton;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ru426.android.xposed.library.ModuleBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class DisableGoogleNowModule extends ModuleBase {
	private static final String TAG = DisableGoogleNowModule.class.getSimpleName();
	
	private static boolean isInitialized;	
	public static boolean isDisableHomeAssist = false;
	
	@Override
	public void init(XSharedPreferences prefs, ClassLoader classLoader, boolean isDebug) {
		super.init(prefs, classLoader, isDebug);
		isDisableHomeAssist = (Boolean) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_home_key), false);
		
		Class<?> xSearchPanelView = XposedHelpers.findClass("com.android.systemui.SearchPanelView", classLoader);
		XposedBridge.hookAllConstructors(xSearchPanelView, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);				
				try{
					xLog(TAG + " : " + "afterHookedMethod hookAllConstructors");
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					if(!isInitialized){
			        	IntentFilter intentFilter = new IntentFilter();
						intentFilter.addAction(Settings.KEY_SETTINGS_CHANGED);
						xRegisterReceiver(mContext, intentFilter);
			        	isInitialized = true;
		        	}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		});
		Object callback[] = new Object[3];
		callback[0] = boolean.class;
		callback[1] = boolean.class;
		callback[2] = new XC_MethodReplacement() {
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
				try{
					xLog(TAG + " : " + "replaceHookedMethod show");					
					if(isDisableHomeAssist){	
						return null;
					}else{
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}			
		};
		xHookMethod(xSearchPanelView, "show", callback, (Boolean) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_home_key), false));
		
		Object callback2[] = new Object[1];
		callback2[0] = new XC_MethodReplacement() {			
			@Override
			protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {				
				try{
					xLog(TAG + " : " + "replaceHookedMethod startAssistActivity");
					if(isDisableHomeAssist){	
						return null;
					}else{
						XposedBridge.invokeOriginalMethod(param.method, param.thisObject, param.args);
					}					
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
				return null;
			}
		};
		xHookMethod(xSearchPanelView, "startAssistActivity", callback2, (Boolean) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_home_key), false));
	}

	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + "OnReceive " + intent.getAction());
		if (intent.getAction().equals(Settings.KEY_SETTINGS_CHANGED)) {        	
        	isDisableHomeAssist = intent.getBooleanExtra(Settings.HOMEKEY_OPTION_ENABLED, false);
		}
	}	
}