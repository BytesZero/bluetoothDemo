package com.zsl.bluetoothdemo.application;

import android.app.Application;

import com.bugtags.library.Bugtags;

/**
 * Created by zsl on 15/9/22.
 */
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //BugTags
        Bugtags.start("ec53c69dfe79cdb66a978138716eb32e",this,Bugtags.BTGInvocationEventBubble);
    }
}
