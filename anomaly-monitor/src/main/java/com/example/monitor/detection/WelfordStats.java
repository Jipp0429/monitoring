package com.example.monitor.detection;

/**
 * Welford's Online Algorithm.
 * 전체 데이터를 메모리에 들고 있지 않고도 스트리밍 방식으로
 * 평균(mean)과 분산(variance)을 갱신할 수 있게 해준다.
 * 디바이스 수가 수천 대로 늘어나도 디바이스당 상태값 3개(n, mean, M2)만 유지하면 되므로
 * 메모리 사용량이 디바이스 수에 대해 선형으로만 증가한다 (안정성의 핵심 근거).
 *
 * 스레드 안전하지 않음 - 디바이스 하나당 인스턴스 하나를 두고
 * 호출 지점(AnomalyDetectorService)에서 동기화한다.
 */
public class WelfordStats {

    private long n = 0;
    private double mean = 0.0;
    private double m2 = 0.0; // 편차 제곱의 합

    public void update(double value) {
        n++;
        double delta = value - mean;
        mean += delta / n;
        double delta2 = value - mean;
        m2 += delta * delta2;
    }

    public double getMean() {
        return mean;
    }

    public double getVariance() {
        if (n < 2) return 0.0;
        return m2 / (n - 1);
    }

    public double getStdDev() {
        return Math.sqrt(getVariance());
    }

    public long getCount() {
        return n;
    }

    /**
     * 현재 값의 Z-score를 계산한다. 표준편차가 0에 가까우면(초기 구간) 0을 반환.
     */
    public double zScore(double value) {
        double std = getStdDev();
        if (std < 1e-6) return 0.0;
        return (value - mean) / std;
    }
}
