package com.brt.ibridge.ui;

import com.brt.ibridge.Switcher;

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

	/**
	 * 类型安全的 View 查找，消除强制类型转换 
	 */
	@SuppressWarnings("unchecked")
	public <T extends View> T find(int id) {
		return (T) mRootView.findViewById(id);
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
