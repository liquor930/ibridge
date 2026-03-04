package com.brt.ibridge;

import android.content.Context;
import android.view.View;

public abstract class Screen {
	private final View mRootView;
	private Switcher mSwitcher;
	protected Context mContext;

	public Screen(Context context, View root) {
		mContext = context;
		mRootView = root;
	}

	public View findViewById(int id) {
		return mRootView.findViewById(id);
	}

	public void setSwitcher(Switcher switcher) {
		mSwitcher = switcher;
	}

	protected void toggleView(int to, Object param) {
		if (mSwitcher != null) {
			mSwitcher.toggleView(to, param);
		}
	}

	public abstract void refreshScreen();
}
