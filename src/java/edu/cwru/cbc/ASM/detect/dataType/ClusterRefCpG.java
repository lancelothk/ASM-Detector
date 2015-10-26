package edu.cwru.cbc.ASM.detect.dataType;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Created by kehu on 12/8/14.
 * Used for record clusters in each RefCpG position.
 */
public class ClusterRefCpG implements Comparable<ClusterRefCpG> {
	private int pos;
	private Set<Vertex> clusterSet;


	public ClusterRefCpG(int pos, Vertex vertex) {
		this.pos = pos;
		this.clusterSet = new HashSet<>();
		this.clusterSet.add(vertex);
	}

	@Override
	public int compareTo(@Nonnull ClusterRefCpG o) {
		return this.pos - o.pos;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ClusterRefCpG)) return false;
		ClusterRefCpG that = (ClusterRefCpG) o;
		return Objects.equals(pos, that.pos);
	}

	@Override
	public int hashCode() {
		return Objects.hash(pos);
	}

	public int getClusterCount() {
		return clusterSet.size();
	}

	public Set<Vertex> getClusterSet() {
		return clusterSet;
	}

	public void addVertex(Vertex vertex) {
		clusterSet.add(vertex);
	}
}
