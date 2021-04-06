package analyser;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Pleases choose your programme: ");
        System.out.println("1. Analyse GitRepo ");
        System.out.println("2. Exit");
        String choice = askInput();
        if (choice.compareTo("1") == 0) {
            run();
        } else if (choice.compareTo("2") == 0) {
            System.exit(0);
        } else {
            System.exit(0);
        }
    }
}


