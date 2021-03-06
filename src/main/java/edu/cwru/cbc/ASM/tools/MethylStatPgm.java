package edu.cwru.cbc.ASM.tools;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import edu.cwru.cbc.ASM.commons.io.IOUtils;
import edu.cwru.cbc.ASM.commons.methylation.CpG;
import edu.cwru.cbc.ASM.commons.methylation.MethylationUtils;
import edu.cwru.cbc.ASM.commons.methylation.RefChr;
import edu.cwru.cbc.ASM.commons.methylation.RefCpG;
import edu.cwru.cbc.ASM.commons.sequence.IUPACCode;
import edu.cwru.cbc.ASM.commons.sequence.MappedRead;
import net.openhft.koloboke.collect.map.hash.HashIntObjMap;
import net.openhft.koloboke.collect.map.hash.HashIntObjMaps;
import org.apache.commons.cli.*;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static edu.cwru.cbc.ASM.commons.methylation.MethylationUtils.extractCpGSite;

/**
 * Created by kehu on 9/16/15.
 * Generate coverage and methyl count of mapped BS-seq data.
 */
public class MethylStatPgm {
	private static final Splitter tabSplitter = Splitter.on("\t");

	public static void main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		options.addOption(Option.builder("c").hasArg().desc("chromosome of input").required().build());
		options.addOption(Option.builder("r").hasArg().desc("Reference File").required().build());
		options.addOption(Option.builder("m").hasArg().desc("MappedRead File").required().build());
		options.addOption(Option.builder("o").hasArg().desc("Output File").required().build());
		options.addOption(Option.builder("p").desc("pair end mode").build());
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String chr = cmd.getOptionValue("c");
		String referenceChromosomeFileName = cmd.getOptionValue("r");
		String mappedReadFileName = cmd.getOptionValue("m");
		String outputFileName = cmd.getOptionValue("o");
		boolean pairEnd = cmd.hasOption("p");

		// load reference
		long start = System.currentTimeMillis();
		RefChr refChr = IOUtils.readReferenceChromosome(referenceChromosomeFileName);
		List<RefCpG> refCpGList = extractCpGSite(refChr.getRefString(), MethylationUtils.REFERENCE_INIT_POS);
		System.out.println("load refMap complete\t" + (System.currentTimeMillis() - start) / 1000.0 + "s");

		HashIntObjMap<RefCpG> refMap = HashIntObjMaps.newMutableMap();
		for (RefCpG refCpG : refCpGList) {
			refMap.put(refCpG.getPos(), refCpG);
		}

		Files.readLines(new File(mappedReadFileName), Charsets.UTF_8, new LineProcessor() {
			@Override
			public boolean processLine(@Nonnull String line) throws IOException {
				if (line.startsWith("chr") || line.startsWith("ref") || line.startsWith("assembly")) {
					return true;
				}
				List<String> itemList = tabSplitter.splitToList(line);
				if (pairEnd) {
					if (!itemList.get(1).equals("+") && !itemList.get(1).equals("-")) {
						throw new RuntimeException("invalid strand! in line:\t" + line);
					}
					if (!IUPACCode.validateNucleotideCode(itemList.get(4)) || !IUPACCode.validateNucleotideCode(
							itemList.get(5))) {
						throw new RuntimeException("invalid character in sequence!\t" + line);
					}
					MappedRead.CpGInRead(itemList.get(1).charAt(0), itemList.get(4), Integer.parseInt(itemList.get(2)),
							refMap, (refCpG, methylStatus) -> refCpG.addCpG(new CpG(null, refCpG, methylStatus)));
					MappedRead.CpGInRead(itemList.get(1).charAt(0), itemList.get(5),
							Integer.parseInt(itemList.get(3)) - itemList.get(5).length(),
							refMap, (refCpG, methylStatus) -> refCpG.addCpG(new CpG(null, refCpG, methylStatus)));
				} else {
					if (!itemList.get(1).equals("+") && !itemList.get(1).equals("-")) {
						throw new RuntimeException("invalid strand! in line:\t" + line);
					}
					if (!IUPACCode.validateNucleotideCode(itemList.get(4))) {
						throw new RuntimeException("invalid character in sequence!\t" + line);
					}
					MappedRead.CpGInRead(itemList.get(1).charAt(0), itemList.get(4), Integer.parseInt(itemList.get(2)),
							refMap, (refCpG, methylStatus) -> refCpG.addCpG(new CpG(null, refCpG, methylStatus)));
				}
				return true;
			}

			@Override
			public Object getResult() {
				return null;
			}
		});

		List<String> resultList = refCpGList.stream()
				.sorted(RefCpG::compareTo)
				.map(r -> String.format("%s\t%d\t%d\t%d\t%f", chr, r.getPos(), r.getCpGCoverage(), r.getMethylCount(),
						r.getMethylLevel()))
				.collect(Collectors.toList());

		Files.asCharSink(new File(outputFileName), Charsets.UTF_8).writeLines(resultList);
	}


}
