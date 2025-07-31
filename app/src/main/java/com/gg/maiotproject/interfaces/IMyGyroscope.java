package com.gg.maiotproject.interfaces;

public interface IMyGyroscope {
    void onNewGyroscopeDataAvailable(float RotX, float RotY, float RotZ, long timestamp);
}
