package com.gg.maiotproject.interfaces;

public interface IMyComplementaryFilter {

    void onNewFilteredAngleAvailable(float filteredRoll);
    void onNewMaxPositiveRollAvailable(double maxPositiveRoll);
    void onNewMaxNegativeRollAvailable(double maxNegativeRoll);

}
