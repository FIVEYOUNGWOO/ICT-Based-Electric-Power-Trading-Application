package com.borax12.mg_project;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class ThreadEx extends Thread{

    private Handler handler;
    private boolean check = true;

    // generator
    public ThreadEx(Handler handler){ this.handler = handler; }
    public void setCheck(boolean check){ this.check = check; }

    @Override
    public void run()
    {
        int value = 0;
        while(check)
        {
            try
            {
                Thread.sleep(10);
            }catch (InterruptedException e){ e.printStackTrace(); }

            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putInt("value",value);
            msg.setData(bundle);
            handler.sendMessage(msg);
            value++;
        }
        super.run();
    }
}
