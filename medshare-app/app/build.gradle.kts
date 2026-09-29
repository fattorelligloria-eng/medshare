import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Os dados de assinatura ficam fora do controle de versão. Sem o arquivo, o
// build de release ainda roda — sai apenas sem assinatura, o que é o correto
// para quem clonou o projeto e não tem a chave.
val assinatura = Properties().apply {
    val arquivo = rootProject.file("keystore.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}
val temAssinatura = assinatura.getProperty("arquivo") != null

android {
    namespace = "br.com.medshare.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.medshare.app"
        minSdk = 26          // Android 8; cobre praticamente todo aparelho em uso
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Endereco padrao da API. Quem instala o APK pode trocar dentro do
        // proprio app, na tela de login, sem precisar recompilar nada.
        buildConfigField("String", "API_PADRAO",
            "\"${project.findProperty("medshareApi") ?: "https://medshare-api.exemplo.br"}\"")
    }

    signingConfigs {
        if (temAssinatura) {
            create("release") {
                storeFile = rootProject.file(assinatura.getProperty("arquivo"))
                storePassword = assinatura.getProperty("senhaDoArquivo")
                keyAlias = assinatura.getProperty("alias")
                keyPassword = assinatura.getProperty("senhaDaChave")
            }
        }
    }

    buildTypes {
        release {
            // Minificação desligada de propósito.
            //
            // O R8 renomeia classes, e a kotlinx.serialization descobre os
            // serializadores pelo nome — quando uma regra de ProGuard falta, o
            // app compila, instala e só quebra na primeira chamada de rede, no
            // celular. As regras em proguard-rules.pro cobrem esse caso, mas
            // isso só se comprova rodando num aparelho de verdade.
            //
            // Depois de confirmar que o app funciona instalado, ligue as duas
            // linhas abaixo: o APK cai de ~18 MB para ~2 MB, o que faz
            // diferença para quem baixa usando dados móveis.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (temAssinatura) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // O lint não bloqueia a geração do APK — numa máquina modesta ele vira
        // o gargalo do build. Continua disponível como tarefa separada:
        //     ./gradlew lint
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    // Teste de contrato: roda na JVM, sem emulador, usando a mesma interface
    // Retrofit e os mesmos modelos que o app usa em produção.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // O teste só roda quando há uma API de verdade no endereço informado.
    systemProperty("medshare.api", System.getProperty("medshare.api") ?: "")
}
