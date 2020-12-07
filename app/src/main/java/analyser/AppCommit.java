package analyser;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    String currDir = System.getProperty("user.dir");

    public AppCommit(String name, int fileNum, String start, String end) {
        repoName = name;
        noOfFiles = fileNum;
        startDate = start;
        endDate = end;
    }

    private void checkoutMaster() throws IOException, InterruptedException {
        File dir = new File(currDir + "/" + repoName);
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String[]> cmd = new ArrayList<>();
        cmd.add(new String[]{"git", "clean", "-fd"});
        cmd.add(new String[]{"git", "stash"});
        cmd.add(new String[]{"git", "master"});
        builder.directory(dir);
        for (String[] strings : cmd) {
            builder.command(strings);
            Process ssh = builder.start();
            ssh.waitFor();
        }
    }

    /*
    Change the string to an empty string if the input is start or current
     */
    private void checkDate(String start, String end) {
        if (start.toLowerCase().compareTo("start") == 0) {
            startDate = "";
        }

        if (end.toLowerCase().compareTo("current") == 0) {
            endDate = "";
        }
    }

    private String removeDayNTime(String date) {
        String temp = date.replace("Date: ", "");
        String temp1 = temp.replaceAll(" [0-9]*:(.*):[0-9]* ", " ");
        String temp2 = temp1.replaceAll("  [A-Z][a-z][a-z] ", "");
        String temp3 = temp2.replaceAll(" [+][0-9]*", "");
        return temp3.replaceAll(" [-][0-9]*", "");
    }

    /*
    Remove all the duplicates from the arraylist
     */
    private ArrayList<String> removeDuplicate(ArrayList<String> list) {
        ArrayList<String> newList = new ArrayList<>();
        for (String date : list) {
            if (!newList.contains(date)) {
                newList.add(date);
            }
        }
        return newList;
    }

    /*
    Calculate the number of days between 2 specified dates.
     */
    private long calcDay(String before, String after) {
        String d1 = formatLocalDate(before, "d/MM/yyyy");
        String d2 = formatLocalDate(after, "d/MM/yyyy");
        LocalDate dateBefore = LocalDate.parse(d1);
        LocalDate dateAfter = LocalDate.parse(d2);
        return ChronoUnit.DAYS.between(dateBefore, dateAfter);
    }

    private String formatLocalDate(String date, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        LocalDate d = LocalDate.parse(date, formatter);
        return d.toString();
    }

    /*
    Format pattern of Date from current pattern to the wanted pattern
     */
    private String formatDate(String date, String currentPattern, String finalPattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(currentPattern, Locale.ENGLISH);
        LocalDate d = LocalDate.parse(date, formatter);
        return d.format(DateTimeFormatter.ofPattern(finalPattern));
    }

    /*
    Get frequently changed files list, where the number of files can be specified. Eg. top 100 frequently changed files
     */
    private ArrayList<String> getFrequentList() throws IOException, InterruptedException {
        checkDate(startDate, endDate);
        checkoutMaster();
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
            if (list.size() != 0) {
                list.remove(0);
            }
        }
        // remove null value from the list
        while (list.remove(null)) ;

        return list;
    }

    /*
    Get date, commit head and commit comment from a specified path
     */
    public String computeLog(String path) throws IOException, InterruptedException {
        // Make sure the repo is at the latest head
        checkoutMaster();
        File dir = new File(currDir + "/" + repoName);
        ProcessBuilder builder = new ProcessBuilder();
        String[] cmd;
        if (path.compareTo("") == 0) {
            cmd = new String[]{"git", "log"};
        } else {
            cmd = new String[]{"git", "log", "-p", path};
        }
        builder.command(cmd);
        builder.directory(dir);
        Process ssh = builder.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
        String output;
        StringBuilder log = new StringBuilder();
        while ((output = stdInput.readLine()) != null) {
            if (output.matches("commit(.*)")) {
                log.append(output.replaceAll("commit ", ""));
                log.append("\n");
            }
            if (output.matches("Date:(.*)")) {
                log.append(removeDayNTime(output));
                log.append("\n");
                for (int i = 0; i < 2; i++) {
                    output = stdInput.readLine();
                    if (output.compareTo("") != 0) {
                        log.append(output);
                        log.append("\n\n");
                    }
                }
            }
        }
        return log.toString();
    }

    /*
    Convert string to arraylist, splitting by the input delimiter
     */
    private ArrayList<String> stringToArrayList(String a, String delimiter) {
        String[] elements = a.split(delimiter);
        List<String> tempList = Arrays.asList(elements);
        return new ArrayList<>(tempList);
    }

    /*
    Compute for period of time between 2 commits from a specified folder path. In this case it should be test folder path
     */
    public String intervalBetweenCommits(String pathToTest, int intervalNum, int cond) throws IOException, InterruptedException {
        ArrayList<String> dateList = new ArrayList<>();
        String log = computeLog(pathToTest);
        ArrayList<String> info = stringToArrayList(log, "\n\n");
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        String today = formatter.format(date);
        for (String i : info) {
            ArrayList<String> list = stringToArrayList(i, "\n");
            dateList.add(list.get(1));
        }
        int counter = 0;
        String testInterval = "";
        dateList = removeDuplicate(dateList);
        testInterval += "1" + "\n" +
                            formatDate(dateList.get(0), "MMM d yyyy", "d/MM/yyyy") + "\n" +
                            today+ "\n\n";

        if (intervalNum > dateList.size() - 1) {
            for (int i = counter; i < dateList.size() - 1; i++) {
                String temp1 = dateList.get(i);
                String temp2 = dateList.get(i + 1);
                String to = formatDate(temp1, "MMM d yyyy", "d/MM/yyyy");
                String from = formatDate(temp2, "MMM d yyyy", "d/MM/yyyy");
                int noInterval = counter + 2;
                long day = calcDay(from, to);
                if (day >= cond) {
                    testInterval +=
                            noInterval + "\n" +
                                    from + "\n" +
                                    to + "\n\n";
                    counter++;
                } else {
                    intervalNum++;
                }
            }
        } else {
            for (int i = counter; i < intervalNum - 1; i++) {
                String temp1 = dateList.get(i);
                String temp2 = dateList.get(i + 1);
                String to = formatDate(temp1, "MMM d yyyy", "d/MM/yyyy");
                String from = formatDate(temp2, "MMM d yyyy", "d/MM/yyyy");
                int noInterval = counter + 2;
                long day = calcDay(from, to);
                if (day >= cond) {
                    testInterval +=
                            noInterval + "\n" +
                                    from + "\n" +
                                    to + "\n\n";
                    counter++;
                } else {
                    intervalNum++;
                }
            }
        }
        System.out.println(testInterval);
        return testInterval;
    }

    /*
    Get frequently changed files from the repo
     */
    public void frequentlyChangedFile() throws IOException, InterruptedException {
        ArrayList<String> list = getFrequentList();
        String name;
        StringBuilder fileNames = new StringBuilder();
        int counter = 1;
        for (String t0 : list) {
            // renaming
            if (t0.compareTo("") != 0) {
                int index1 = t0.indexOf("..");
                int index2 = t0.indexOf(". ");
                int index3 = t0.indexOf("   ");
                String t1 = t0.substring(0, index1);
                String t2 = t0.substring(index2 + 1, index3);
                name = "\t" + counter + "." + t1 + " , " + t2 + " commit(s)";
                fileNames.append(name).append("\n");
                counter++;
            }
        }
        System.out.println("Top " + noOfFiles + " frequently changed files:");
        System.out.println(fileNames);
    }

    /*
    Compute a short summary of the repo, such as repo age, repo name, number of files, number of commits
     */
    public void summary() {
        checkDate(startDate, endDate);
        try {
            File dir = new File(currDir + "/" + repoName);
            ProcessBuilder builder = new ProcessBuilder();
            String[] cmd;
            cmd = new String[]{"git", "summary"};
            builder.command(cmd);
            builder.directory(dir);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            for (int i = 0; i < 6; i++) {
                output = stdInput.readLine();
                System.out.println(output);
            }
            stdInput.close();
        } catch (Exception e) {
            // The program will never reach this point
            System.exit(0);
        }
    }
}
