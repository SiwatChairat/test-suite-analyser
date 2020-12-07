package analyser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Siwat
 */
@SuppressWarnings("ALL")
// THIS IS CURRENTLY NOT IN-USE
public class AppRepo {

    /*
    Ask for github repo link and from the link get the name of the repo
     */
    public ArrayList<String> getRepo() throws IOException {
        ArrayList<String> list = new ArrayList<>();
        System.out.println("Enter GitHub Repo HTTPS: ");
        Scanner in = new Scanner(System.in);
        String repoLink = in.nextLine();
        list.add(repoLink);

        String temp0 = repoLink.replace("https://github.com/", "");
        String temp1 = temp0.replace(".git", "");
        String repoName = temp1.substring(temp1.indexOf("/") + 1);
        list.add(repoName);

        // index 0: repoLink, index 1: repoName
        return list;
    }

    /*
    Check whether the specified already exist or not.
     */
    public boolean checkRepo(String repoName) {
        String currDir = System.getProperty("user.dir");
        String finalPath = currDir + "/" + repoName;
        File dir = new File(currDir + "/" + repoName);

        try {
            //change the builder according to device's shell
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("pwd");
            builder.directory(dir);
            Process ssh = builder.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
            String path = null;
            while ((path = stdInput.readLine()) != null) {
                System.out.println(path);
                if (path.compareTo(finalPath) == 0) {
                    return true;
                }
            }
            stdInput.close();
        } catch (Exception e) {
            // The file does not exist
            return false;
        }
        return false;
    }

    /*
    Download the specified repo link, as well as asking permission from the user first
     */
    public void downloadRepo(String repoLink) throws IOException {
        Scanner in = new Scanner(System.in);
        File dir = new File(System.getProperty("user.dir"));

        // Ask user for permission
        System.out.println("This repo will be cloned to your local device [y/n]");
        String ans = in.nextLine();

        // If user does not allow to download the Git Project, then terminate the program
        if ((ans.toLowerCase()).compareTo("n") == 0) {
            System.out.println("terminating ...");
            System.exit(0);
        }

        ProcessBuilder builder = new ProcessBuilder();
        String[] cmd = {"git", "clone", repoLink};
        builder.command(cmd);
        builder.directory(dir);
        Process ssh = builder.start();
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(ssh.getErrorStream()));

        System.out.println("running ...");
        String output = null;
        while ((output = stdInput.readLine()) != null) {
            System.out.println(output);
        }
        stdInput.close();
        while ((output = stdError.readLine()) != null) {
            System.out.println(output);
        }
        stdError.close();
    }
}
