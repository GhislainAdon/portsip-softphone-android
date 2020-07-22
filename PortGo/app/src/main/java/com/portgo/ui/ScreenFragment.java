package com.portgo.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.portgo.R;


public class ScreenFragment extends Fragment {
	Context context = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View myView;
		super.onCreateView(inflater, container, savedInstanceState);
		context = getActivity();
		myView = inflater.inflate(R.layout.activity_avcall_black, null);

		return myView;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	@Override
	public void onResume() {
        super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}
