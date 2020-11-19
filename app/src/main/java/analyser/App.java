package analyser;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * @author Siwat
 */

// NEED TO SET UP ANY SDK BEFORE RUNNING ELSE THE COMMAND WONT WORK
public class App {
    static boolean exit = false;

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

    private static String askInput() {
        Scanner in = new Scanner(System.in);
        String input = "";
        while (input.compareTo("") == 0) {
            input = in.nextLine();
        }
        return input;
    }

    public static void run() throws IOException {
        String repoName = "";
        System.out.println("1. Existing repo");
        System.out.println("2. New repo");
        String ans = askInput();
        if (ans.compareTo("1") == 0){
            System.out.println("Please enter repo name");
            repoName = askInput();
        } else if (ans.compareTo("2") == 0) {
            AppRepo app = new AppRepo();
            ArrayList<String> list = app.getRepo();
            String repoLink = list.get(0);
            repoName = list.get(1);
            if (app.checkRepo(repoName)) {
                System.out.println("Repo already exists");
            } else {
                System.out.println("Let's download the repo");
                app.downloadRepo(repoLink);
            }
        } else if (ans.toLowerCase().compareTo("q") == 0){
            exit = true;
        }

        System.out.println("Do you like to measure compile time? [y/n]");
        String a = askInput();
        if (a.toLowerCase().compareTo("y") == 0) {
            System.out.println("Enter head of the wanted commit: ");
            String head = askInput();
            System.out.println("How many repetitions you would like to run? [1-5]");
            String rep = askInput();

            AppCompile c = new AppCompile(repoName, head, Integer.parseInt(rep));
            String compileTime = c.getCompileTime();
            String testResult = c.getTestResult();
            String changeLink = c.getChangeLink();
            ArrayList<String> timeResult = c.getTimeList();
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            System.out.println("\nComputed on: " + dtf.format(now));
            //deviceBasicSpecs();
            System.out.println("Commit HEAD at: " + head);
            System.out.println("Commit comment: " + c.getCommitComment() + "\n");
            System.out.println(testResult);
            System.out.println(timeResult);
            System.out.println("The average compile time from " + rep + " repetition(s) is " + compileTime + "\n");
            System.out.println("Changes at: " + changeLink);
        } else if (a.toLowerCase().compareTo("q") == 0) {
            exit = true;
        }


//        System.out.println("Do you like to know most frequently change files? [y/n]");
//        String b = askInput()
//        System.out.println("Please enter start date (e.g. 01/01/2000) or press enter");
//        String start = askInput()
//        System.out.println("Please enter end date (e.g. 01/01/2000) or press enter");
//        String end = askInput()
//        if (b.toLowerCase().compareTo("y") == 0) {
//            AppCommit a1 = new AppCommit("lottie-android", 3 ,start, end);
//            a1.frequentlyChangedFile();
//        } else if (b.toLowerCase().compareTo("q") == 0){
//            exit = true;
//        }

    }

    public static void main(String[] args) throws IOException {
        run();
        // "rm -rf" to remove the repo after extracting information

    }
}
