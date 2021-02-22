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
    ArrayList<String> testCaseList = new ArrayList<>();
    String changeLink;
    String currDir = System.getProperty("user.dir").replaceAll("app", "");
    
    public AppCompile(String name, String head) {
        repoName = name;
        // Replacing any whitespace to prevent error causing by extra whitespace.
        repoHead = head.replace(" ", "");
    }

    private boolean checkGradle() {
        File file = new File(currDir + "gitProjects/" + repoName + "/" + "build.gradle");
        return file.exists();
    }

    /*
    Compile the project by removing any untracked file, stash all the repo and then checkout to the desired head and
    build the project.
    */
    private void compileGradle() {
        File dir = new File(currDir + "gitProjects/" + repoName);
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
                if (Arrays.toString(strings).contains("show")) {
                    modifyGradle();
                }
                // If the ProcessBuilder has been running for 10 minutes and still not finish, terminate it.
                if (!ssh.waitFor(10, TimeUnit.MINUTES)) {
                    ssh.destroyForcibly();
                    // Make the Thread sleep for 1 seconds to allow ssh to terminate successfully
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            System.exit(0);
        }
        cmd.clear();
    }

    private void compileMaven() {
        File dir = new File(currDir + "gitProjects/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            cmd.add(new String[]{"git", "clean", "-fd"});
            cmd.add(new String[]{"git", "stash"});
            cmd.add(new String[]{"git", "checkout", repoHead});
            cmd.add(new String[]{"git", "show", "head"});
            cmd.add(new String[]{"mvn", "clean", "-fn", "test"});

            // Run each command in the ArrayList<String[]> cmd
            for (String[] strings : cmd) {
                builder.command(strings);
                Process ssh = builder.start();
                // If the ProcessBuilder has been running for 10 minutes and still not finish, terminate it.
                if (!ssh.waitFor(10, TimeUnit.MINUTES)) {
                    ssh.destroyForcibly();
                    // Make the Thread sleep for 10 seconds to allow ssh to terminate successfully
                    Thread.sleep(10000);
                }
            }
        } catch (Exception e) {
            System.exit(0);
        }
        cmd.clear();
    }

    public int extractNum (String a, String b) {
        int indexA = a.indexOf(b);
        String s1 = a.substring(indexA);
        int indexS1 = s1.indexOf("\"");
        int indexS2 = s1.indexOf("\"", indexS1 + 1);
        String s2 = s1.substring(indexS1 + 1, indexS2);
        int num = Integer.parseInt(s2);
        return num;
    }

    /*
    Return test result.
     */
    public String getFinalTestResult() {
        String testResult = "";
        File dir = new File(currDir + "/gitProjects" );
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String> commands = new ArrayList<>();
        int testCompleted = 0;
        int testSkipped = 0;
        int testFailures = 0;
        int testErrors = 0;
        try {
            builder.directory(dir);
            commands.add("/bin/sh");
            commands.add("-c");
            commands.add("grep -r --include '*xml' '<testsuite' " + repoName);
            builder.command(commands);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            while ((output= stdInput.readLine()) != null) {
                String temp = output.replaceAll("timestamp=(.*)\"", "");
                testCompleted += extractNum(temp, "tests=");
                testSkipped += extractNum(temp, "skipped=");
                testFailures += extractNum(temp, "failures=");
                testErrors += extractNum(temp, "errors=");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        testResult = "completed: " + testCompleted + " skipped: " + testSkipped + " failures: " + testFailures + " errors: " + testErrors;
        return testResult;
    }

    public ArrayList<String> getTestCaseList() {
        File dir = new File(currDir + "/gitProjects" );
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String> commands = new ArrayList<>();
        try {
            builder.directory(dir);
            commands.add("/bin/sh");
            commands.add("-c");
            commands.add("grep -r --include '*xml' '<testcase' " + repoName);
            builder.command(commands);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            while ((output= stdInput.readLine()) != null) {
                String temp = output.replaceAll("time=(.*)\"", "");
                testCaseList.add(temp);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return testCaseList;
    }

    /*
    Run "git config --get remote.origin.url" to get repo link and then add the head to create a link, which navigates to
    the changes on that commit.
     */
    public String getChangeLink() throws IOException {
        ProcessBuilder builder = new ProcessBuilder();
        String[] cmd = {"git", "config", "--get", "remote.origin.url"};
        File dir = new File(currDir + "gitProjects/" + repoName);
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
        File file = new File(currDir + "gitProjects/" + repoName + "/build.gradle");
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
        return getFinalTestResult();
    }
}
