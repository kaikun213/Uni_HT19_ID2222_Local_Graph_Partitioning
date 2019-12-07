package se.kth.jabeja;

import org.apache.log4j.Logger;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.config.NodeSelectionPolicy;
import se.kth.jabeja.io.FileIO;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static se.kth.jabeja.AnnealingType.EXPONENTIAL;
import static se.kth.jabeja.AnnealingType.LINEAR;

public class Jabeja {
  final static Logger logger = Logger.getLogger(Jabeja.class);
  private final Config config;
  private final HashMap<Integer/*id*/, Node/*neighbors*/> entireGraph;
  private final List<Integer> nodeIds;
  private int numberOfSwaps;
  private int round;
  private double T;
  private boolean resultFileCreated = false;

  // New
  private Result result;
  private Result[] liveData;
  private double T_min = 0.00001;
  private XYChart realtime;
  private int chartIdx;

  //-------------------------------------------------------------------
  public Jabeja(HashMap<Integer, Node> graph, Config config, int chartIdx) {
    this.entireGraph = graph;
    this.nodeIds = new ArrayList(entireGraph.keySet());
    this.round = 0;
    this.numberOfSwaps = 0;
    this.config = config;
    this.T = config.getAnnealingType() == LINEAR ? config.getTemperature() : 1.0;
    this.result = new Result(config);
    this.liveData = new Result[config.getRounds()];
    this.chartIdx = chartIdx;

    Result initResult = new Result(config);
    initResult.edgeCut[round] = 0;
    this.realtime = QuickChart.getChart("Real-time Edge count", "Rounds", "Edge cuts", initResult.getIdentifier(), initResult.xRange(), initResult.getEdgeCut());
    realtime.getStyler().setLegendVisible(true);
    realtime.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
  }

  public XYChart getRealtime() {
    return this.realtime;
  }

  public int getChartIdx() {
    return this.chartIdx;
  }

  //-------------------------------------------------------------------
  public Result startJabeja(SwingWrapper<XYChart> realTimeDisplay) throws IOException {

    for (round = 0; round < config.getRounds(); round++) {
      for (int id : entireGraph.keySet()) {
        sampleAndSwap(id);
      }

      //one cycle for all nodes have completed.
      //reduce the temperature
      saCoolDown();


      // Update stats and redraw graph
      report();
      realtime.updateXYSeries(this.result.getIdentifier(), liveData[round].xRange(), liveData[round].getEdgeCut(), null);
      realTimeDisplay.repaintChart(chartIdx);
    }

    return result;
  }

  /**
   * Simulated analealing cooling function
   */
  private void saCoolDown(){
    switch (config.getAnnealingType()) {
      case LINEAR : {
        if (T > 1)
          T -= config.getDelta();
        if (T < 1)
          T = 1;
      }
      break;
      case EXPONENTIAL: {
        if (T > T_min) {
          T *= config.getAlpha();
        }
        if (T < T_min) {
          T = T_min;
        }
      }
      break;
      case CUSTOM: {
        // TODO - improve with custom annealing function
        T *= config.getAlpha();
      }
    }
  }

  private boolean acceptance(double oldBenefit, double newBenefit) {
    switch (config.getAnnealingType()) {
      case EXPONENTIAL:
        // Non-linear: If newBenefit better than oldBenefit -> always 100% probability, otherwise lowers with T and diff
        // NOTE: On the webpage they use the cost. In that case the formula would have been needed to be switched.
        double probability = Math.exp((newBenefit-oldBenefit)/T);
        return probability > Math.random();
      // Current Temperature T biases towards selecting new states (in the initial rounds)
      case LINEAR: return newBenefit * T > oldBenefit;
      // TODO: Improve with custom annealing function
      case CUSTOM: return Math.exp((newBenefit-oldBenefit)/T) > Math.random();
      default: return false;
    }
  }

  /**
   * Sample and swap algorith at node p
   * @param nodeId
   */
  private void sampleAndSwap(int nodeId) {
    Node partner = null;
    Node nodep = entireGraph.get(nodeId);

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.LOCAL) {
      // swap with random neighbors
      partner = findPartner(nodeId, getNeighbors(nodep));
    }

    if (config.getNodeSelectionPolicy() == NodeSelectionPolicy.HYBRID
            || config.getNodeSelectionPolicy() == NodeSelectionPolicy.RANDOM) {
      // if local policy fails then randomly sample the entire graph
      partner = findPartner(nodeId, getSample(nodeId));
    }

    // swap the colors
    if (partner != null) {
      numberOfSwaps++;
      int colorp = nodep.getColor();
      nodep.setColor(partner.getColor());
      partner.setColor(colorp);
    }

    // NOTE: Paper suggests cool down after each swap -> code suggests global cool down instead (after all node-swaps).
    // saCoolDown();
  }

  public Node findPartner(int nodeId, Integer[] nodes){

    Node nodep = entireGraph.get(nodeId);

    Node bestPartner = null;
    double highestBenefit = 0;

    // Iterate over possible swap-partners and calculate cost/benefit
    for (Integer n : nodes){
      Node potentialPartner = entireGraph.get(n);
      // Calculate current benefit -> Sum of neighbours with same color for both nodes
      double nodepDegree = getDegree(nodep, nodep.getColor());
      double ppDegree = getDegree(potentialPartner, potentialPartner.getColor());
      double previousBenefit = nodepDegree + ppDegree;

      // Calculate potential benefit -> Sum of neighbours with same color for both nodes when color is switched
      // NOTE: This does not account for the node itself being a neighbour, as it would have an updated color.
      //       But follows the algorithm from the paper.
      double nodepSwitchDegree = getDegree(nodep, potentialPartner.getColor());
      double ppSwitchDegree = getDegree(potentialPartner, nodep.getColor());
      double potentialBenefit = nodepSwitchDegree + ppSwitchDegree;

      if (acceptance(previousBenefit, potentialBenefit) && potentialBenefit > highestBenefit) {
        bestPartner = potentialPartner;
        highestBenefit = potentialBenefit;
      }
    }

    return bestPartner;
  }

  /**
   * The the degreee on the node based on color
   * @param node
   * @param colorId
   * @return how many neighbors of the node have color == colorId
   */
  private int getDegree(Node node, int colorId){
    int degree = 0;
    for(int neighborId : node.getNeighbours()){
      Node neighbor = entireGraph.get(neighborId);
      if(neighbor.getColor() == colorId){
        degree++;
      }
    }
    return degree;
  }

  /**
   * Returns a uniformly random sample of the graph
   * @param currentNodeId
   * @return Returns a uniformly random sample of the graph
   */
  private Integer[] getSample(int currentNodeId) {
    int count = config.getUniformRandomSampleSize();
    int rndId;
    int size = entireGraph.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    while (true) {
      rndId = nodeIds.get(RandNoGenerator.nextInt(size));
      if (rndId != currentNodeId && !rndIds.contains(rndId)) {
        rndIds.add(rndId);
        count--;
      }

      if (count == 0)
        break;
    }

    Integer[] ids = new Integer[rndIds.size()];
    return rndIds.toArray(ids);
  }

  /**
   * Get random neighbors. The number of random neighbors is controlled using
   * -closeByNeighbors command line argument which can be obtained from the config
   * using {@link Config#getRandomNeighborSampleSize()}
   * @param node
   * @return
   */
  private Integer[] getNeighbors(Node node) {
    ArrayList<Integer> list = node.getNeighbours();
    int count = config.getRandomNeighborSampleSize();
    int rndId;
    int index;
    int size = list.size();
    ArrayList<Integer> rndIds = new ArrayList<Integer>();

    if (size <= count)
      rndIds.addAll(list);
    else {
      while (true) {
        index = RandNoGenerator.nextInt(size);
        rndId = list.get(index);
        if (!rndIds.contains(rndId)) {
          rndIds.add(rndId);
          count--;
        }

        if (count == 0)
          break;
      }
    }

    Integer[] arr = new Integer[rndIds.size()];
    return rndIds.toArray(arr);
  }


  private int[] getEdgeCutAndMigrations() {
    int grayLinks = 0;
    int migrations = 0; // number of nodes that have changed the initial color
    int size = entireGraph.size();

    for (int i : entireGraph.keySet()) {
      Node node = entireGraph.get(i);
      int nodeColor = node.getColor();
      ArrayList<Integer> nodeNeighbours = node.getNeighbours();

      if (nodeColor != node.getInitColor()) {
        migrations++;
      }

      if (nodeNeighbours != null) {
        for (int n : nodeNeighbours) {
          Node p = entireGraph.get(n);
          int pColor = p.getColor();

          if (nodeColor != pColor)
            grayLinks++;
        }
      }
    }

    int edgeCut = grayLinks / 2;

    return new int[]{edgeCut, migrations};
  }

  /**
   * Generate a report which is stored in a file in the output dir.
   *
   * @throws IOException
   */
  private void report() throws IOException {
    int[] infos = getEdgeCutAndMigrations();
    int edgeCut = infos[0];
    int migrations = infos[1];

    logger.info("round: " + round +
            ", edge cut:" + edgeCut +
            ", swaps: " + numberOfSwaps +
            ", migrations: " + migrations +
            ", T: " + T);

    save(edgeCut, migrations);
    //saveToFile(edgeCut, migrations);
  }


  /**
   * Make an array of results
   */
  private void save(int edgeCuts, int migrations) {
    this.result.edgeCut[round] = edgeCuts;
    this.result.migrations[round] = migrations;
    this.result.swaps[round] = numberOfSwaps;
    this.liveData[round] = this.result;
  }

  private void saveToFile(int edgeCuts, int migrations) throws IOException {
    String delimiter = "\t\t";
    String outputFilePath;

    //output file name
    File inputFile = new File(config.getGraphFilePath());
    outputFilePath = config.getOutputDir() +
            File.separator +
            inputFile.getName() + "_" +
            "NS" + "_" + config.getNodeSelectionPolicy() + "_" +
            "GICP" + "_" + config.getGraphInitialColorPolicy() + "_" +
            "T" + "_" + config.getTemperature() + "_" +
            "D" + "_" + config.getDelta() + "_" +
            "RNSS" + "_" + config.getRandomNeighborSampleSize() + "_" +
            "URSS" + "_" + config.getUniformRandomSampleSize() + "_" +
            "A" + "_" + config.getAlpha() + "_" +
            "R" + "_" + config.getRounds() + ".txt";

    if (!resultFileCreated) {
      File outputDir = new File(config.getOutputDir());
      if (!outputDir.exists()) {
        if (!outputDir.mkdir()) {
          throw new IOException("Unable to create the output directory");
        }
      }
      // create folder and result file with header
      String header = "# Migration is number of nodes that have changed color.";
      header += "\n\nRound" + delimiter + "Edge-Cut" + delimiter + "Swaps" + delimiter + "Migrations" + delimiter + "Skipped" + "\n";
      FileIO.write(header, outputFilePath);
      resultFileCreated = true;
    }

    FileIO.append(round + delimiter + (edgeCuts) + delimiter + numberOfSwaps + delimiter + migrations + "\n", outputFilePath);
  }
}
