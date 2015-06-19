package edu.cwru.cbc.ASM.detect;

import java.util.List;


/**
 * Created by kehu on 12/9/14.
 * Common utils for Detection
 */
public class DetectionUtils {

	/**
	 * Given fdr and p value list, return FDR cutoff
	 * Using Benjamini–Hochberg–Yekutieli procedure
	 *
	 * @param pvalueList p value list
	 * @param fdr        FDR alpha
	 * @return p value cutoff.
	 */
	public static double getBHYFDRCutoff(List<Double> pvalueList, double fdr) {
		pvalueList.sort(Double::compare);
		double cm = 0;
		for (double i = 1; i < pvalueList.size(); i++) {
			cm += 1 / i;
		}
		if (pvalueList.get(0) > fdr * (1) / (double) pvalueList.size() / cm) {
			System.out.printf("first P values(%d) > a*k/(m*cm). reject all p values.\n", pvalueList.size());
			return -1;
		}
		for (int i = 1; i < pvalueList.size(); i++) {
			if (pvalueList.get(i) > fdr * (i + 1) / (double) pvalueList.size() / cm) {
				System.out.println("k is " + i);
				System.out.println("n is " + pvalueList.size());
				System.out.println("cm is " + cm);
				return pvalueList.get(i - 1);
			}
		}
		System.out.printf("all P values(%d) <= a*k/(m*cm). Use the last p as cutoff.\n", pvalueList.size());
		return pvalueList.get(pvalueList.size() - 1);
	}

	/**
	 * Given fdr and p value list, return FDR cutoff
	 * Using Benjamini–Hochberg procedure
	 *
	 * @param pvalueList p value list
	 * @param fdr        FDR alpha
	 * @return p value cutoff.
	 */
	public static double getBHFDRCutoff(List<Double> pvalueList, double fdr) {
		pvalueList.sort(Double::compare);
		if (pvalueList.get(0) > fdr * (1) / (double) pvalueList.size()) {
			System.out.printf("first P values(%d) > a*k/m. reject all p values.\n", pvalueList.size());
			return -1;
		}
		for (int i = 1; i < pvalueList.size(); i++) {
			if (pvalueList.get(i) > fdr * (i + 1) / (double) pvalueList.size()) {
				System.out.println("k is " + i);
				System.out.println("n is " + pvalueList.size());
				return pvalueList.get(i - 1);
			}
		}
		System.out.printf("all P values(%d) <= a*k/m. Use the last p as cutoff.\n", pvalueList.size());
		return pvalueList.get(pvalueList.size() - 1);
	}
}
