package se.kth.jabeja;

import lombok.Data;
import se.kth.jabeja.config.Config;

import java.util.stream.IntStream;

@Data
public class Result {

    double[] edgeCut;
    double[] migrations;
    double[] swaps;
    int runs;
    String identifier;

    public Result(Config config) {
        this.runs = config.getRounds();
        this.edgeCut = new double[runs];
        this.migrations = new double[runs];
        this.swaps = new double[runs];
        this.identifier = getIdentifier(config);
    }

    public double[] xRange() {
        return IntStream.range(0, runs).asDoubleStream().toArray();
    }

    public String getIdentifier(Config config) {
        switch (config.getAnnealingType()) {
            case LINEAR: return config.getAnnealingType() + "_" + round(config.getDelta());
            case EXPONENTIAL: return config.getAnnealingType() + "_" + round(config.getAlpha());
            case CUSTOM: return config.getAnnealingType() + "_" + round(config.getAlpha());
            default: return "";
        }
    }

    private double round(double d) {
        return Math.round(d * 10000.0) / 10000.0;
    }
}
