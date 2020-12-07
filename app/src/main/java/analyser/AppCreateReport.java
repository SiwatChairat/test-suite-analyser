package analyser;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AppCreateReport {
    String fileName;
    String testPath;
    String repoName;
    int intervalNum;

    public AppCreateReport(String file, String path , String repo,int num) {
        fileName = file;
        testPath = path;
        repoName = repo;
        intervalNum = num;
    }

    /*
    Convert string to arraylist using "\n" as delimiter and get the specified index
     */
    private String getIndex(String info, int index) {
        ArrayList<String> list = stringToArrayList(info, "\n");
        return list.get(index);
    }

    /*
    Return index of the arraylist if that index of the arraylist contain a specified string
     */
    private int getIndexIfContain(ArrayList<String> list, String string) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).contains(string)) {
                return i;
            }
        }
        return 0;
    }

    /*
    Convert LocalDate datatype to Date datatype
     */
    public Date localDateToDate(LocalDate localDate) {
        return java.sql.Date.valueOf(localDate);
    }

    /*
    Convert String datatype to Date datatype with specified pattern of string
     */
    private Date stringToDate(String date, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
        LocalDate d = LocalDate.parse(date, formatter);
        return localDateToDate(d);
    }

    /*
    Check whether the input date is in the specified interval or not
     */
    boolean isInRange(Date testDate, Date startDate, Date endDate) {
        return testDate.getTime() >= startDate.getTime() &&
                testDate.getTime() <= endDate.getTime();
    }

    /*
    Convert string to arraylist, splitting by the input delimiter
     */
    private ArrayList<String> stringToArrayList(String a, String delimiter) {
        String[] elements = a.split(delimiter);
        List<String> tempList = Arrays.asList(elements);
        return new ArrayList<>(tempList);
    }

    /*
    Return an arraylist of commit head within date interval specified
     */
    public ArrayList<String> headInInterval(Date startDate, Date endDate, ArrayList<String> data) {
        ArrayList<String> headList = new ArrayList<>();
        for (int i = 0; i < data.size() - 1; i++) {
            ArrayList<String> info = stringToArrayList(data.get(i), "\n");
            Date date = stringToDate(info.get(1), "MMM d yyyy");
            if (isInRange(date, startDate, endDate)) {
                headList.add(info.get(0));
            }
        }
        return headList;
    }

    /*
    Create and write to .csv file with 7 columns, NO. INTERVAL, INTERVAL RANGE, DATE, HEAD, COMMENT, TEST DATA,
    CHANGES LINK
     */
    public void writeToCsv() throws IOException, InterruptedException {
        AppCommit a1 = new AppCommit(repoName, 1, "", "");
        String testInterval = a1.intervalBetweenCommits(testPath, intervalNum, 0);
        String log = a1.computeLog("");
        ArrayList<String> list1 = stringToArrayList(testInterval, "\n\n");
        ArrayList<String> list2 = stringToArrayList(log, "\n\n");
        FileWriter csvWriter = new FileWriter(fileName + ".csv");
        csvWriter.append("NO. INTERVAL");
        csvWriter.append(",");
        csvWriter.append("INTERVAL RANGE");
        csvWriter.append(",");
        csvWriter.append("DATE");
        csvWriter.append(",");
        csvWriter.append("HEAD");
        csvWriter.append(",");
        csvWriter.append("COMMENT");
        csvWriter.append(",");
        csvWriter.append("TEST DATA");
        csvWriter.append(",");
        csvWriter.append("CHANGES LINK");
        csvWriter.append("\n");
        for (int i = 0; i < list1.size() - 1; i++) {
            String intervalNo = getIndex(list1.get(i), 0);
            String from = getIndex(list1.get(i), 1);
            String to = getIndex(list1.get(i), 2);
            String previousHead = "";
            String previousResult = "";
            Date test1 = stringToDate(from, "d/MM/yyyy");
            Date test2 = stringToDate(to, "d/MM/yyyy");
            ArrayList<String> head = headInInterval(test1, test2, list2);
            System.out.println("Number of file in interval " + (i + 1) + ": " + head.size());
            for (int j = 0; j < head.size() - 1; j++) {
                String head1 = head.get(j);
                String head2 = head.get(j + 1);
                String result1;
                String result2;
                AppCompile appCompile1 = new AppCompile(repoName, head1);
                AppCompile appCompile2 = new AppCompile(repoName, head2);
                if (head1.compareTo(previousHead) == 0) {
                    result1 = previousResult;
                    result2 = appCompile2.buildProject();
                } else {
                    result1 = appCompile1.buildProject();
                    result2 = appCompile2.buildProject();
                    previousHead = head2;
                    previousResult = result2;
                }
                String changeLink = appCompile1.getChangeLink();
                // Print to terminal for checking
                System.out.println(result1);
                System.out.println(result2);
                // If there is test results as well as the results of head1 and head2 are the same.
                if (result1.compareTo(result2) == 0 && result1.compareTo("No test results found") != 0) {
                    ArrayList<String> list3 = stringToArrayList(list2.get(getIndexIfContain(list2, head1)), "\n");
                    String date = list3.get(1);
                    String comment = list3.get(2);
                    csvWriter.append(intervalNo);
                    csvWriter.append(",");
                    csvWriter.append(from);
                    csvWriter.append("-");
                    csvWriter.append(to);
                    csvWriter.append(",");
                    csvWriter.append(date);
                    csvWriter.append(",");
                    csvWriter.append(head1);
                    csvWriter.append(",");
                    csvWriter.append(comment);
                    csvWriter.append(",");
                    csvWriter.append(result1);
                    csvWriter.append(",");
                    csvWriter.append(changeLink);
                    csvWriter.append("\n");
                    csvWriter.flush();
                    System.out.println(j);
                }
            }
        }
        csvWriter.close();
    }
}
