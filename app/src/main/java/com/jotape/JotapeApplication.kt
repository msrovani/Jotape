package com.jotape

import android.app.Application
// import com.jotape.domain.repository.InteractionRepository // Não parece ser usado diretamente aqui
import dagger.hilt.android.HiltAndroidApp
// import kotlinx.coroutines.CoroutineScope // Não parece ser usado diretamente aqui
// import kotlinx.coroutines.Dispatchers // Não parece ser usado diretamente aqui
// import kotlinx.coroutines.SupervisorJob // Não parece ser usado diretamente aqui
// import kotlinx.coroutines.launch // Não parece ser usado diretamente aqui
// import javax.inject.Inject // Não é mais necessário aqui
import android.util.Log
// import androidx.hilt.work.HiltWorkerFactory // Removido - Hilt configura automaticamente
// import androidx.work.Configuration // Removido - Hilt configura automaticamente

@HiltAndroidApp
// Removido: , Configuration.Provider
class JotapeApplication : Application() {

    // Removido: Injeção de InteractionRepository - Se não for usado, pode remover
    // @Inject
    // lateinit var interactionRepository: InteractionRepository

    // Removido: Injeção da workerFactory
    // @Inject
    // lateinit var workerFactory: HiltWorkerFactory

    // Removido: applicationScope se não for usado
    // private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Removido: Sobrescrita da configuração do WorkManager
    // override val workManagerConfiguration: Configuration
    //     get() = Configuration.Builder()
    //         .setWorkerFactory(workerFactory)
    //         .build()

    override fun onCreate() {
        super.onCreate()
        Log.i("JotapeApplication", "Application Created. Hilt will configure WorkManager.")
        // triggerInitialSync() // Mantenha comentado ou remova se não for mais necessário
    }

    // Removido: triggerInitialSync se não for mais usado
    // private fun triggerInitialSync() {
    //     applicationScope.launch {
    //         try {
    //             // Comentar a chamada ao método removido
    //             // interactionRepository.syncUnsyncedInteractions()
    //         } catch (e: Exception) {
    //             Log.e("JotapeApplication", "Initial sync failed", e)
    //         }
    //     }
    // }
}