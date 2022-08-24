package sun.gradle.plugin.aspectjx
/**
 * class description here
 * @author simon* @version 1.0.0* @since 2016-05-05
 */
class AJXExtension {
    private static final List<String> DEFAULT_FORCE_EXCLUDE_FILES = Arrays.asList(
            '*.R.class',
            '*.R$*.class',
            '*.BR.class',
            '*.DataBinderMapperImpl.class',
            '*\\R.jar'
    )
    List<String> includes = new ArrayList<>()
    List<String> excludes = new ArrayList<>()
    List<String> forceExcludeFiles = DEFAULT_FORCE_EXCLUDE_FILES


    List<String> ajcArgs = new ArrayList<>()

    boolean enabled = true

    boolean logEnabled = false
    AJXExtension include(String... filters) {
        if (filters != null) {
            this.includes.addAll(filters)
        }
        return this
    }

    AJXExtension setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled
        AJXLog.setEnabled(logEnabled)
        return this
    }

    AJXExtension exclude(String... filters) {
        if (filters != null) {
            this.excludes.addAll(filters)
        }
        return this
    }

    AJXExtension froceExcludeFile(String... filters) {
        if (filters != null) {
            if (forceExcludeFiles.is(DEFAULT_FORCE_EXCLUDE_FILES)) {
                forceExcludeFiles = new ArrayList<>()
            }
            forceExcludeFiles.addAll(filters)
        }
        return this
    }

    AJXExtension ajcArgs(String... ajcArgs) {
        if (ajcArgs != null) {
            this.ajcArgs.addAll(ajcArgs)
        }

        return this
    }


    @Override
    public String toString() {
        return "AJXExtension{" +
                "includes=" + includes +
                ", excludes=" + excludes +
                ", forceExcludeFiles=" + forceExcludeFiles +
                ", ajcArgs=" + ajcArgs +
                ", enabled=" + enabled +
                ", logEnabled=" + logEnabled +
                '}'
    }
}
