package com.ghasto.logistical_improvements.batch_size;

public interface BatchSizeAccessor {
    int getMinimumBatchSize();
    void setMinimumBatchSize(int minimumBatchSize);
}
