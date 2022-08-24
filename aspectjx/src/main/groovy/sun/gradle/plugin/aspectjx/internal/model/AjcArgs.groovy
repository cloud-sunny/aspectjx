package sun.gradle.plugin.aspectjx.internal.model

import sun.gradle.plugin.aspectjx.internal.AJXTask


class AjcArgs implements Serializable {
    List<AJXTask> tasks = new ArrayList<>()
    List<String> common
}