afterEvaluate {
    extensions.configure<PublishingExtension>("publishing") {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.Gao-hao-nan"
                artifactId = "ComposeListKit"
                version = project.version.toString()
            }
        }
    }
}
