package org.boulderse.ijserver.testdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class ExceptionHandlingTest {

    public int getPlayerScore(String playerFile) {
        try {
            Scanner contents = new Scanner(new File(playerFile));
            return Integer.parseInt(contents.nextLine());
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("File not found");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("File not found");
        }
    }
}
