package edu.olivet.harvester.utils.common;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/17/17 10:58 AM
 */
public class ThreadHelper {
    /**
     * 将待处理的集合按照线程总数分解
     * @param list			待处理集合
     * @param threadCount	线程总数
     */
    public static <T> List<List<T>> assign(List<T> list, int threadCount) {
        if (list == null || threadCount <= 0) {
            throw new IllegalArgumentException("非法参数，待处理集合不能为空，线程总数必须大于0");
        }

        int size = list.size();
        int step = size / threadCount;
        step = step > 0 ? step : 1;

        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            int from = i * step;
            if (from >= size) {
                result.add(new ArrayList<T>(0));
                continue;
            }

            int to = (i + 1) * step;
            if (i == threadCount - 1 && to < size) {
                to = size;
            }
            result.add(list.subList(from, to));
        }

        return result;
    }
}
