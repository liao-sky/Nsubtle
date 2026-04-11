package com.nsubtle.util;

import net.minecraft.core.BlockPos;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MarkUtils {

    public static class BoneMealMarkHelper {
        private static final Map<BlockPos, Boolean> MARK = new ConcurrentHashMap<>();

        public static void mark(BlockPos pos) {
            MARK.put(pos, true);
        }

        public static boolean isMarked(BlockPos pos) {
            return MARK.getOrDefault(pos, false);
        }

        public static void clear(BlockPos pos) {
            MARK.remove(pos);
        }
    }
}
