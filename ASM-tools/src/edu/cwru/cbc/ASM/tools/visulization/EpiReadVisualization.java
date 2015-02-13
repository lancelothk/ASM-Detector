package edu.cwru.cbc.ASM.tools.visulization;

import edu.cwru.cbc.ASM.commons.DataType.EpiRead;
import edu.cwru.cbc.ASM.commons.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Created by kehu on 11/13/14.
 * Align EpiRead to better visualization.
 */
public class EpiReadVisualization {
    public static void main(String[] args) throws IOException {
		String homeDirectory = System.getProperty("user.home");
		File inputFile = new File(
				homeDirectory + "/experiments/ASM/result_i90_r1/intervals_epiread_large/chr22-15240531-15242472");
		alignEpiRead(inputFile, EpiRead.EpiReadFormat.extEpiread);
	}

	private static void alignEpiRead(File inputFile, EpiRead.EpiReadFormat format) throws IOException {
		List<EpiRead> epiReadList = Utils.readEpiReadFile(inputFile, format);
		epiReadList.sort(EpiRead::compareTo);
		int initialPos = epiReadList.get(0).getCpgOrder();

		BufferedWriter alignedEpiReadWriter = new BufferedWriter(
				new FileWriter(inputFile.getAbsolutePath() + ".alignedEpiRead"));
		for (EpiRead epiRead : epiReadList) {
			alignedEpiReadWriter.write(epiRead.getId() + "\t" + epiRead.toDisplay(initialPos) + "\n");
		}
		alignedEpiReadWriter.close();
	}

}