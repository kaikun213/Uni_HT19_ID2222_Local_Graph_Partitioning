package se.kth.jabeja;

import org.apache.log4j.Logger;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import se.kth.jabeja.io.CLI;
import se.kth.jabeja.io.GraphReader;
import se.kth.jabeja.config.Config;
import se.kth.jabeja.rand.RandNoGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class Main {
    final static Logger logger = Logger.getLogger(Main.class);

    /**
     * Holds all the configurations parameters.
     */
    private Config config;

    HashMap<Integer, Node> graph;

    List<XYChart> realtimeCharts = new ArrayList<XYChart>();
    SwingWrapper<XYChart> realTimeDisplay;

    public static void main(String[] args) throws IOException {
        new Main().startApp(args);
    }

    private void startApp(String[] args) throws IOException {
        config = (new CLI()).parseArgs(args);

        //set seed for the application
        //Note for the results to be deterministic use
        //only one random generator.
        RandNoGenerator.setSeed(config.getSeed());

        //start JaBeJa
        runJabejas();
    }

    /**
     * parses the input graph
     *
     * @return
     */
    private HashMap<Integer, Node> readGraph() {
        GraphReader graphReader = new GraphReader();
        graph = graphReader.readGraph(config.getGraphFilePath(), config.getGraphInitialColorPolicy(), config.getNumPartitions());
        return graph;
    }

    /**
     * start the jabeja algorithm
     *
     * @return
     */
    private Result startJabeja(Jabeja host) throws IOException {
        return host.startJabeja(realTimeDisplay);
    }


    private Jabeja initJabeja(AnnealingType annealingType, Double delta, Double alpha, int chartIdx) {
        //read the input graph -> reset to default coloring/partitioning
        HashMap<Integer, Node> graph = readGraph();
        Config newConfig = config.copy();
        newConfig.setAnnealingType(annealingType);
        newConfig.setDelta(delta);
        newConfig.setAlpha(alpha);

        System.out.println(config.getRestartAtRound());

        Jabeja host = new Jabeja(graph, newConfig, chartIdx);
        this.realtimeCharts.add(host.getRealtime());

        return host;
    }

    private void runJabejas() throws IOException {
        int runs = 3;
        int annealingTypes = AnnealingType.values().length;
        Jabeja[] hosts = new Jabeja[runs * annealingTypes];
        Result[] results = new Result[runs * annealingTypes];

        double initAlpha = 0.85;
        double deltaAlpha = 0.05;

        double initDelta = 0.002;
        double deltaDelta = 0.001;

        double initCustomAlpha = 0.001;
        double deltaCustomAlpha = 0.001;

        // Initialize
        for (int i=0; i<runs; i++){
            hosts[i*annealingTypes] = initJabeja(AnnealingType.LINEAR, initDelta + deltaDelta*i, 0.0, i*annealingTypes);
            hosts[(i*annealingTypes)+1] = initJabeja(AnnealingType.EXPONENTIAL, 0.0, initAlpha + deltaAlpha*i, i*annealingTypes+1);
            hosts[(i*annealingTypes)+2] = initJabeja(AnnealingType.CUSTOM, 0.0, initCustomAlpha + deltaCustomAlpha*i, i*annealingTypes+2);
        }
        realTimeDisplay = new SwingWrapper<XYChart>(realtimeCharts);
        realTimeDisplay.displayChartMatrix();

        // Run in parallel
        Arrays.stream(hosts).parallel().forEach(host -> {
            try {
                results[host.getChartIdx()] = startJabeja(host);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        printResults(results);
    }


    private void printResults(Result[] results) throws IOException {
        XYChart edgeCut = new XYChartBuilder().title("Edge cuts").xAxisTitle("Rounds").yAxisTitle("Edge cuts").build();
        XYChart migrations = new XYChartBuilder().title("Migrations").xAxisTitle("Rounds").yAxisTitle("Migrations").build();
        XYChart swaps = new XYChartBuilder().title("Swaps").xAxisTitle("Rounds").yAxisTitle("Swaps").build();

        for (Result result : results) {
            XYSeries edgeSeries = edgeCut.addSeries(result.getIdentifier(), result.xRange(), result.edgeCut);
            XYSeries migrationSeries = migrations.addSeries(result.getIdentifier(), result.xRange(), result.migrations);
            XYSeries swapsSeries = swaps.addSeries(result.getIdentifier(), result.xRange(), result.swaps);

            edgeSeries.setMarker(SeriesMarkers.NONE);
            migrationSeries.setMarker(SeriesMarkers.NONE);
            swapsSeries.setMarker(SeriesMarkers.NONE);
        }

        // Customize Chart
        // edgeCut.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        edgeCut.getStyler().setLegendVisible(true);
        // migrations.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        migrations.getStyler().setLegendVisible(true);
        // swaps.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        swaps.getStyler().setLegendVisible(true);

        new SwingWrapper<XYChart>(edgeCut).displayChart();
        new SwingWrapper<XYChart>(migrations).displayChart();
        new SwingWrapper<XYChart>(swaps).displayChart();

        BitmapEncoder.saveBitmap(edgeCut, "./Edge_Cuts", BitmapEncoder.BitmapFormat.PNG);
        BitmapEncoder.saveBitmap(migrations, "./Migrations", BitmapEncoder.BitmapFormat.PNG);
        BitmapEncoder.saveBitmap(swaps, "./Swaps", BitmapEncoder.BitmapFormat.PNG);

    }
}
