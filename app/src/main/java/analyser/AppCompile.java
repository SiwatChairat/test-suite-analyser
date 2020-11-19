package analyser;

import java.io.*;
import java.util.*;


/**
 * @author Siwat
 */

/*
Make sure your local SDK is linked to the project
 */

public class AppCompile {
    String repoName;
    String repoHead;
    int noOfRep;

    public AppCompile(String name, String head, int rep) {
        repoName = name;
        repoHead = head.replace(" ", "");
        noOfRep = rep;
    }

    String currDir = System.getProperty("user.dir");
    ArrayList<String> testList = new ArrayList<>();
    ArrayList<String> timeList = new ArrayList<>();
    String commitComment;
    String changeLink;

    private void calcCompileTime() {
        File dir = new File(currDir + "/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        String currentLine = "";
        for (int j = 1; j <= noOfRep; j++){
            try {
                builder.directory(dir);
                cmd.add(new String[]{"git", "clean", "-fd"});
                cmd.add(new String[]{"git", "checkout", repoHead});
                modifyGradle();
                cmd.add(new String[]{"./gradlew", "clean", "build", "--continue", "test"});

                System.out.println("computing ... " + "(" + j + ")");

                for (int i = 0; i < cmd.size(); i++) {
                    builder.command(cmd.get(i));
                    Process ssh = builder.start();
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(ssh.getErrorStream()));
                    String output;
                    stdInput.close();

                    while ((output = stdError.readLine()) != null) {
                        if (i < 2) {
                            if (output.contains("HEAD is now at")) {
                                commitComment = output;
                            }
                            System.out.println(output);
                        }
                        if ((output.matches("(.*)tests completed(.*)"))) {
                            testList.add(output);
                        }
                        currentLine = output;
                    }
                    stdError.close();
                    ssh.waitFor();
                }
            } catch (Exception e) {
                // The program will never reach this point
                System.exit(0);
            }
            timeList.add(currentLine);
            cmd.clear();
        }
    }

    private double extractTime(String timeS) {
        double time = 0.0;
        int index1 = timeS.indexOf("n ") + 1;
        int index2 = timeS.length();
        String temp = timeS.substring(index1, index2);
        List<String> list = new ArrayList<>(Arrays.asList(temp.split(" ")));
        for (String t : list) {
            int index3;
            // minutes
            if (t.matches("[0-9]*m")) {
                index3 = t.indexOf("m");
                String temp1 = t.substring(0, index3);
                time += Double.parseDouble(temp1) * 60000;
            }
            // seconds
            else if (t.matches("[0-9]*s")) {
                index3 = t.indexOf("s");
                String temp1 = t.substring(0, index3);
                time += Double.parseDouble(temp1) * 1000;
            }
            // milli seconds
            else if (t.matches("[0-9]*ms")) {
                index3 = t.indexOf("ms");
                String temp1 = t.substring(0, index3);
                time += Double.parseDouble(temp1);
            }
        }
        return time;
    }

    // Check that each build has the same test results.
    private boolean checkAllTestResult(ArrayList<String> list) {
        for (String s : list) {
            if (!s.equals(list.get(0))) {
                return false;
            }
        }
        return true;
    }

    public String getTestResult() {
        String testResult = "The test results are not the same";
        if (checkAllTestResult(testList) && testList.size() != 0) {
            testResult = testList.get(0);
            testList.clear();
            return testResult;
        }
        return testResult;
    }

    public ArrayList<String> getTimeList() {
        return timeList;
    }

    public String getCommitComment() {
        return commitComment.substring(24);
    }

    public String getChangeLink() throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        String[] cmd = {"git", "config", "--get", "remote.origin.url"};
        File dir = new File(currDir + "/" + repoName);
        builder.command(cmd);
        builder.directory(dir);
        Process ssh = builder.start();
        String output;
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
        while ((output = stdInput.readLine()) != null) {
            changeLink = output + "/" + repoHead;
        }
        return changeLink;
    }

    private void modifyGradle() throws IOException {
        File fileToBeModified = new File(currDir + "/" + repoName + "/build.gradle");
        String oldContent = "";
        BufferedReader reader = new BufferedReader(new FileReader(fileToBeModified));
        String line = reader.readLine();
        while (line != null)
        {
            oldContent = oldContent + line + System.lineSeparator();
            line = reader.readLine();
        }
        String newContent = oldContent.replaceAll("git = Grgit.open(.*)", "git = Grgit.open(currentDir: project.rootDir)");
        FileWriter writer = new FileWriter(fileToBeModified);
        writer.write(newContent);
        reader.close();
        writer.close();
    }

    public String getCompileTime() throws IOException {
        calcCompileTime();
        String time;
        double totalTime = 0.0;
        double averageTime;
        for (String t0 : timeList) {
            totalTime += extractTime(t0);
        }
        // convert millisecond to second and average it
        averageTime = ((totalTime / 1000) / timeList.size());
        time = String.format("%.2f" , averageTime) + " second(s)";
        return time;
    }
}
