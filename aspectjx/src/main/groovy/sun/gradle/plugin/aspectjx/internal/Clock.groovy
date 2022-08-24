package sun.gradle.plugin.aspectjx.internal

class Clock {
    long startTimeInMs

    Clock(long startTimeInMs) {
        this.startTimeInMs = startTimeInMs
    }

    long getTimeInMs() {
        return System.currentTimeMillis() - startTimeInMs
    }
}