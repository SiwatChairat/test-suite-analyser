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
    ArrayList<String> testResultList = new ArrayList<>();
    ArrayList<String> testCaseList = new ArrayList<>();
    String changeLink;

    public AppCompile(String name, String head) {
        repoName = name;
        // Replacing any whitespace to prevent error causing by extra whitespace.
        repoHead = head.replace(" ", "");
    }

    private boolean checkGradle() {
        File file = new File(currDir + "/" + repoName + "/" + "build.gradle");
        return file.exists();
    }

    /*
    Compile the project by removing any untracked file, stash all the repo and then checkout to the desired head and
    build the project.
    */
    private void compileGradle() {
        File dir = new File(currDir + "/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            cmd.add(new String[]{"git", "clean", "-fd"});
            cmd.add(new String[]{"git", "stash"});
            cmd.add(new String[]{"git", "checkout", repoHead});
            cmd.add(new String[]{"git", "show", "head"});
            cmd.add(new String[]{"./gradlew", "clean", "--continue", "test"});

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

                    // Check test for gradle
                    if ((output.matches("(.*)tests completed(.*)"))) {
                        testResultList.add(output);
                    }
                }

                // Read error to find test results, error and build time
                while ((output = stdError.readLine()) != null) {
                    //System.out.println(output);

                    // Check test for gradle
                    if ((output.matches("(.*)tests completed(.*)"))) {
                        testResultList.add(output);
                    }
                }
                //TODO: find a way to make processbuilder terminate within 10 minutes if the program loops

                // THIS CURRENTLY DOES NOT WORK
                // If the ProcessBuilder has been running for 10 minutes and still not finish, terminate it.
                if (!ssh.waitFor(10, TimeUnit.MINUTES)) {
                    ssh.destroyForcibly();
                    // Make the Thread sleep for 10 seconds to allow ssh to terminate successfully
                    Thread.sleep(10000);
                }
                stdInput.close();
                stdError.close();
            }
        } catch (Exception e) {
            System.exit(0);
        }
        cmd.clear();
    }

    private void compileMaven() {
        File dir = new File(currDir + "/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            cmd.add(new String[]{"git", "clean", "-fd"});
            cmd.add(new String[]{"git", "stash"});
            cmd.add(new String[]{"git", "checkout", repoHead});
            cmd.add(new String[]{"git", "show", "head"});
            cmd.add(new String[]{"./mvnw", "clean", "test", "-fae"});

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
                    // Check test for maven
                    if ((output.matches("(.*)Tests run: [0-9]*, Failures: [0-9]*, Errors: [0-9]*, Skipped: [0-9]*"))) {
                        testResultList.add(output);
                    }
                }

                // Read error to find test results, error and build time
                while ((output = stdError.readLine()) != null) {
                    //System.out.println(output);

                    // Check test for maven
                    if ((output.matches("(.*)Tests run: [0-9]*, Failures: [0-9]*, Errors: [0-9]*, Skipped: [0-9]*"))) {
                        testResultList.add(output);
                    }

                }
                //TODO: find a way to make processbuilder terminate within 10 minutes if the program loops

                // THIS CURRENTLY DOES NOT WORK
                // If the ProcessBuilder has been running for 10 minutes and still not finish, terminate it.
                if (!ssh.waitFor(10, TimeUnit.MINUTES)) {
                    ssh.destroyForcibly();
                    // Make the Thread sleep for 10 seconds to allow ssh to terminate successfully
                    Thread.sleep(10000);
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
    public String getFinalTestResult() {
        String testResult = "No test results found";
        if (checkAllTestResult(testResultList) && testResultList.size() != 0) {
            testResult = testResultList.get(0);
            testResultList.clear();
            return testResult;
        }
        return testResult;
    }

    public ArrayList<String> getTestCaseList() {
        return testCaseList;
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
    public void modifyGradle() throws IOException {
        File file = new File(currDir + "/" + repoName + "/build.gradle");
        boolean hasTestLogging = false;
        if (file.exists()) {
            StringBuilder oldContent = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                if (line.replaceAll(" ", "").compareTo("test{") == 0) {
                    oldContent.append(line).append(System.lineSeparator());
                    int bracketCounter = 0;
                    bracketCounter++;
                    while (bracketCounter != 0) {
                        line = reader.readLine();
                        oldContent.append(line).append(System.lineSeparator());
                        if (line.contains("testLogging")) {
                            hasTestLogging = true;
                            line = reader.readLine();
                            oldContent.append(line).append(System.lineSeparator());
                            oldContent.append("\t events \"PASSED\", \"STARTED\", \"FAILED\", \"SKIPPED\"").
                                    append(System.lineSeparator());
                        }
                        if (line.contains("{")) {
                            bracketCounter++;
                        } else if (line.contains("}")) {
                            bracketCounter--;
                        }
                    }
                    if (!hasTestLogging) {
                        oldContent.setLength(oldContent.length() - 2);
                        oldContent.append("testLogging {").append(System.lineSeparator());
                        oldContent.append("\t events \"PASSED\", \"STARTED\", \"FAILED\", \"SKIPPED\"").
                                append(System.lineSeparator());
                        oldContent.append("}").append(System.lineSeparator());
                        oldContent.append("}").append(System.lineSeparator());
                    }
                } else {
                    oldContent.append(line).append(System.lineSeparator());
                }
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
    Compile the project and return test results
     */
    public String buildProject() {
        System.out.println("build project");
        if (checkGradle()) {
            System.out.println("running GRADLE");
            compileGradle();
        } else {
            System.out.println("running MAVEN");
            compileMaven();
        }
        System.out.println(getTestCaseList());
        System.out.println(getTestCaseList().size());
        return getFinalTestResult();
    }
}

//grep testcase
// grep 'failure message' target/surefire-reports/*
// This message should find all the list of test failtures, then i have to use this message and store in a string and compare it with the next one
// both maven and gradle produce the same test report, but there is a time function that i need to remove
// remove the time function because not tall th