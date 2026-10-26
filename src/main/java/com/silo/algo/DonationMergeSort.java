package com.silo.algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hand-written top-down Merge Sort (no Collections.sort / List.sort). O(n log n) time in best, average and
 * worst cases; O(n) extra space. Stable. Returns a new list and leaves the input untouched.
 */
public final class DonationMergeSort {
    private DonationMergeSort() { }

    public static <T> List<T> sort(List<T> input, Comparator<? super T> cmp) {
        List<T> work = new ArrayList<>(input);
        if (work.size() < 2) return work;
        @SuppressWarnings("unchecked")
        T[] a = (T[]) work.toArray();
        @SuppressWarnings("unchecked")
        T[] tmp = (T[]) new Object[a.length];
        mergeSort(a, tmp, 0, a.length - 1, cmp);
        List<T> out = new ArrayList<>(a.length);
        for (T t : a) out.add(t);
        return out;
    }

    private static <T> void mergeSort(T[] a, T[] tmp, int lo, int hi, Comparator<? super T> cmp) {
        if (lo >= hi) return;
        int mid = (lo + hi) >>> 1;
        mergeSort(a, tmp, lo, mid, cmp);
        mergeSort(a, tmp, mid + 1, hi, cmp);
        int i = lo, j = mid + 1, k = lo;
        while (i <= mid && j <= hi) tmp[k++] = cmp.compare(a[j], a[i]) < 0 ? a[j++] : a[i++]; // ties take left: stable
        while (i <= mid) tmp[k++] = a[i++];
        while (j <= hi) tmp[k++] = a[j++];
        for (k = lo; k <= hi; k++) a[k] = tmp[k];
    }
}
