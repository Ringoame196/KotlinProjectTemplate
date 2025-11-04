import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.RefAlreadyExistsException
import org.eclipse.jgit.api.errors.RefNotFoundException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

plugins {
    kotlin("jvm") version "2.1.10"

    // 便利系プラグイン群
    id("com.github.ben-manes.versions") version "0.41.0" // 依存バージョンチェック
    id("com.palantir.git-version") version "0.12.3" // Gitバージョン管理
    id("com.github.johnrengelman.shadow") version "7.1.2" // fat-jar 生成
}

group = "com.github.ringoame196"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
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
    dependsOn("shadowJar")
}

tasks.register("setup") {
    group = "project setup"
    description = "初期化: developer ブランチを作成し、切り替えます。"

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
            repository.close()
        }

        println("🎉 setup タスク完了！ 開発ブランチが準備されました。")
    }
}
