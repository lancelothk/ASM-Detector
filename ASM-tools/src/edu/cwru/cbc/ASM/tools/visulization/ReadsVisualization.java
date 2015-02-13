package edu.cwru.cbc.ASM.tools.visulization;

import edu.cwru.cbc.ASM.commons.DataType.MappedRead;
import edu.cwru.cbc.ASM.commons.DataType.ReadComparator;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Align reads to readable format with padding.
 */

public class ReadsVisualization {
    private static String ref;

    public static void main(String[] args) throws IOException {
        String targetFileName;
        if (args.length == 0) {
            targetFileName = "/home/kehu/experiments/ASM/simulation/i90_r1_chr20.first_100.sim";
        } else {
            targetFileName = args[0];
        }
        ReadsVisualization.align(targetFileName);
    }

    public static void align(String fileName) throws IOException {
        File targetFile = new File(fileName);
        if (targetFile.isDirectory()) {
            File[] files = targetFile.listFiles();
            for (File file : files) {
                if (file.isFile() && !file.isHidden() && !file.getName().endsWith(".aligned")) {
                    String outputFileName = file.getAbsolutePath() + ".aligned";
                    List<MappedRead> readsList = readMappedReads(file);
                    alignReads(readsList, ref, outputFileName);
                }
            }
        } else {
            String outputFileName = fileName + ".aligned";
            List<MappedRead> readsList = readMappedReads(targetFile);
            alignReads(readsList, ref, outputFileName);
        }
    }

    public static ArrayList<MappedRead> readMappedReads(File inputFile) throws IOException {
        ArrayList<MappedRead> readsList = new ArrayList<>();
        BufferedReader bufferedReader;
        bufferedReader = new BufferedReader(new FileReader(inputFile));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("ref")) {
                ref = line.split("\t")[1];
                continue;
            }
            String[] items = line.split("\t");
            if (!items[1].equals("+") && !items[1].equals("-")) {
                System.err.println(line);
                System.err.println("invalid strand symbol!");
            }
            readsList.add(
                    new MappedRead(items[0], items[1].charAt(0), Integer.parseInt(items[2]), Integer.parseInt(items[3]),
                                   items[4], items[5]));
        }
        bufferedReader.close();
        return readsList;
    }

    public static void alignReads(List<MappedRead> readsList, String ref, String outputFileName) throws IOException {
        // sort reads first
        readsList.sort(new ReadComparator());
        // set initial position
        int initialPos = readsList.get(0).getStart();
        BufferedWriter bufferedWriter;
        bufferedWriter = new BufferedWriter(new FileWriter(outputFileName));
        if (ref != null) {
            bufferedWriter.write(String.format("ref:\t%s\n", ref));
        }
        for (MappedRead read : readsList) {
            bufferedWriter.write(read.toString(initialPos) + "\n");
        }
        bufferedWriter.close();
    }
}