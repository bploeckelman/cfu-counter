import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CFUCounter {

    public static void main(String... args) {
        System.out.println("CFU Counter");
        System.out.println("===========");

        Scanner scanner = new Scanner(System.in);
        String userInput = "";

        CFUCounter cfuCounter = new CFUCounter();
        do {
            cfuCounter.getAndCalculateOneRow();

            System.out.print("Enter more data (y/n)?  ");
            userInput = scanner.nextLine();
            System.out.println();
        } while (userInput.equals("y"));
        cfuCounter.outputCalculatedValues();

        System.out.println("\nAll done, thanks for playing!");
    }

    // ------------------------------------------------------------------------

    static class CFUAverageAndStdDev {
        double averageColoniesPerMill;
        double stdDevColoniesPerMill;
    }

    Scanner scanner;
    List<CFUAverageAndStdDev> cfuAverageAndStdDevs;

    // ------------------------------------------------------------------------

    CFUCounter() {
        this.scanner = new Scanner(System.in);
        this.cfuAverageAndStdDevs = new ArrayList<>();
    }

    void getAndCalculateOneRow() {
        // 0. ask for dilution factor
        int dilutionFactor = -1;
        while (dilutionFactor < 0) {
            System.out.print("What is the dilution factor?  ");
            dilutionFactor = Integer.parseInt(scanner.nextLine(), 10);
            if (dilutionFactor < 0) {
                System.out.println("Dilution factor must be < 0");
            }
        }
        System.out.println(); //just a nice formatting thing. isn't that nice?

        // 1. ask how many replicates
        int numReplicates = 0;
        while (numReplicates < 2) {
            System.out.print("How many replicates?  ");
            numReplicates = Integer.parseInt(scanner.nextLine(), 10);
            if (numReplicates < 2) {
                System.out.println("Number of replicates must be > 1");
            }
        }

        // 2. user inputs that many integers
        int[] coloniesPerReplicate = new int[numReplicates];
        for (int i = 0; i < numReplicates; ++i) {
            int numColonies = 0;
            while (numColonies < 1) {
                System.out.print("Please enter colonies for replicate " + (i+1) + ": ");
                numColonies = Integer.parseInt(scanner.nextLine(), 10);
                if (numColonies < 1) {
                    System.out.println("Colonies per replicate must be > 0");
                }
            }
            coloniesPerReplicate[i] = numColonies;
        }

        // 4. calculate average and std deviation of those numbers
        int sumColonies = 0;
        for (int i = 0; i < coloniesPerReplicate.length; ++i) {
            sumColonies += coloniesPerReplicate[i];
        }
        double averageColonies = (double) sumColonies / numReplicates;

        double[] colonySquares = new double[numReplicates];
        for (int i = 0; i < coloniesPerReplicate.length; ++i) {
            colonySquares[i] = Math.pow(coloniesPerReplicate[i] - averageColonies, 2.0);
        }
        double stdDevNumerator = 0;
        for (Double square : colonySquares) {
            stdDevNumerator += square;
        }
        double coloniesSampleStdDev = Math.sqrt(stdDevNumerator / (numReplicates - 1));
        System.out.println();

        // 4. calculate CFU/mL and std dev of CFU/mL
        final int microliterSpots = 5; // NOTE: assumes 5uL spots

        CFUAverageAndStdDev cfuAverageAndStdDev = new CFUAverageAndStdDev();
        cfuAverageAndStdDev.averageColoniesPerMill = (averageColonies      / microliterSpots) * 1000.0 * Math.pow(10., dilutionFactor);
        cfuAverageAndStdDev.stdDevColoniesPerMill  = (coloniesSampleStdDev / microliterSpots) * 1000.0 * Math.pow(10., dilutionFactor);
        cfuAverageAndStdDevs.add(cfuAverageAndStdDev);
    }

    void outputCalculatedValues() {
        // print out to csv file for import into excel or whatever
        System.out.print("Enter a filename to output the results to: ");
        String fileName = scanner.nextLine();
        fileName = fileName.trim() + ".csv";
        System.out.println();

        Path filePath = Paths.get("./", fileName);

        // if this is the first time we're writing to this file, we need to output a row with column headers
        String headers = "Sample,CFU/mL,STDDEV\n";
        StringBuilder stringBuilder = new StringBuilder();
        if (!Files.exists(filePath)) {
            stringBuilder.append(headers);
        }

        // print out values for this row to both the console and a file
        for (int i = 0; i < cfuAverageAndStdDevs.size(); ++i) {
            int row = (i+1);
            CFUAverageAndStdDev cfuAverageAndStdDev = cfuAverageAndStdDevs.get(i);
            stringBuilder.append(row).append(",")
                         .append(String.format("%.4e", cfuAverageAndStdDev.averageColoniesPerMill)).append(",")
                         .append(String.format("%.4e", cfuAverageAndStdDev.stdDevColoniesPerMill)) .append("\n");
        }

        // print the results to the console
        System.out.println("Results:");
        System.out.println("========");
        System.out.println(stringBuilder.toString());

        // open a file for writing, creating the file if it doesn't already exist, appending to the file if it does exist
        try {
            Files.write(filePath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.printf("Failed to write cfu count csv '%s'\n%s", filePath.toString(), e);
        }
    }

}
