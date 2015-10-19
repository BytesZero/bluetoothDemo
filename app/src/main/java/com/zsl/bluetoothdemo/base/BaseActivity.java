package com.zsl.bluetoothdemo.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.bugtags.library.Bugtags;

/**
 * Created by zsl on 15/9/22.
 */
public class BaseActivity extends AppCompatActivity{
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity=this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //注：回调 1
        Bugtags.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //注：回调 2
        Bugtags.onPause(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        //注：回调 3
        Bugtags.onDispatchTouchEvent(this, event);
        return super.dispatchTouchEvent(event);
    }

    /**
     * 显示长的吐司
     * @param content
     */
    protected void showLongToast(String content){
        Toast.makeText(mActivity, ""+content, Toast.LENGTH_LONG).show();
    }

    /**
     *  显示短的吐司
     * @param content
     */
    protected void showShortToast(String content){
        Toast.makeText(mActivity, "", Toast.LENGTH_SHORT).show();
    }


    /**
     * startActivity
     * @param cls
     */
    protected void baseStartActivity(Class<?> cls){
        Intent intent=new Intent(mActivity,cls);
        startActivity(intent);
    }

    /**
     * startActivityForResult
     * @param cls
     * @param requestCode
     */
    protected void baseStartActivityForResult(Class<?> cls,int requestCode){
        Intent intent=new Intent(mActivity,cls);
        startActivityForResult(intent,requestCode);
    }
}
