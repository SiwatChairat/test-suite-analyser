package analyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * @author Siwat
 */

/*
Git extras package needed to use this class
Version: 6.1.0
https://github.com/tj/git-extras/blob/master/Installation.md
*/
public class AppCommit {

    String repoName;
    String startDate;
    String endDate;
    int noOfFiles;

    public AppCommit(String name, int fileNum ,String start, String end) {
        repoName = name;
        noOfFiles = fileNum;
        startDate = start;
        endDate = end;
    }

    String currDir = System.getProperty("user.dir");

    public void summary() {
        try {
            File dir = new File(currDir + "/" + repoName);
            ProcessBuilder builder = new ProcessBuilder();
            String[] cmd = {"git", "summary"};
            builder.command(cmd);
            builder.directory(dir);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            for (int i = 0; i < 5; i++) {
                output = stdInput.readLine();
                System.out.println(output);
            }
            stdInput.close();
        } catch (Exception e) {
            // The program will never reach this point
            System.exit(0);
        }
    }

    private ArrayList<String> getList() {
        ArrayList<String> list = new ArrayList<>();
        try {
            File dir = new File(currDir + "/" + repoName);
            ProcessBuilder builder = new ProcessBuilder();
            String[] cmd;
            if (startDate.compareTo("") != 0 && endDate.compareTo("") == 0) {
                cmd = new String[]{"git", "effort", "--", "--after=\"" + startDate + "\""};
            } else if (startDate.compareTo("") == 0 && endDate.compareTo("") != 0) {
                cmd = new String[]{"git", "effort", "--", "--before=\"" + endDate + "\""};
            } else if (startDate.compareTo("") != 0 && endDate.compareTo("") != 0) {
                cmd = new String[]{"git", "effort", "--", "--after=\"" + startDate + "\"" + "--before=\"" + endDate + "\""};
            } else {
                cmd = new String[]{"git", "effort"};
            }

            builder.command(cmd);
            builder.directory(dir);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;

            System.out.println("Computing ...");

            while ((output = stdInput.readLine()) != null) {
                if (output.compareTo("") == 0) {
                    for (int i = 0; i < noOfFiles + 2; i++) {
                        output = stdInput.readLine();
                        list.add(output);
                    }
                }
            }
            stdInput.close();
        } catch (Exception e) {
            // The program will never reach this point
            System.exit(0);
        }

        // remove all the unwanted data
        for (int i = 0; i < noOfFiles + 4; i++) {
            System.out.println(list);
            if (list.size() != 0) {
                list.remove(0);
            }
        }

        // remove null value from the list
        while (list.size() != 0) {
            list.remove(null);
        }

        return list;
    }

    public void frequentlyChangedFile() {
        ArrayList<String> list = getList();
        String name;
        StringBuilder fileNames = new StringBuilder();
        for (String t0 : list) {
            // renaming
            int index1 = t0.indexOf("..");
            int index2 = t0.indexOf(". ");
            int index3 = t0.indexOf("   ");
            String t1 = t0.substring(0, index1);
            String t2 = t0.substring(index2 + 1, index3);
            name = t1 + " , " + t2 + " commit(s)";
            fileNames.append(name).append("\n");
        }
        System.out.println("Top " + noOfFiles + " frequently changed files");
        System.out.println(fileNames);
    }

}
