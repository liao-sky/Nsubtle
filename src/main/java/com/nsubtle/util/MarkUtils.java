package com.nsubtle.util;

public class MarkUtils {

    public static class GrowthContext {
        private static final ThreadLocal<Boolean> IS_BONEMEAL_GROWTH = new ThreadLocal<>();

        public static void markAsBonemeal() {
            IS_BONEMEAL_GROWTH.set(true);
        }

        public static boolean isBonemealGrowth() {
            return IS_BONEMEAL_GROWTH.get() == Boolean.TRUE;
        }

        public static void clear() {
            IS_BONEMEAL_GROWTH.remove();
        }
    }
}
