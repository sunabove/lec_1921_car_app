package com.carapp;

import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ComActivity extends AppCompatActivity implements ComInterface {

    public abstract int getLayoutId() ;

    public <T extends View> T findViewById(@IdRes int id) {
        return (T) super.findViewById(id);
    }

    public void postDelayed(Runnable r, int delayMillis) {
        new Handler().postDelayed( r, delayMillis);
    }

    public void sleep( long millis ) {
        try {
            Thread.currentThread().sleep(millis);
        } catch ( Exception e ) {
            //
        }
    }

    public int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( this.getLayoutId() );
    }

}
