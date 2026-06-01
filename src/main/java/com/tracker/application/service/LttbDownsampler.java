package com.tracker.application.service;

import com.tracker.domain.PricePoint;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class LttbDownsampler {

    public List<PricePoint> downsample(List<PricePoint> data, int targetSize) {
        if (data == null || data.isEmpty() || targetSize >= data.size()) {
            return data;
        }
        if (targetSize < 2) {
            return List.of(data.getFirst(), data.getLast());
        }

        int n = data.size();
        List<PricePoint> result = new ArrayList<>(targetSize);
        result.add(data.getFirst());

        double bucketSize = (double) (n - 2) / (targetSize - 2);

        for (int i = 0; i < targetSize - 2; i++) {
            int avgRangeStart = (int) Math.floor((i + 1) * bucketSize) + 1;
            int avgRangeEnd = (int) Math.floor((i + 2) * bucketSize) + 1;
            if (avgRangeEnd > n - 1) {
                avgRangeEnd = n - 1;
            }

            double avgX = 0;
            double avgY = 0;
            int avgCount = avgRangeEnd - avgRangeStart;
            for (int j = avgRangeStart; j < avgRangeEnd; j++) {
                avgX += data.get(j).timestamp().toEpochMilli();
                avgY += data.get(j).price().amount().doubleValue();
            }
            avgX /= avgCount;
            avgY /= avgCount;

            int bucketStart = (int) Math.floor(i * bucketSize) + 1;
            int bucketEnd = (int) Math.floor((i + 1) * bucketSize) + 1;

            PricePoint lastSelected = result.getLast();
            double pointAx = lastSelected.timestamp().toEpochMilli();
            double pointAy = lastSelected.price().amount().doubleValue();

            double maxArea = -1;
            int maxAreaIndex = bucketStart;

            for (int j = bucketStart; j < bucketEnd; j++) {
                double bx = data.get(j).timestamp().toEpochMilli();
                double by = data.get(j).price().amount().doubleValue();
                double area =
                    Math.abs(
                        (pointAx - avgX) * (by - pointAy) -
                            (pointAx - bx) * (avgY - pointAy)
                    ) / 2;
                if (area > maxArea) {
                    maxArea = area;
                    maxAreaIndex = j;
                }
            }

            result.add(data.get(maxAreaIndex));
        }

        result.add(data.getLast());
        return result;
    }
}
