package analyser;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Siwat
 */

/*
 Programming running with JDK 13

 Before running to program please make sure that the git project is runnable using ./gradlew clean build --continue test
 */
public class App {

    /*
    Recursively keep asking for input, a blank line is not allowed
     */
    private static String askInput() {
        Scanner in = new Scanner(System.in);
        String input = "";
        while (input.compareTo("") == 0) {
            input = in.nextLine();
        }
        return input;
    }

    public static void run() throws IOException, InterruptedException {
        System.out.println("Please enter repo name: ");
        String repoName = askInput();
        System.out.println("Please enter test path from repo root: ");
        String testPath = askInput();
        System.out.println("Please enter number of test interval: ");
        int num = Integer.parseInt(askInput());
        System.out.println("Please enter report name: ");
        String reportName = askInput();
        AppCreateReport appCreateReport = new AppCreateReport(reportName + " report", testPath, repoName, num);
        appCreateReport.writeToCsv();
    }

    public static void testRun() {
        String currDir = System.getProperty("user.dir");
        File dir = new File(currDir + "/" + "okhttp");
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            cmd.add(new String[]{"git", "checkout", "38e728b0af5414e384f535ce905cf8573ec9a5c6"});
            cmd.add(new String[]{"./gradlew", "clean", "--continue", "test"});
            for (String[] s : cmd) {
                builder.command(s);
                Process ssh = builder.start();
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(ssh.getErrorStream()));
                String output;

                // Read input to find test results as well as build time
                while ((output = stdInput.readLine()) != null) {
                    System.out.println(output);
                }

                // Read error to find test results, error and build time
                while ((output = stdError.readLine()) != null) {
                    System.out.println(output);
                    //TODO: find a way to check every single test result from each build to improve accuracy

                }

            }
        } catch (Exception e) {
            System.exit(0);
        }
    }

    public static void testRun2() {
        AppCompile appCompile = new AppCompile("dubbo", "");
        appCompile.buildProject();
    }

    public static void testRun3() throws IOException {
        AppCompile appCompile = new AppCompile("okhttp", "");
        appCompile.modifyGradle();
    }

    public static void testRun4() {
        String currDir = System.getProperty("user.dir");
        File dir = new File(currDir + "/gitProjects" );
        ProcessBuilder builder = new ProcessBuilder();
        ArrayList<String> commands = new ArrayList<>();
        try {
            builder.directory(dir);
            commands.add("/bin/sh");
            commands.add("-c");
            commands.add("grep -r --include '*xml' '<testsuite' okhttp");
            builder.command(commands);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            while ((output= stdInput.readLine()) != null) {
                //Print to see the results
                // nothing is print, taking the time taken in terminal, which is 1 second. I assume that this does not work.
                String temp = output.replaceAll("timestamp=(.*)\"", "");
                System.out.println(temp);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static boolean testRun5() {
            String currDir = System.getProperty("user.dir");
            File file = new File(currDir + "/gitProjects/" + "retrofit" + "/" + "build.gradle");
            return file.exists();
    }




    public static void main(String[] args) throws IOException, InterruptedException {
        run();

    }
}
