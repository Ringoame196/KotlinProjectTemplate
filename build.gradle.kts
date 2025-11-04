import org.gradle.internal.impldep.org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.RefNotFoundException
import org.eclipse.jgit.lib.Repository

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.ben-manes.versions") version "0.41.0"
    id("com.palantir.git-version") version "0.12.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.ringoame196"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.register("setup") {
    doLast {
        val projectDir = project.projectDir
        val repository = try {
            FileRepositoryBuilder()
                .setGitDir(File(projectDir, ".git"))
                .readEnvironment()
                .findGitDir()
                .build()
        } catch (ex: Exception) {
            error("❌ Git リポジトリが見つかりません: ${ex.message}")
        }
        val git = Git(repository as Repository?)

        try {
            val branches = git.branchList().call().map { it.name }
            val targetBranch = "refs/heads/developer"

            if (targetBranch !in branches) {
                println("🌱 'developer' ブランチを新規作成します...")
                git.branchCreate().setName("developer").call()
            } else {
                println("🔁 'developer' ブランチは既に存在します。")
            }

            println("🔀 'developer' ブランチに切り替え中...")
            git.checkout().setName("developer").call()
            println("✅ 'developer' ブランチに切り替え完了！")

        } catch (e: RefAlreadyExistsException) {
            println("⚠️ 'developer' ブランチは既に存在しています。スキップします。")
        } catch (e: RefNotFoundException) {
            println("❌ 'developer' ブランチの作成または切り替えに失敗しました。")
        } catch (e: Exception) {
            println("⚠️ Git 操作中にエラーが発生しました: ${e.message}")
        } finally {
            git.close()
        }

    }
}