package edu.cwru.cbc.ASM.tools;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import edu.cwru.cbc.ASM.commons.CMDHelper;
import edu.cwru.cbc.ASM.commons.io.GroupedReadsLineProcessor;
import edu.cwru.cbc.ASM.commons.methylation.CpG;
import edu.cwru.cbc.ASM.commons.methylation.MethylStatus;
import edu.cwru.cbc.ASM.commons.methylation.MethylationUtils;
import edu.cwru.cbc.ASM.commons.methylation.RefCpG;
import edu.cwru.cbc.ASM.commons.sequence.MappedRead;
import net.openhft.koloboke.collect.map.hash.HashIntObjMap;
import net.openhft.koloboke.collect.map.hash.HashIntObjMaps;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lancelothk on 12/23/15.
 * Visualize methylation pattern and SNP
 */
public class MethylFigurePgm {
	private static final int CG_RADIUS = 20;
	private static final int HEIGHT_INTERVAL = 24;
	private static final int BPWIDTH = 10;
	private static int commonFontSize = 16;

	public static void main(String[] args) throws ParseException, IOException {
		Options options = new Options();
		options.addOption(Option.builder("i").hasArg().required().desc("input grouped read file").build());
		options.addOption(Option.builder("p").hasArg().desc("SNP position").build());
		options.addOption(Option.builder("a").hasArg().desc("allele pair. E.g. A-G").build());
		options.addOption(Option.builder("s").hasArg().desc("font size").build());

		CommandLine cmd = new CMDHelper(args, "mfig [options]", options).build();

		String groupedReadFile = cmd.getOptionValue("i");
		int snpPosition = -1;
		if (cmd.hasOption("p")) {
			snpPosition = Integer.parseInt(cmd.getOptionValue("p"));
		}
		String alleles = "";
		if (cmd.hasOption("a")) {
			String input = cmd.getOptionValue("a");
			if (input.length() != 3 || input.charAt(1) != '-') {
				System.err.println("incorrect format of allele pair!");
			} else {
				alleles = input;
			}
		}
		if (cmd.hasOption("s")) {
			commonFontSize = Integer.parseInt(cmd.getOptionValue("s"));
		}

		Pair<String, List<List<MappedRead>>> result = Files.readLines(new File(groupedReadFile),
				Charsets.UTF_8, new GroupedReadsLineProcessor());
		String ref = result.getLeft();
		if (result.getRight().size() != 2) {
			throw new RuntimeException("more than 2 groups of reads!");
		}
		// require reads contains only uppercase.
		draw(groupedReadFile, snpPosition, ref, result.getRight().get(0), result.getRight().get(1), alleles);
	}

	private static void draw(String groupedReadFile, int snpPosition, String ref, List<MappedRead> group1,
	                         List<MappedRead> group2, String alleles) {
		if (group2.size() == 0) {
			int minStart = group1.stream().mapToInt(MappedRead::getStart).min().getAsInt();
			List<RefCpG> refCpGList = MethylationUtils.extractCpGSite(ref, minStart);
			HashIntObjMap<RefCpG> refMap = HashIntObjMaps.newMutableMap();
			for (RefCpG refCpG : refCpGList) {
				refMap.put(refCpG.getPos(), refCpG);
			}
			for (MappedRead mappedRead : group1) {
				mappedRead.generateCpGsInRead(refMap);
			}
			drawCompactFigure(group1, snpPosition, refCpGList, groupedReadFile + ".compact", alleles);
		} else {
			int minStart = Math.min(group1.stream().mapToInt(MappedRead::getStart).min().getAsInt(),
					group2.stream().mapToInt(MappedRead::getStart).min().getAsInt());
			int maxEnd = Math.max(group1.stream().mapToInt(MappedRead::getEnd).max().getAsInt(),
					group2.stream().mapToInt(MappedRead::getEnd).max().getAsInt());
			List<RefCpG> refCpGList = MethylationUtils.extractCpGSite(ref, minStart);
			HashIntObjMap<RefCpG> refMap = HashIntObjMaps.newMutableMap();
			for (RefCpG refCpG : refCpGList) {
				refMap.put(refCpG.getPos(), refCpG);
			}
			for (MappedRead mappedRead : group1) {
				mappedRead.generateCpGsInRead(refMap);
			}
			for (MappedRead mappedRead : group2) {
				mappedRead.generateCpGsInRead(refMap);
			}
			drawCompactFigure(group1, group2, snpPosition, groupedReadFile + ".compact", alleles);
//			drawFigure(group1, group2, minStart, maxEnd, snpPosition, groupedReadFile + ".png");
		}
	}

	private static void drawCompactFigure(List<MappedRead> group1, int snpPosition, List<RefCpG> refCpGList,
	                                      String outputFileName, String alleles) {
		int imageHeight = (group1.size() + 1) * HEIGHT_INTERVAL, imageWidth = (refCpGList.size() + 3) * CG_RADIUS;

		try {
			EPSWriter epsWriter = new EPSWriter(outputFileName, imageWidth, imageHeight);
			Graphics2D graphWriter = epsWriter.getGraphWriter();
			graphWriter.setBackground(Color.WHITE);
			graphWriter.clearRect(0, 0, imageWidth, imageHeight);
			graphWriter.setPaint(Color.BLACK);
			graphWriter.setFont(new Font("Helvetica", Font.PLAIN, commonFontSize));

			int height = 0;
			drawCompactGroup(graphWriter, refCpGList, height, snpPosition, group1, alleles);

			epsWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void drawCompactFigure(List<MappedRead> group1, List<MappedRead> group2, int snpPosition,
	                                      String outputFileName, String alleles) {
		// always put methyl group first
		long methylCountGroup1 = group1.stream()
				.flatMap(r -> r.getCpgList().stream())
				.filter(cpg -> cpg.getMethylStatus() == MethylStatus.C)
				.count();
		long methylCountGroup2 = group2.stream()
				.flatMap(r -> r.getCpgList().stream())
				.filter(cpg -> cpg.getMethylStatus() == MethylStatus.C)
				.count();
		if (methylCountGroup1 < methylCountGroup2) {
			List<MappedRead> tmp = group1;
			group1 = group2;
			group2 = tmp;
		}
		if (snpPosition != -1) {
			// only reads cover given snp position are shown.
			group1 = selectValidReads(group1, snpPosition);
			group2 = selectValidReads(group2, snpPosition);
		} else {
			// only reads cover more than 2 refCpGs are shown.
			group1 = group1.stream().filter(r -> r.getCpgList().size() >= 2).collect(Collectors.toList());
			group2 = group2.stream().filter(r -> r.getCpgList().size() >= 2).collect(Collectors.toList());
		}
		List<RefCpG> refCpGList = selectValidSortedRefCpG(group1, group2);
		int imageHeight = (group1.size() + group2.size() + 1) * HEIGHT_INTERVAL, imageWidth = (refCpGList.size() + 3) * CG_RADIUS;
		try {
			EPSWriter epsWriter = new EPSWriter(outputFileName, imageWidth, imageHeight);
			Graphics2D graphWriter = epsWriter.getGraphWriter();
			graphWriter.setBackground(Color.WHITE);
			graphWriter.clearRect(0, 0, imageWidth, imageHeight);
			graphWriter.setPaint(Color.BLACK);
			graphWriter.setFont(new Font("Helvetica", Font.PLAIN, commonFontSize));

			int height = 0;
			if (snpPosition == -1) {
				height = drawCompactGroup(graphWriter, refCpGList, height, group1);
			} else {
				height = drawCompactGroup(graphWriter, refCpGList, height, snpPosition, group1, alleles);
			}

			// add line
			graphWriter.setStroke(new BasicStroke(4.0f));
			graphWriter.setPaint(Color.BLUE);
			graphWriter.drawLine(0, height + HEIGHT_INTERVAL / 2, refCpGList.size() * CG_RADIUS,
					height + HEIGHT_INTERVAL / 2);
			graphWriter.setPaint(Color.orange);
			graphWriter.drawLine(refCpGList.size() * CG_RADIUS + CG_RADIUS / 2, 0,
					refCpGList.size() * CG_RADIUS + CG_RADIUS / 2, imageHeight);
			graphWriter.setPaint(Color.BLACK);
			height += HEIGHT_INTERVAL;
			graphWriter.setStroke(new BasicStroke());

			if (snpPosition == -1) {
				height = drawCompactGroup(graphWriter, refCpGList, height, group2);
			} else {
				height = drawCompactGroup(graphWriter, refCpGList, height, snpPosition, group2, alleles);
			}

			epsWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static List<RefCpG> selectValidSortedRefCpG(List<MappedRead> group1, List<MappedRead> group2) {
		Set<RefCpG> validRefCpGSet = new HashSet<>();
		group1.forEach(mappedRead -> mappedRead.getCpgList().forEach(cpg -> validRefCpGSet.add(cpg.getRefCpG())));
		group2.forEach(mappedRead -> mappedRead.getCpgList().forEach(cpg -> validRefCpGSet.add(cpg.getRefCpG())));
		return validRefCpGSet.stream().sorted(RefCpG::compareTo).collect(Collectors.toList());
	}

	private static List<MappedRead> selectValidReads(List<MappedRead> group, int snpPosition) {
		List<MappedRead> validGroup = new ArrayList<>();
		for (MappedRead mappedRead : group) {
			if (snpPosition >= mappedRead.getStart() && snpPosition <= mappedRead.getEnd()) {
				String sequence = mappedRead.getStrand() == '+' ? mappedRead.getSequence() : mappedRead.getComplementarySequence();
				char snp = sequence.charAt(snpPosition - mappedRead.getStart());
				if (snp != '-') {
					validGroup.add(mappedRead);
				}
			}
		}
		return validGroup;
	}

	private static int drawCompactGroup(Graphics2D graphWriter, List<RefCpG> refCpGList, int height,
	                                    List<MappedRead> group) {
		for (MappedRead mappedRead : group) {
			// add cpg
			for (CpG cpG : mappedRead.getCpgList()) {
				if (cpG.getMethylStatus() == MethylStatus.C) {
					graphWriter.fill(
							new Ellipse2D.Double(refCpGList.indexOf(cpG.getRefCpG()) * CG_RADIUS, height,
									CG_RADIUS,
									CG_RADIUS));
				} else if (cpG.getMethylStatus() == MethylStatus.T) {
					graphWriter.draw(
							new Ellipse2D.Double(refCpGList.indexOf(cpG.getRefCpG()) * CG_RADIUS, height,
									CG_RADIUS,
									CG_RADIUS));
				}
			}
			height += HEIGHT_INTERVAL;
		}
		return height;
	}

	private static int drawCompactGroup(Graphics2D graphWriter, List<RefCpG> refCpGList, int height, int snpPosition,
	                                    List<MappedRead> group, String alleles) {
		Set<Integer> refCpGPositions = new HashSet<>();
		for (RefCpG refCpG : refCpGList) {
			refCpGPositions.add(refCpG.getPos());
		}
		for (MappedRead mappedRead : group) {
			if (snpPosition >= mappedRead.getStart() && snpPosition <= mappedRead.getEnd()) {
				String sequence = mappedRead.getStrand() == '+' ? mappedRead.getSequence() : mappedRead.getComplementarySequence();
				char snp = sequence.charAt(snpPosition - mappedRead.getStart());
				if (snp != '-') {
					// add cpg
					for (CpG cpG : mappedRead.getCpgList()) {
						if (cpG.getMethylStatus() == MethylStatus.C) {
							graphWriter.fill(
									new Ellipse2D.Double(refCpGList.indexOf(cpG.getRefCpG()) * CG_RADIUS, height,
											CG_RADIUS,
											CG_RADIUS));
						} else if (cpG.getMethylStatus() == MethylStatus.T) {
							graphWriter.draw(
									new Ellipse2D.Double(refCpGList.indexOf(cpG.getRefCpG()) * CG_RADIUS, height,
											CG_RADIUS,
											CG_RADIUS));
						}
					}

					FontMetrics fm = graphWriter.getFontMetrics();
					char snpToPrint = reverseBisulfite(snp, alleles, mappedRead.getStrand());
					graphWriter.drawString(String.valueOf(snpToPrint),
							(float) ((refCpGList.size() + 1) * CG_RADIUS - fm.charWidth(snpToPrint) / 2.0),
							(float) (height + HEIGHT_INTERVAL * 2 / 3.0));
					graphWriter.drawString(String.valueOf(mappedRead.getStrand()),
							(float) ((refCpGList.size() + 2) * CG_RADIUS - fm.charWidth(mappedRead.getStrand()) / 2.0),
							(float) (height + HEIGHT_INTERVAL * 2 / 3.0));
					height += HEIGHT_INTERVAL;
				}
			}
		}
		return height;
	}

	private static char reverseBisulfite(char snp, String alleles, char strand) {
		// AG: we cannot tell which allele -A come from
		// CT: we cannot tell which allele +T come from
		// AT: same to origin
		// AC: +T -> +C
		// CG: +T -> +C, -A -> -G
		// GT: -A -> -G
		if (alleles.indexOf('A') != -1 && alleles.indexOf('C') != -1 && snp == 'T' && strand == '+') {
			return 'C';
		} else if (alleles.indexOf('C') != -1 && alleles.indexOf('G') != -1 && snp == 'T' && strand == '+') {
			return 'C';
		} else if (alleles.indexOf('C') != -1 && alleles.indexOf('G') != -1 && snp == 'A' && strand == '-') {
			return 'G';
		} else if (alleles.indexOf('T') != -1 && alleles.indexOf('G') != -1 && snp == 'A' && strand == '-') {
			return 'G';
		}
		return snp;
	}

	/**
	 * Draw methyl figure with including distance between CpG sites
	 */
	private static void drawFigure(List<MappedRead> group1, List<MappedRead> group2, int minStart, int maxEnd,
	                               int snpPosition, String outputFileName) {
		int imageHeight = (group1.size() + group2.size() + 1) * HEIGHT_INTERVAL, imageWidth = (maxEnd - minStart + 1) * BPWIDTH;
		BufferedImage pngImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphWriter = pngImage.createGraphics();
		graphWriter.setBackground(Color.WHITE);
		graphWriter.clearRect(0, 0, imageWidth, imageHeight);
		graphWriter.setPaint(Color.BLACK);
		graphWriter.setFont(new Font("Arial", Font.PLAIN, commonFontSize));

		int height = 0;
		height = drawGroup(graphWriter, minStart, height, snpPosition, group1);

		// add line
		graphWriter.setStroke(new BasicStroke(4.0f));
		graphWriter.setPaint(Color.BLUE);
		graphWriter.drawLine(0, height + HEIGHT_INTERVAL / 2, imageWidth,
				height + HEIGHT_INTERVAL / 2);
		graphWriter.setPaint(Color.BLACK);
		height += HEIGHT_INTERVAL;
		graphWriter.setStroke(new BasicStroke());

		drawGroup(graphWriter, minStart, height, snpPosition, group2);

		File outputPNG = new File(outputFileName);
		try {
			ImageIO.write(pngImage, "png", outputPNG);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static int drawGroup(Graphics2D graphWriter, int minStart, int height, int snpPosition,
	                             List<MappedRead> group) {
		for (MappedRead mappedRead : group) {
			// add line
			graphWriter.drawLine((mappedRead.getStart() - minStart) * BPWIDTH, height + HEIGHT_INTERVAL / 2,
					(mappedRead.getEnd() - minStart + 1) * BPWIDTH, height + HEIGHT_INTERVAL / 2);

			// add cpg
			for (CpG cpG : mappedRead.getCpgList()) {
				if (cpG.getMethylStatus() == MethylStatus.C) {
					graphWriter.fill(
							new Ellipse2D.Double((cpG.getPos() - minStart) * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
				} else if (cpG.getMethylStatus() == MethylStatus.T) {
					graphWriter.draw(
							new Ellipse2D.Double((cpG.getPos() - minStart) * BPWIDTH, height, CG_RADIUS, CG_RADIUS));
				}
			}
			String sequence = mappedRead.getStrand() == '+' ? mappedRead.getSequence() : mappedRead.getComplementarySequence();
			if (snpPosition >= mappedRead.getStart() && snpPosition <= mappedRead.getEnd()) {
				char snp = sequence.charAt(snpPosition - mappedRead.getStart());
				if (snp != '-') {
					graphWriter.drawString(String.valueOf(snp), (snpPosition - minStart) * BPWIDTH,
							height + HEIGHT_INTERVAL * 2 / 3);
				}
			}
			height += HEIGHT_INTERVAL;
		}
		return height;
	}
}
