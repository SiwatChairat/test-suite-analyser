package analyser;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
    Computing for device's spec
     */
    private static void deviceBasicSpecs() {
        String nameOS = "os.name";
        String versionOS = "os.version";
        String architectureOS = "os.arch";
        System.out.println("\nName of the OS: " +
                System.getProperty(nameOS));
        System.out.println("Version of the OS: " +
                System.getProperty(versionOS));
        System.out.println("Architecture of the OS: " +
                System.getProperty(architectureOS) + "\n");
    }

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
        System.out.println("Please enter test path(from project repo): ");
        String testPath = askInput();
        System.out.println("Please enter number of test interval: ");
        int num = Integer.parseInt(askInput());
        AppCreateReport appCreateReport = new AppCreateReport(repoName + " report1", testPath, repoName, num);
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
        // use okhttp because there are test cases in okhttp/test-results/test path
        File dir = new File(currDir + "/okhttp" );
        ProcessBuilder builder = new ProcessBuilder();
        try {
            builder.directory(dir);
            builder.command(new String[]{"find", ".", "'*.xml'", "-print0", "-exec", "grep", "'<failure message='", "{}", "+"});
            //find . -name '*.xml' -print0 -exec grep '<failure message=' {} +
            // the command should print lines in all of .xml files that contain failure message
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String output;
            while ((output= stdInput.readLine()) != null) {
                //Print to see the results
                // nothing is print, taking the time taken in terminal, which is 1 second. I assume that this does not work.
                System.out.println(output);
            }
        } catch (Exception e) {

        }
    }



    public static void main(String[] args) throws IOException, InterruptedException {
        //run();
        testRun4();
    }
}
