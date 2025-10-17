package org.boulderse.ijserver.testdata;
import java.io.*;
import java.util.*;

public class DataProcessor {

        public static void main(String[] args) {
            String inputFile = "bigdata.txt";
            String outputFile = "output.txt";
            List<String> data = new ArrayList<>();

            try {
                BufferedReader br = new BufferedReader(new FileReader(inputFile));
                String line;
                while ((line = br.readLine()) != null) {
                    data.add(line);
                }
            } catch (Exception e) {
                System.out.println("Error reading file");
            }

            for (int i = 0; i < data.size(); i++) {
                for (int j = i + 1; j < data.size(); j++) {
                    data.set(i, data.get(i) + data.get(j));
                }
            }

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
                for (String s : data) {
                    bw.write(s);
                    bw.newLine();
                }
                bw.close();
            } catch (Exception e) {
                System.out.println("Error writing file");
            }

            System.out.println("Processing done!");
        }

}
