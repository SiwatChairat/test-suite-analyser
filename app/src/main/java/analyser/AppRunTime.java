package analyser;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AppRunTime {
    String repoName;
    String path;
    String currDir = System.getProperty("user.dir").replaceAll("app", "");

    public AppRunTime(String name, String pathToFile) {
        repoName = name;
        path = pathToFile;
    }

    ArrayList<String> getHeadList () throws FileNotFoundException {
        ArrayList<String> listOfHead = new ArrayList<>();
        File file = new File(path);
        Scanner sc = new Scanner(file);
        String content;
        while (sc.hasNextLine()) {
            content = sc.nextLine();
            if (!listOfHead.contains(content)) {
                listOfHead.add(content);
            }
        }
        return listOfHead;
    }

    private String calcCompileTime(String head) throws IOException, InterruptedException {
        File dir = new File(currDir + "gitProjects/" + repoName);
        ArrayList<String[]> cmd = new ArrayList<>();
        ProcessBuilder builder = new ProcessBuilder();
        String time = "";
                builder.directory(dir);
                cmd.add(new String[]{"git", "checkout", head});
                cmd.add(new String[]{"./gradlew", "clean", "--continue", "test"});
                for (int i = 0; i < 2; i++) {
                    builder.command(cmd.get(i));
                    Process ssh = builder.start();
                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(ssh.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(ssh.getErrorStream()));
                    String output;
                    while ((output = stdInput.readLine()) != null) {
                        if ((output.matches("BUILD(.*)in(.*)"))) {
                            time = output;
                        }
                    }
                    while ((output = stdError.readLine()) != null) {
                        if ((output.matches("BUILD(.*)in(.*)"))) {
                            time = output;
                        }
                    }
                    ssh.waitFor();
                }
                return time;
            }

    private double extractTime(String t1) {
        double time = 0.0;
        int index1 = t1.indexOf("n ") + 1;
        int index2 = t1.length();
        String temp = t1.substring(index1, index2);
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

    private String getMean(ArrayList<String> timeList) {
        double totalTime = 0.0;
        double averageTime;
        for (String t0 : timeList) {
            totalTime += extractTime(t0);
        }
        // convert millisecond to second and average it
        averageTime = ((totalTime / 1000) / timeList.size());
        return String.format("%.2f" , averageTime) + " s";
    }

    private String getMedian(ArrayList<String> timeList) {
        double t = 0.0;
        ArrayList<Double> list = new ArrayList<>();
        for (String time : timeList) {
            t = extractTime(time);
            list.add(t);
        }
        Double[] timeArray = list.toArray(new Double[3]);
        Arrays.sort(timeArray);
        double median;
        if (timeArray.length % 2 == 0) {
            median = (timeArray[timeArray.length/2] + timeArray[timeArray.length/2 - 1])/2;
        }
        else {
            median = timeArray[timeArray.length/2];
        }
        return String.format("%.2f" , median / 1000) + " s";
    }

    public void exec() throws IOException, InterruptedException {
        int count = 0;
        ArrayList<String> list = getHeadList();
        File file = new File(repoName + " runtime report.csv");
        FileWriter csvWriter = new FileWriter(file);
        csvWriter.append("PROJECT");
        csvWriter.append(",");
        csvWriter.append("HEAD");
        csvWriter.append(",");
        csvWriter.append("RUN 1");
        csvWriter.append(",");
        csvWriter.append("RUN 2");
        csvWriter.append(",");
        csvWriter.append("RUN 3");
        csvWriter.append(",");
        csvWriter.append("MEAN RUNTIME");
        csvWriter.append(",");
        csvWriter.append("MEDIAN RUNTIME");
        csvWriter.append("\n");
        for (String head : list) {
            count++;
            ArrayList<String> timeList = new ArrayList<>();
            System.out.println("HEAD: " + count + " RUNNING: " + head);
            System.out.println("RUN 1");
            String run1 = calcCompileTime(head);
            System.out.println("RUN 2");
            String run2 = calcCompileTime(head);
            System.out.println("RUN 3");
            String run3 = calcCompileTime(head);
            timeList.add(run1);
            timeList.add(run2);
            timeList.add(run3);
            String mean = getMean(timeList);
            String median = getMedian(timeList);
            csvWriter.append(repoName);
            csvWriter.append(",");
            csvWriter.append(head);
            csvWriter.append(",");
            csvWriter.append(String.format("%.2f" , extractTime(run1) / 1000) + " s");
            csvWriter.append(",");
            csvWriter.append(String.format("%.2f" , extractTime(run2) / 1000) + " s");
            csvWriter.append(",");
            csvWriter.append(String.format("%.2f" , extractTime(run3) / 1000) + " s");
            csvWriter.append(",");
            csvWriter.append(mean);
            csvWriter.append(",");
            csvWriter.append(median);
            csvWriter.append("\n");
            csvWriter.flush();
        }
        csvWriter.close();
    }
}
