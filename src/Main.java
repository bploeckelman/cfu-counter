import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void main(String... args) {
        System.out.println("CFU Counter");
        System.out.println("===========");

        var scanner = new Scanner(System.in);

        // 0. ask for dilution factor
        int dilutionFactor = -1;
        while (dilutionFactor < 0) {
            System.out.print("What is the dilution factor?  ");
            dilutionFactor = scanner.nextInt(10);
            if (dilutionFactor < 0) {
                System.out.println("Dilution factor must be < 0");
            }
        }
        System.out.println(); //just a nice formatting thing. isn't that nice?

        // 1. ask how many replicates
        int numReplicates = 0;
        while (numReplicates < 1) {
            System.out.print("How many replicates?  ");
            numReplicates = scanner.nextInt(10);
            if (numReplicates < 1) {
                System.out.println("Number of replicates must be > 0");
            }
        }
        System.out.println(); //just a nice formatting thing. isn't that nice?

        // 2. user inputs that many integers
        var coloniesPerReplicate = new int[numReplicates];
        for (int i = 0; i < numReplicates; ++i) {
            int numColonies = 0;
            while (numColonies < 1) {
                System.out.print("Please enter colonies for replicate " + (i+1) + ": ");
                numColonies = scanner.nextInt(10);
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

        var colonySquares = new double[numReplicates];
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
        double cfuPerMill = (averageColonies / microliterSpots) * 1000.0 * Math.pow(10., dilutionFactor);
        double cfuPerMillSampleStdDev = (coloniesSampleStdDev / microliterSpots) * 1000.0 * Math.pow(10., dilutionFactor);

        // 5. print the result out to csv so it can be imported into excel
        System.out.println("Results:");
        System.out.println("========");

        System.out.print("Colonies per replicate: [");
        for (int i = 0; i < coloniesPerReplicate.length; ++i) {
            System.out.print(coloniesPerReplicate[i]);
            if (i != coloniesPerReplicate.length - 1) {
                System.out.print(", ");
            }
        }
        System.out.print("]\n");
        System.out.printf(Locale.US, "CFU/mL: %.4e\n", cfuPerMill);
        System.out.printf(Locale.US, "CFU/mL Standard Deviation: %.4e\n", cfuPerMillSampleStdDev);

        // print out to csv file for import into excel or whatever
        var data = new StringBuilder();
        var filePath = Paths.get("./cfu-counts.csv");

        // if this is the first time we're writing to this file, we need to output a row with column headers
        if (!Files.exists(filePath)) {
            data.append("CFU/mL,STDDEV\n");
        }

        // construct a row containing data for this run
        data.append(String.format("%.4e", cfuPerMill))
            .append(",")
            .append(String.format("%.4e", cfuPerMillSampleStdDev))
            .append("\n");

        // open a file for writing, creating the file if it doesn't already exist, appending to the file if it does exist
        try {
            Files.writeString(filePath, data.toString(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.printf("Failed to write cfu count csv '%s'\n%s", filePath.toString(), e);
        }

        System.out.println("\nAll done, thanks for playing!");
    }

}
