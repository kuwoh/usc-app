package com.tutorial.game;

import org.libsdl.app.SDLActivity;
import android.os.Bundle;
import com.usc.storage.StorageBridge;

public class HelloSDL2Activity extends SDLActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StorageBridge.init(this);
    }
}