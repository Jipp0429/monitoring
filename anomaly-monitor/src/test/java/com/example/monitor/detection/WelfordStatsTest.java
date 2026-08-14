package com.example.monitor.detection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WelfordStatsTest {

    private static final double DELTA = 1e-9;

    @Test
    void meanAndVarianceMatchKnownDataset() {
        // {2,4,4,4,5,5,7,9}: mean=5, sample variance(n-1)=32/7, stddev=sqrt(32/7)
        double[] values = {2, 4, 4, 4, 5, 5, 7, 9};
        WelfordStats stats = new WelfordStats();
        for (double v : values) {
            stats.update(v);
        }

        assertThat(stats.getCount()).isEqualTo(values.length);
        assertThat(stats.getMean()).isCloseTo(5.0, within(DELTA));
        assertThat(stats.getVariance()).isCloseTo(32.0 / 7.0, within(DELTA));
        assertThat(stats.getStdDev()).isCloseTo(Math.sqrt(32.0 / 7.0), within(DELTA));
    }

    @Test
    void varianceIsZeroWithFewerThanTwoSamples() {
        WelfordStats stats = new WelfordStats();
        assertThat(stats.getVariance()).isZero();

        stats.update(42.0);
        assertThat(stats.getCount()).isEqualTo(1);
        assertThat(stats.getVariance()).isZero();
        assertThat(stats.getMean()).isCloseTo(42.0, within(DELTA));
    }

    @Test
    void zScoreIsZeroWhenStdDevNearZero() {
        WelfordStats stats = new WelfordStats();
        // 동일한 값만 반복되면 stdDev가 0에 수렴한다.
        for (int i = 0; i < 20; i++) {
            stats.update(10.0);
        }

        assertThat(stats.getStdDev()).isCloseTo(0.0, within(1e-6));
        assertThat(stats.zScore(999.0)).isZero();
    }

    @Test
    void zScoreSignMatchesDeviationDirection() {
        WelfordStats stats = new WelfordStats();
        for (int i = 0; i < 30; i++) {
            // 평균 50, 편차가 있는 값들을 번갈아 넣어 표준편차를 확보한다.
            stats.update(i % 2 == 0 ? 48.0 : 52.0);
        }

        double zAboveMean = stats.zScore(80.0);
        double zBelowMean = stats.zScore(20.0);

        assertThat(zAboveMean).isPositive();
        assertThat(zBelowMean).isNegative();
        assertThat(Math.abs(zAboveMean)).isGreaterThan(3.0);
        assertThat(Math.abs(zBelowMean)).isGreaterThan(3.0);
    }
}
