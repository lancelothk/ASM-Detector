package edu.cwru.cbc.ASM.detect.dataType;

import edu.cwru.cbc.ASM.commons.methylation.RefCpG;
import edu.cwru.cbc.ASM.commons.sequence.MappedRead;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by kehu on 11/17/14.
 * Container of group result
 */
public class GroupResult implements Comparable<GroupResult> {
	private List<RefCpG> refCpGList;
	private List<MappedRead> mappedReadList;
	private double avgMethylLevel;
	private double mec;

	public GroupResult(List<RefCpG> refCpGList, List<MappedRead> mappedReadList, double mec) {
		this.refCpGList = refCpGList;
		this.refCpGList.sort(RefCpG::compareTo);
		this.mappedReadList = mappedReadList;
		this.mappedReadList.sort((MappedRead read1, MappedRead read2) -> read1.getStart() - read2.getStart());
		this.mec = mec;
	}

	@Override
	public int compareTo(@Nonnull GroupResult o) {
		// sort by min position of refCpGList. Require refCpGList to be sorted.
		return this.refCpGList.get(0).getPos() - o.refCpGList.get(0).getPos();
	}

	public int getFirstPos() {
		return refCpGList.get(0).getPos();
	}

	public List<RefCpG> getRefCpGList() {
		return refCpGList;
	}

	public List<MappedRead> getMappedReadList() {
		return mappedReadList;
	}

	public double getMec() {
		return mec;
	}

	public double getAvgMethylLevel() {
		return avgMethylLevel;
	}

	public void setAvgMethylLevel(double avgMethylLevel) {
		this.avgMethylLevel = avgMethylLevel;
	}
}
