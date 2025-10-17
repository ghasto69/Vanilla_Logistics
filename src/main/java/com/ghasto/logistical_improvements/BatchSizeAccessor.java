package com.ghasto.logistical_improvements;

public interface BatchSizeAccessor {
    int getMinimumBatchSize();
    void setMinimumBatchSize(int minimumBatchSize);
}
