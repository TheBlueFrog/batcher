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

    private List<Pair<Integer, Pair<String, Double>>> specialIncomeConstants;

    public void run() {
        String wd = "c:\\Users\\mike\\src\\money";
        String cp = "-cp out\\production\\money com.mike.money.Main";

        List<String> ssaScenarios = new ArrayList<String>();
        ssaScenarios.add("r62");
        ssaScenarios.add("r65");

        List<String> roiScenarios = new ArrayList<String>();
        roiScenarios.add("1");
        roiScenarios.add("2");
        roiScenarios.add("3");
        roiScenarios.add("4");

        // special income of different scenarios to run
        List<Pair<Integer, Pair<String, Double>>> SpecialIncomeScenarios = new ArrayList<Pair<Integer, Pair<String, Double>>>();
        SpecialIncomeScenarios.add(new Pair<Integer, Pair<String, Double>>(2018, new Pair("\"Trading Ameri\"", 300000.0)));
        SpecialIncomeScenarios.add(new Pair<Integer, Pair<String, Double>>(2018, new Pair("\"Trading Ameri\"", 150000.0)));

        // special income that happens in all scenarios
        specialIncomeConstants = new ArrayList<Pair<Integer, Pair<String, Double>>>();
        // last 20K of Dad's estate released
        specialIncomeConstants.add(new Pair<Integer, Pair<String, Double>>(2017, new Pair("\"College Wells\"", (3 * (20000.0 / 7)))));

        List<String> fullOutput = new ArrayList<String>();

        // for the years collections of maps of year to assets
        Map<Integer, List<Pair<String, Integer>>> assets = new HashMap<Integer, List<Pair<String, Integer>>>();
        List<String> dsNames = new ArrayList<String>();
        List<Integer> years = new ArrayList<Integer>();

        PrintWriter scenarioOutput = null;
        try {
            for (String ssa : ssaScenarios) {
                for (String roi : roiScenarios) {
                    for (Pair<Integer, Pair<String, Double>> sis : SpecialIncomeScenarios) {
                        String cmd = String.format("java %s -loadSSA SSAnalyze-%s.csv -roi 0.0%s %s %s",
                                cp, ssa, roi, getSpecialIncome(sis), getShowAccounts());

                        Exec e = new Exec(cmd, wd);
                        List<String> jobOutput = e.run();

                        fullOutput.addAll(jobOutput);

                        Map<Integer, Integer> f = filter(jobOutput);

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

            scenarioOutput.print("     ");

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
     * @param jobOutput raw output from simulation process
     *
     * @return          assets for each year
     */
    private  Map<Integer, Integer> filter(List<String> jobOutput) {
        /*
No special after-tax income
SSAnalyze-r62-f70.csv
Noga retires at age 62 in 2019
Annual investment income rate 0.0400
Year      Exp   Taxes Tot Out     SSA     MRD    Work    Gain  Liquid  Assets  Wells F  College  Trading  eBay st  eBay 40 Securion     TIAA  IRA M A  Roth M   Intel s  Intel 4  IRA N AIRA N UBS
  -1        0       0       0       0       0       0       0       0  590631       40      144       52       43       73      135       66        6       16        2        4        2        8
2016    90600   30951  121551       0    7599  101000   17714       0  608345       27      150       54       45       76      135       66        6       16        6        8        7       12
...
may end early with an exception if we go broke
        */

        int colWidth = 8;   // except for the year which is 4 (5 really)
        int assetCol = 8;

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
        return "";
//        return " -showAccounts";
    }

    private  String getSpecialIncome(Pair<Integer, Pair<String, Double>> sis) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(" -specialInc %d %s %f", sis.getKey(), sis.getValue().getKey(), sis.getValue().getValue()));

        for (Pair<Integer, Pair<String, Double>> s : specialIncomeConstants)
            sb.append(String.format(" -specialInc %d %s %f", s.getKey(), s.getValue().getKey(), s.getValue().getValue()));

        return sb.toString();
    }

    private  String getSpecialIncome2(Pair<Integer, Pair<String, Double>> sis) {
        return String.format("si-%.1fk", sis.getValue().getValue() / 100000.0);
    }

}
