package com.ru426.android.xposed.parts.keybutton.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.media.audiofx.BassBoost.Settings;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.ru426.android.xposed.parts.keybutton.R;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {

	private final String TAG = getClass().getName();

	private static final String ANDROIDNS = "http://schemas.android.com/apk/res/android";
	private static final String APPLICATIONNS = "http://schemas.android.com/apk/res/"+Settings.class.getPackage().getName();
	private static final int DEFAULT_VALUE = 50;

	private int mMaxValue = 100;
	private int mMinValue = 0;
	private int mInterval = 1;
	private int mCurrentValue;
	private String mUnitsLeft = "";
	private String mUnitsRight = "";
	private SeekBar mSeekBar;

	private TextView mStatusText;
	private TextView unitsRight;
	private Resources mResources;

	public SeekBarPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initPreference(context, attrs);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initPreference(context, attrs);
	}

	private void initPreference(Context context, AttributeSet attrs) {
		setValuesFromXml(attrs);
		mSeekBar = new SeekBar(context, attrs);
		mSeekBar.setMax(mMaxValue - mMinValue);
		mSeekBar.setOnSeekBarChangeListener(this);

		setWidgetLayoutResource(R.layout.seek_bar_preference);
		
		mResources = context.getResources();
	}

	private void setValuesFromXml(AttributeSet attrs) {
		mMaxValue = attrs.getAttributeIntValue(ANDROIDNS, "max", 100);
		mMinValue = attrs.getAttributeIntValue(APPLICATIONNS, "min", 0);

		mUnitsLeft = getAttributeStringValue(attrs, APPLICATIONNS, "unitsLeft", "");
		String units = getAttributeStringValue(attrs, APPLICATIONNS, "units", "");
		mUnitsRight = getAttributeStringValue(attrs, APPLICATIONNS, "unitsRight", units);

		try {
			String newInterval = attrs.getAttributeValue(APPLICATIONNS, "interval");
			if (newInterval != null)
				mInterval = Integer.parseInt(newInterval);
		} catch (Exception e) {
			Log.e(TAG, "Invalid interval value", e);
		}

	}

	private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
		String value = attrs.getAttributeValue(namespace, name);
		if (value == null)
			value = defaultValue;

		return value;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);

		// The basic preference layout puts the widget frame to the right of the
		// title and summary,
		// so we need to change it a bit - the seekbar should be under them.
		LinearLayout layout = (LinearLayout) view;
		layout.setOrientation(LinearLayout.VERTICAL);

		return view;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBindView(View view) {
		super.onBindView(view);

		try {
			// move our seekbar to the new view we've been given
			ViewParent oldContainer = mSeekBar.getParent();
			ViewGroup newContainer = (ViewGroup) view.findViewById(R.id.seekBarPrefBarContainer);

			if (oldContainer != newContainer) {
				// remove the seekbar from the old view
				if (oldContainer != null) {
					((ViewGroup) oldContainer).removeView(mSeekBar);
				}
				// remove the existing seekbar (there may not be one) and add
				// ours
				newContainer.removeAllViews();
				newContainer.addView(mSeekBar, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			}
		} catch (Exception ex) {
			Log.e(TAG, "Error binding view: " + ex.toString());
		}

		// if dependency is false from the beginning, disable the seek bar
		if (view != null && !view.isEnabled()) {
			mSeekBar.setEnabled(false);
		}

		updateView(view);
	}

	/**
	 * Update a SeekBarPreference view with our current state
	 * 
	 * @param view
	 */
	protected void updateView(View view) {

		try {
			mStatusText = (TextView) view.findViewById(R.id.seekBarPrefValue);

			mStatusText.setText(String.valueOf(mCurrentValue/10.0d));
			mStatusText.setMinimumWidth(30);

			mSeekBar.setProgress(mCurrentValue - mMinValue);

			unitsRight = (TextView) view.findViewById(R.id.seekBarPrefUnitsRight);
			unitsRight.setText(mUnitsRight);

			TextView unitsLeft = (TextView) view.findViewById(R.id.seekBarPrefUnitsLeft);
			unitsLeft.setText(mUnitsLeft);

		} catch (Exception e) {
			Log.e(TAG, "Error updating seek bar preference", e);
		}

	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		int newValue = progress + mMinValue;

		if (newValue > mMaxValue)
			newValue = mMaxValue;
		else if (newValue < mMinValue)
			newValue = mMinValue;
		else if (mInterval != 1 && newValue % mInterval != 0)
			newValue = Math.round(((float) newValue) / mInterval) * mInterval;

		// change rejected, revert to the previous value
		if (!callChangeListener(newValue)) {
			seekBar.setProgress(mCurrentValue - mMinValue);
			return;
		}

		// change accepted, store it
		mCurrentValue = newValue;
		String unit = mResources.getQuantityString(R.plurals.settings_hook_keybuttonview_back_wait_unit, 0);
		if(unitsRight != null){
			if(mCurrentValue > 19){
				unit = mResources.getQuantityString(R.plurals.settings_hook_keybuttonview_back_wait_unit, 2);
			}else{
				unit = mResources.getQuantityString(R.plurals.settings_hook_keybuttonview_back_wait_unit, 1);
			}
			unitsRight.setText(mUnitsRight = unit);
		}
		
		mStatusText.setText(String.valueOf(newValue/10.0d));
		persistInt(newValue);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		notifyChanged();
		if(mOnSeekBarPreferenceChangeListener != null) mOnSeekBarPreferenceChangeListener.onStopTrackingTouch(seekBar, mUnitsRight);
	}
	
	private OnSeekBarPreferenceChangeListener mOnSeekBarPreferenceChangeListener;
	public void setOnSeekBarPreferenceChangeListener(OnSeekBarPreferenceChangeListener listener){
		mOnSeekBarPreferenceChangeListener = listener;
	}
	public interface OnSeekBarPreferenceChangeListener{
		public void onStopTrackingTouch(SeekBar seekBar, String unit);
	}

	@Override
	protected Object onGetDefaultValue(TypedArray ta, int index) {
		int defaultValue = ta.getInt(index, DEFAULT_VALUE);
		return defaultValue;
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

		if (restoreValue) {
			mCurrentValue = getPersistedInt(mCurrentValue);
		} else {
			int temp = 0;
			try {
				temp = (Integer) defaultValue;
			} catch (Exception ex) {
				Log.e(TAG, "Invalid default value: " + defaultValue.toString());
			}

			persistInt(temp);
			mCurrentValue = temp;
		}

	}

	/**
	 * make sure that the seekbar is disabled if the preference is disabled
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mSeekBar.setEnabled(enabled);
	}

	@Override
	public void onDependencyChanged(Preference dependency, boolean disableDependent) {
		super.onDependencyChanged(dependency, disableDependent);

		// Disable movement of seek bar when dependency is false
		if (mSeekBar != null) {
			mSeekBar.setEnabled(!disableDependent);
		}
	}
}
