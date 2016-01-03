import javafx.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/2/2016.
 */
public class Main {

    public static void main(String args[]) {

        String wd = "c:\\Users\\mike\\src\\money";
        String cp = "-cp out\\production\\money com.mike.money.Main";

        List<String> ssaScenarios = new ArrayList<String>();
        ssaScenarios.add("r62-f70");
        ssaScenarios.add("r65-f70");

        List<String> roiScenarios = new ArrayList<String>();
        roiScenarios.add("1");
        roiScenarios.add("2");
        roiScenarios.add("3");
        roiScenarios.add("4");

        List<Boolean> SpecialIncomeScenarios = new ArrayList<Boolean>();
        SpecialIncomeScenarios.add(true);
        SpecialIncomeScenarios.add(false);

        List<String> fullOutput = new ArrayList<String>();

        // for the years collections of maps of year to assets
        Map<Integer, List<Pair<String, Integer>>> assets = new HashMap<Integer, List<Pair<String, Integer>>>();
        List<String> dsNames = new ArrayList<String>();
        List<Integer> years = new ArrayList<Integer>();

        PrintWriter scenarioOutput = null;
        try {
            for (String ssa : ssaScenarios) {
                for (String roi : roiScenarios) {
                    for (Boolean sis : SpecialIncomeScenarios) {
                        String cmd = String.format("java %s -loadSSA SSAnalyze-%s.csv -roi 0.0%s %s %s",
                                cp, ssa, roi, getSpecialIncome(sis), getShowAccounts());
                        //        "-noSpecials -showAccounts > roi-1-r62-f70.txt",

                        Exec e = new Exec(cmd, wd);
                        List<String> jobOutput = e.run();

                        fullOutput.addAll(jobOutput);

                        Map<Integer, Integer> f = filter(ssa, roi, sis, jobOutput);

                        String ds = String.format("%s-roi-%s-%s", ssa, roi, getSpecialIncome2(sis));
                        dsNames.add(ds);

                        for (Integer year : f.keySet()) {
                            if ( ! assets.containsKey(year)) {
                                assets.put(year, new ArrayList<Pair<String, Integer>>());
                                years.add(year);
                            }

                            List<Pair<String, Integer>> a = assets.get(year);
                            a.add(new Pair<String, Integer>(ds, f.get(year)));
                        }
                    }
                }
            }

            scenarioOutput = new PrintWriter(new FileWriter("run.txt"));

            // output column headers
            for(String s : dsNames)
                scenarioOutput.print(String.format("%20s", s));
            scenarioOutput.println();

            // output table
            for (int y : years) {
                scenarioOutput.print(String.format("%4d ", y));

                for (Pair<String, Integer> p : assets.get(y)) {
                    scenarioOutput.print(String.format("%20d", p.getValue()));
                }
                scenarioOutput.println();
            }

//            for (String key : assets.keySet()) {
//                scenarioOutput.println(key);
//                Map<Integer, Integer> x = assets.get(key);
//                for (Integer i : x.keySet()) {
//
//                    scenarioOutput.println(s);
//                    //                        System.out.println(s);
//                }
//            }

            scenarioOutput.println("Simulation Data");
            if (getShowAccounts().length() > 0)
                for (String s : fullOutput)
                    scenarioOutput.println(s);

            scenarioOutput.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Map<Integer, Integer> filter(String ssa, String roi, Boolean sis, List<String> jobOutput) {
        /*
        No special after-tax income
        SSAnalyze-r62-f70.csv
        Noga retires at age 62 in 2019
        Annual investment income rate 0.0400
        Year      Exp   Taxes Tot Out     SSA     MRD    Work  Liquid  Assets
        -1        0       0       0       0       0       0       0  590631
2016    90600   27150  117750       0    7599  101000       0  612146
        2017    90600   27404  118004       0    7615  102000       0  635773
        */

        Map<Integer, Integer> keep = new HashMap<Integer, Integer>();

        // fill whole thing with zero so we don't wind up with
        // missing cells because the simulation stops at some
        // point
        for (int y = 2016; y < 2048; ++y)
            keep.put(y, 0);

        // fill with what we have
        for(String s : jobOutput) {
            if (s.startsWith("20")) {
                int year = Integer.parseInt(s.substring(0,4));
                int assets = Integer.parseInt(s.substring(61, 69).trim());
                keep.put(year, assets);
            }
        }
        return keep;
    }

    private static String getShowAccounts() {
        return "";//-showAccounts";
    }

    private static String getSpecialIncome(Boolean sis) {
        if (sis)
            return "";
        else
            return "-noSpecials";
    }
    private static String getSpecialIncome2(Boolean sis) {
        if (sis)
            return "Si";
        else
            return "noSi";
    }

}