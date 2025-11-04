import com.palantir.gradle.gitversion.VersionDetails
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

plugins {
    kotlin("jvm") version "2.1.10"

    // 便利系プラグイン群
    id("com.palantir.git-version") version "0.12.3" // Gitバージョン管理
    id("com.github.johnrengelman.shadow") version "7.1.2" // fat-jar 生成
    id("com.diffplug.spotless") version "6.25.0"
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("1.2.1") // Kotlin 2.1 に対応
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}

group = "com.github.ringoame196"

// Git バージョン情報を取得
val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val details = versionDetails()
version = details.lastTag ?: "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")
}

kotlin {
    jvmToolchain(22)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named("build") {
    dependsOn("spotlessCheck")
    dependsOn("shadowJar")
}

tasks.named<Jar>("jar") {
    enabled = false // 通常のjarを無効化
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("KotlinProjectTemplate")
    archiveClassifier.set("") // "-all" を外す場合
    manifest {
        attributes["Main-Class"] = "com.github.ringoame196.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.register("setup") {
    group = "project setup"
    description = "初期化: developer ブランチと v1.0.0 タグを作成・切り替えます。"

    doLast {
        println("🧰 Kotlin Project Setup を開始します…")

        val projectDir = project.projectDir
        val repository = try {
            FileRepositoryBuilder()
                .findGitDir(projectDir)
                .build()
        } catch (ex: Exception) {
            error("❌ Git リポジトリが見つかりません: ${ex.message}")
        }

        val git = Git(repository)

        try {
            // developerブランチ作成
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

            // タグ作成（存在しなければ）
            val tags = git.tagList().call().map { it.name }
            val tagName = "refs/tags/v1.0.0"

            if (tagName !in tags) {
                println("🏷️ タグ 'v1.0.0' を新規作成します...")
                git.tag().setName("v1.0.0").setMessage("Initial version tag").call()
                println("✅ タグ 'v1.0.0' を作成しました。")
            } else {
                println("🔁 タグ 'v1.0.0' は既に存在します。スキップします。")
            }

        } catch (e: Exception) {
            println("⚠️ Git 操作中にエラーが発生しました: ${e.message}")
        } finally {
            git.close()
            repository.close()
        }

        println("🎉 setup タスク完了！ developerブランチとタグが準備されました。")
    }
}