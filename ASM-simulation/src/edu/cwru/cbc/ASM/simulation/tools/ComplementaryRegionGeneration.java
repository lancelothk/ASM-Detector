package edu.cwru.cbc.ASM.simulation.tools;

import edu.cwru.cbc.ASM.commons.CommonsUtils;
import edu.cwru.cbc.ASM.commons.CpG.RefChr;
import edu.cwru.cbc.ASM.commons.GenomicInterval;
import edu.cwru.cbc.ASM.commons.bed.BedUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kehu on 4/27/15.
 * Generate complementary regions. E.g. generate nonCGI from CGI and ref genome.
 */
public class ComplementaryRegionGeneration {

	public static void main(String[] args) throws IOException {
		String referenceGenomeFileName = "/home/kehu/experiments/ASM/data/hg18_chr20.fa";
		String targetRegionFileName =
				"/home/kehu/experiments/ASM/simulation/CpGIslandsRegions" + "/cpgIslandExt_hg18_UCSCGB_chr20.bed";

		// read reference and refCpGs
		RefChr refChr = CommonsUtils.readReferenceGenome(referenceGenomeFileName);

		// read target regions
		List<GenomicInterval> targetRegionsMap = BedUtils.readSingleChromBedRegions(targetRegionFileName);
		Collections.sort(targetRegionsMap);

		// generate non-ASM regions
		List<GenomicInterval> nonASMRegions = generateNonASMRegions(refChr, targetRegionsMap);
		BufferedWriter writer = new BufferedWriter(new FileWriter(targetRegionFileName.replace(".bed", "_nonCGI.bed")));
		for (GenomicInterval genomicInterval : nonASMRegions) {
			writer.write(genomicInterval.toBedString() + "\n");
		}
		writer.close();
	}


	/**
	 * generate Non ASM regions which covers the region not covered by ASM regions.
	 *
	 * @return list of non ASM regions. In position increasing order.
	 */
	private static List<GenomicInterval> generateNonASMRegions(RefChr refChr, List<GenomicInterval> targetRegionList) {
		List<GenomicInterval> nonASMRegions = new ArrayList<>();
		int lastEnd = -1;
		for (int i = 0; i <= targetRegionList.size(); i++) {
			if (i == 0) {
				nonASMRegions.add(
						new GenomicInterval(refChr.getChr(), refChr.getStart(), targetRegionList.get(i).getStart() - 1,
								String.valueOf(nonASMRegions.size() + 1)));
				lastEnd = targetRegionList.get(i).getEnd();
			} else if (i == targetRegionList.size()) {
				nonASMRegions.add(new GenomicInterval(refChr.getChr(), lastEnd + 1, refChr.getEnd(),
						String.valueOf(nonASMRegions.size() + 1)));
			} else {
				nonASMRegions.add(
						new GenomicInterval(refChr.getChr(), lastEnd + 1, targetRegionList.get(i).getStart() - 1,
								String.valueOf(nonASMRegions.size() + 1)));
				lastEnd = targetRegionList.get(i).getEnd();
			}
		}
		return nonASMRegions;
	}
}