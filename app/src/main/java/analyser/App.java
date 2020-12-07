package analyser;

import java.io.*;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public static void run() throws IOException, InterruptedException{
        System.out.println("Please enter repo name: ");
        String repoName = askInput();
        System.out.println("Please enter test path(from project repo): ");
        String testPath = askInput();
        System.out.println("Please enter number of test interval: ");
        int num = Integer.parseInt(askInput());
        AppCreateReport appCreateReport = new AppCreateReport(repoName + " report", testPath, repoName, num);
        appCreateReport.writeToCsv();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        run();
    }
}
