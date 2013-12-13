package com.ru426.android.xposed.parts.keybutton;

import java.util.Iterator;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.view.KeyEvent;
import android.widget.Toast;

import com.ru426.android.xposed.library.ModuleBase;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class AndroidPolicyModule extends ModuleBase {
	private static final String TAG = AndroidPolicyModule.class.getSimpleName();
	
	private static boolean isInitialized;
	private static boolean isBackKeyOptionEnabled;
	private static int backPressWaitTime;
	private static Object phonewindowmanager;
	private static KillConceptAsyncTask mBackLongPressAsyncTask;

	@Override
	public void initZygote(final XSharedPreferences prefs, boolean isDebug) {
		super.initZygote(prefs, isDebug);
		isBackKeyOptionEnabled = (Boolean) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_back_key), false);
		backPressWaitTime = (Integer) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_back_wait_key), 15) * 100;
		Class<?> xPhoneWindowManager = XposedHelpers.findClass("com.android.internal.policy.impl.PhoneWindowManager", null);
		Object callback[] = new Object[4];
		callback[0] = XposedHelpers.findClass("android.view.WindowManagerPolicy$WindowState", null);
		callback[1] = KeyEvent.class;
		callback[2] = int.class;
		callback[3] = new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				super.afterHookedMethod(param);
				try{
					xLog(TAG + " : " + "afterHookedMethod interceptKeyBeforeDispatching");
					mContext = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
					phonewindowmanager = param.thisObject;
					KeyEvent event = (KeyEvent) param.args[1];
					final int keyCode = event.getKeyCode();
			        final int repeatCount = event.getRepeatCount();
			        final int flags = event.getFlags();
			        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
			        final boolean canceled = event.isCanceled();
			        
			        if(keyCode == KeyEvent.KEYCODE_BACK){
			        	if(!isInitialized){
				        	IntentFilter intentFilter = new IntentFilter();
							intentFilter.addAction(Settings.KEY_SETTINGS_CHANGED);
							xRegisterReceiver(mContext, intentFilter);
				        	isInitialized = true;
			        	}
			        	if(mContext != null && isBackKeyOptionEnabled){						
							if(keyCode == KeyEvent.KEYCODE_BACK && repeatCount > 0 && flags == 200 && down && !canceled ){
								mBackLongPressAsyncTask = new KillConceptAsyncTask();
								mBackLongPressAsyncTask.execute(backPressWaitTime);
							}else if(canceled){
								if(mBackLongPressAsyncTask != null && (mBackLongPressAsyncTask.getStatus() == AsyncTask.Status.PENDING | mBackLongPressAsyncTask.getStatus() == AsyncTask.Status.RUNNING)){
									mBackLongPressAsyncTask.cancel(true);
								}
							}
						}
			        }
				} catch (Throwable throwable) {
					XposedBridge.log(throwable);
				}
			}			
		};
		xHookMethod(xPhoneWindowManager, "interceptKeyBeforeDispatching", callback, (Boolean) xGetValue(prefs, xGetString(R.string.settings_hook_keybuttonview_back_key), false));
	}
	
	static class KillConceptAsyncTask extends AsyncTask<Integer, Void, Boolean>{
		@Override
		protected synchronized Boolean doInBackground(Integer... params) {
			boolean result = false;
			if(mContext != null){
				try{
		            if(phonewindowmanager != null){
		                XposedHelpers.callMethod(phonewindowmanager, "performHapticFeedbackLw", new Object[]{ null, 0, false} );            		
		            }
		            String homeAppPackageName = "com.android.launcher";
					String topActivityPackageName;
					Intent intent = new Intent("android.intent.action.MAIN");
			        intent.addCategory("android.intent.category.HOME");
			        ActivityInfo activityinfo = mContext.getPackageManager().resolveActivity(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT).activityInfo;
			        if (activityinfo != null) homeAppPackageName = activityinfo.packageName;
			        ActivityManager activitymanager = null;
			        if(mContext != null) activitymanager = (ActivityManager) mContext.getSystemService("activity");
			        if(activitymanager != null){
			        	Iterator<?> iterator = activitymanager.getRunningTasks(1).iterator();
				        do{
				        	RunningTaskInfo runningtaskinfo = (RunningTaskInfo) iterator.next();
				            topActivityPackageName = runningtaskinfo.topActivity.getPackageName();
				            if(topActivityPackageName == null || homeAppPackageName.equals(topActivityPackageName)){
				                break;
				            }
				            Thread.sleep(params[0]);
				            result = (Boolean) XposedHelpers.callMethod(activitymanager, "removeTask", new Object[]{ runningtaskinfo.id, 1} );
				        }while(iterator.hasNext());
			        }		        
		        } catch (IllegalArgumentException e) {
					XposedBridge.log(e);
				} catch (ClassCastException e) {
					XposedBridge.log(e);
				} catch (InterruptedException e) {
					XposedBridge.log(e);
				}
			}
			return result;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if(mContext != null && result != null && result){
				Configuration config = new Configuration();
				config.locale = Locale.getDefault();
			    xModuleResources.updateConfiguration(config, null);
	            Toast.makeText(mContext, xModuleResources.getString(R.string.app_killed_message), Toast.LENGTH_SHORT).show();	
            }
		}
	}

	@Override
	protected void xOnReceive(Context context, Intent intent) {
		super.xOnReceive(context, intent);
		xLog(TAG + " : " + "OnReceive " + intent.getAction());
		if (intent.getAction().equals(Settings.KEY_SETTINGS_CHANGED)) {
			isBackKeyOptionEnabled = intent.getBooleanExtra(Settings.BACKKEY_OPTION_ENABLED, false);
			int kill_app_longpress_back_wait = intent.getIntExtra(Settings.BACKKEY_WAIT_TIME, 15);
        	backPressWaitTime = kill_app_longpress_back_wait*100;
		}
	}	
}