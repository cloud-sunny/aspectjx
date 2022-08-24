package sun.gradle.plugin.aspectjx.internal


import sun.gradle.plugin.aspectjx.AJXLog

/**
 * class description here
 * @author simon* @version 1.0.0* @since 2018-03-14
 */
class AJXTask {

    String inPath
    String outputJar
    String outputDir

    AJXTask() {
    }

    List<String> getIoArgs() {
        def args = []
        args << '-inpath'
        args << inPath
        AJXLog.d("inPath: ${inPath}")
        if (outputDir != null) {
            args << '-d'
            args << outputDir
            AJXLog.d("outputDir: ${outputDir}")
        } else if (outputJar != null) {
            args << '-outjar'
            args << outputJar
            AJXLog.d("outputJar: ${outputJar}")
        }
        return args
    }
}
