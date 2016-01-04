package com.mike.money;

import com.mike.batch.Exec;
import javafx.util.Pair;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/3/2016.
 */
public class Money {

    public void run() {
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

            if (getShowAccounts().length() > 0) {
                scenarioOutput.println();
                scenarioOutput.println("--------- Simulation Data ----------");
                scenarioOutput.println();

                for (String s : fullOutput)
                    scenarioOutput.println(s);
            }

            scenarioOutput.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * remove all the extra stuff and create a map of years to the total
     * asset column.  dump all the rest
     *
     * @param ssa       social security scenario
     * @param roi       rate of return
     * @param sis       special income flag
     * @param jobOutput raw output from simulation process
     *
     * @return          assets for each year
     */
    private  Map<Integer, Integer> filter(String ssa, String roi, Boolean sis, List<String> jobOutput) {
        /*
No special after-tax income
SSAnalyze-r62-f70.csv
Noga retires at age 62 in 2019
Annual investment income rate 0.0400
Year      Exp   Taxes Tot Out     SSA     MRD    Work  Liquid  Assets
-1          0       0       0       0       0       0       0  590631
2016    90600   27150  117750       0    7599  101000       0  612146
2017    90600   27404  118004       0    7615  102000       0  635773
...
may end early with an exception if we go broke
        */

        int colWidth = 8;   // except for the year which is 4 (5 really)
        int assetCol = 7;

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
                int i = 4 + 1 + (colWidth * assetCol);
                int assets = Integer.parseInt(s.substring(i, i + 8).trim());
                keep.put(year, assets);
            }
        }
        return keep;
    }

    private  String getShowAccounts() {
        return "";//-showAccounts";
    }

    private  String getSpecialIncome(Boolean sis) {
        if (sis)
            return "";
        else
            return "-noSpecials";
    }
    private  String getSpecialIncome2(Boolean sis) {
        if (sis)
            return "Si";
        else
            return "noSi";
    }

}
