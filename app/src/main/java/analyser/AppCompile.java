package analyser;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @author Siwat
 */

/*
Make sure your local SDK is linked to the project
 */

public class AppCompile {
    String repoName;
    String repoHead;
    String currDir = System.getProperty("user.dir");
    ArrayList<String> testList = new ArrayList<>();
    ArrayList<String> timeList = new ArrayList<>();
    String changeLink;

    public AppCompile(String name, String head) {
        repoName = name;
        // Replacing any whitespace to prevent error causing by extra whitespace.
        repoHead = head.replace(" ", "");
    }

    /*
    Compile the project by removing any untracked file, stash all the repo and then checkout to the desired head and
    build the project.
     */
    private void compile() {
        File dir = new File(currDir + "/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            cmd.add(new String[]{"git", "clean", "-fd"});
            cmd.add(new String[]{"git", "stash"});
            cmd.add(new String[]{"git", "checkout", repoHead});
            cmd.add(new String[]{"git", "show", "head"});
            cmd.add(new String[]{"./gradlew", "clean", "build", "--continue", "test"});

            // Run each command in the ArrayList<String[]> cmd
            for (String[] strings : cmd) {
                builder.command(strings);
                Process ssh = builder.start();
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(ssh.getErrorStream()));
                String output;

                // When the ProcessBuilder reaches "git show head"  modify "build.gradle" file to prevent git repository
                // not found error. This is because sometimes, the root of the git project is not specified by
                // the developers.
                if (Arrays.toString(strings).contains("show")) {
                    modifyGradle();
                }

                // Read input to find test results as well as build time
                while ((output = stdInput.readLine()) != null) {
                    //System.out.println(output);
                    if ((output.matches("(.*)tests completed(.*)"))) {
                        testList.add(output);
                    }
                    if ((output.matches("BUILD(.*)in(.*)"))) {
                        timeList.add(output);
                    }
                }

                // Read error to find test results, error and build time
                while ((output = stdError.readLine()) != null) {
                    //System.out.println(output);
                    //TODO: find a way to check every single test result from each build to improve accuracy
                    if ((output.matches("(.*)tests completed(.*)"))) {
                        testList.add(output);
                    }
                    if (output.matches("(.*)What went wrong:(.*)")) {
                        while (!(output = stdError.readLine()).equals("")) {
                            System.out.println(output);
                        }
                    }
                    if ((output.matches("BUILD(.*)in(.*)"))) {
                        timeList.add(output);
                    }
                }
                //TODO: find a way to make processbuilder terminate

                // THIS CURRENTLY DOES NOT WORK
                // If the ProcessBuilder has been running for 10 minutes and still not finish, terminate it.
                if (!ssh.waitFor(10, TimeUnit.MINUTES)) {
                    ssh.destroyForcibly();
                    ssh.waitFor();
                }
                stdInput.close();
                stdError.close();
            }
        } catch (Exception e) {
            System.exit(0);
        }
        cmd.clear();
    }

    /*
    Convert build time from string to double.
     */
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

    /*
    Check whether the test results generated from the same head are the same or not.
     */
    private boolean checkAllTestResult(ArrayList<String> list) {
        for (String s : list) {
            if (!s.equals(list.get(0))) {
                return false;
            }
        }
        return true;
    }

    /*
    Return test result.
     */
    public String getTestResult() {
        String testResult = "No test results found";
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

    /*
    Run "git config --get remote.origin.url" to get repo link and then add the head to create a link, which navigates to
    the changes on that commit.
     */
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
            String temp = output.substring(0, output.length() - 4);
            changeLink = temp + "/commit/" + repoHead;
        }
        return changeLink;
    }

    /*
    Modify build.gradle, if the developers forgot to put repo root in the file
     */
    private void modifyGradle() throws IOException {
        File file = new File(currDir + "/" + repoName + "/build.gradle");
        if (file.exists()) {
            StringBuilder oldContent = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                oldContent.append(line).append(System.lineSeparator());
                line = reader.readLine();
            }
            String newContent = oldContent.toString().replaceAll("git = Grgit.open(.*)",
                    "git = Grgit.open(currentDir: project.rootDir)");
            FileWriter writer = new FileWriter(file);
            writer.write(newContent);
            writer.flush();
            reader.close();
            writer.close();
        }

    }

    /*
    Get averaged compile time
     */
    public String getAveragedCompileTime() {
        compile();
        String time;
        double totalTime = 0.0;
        double averageTime;
        for (String t0 : timeList) {
            totalTime += extractTime(t0);
        }
        // convert millisecond to second and average it
        averageTime = ((totalTime / 1000) / timeList.size());
        time = String.format("%.2f", averageTime) + " second(s)";
        return time;
    }

    /*
    Compile the project and return test results
     */
    public String buildProject() {
        compile();
        return getTestResult();
    }
}
