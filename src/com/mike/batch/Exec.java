package com.mike.batch; /**
 * Created by mike on 1/2/2016.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Exec {
    private final String mCmd;
    private final String mWorkingDir;

    public Exec(String cmd, String workingDir) {
        mCmd = cmd;
        mWorkingDir = workingDir;
    }

    public List<String> run () {
        List<String> output = new ArrayList<String>();
        try {
            File wd = new File(mWorkingDir);
            Process p = Runtime.getRuntime().exec(mCmd, null, wd);
            BufferedReader bri = new BufferedReader (new InputStreamReader(p.getInputStream()));
            BufferedReader bre = new BufferedReader (new InputStreamReader(p.getErrorStream()));

            String line;
            while ((line = bri.readLine()) != null) {
//                System.out.println(line);
                output.add(line);
            }
            bri.close();
            while ((line = bre.readLine()) != null) {
//                System.out.println(line);
                output.add(line);
            }
            bre.close();
            p.waitFor();
//            System.out.println("Done.");
        }
        catch (Exception err) {
            //err.printStackTrace();
            output.add(err.toString());
        }

        return output;
    }
}
