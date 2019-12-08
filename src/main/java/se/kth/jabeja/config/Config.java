package se.kth.jabeja.config;

import se.kth.jabeja.AnnealingType;

public class Config {
  private Integer numPartitions;
  private Integer rounds;
  private Integer randomNeighborsSampleSize;
  private Double temperature;
  private Double delta;
  private Integer seed;
  private Integer uniformRandomSampleSize;
  private String graphFile;
  private String outputDir;
  private GraphInitColorPolicy initColorPolicy;
  private NodeSelectionPolicy nodeSelectionPolicy;
  private Double alpha;
  private AnnealingType annealingType;
  private Integer restartAtRound;

  public Config setAlpha(Double alpha) {
    this.alpha = alpha;
    return this;
  }

  public Config setAnnealingType(AnnealingType annealingType) {
    this.annealingType = annealingType;
    return this;
  }

  public Config setGraphInitialColorPolicy(GraphInitColorPolicy policy) {
    this.initColorPolicy = policy;
    return this;
  }

  public Config setOutputDir(String outputDir) {
    this.outputDir = outputDir;
    return this;
  }

  public Config setNodeSelectionPolicy(NodeSelectionPolicy nodeSelectionPolicy) {
    this.nodeSelectionPolicy = nodeSelectionPolicy;
    return this;
  }

  public Config setGraphFilePath(String graphFilePath) {
    this.graphFile = graphFilePath;
    return this;
  }

  public Config setNumPartitions(Integer num_partitions) {
    this.numPartitions = num_partitions;
    return this;
  }

  public Config setRounds(Integer rounds) {
    this.rounds = rounds;
    return this;
  }

  public Config setRandNeighborsSampleSize(Integer closeby_neighbors) {
    this.randomNeighborsSampleSize = closeby_neighbors;
    return this;
  }

  public Config setTemperature(Double temperature) {
    this.temperature = temperature;
    return this;
  }

  public Config setDelta(Double delta) {
    this.delta = delta;
    return this;
  }

  public Config setSeed(Integer seed) {
    this.seed = seed;
    return this;
  }

  public Config setUniformRandSampleSize(Integer rnd_list_size) {
    this.uniformRandomSampleSize = rnd_list_size;
    return this;
  }

  public Config setRestartAtRound(Integer round) {
    this.restartAtRound = round;
    return this;
  }

  public Integer getNumPartitions() {
    if (numPartitions == null) {
      throw new NullPointerException("Num partitions is not set");
    }
    return numPartitions;
  }

  public Integer getRounds() {
    if (rounds == null) {
      throw new NullPointerException("Rounds is not set");
    }
    return rounds;
  }

  public Integer getRandomNeighborSampleSize() {
    if (randomNeighborsSampleSize == null) {
      throw new NullPointerException("Close by neighbors are not set");
    }
    return randomNeighborsSampleSize;
  }

  public Double getTemperature() {
    if (temperature == null) {
      throw new NullPointerException("Temperature is not set");
    }
    return temperature;
  }

  public Double getDelta() {
    if (delta == null) {
      throw new NullPointerException("Delta is not set");
    }
    return delta;
  }

  public Integer getSeed() {
    if (seed == null) {
      throw new NullPointerException("Seed is not set");
    }
    return seed;
  }

  public Integer getUniformRandomSampleSize() {
    if (uniformRandomSampleSize == null) {
      throw new NullPointerException("Random list size is not set");
    }
    return uniformRandomSampleSize;
  }

  public String getGraphFilePath() {
    if (graphFile == null) {
      throw new NullPointerException("Graph file path is not set");
    }
    return graphFile;
  }

  public GraphInitColorPolicy getGraphInitialColorPolicy() {
    if (initColorPolicy == null) {
      throw new NullPointerException("Graph initial color policy is not defined.");
    }
    return initColorPolicy;
  }

  public NodeSelectionPolicy getNodeSelectionPolicy() {
    if (nodeSelectionPolicy == null) {
      throw new NullPointerException("Node selection policy is not defined.");
    }
    return nodeSelectionPolicy;
  }

  public String getOutputDir() {
    if (outputDir == null) {
      throw new NullPointerException("Output dir is not set");
    }
    return outputDir;

  }

  public Double getAlpha() {
    if (alpha == null) {
      throw new NullPointerException("Alpha is not set");
    }
    return alpha;
  }

  public AnnealingType getAnnealingType() {
    if (annealingType == null) {
      throw new NullPointerException("Annealing type is not set");
    }
    return annealingType;
  }

  public Integer getRestartAtRound() {
    return this.restartAtRound;
  }

  public Config createJabejaConfig() {
    return new Config();
  }

  public Config copy() {
    Config c = new Config();
    c.setUniformRandSampleSize(this.uniformRandomSampleSize);
    c.setRandNeighborsSampleSize(this.randomNeighborsSampleSize);
    c.setGraphFilePath(this.graphFile);
    c.setNumPartitions(this.numPartitions);
    c.setRounds(this.rounds);
    c.setTemperature(this.temperature);
    c.setDelta(this.delta);
    c.setSeed(this.seed);
    c.setOutputDir(this.outputDir);
    c.setGraphInitialColorPolicy(this.initColorPolicy);
    c.setNodeSelectionPolicy(this.nodeSelectionPolicy);
    c.setAlpha(this.alpha);
    c.setAnnealingType(this.annealingType);
    c.setRestartAtRound(this.restartAtRound);
    return c;
  }

}