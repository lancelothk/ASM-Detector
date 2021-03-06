package edu.cwru.cbc.ASM.tools.conversion;

import org.apache.commons.cli.*;

import java.io.IOException;

/**
 * Created by kehu on 5/4/15.
 * FormatConversion program entrance.
 */
public class FormatConversionPgm {
	public static void main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		options.addOption("m", true, "conversion mode. 1:MappedRead->MR;2.MR->MappedRead;3. split file into chrs");
		options.addOption("i", true, "input file/path");
		options.addOption("o", true, "output file/path");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		String mode = cmd.getOptionValue("m");
		String inputFileName = cmd.getOptionValue("i");
		String outputFileName = cmd.getOptionValue("o");

		switch (mode) {
			case "1":
				MappedReadToMR.conversion(inputFileName, outputFileName);
				break;
			case "2":
				MRToMappedRead.conversion(inputFileName, outputFileName);
				break;
			case "3":
				SplitFileByChr.split(inputFileName, outputFileName);
				break;
			default:
				System.err.println("Unknown mode!");
		}

	}
}
