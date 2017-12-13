package com.taxi.baidunavisdkdemo;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

/**
 * Created by shizhengui on 2017/12/5.
 */

public class LogUtil {
    public static void showToast(Context context,CharSequence text){
        Toast toast = Toast.makeText(context,text,Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER,0,0);
        toast.show();
    }
}
