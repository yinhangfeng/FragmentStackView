package com.example.yinhangfeng.fragmentstackview.test;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by yhf on 2015/6/26.
 */
public class Fragment1 extends Fragment {

    private static int count = 0;

    private static final int[] COLORS = {Color.RED, Color.GREEN, Color.BLUE, Color.LTGRAY};

    private int id = count++;
    private String TAG = "Fragment1:" + id;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment1, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = (TextView) view.findViewById(R.id.text);
        textView.setText(TAG);
        textView.setBackgroundColor(COLORS[id % COLORS.length]);
    }
}
