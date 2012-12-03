package com.example.twitterapp;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;

class MyViewPagerAdapter extends PagerAdapter {

	ArrayList<View> data;
	private Context ctx;
	private MyViewPagerAdapter adapter;

	public MyViewPagerAdapter(Context ctx, ArrayList<View> data) {
		this.ctx = ctx;
		this.data = data;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public float getPageWidth(int position) {
		/*
		if (position == getCount() - 1) {
			return (0.482375f);
		} else {
			return (0.25025f);
		}
		*/
		return(1.0f);
	}

	@Override
	public Object instantiateItem(View collection, int position) {
		View card = data.get(position);
		((ViewPager) collection).addView(data.get(position));
		return card;
	}

	@Override
	public void destroyItem(View collection, int position, Object view) {
		((ViewPager) collection).removeView((View) view);
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	@Override
	public Parcelable saveState() {
		return null;
	}

	@Override
	public void restoreState(Parcelable arg0, ClassLoader arg1) {
	}

	@Override
	public void startUpdate(View arg0) {
	}

	@Override
	public void finishUpdate(View arg0) {
	}

	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}
}
